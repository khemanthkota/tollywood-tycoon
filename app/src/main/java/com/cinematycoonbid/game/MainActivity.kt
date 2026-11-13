package com.cinematycoonbid.game

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import org.json.JSONObject
import android.app.AlertDialog
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var foreground = false
    private inner class BatteryBridge {
        @JavascriptInterface fun refresh() { runOnUiThread {
            val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiver.onReceive(this@MainActivity, battery)
        } }
    }
    private var batteryReceiverRegistered = false
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level < 0 || scale <= 0 || !::webView.isInitialized) return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val percent = (100.0 * level / scale).toInt().coerceIn(0, 100)
            webView.evaluateJavascript("window.CinemaBattery && window.CinemaBattery.update($percent, $charging)", null)
        }
    }
    private var auctionScreenAwake = false
    private fun syncScreenAwake() {
        if (foreground && auctionScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private inner class ScreenBridge {
        @JavascriptInterface fun setAuctionActive(active: Boolean) {
            runOnUiThread { auctionScreenAwake = active; syncScreenAwake() }
        }
    }
    private var pendingMicRequest: PermissionRequest? = null
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        val request = pendingMicRequest
        pendingMicRequest = null
        if (allowed) request?.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else request?.deny()
    }
    private fun audioEvent(status: String) {
        runOnUiThread {
            if (!isDestroyed) webView.evaluateJavascript("window.CinemaAudio && window.CinemaAudio.status(" + JSONObject.quote(status) + ")", null)
        }
    }
    private inner class VoiceBridge {
        @JavascriptInterface fun stop() { runOnUiThread { speech?.stop(); audioEvent("idle") } }
        @JavascriptInterface fun speak(text: String, language: String, volume: Double, fallback: String) {
            runOnUiThread {
                if (!foreground) return@runOnUiThread
                val engine = speech
                if (!speechReady || engine == null) { audioEvent("Voice engine is not ready. Check Android text-to-speech settings."); return@runOnUiThread }
                var locale = Locale.forLanguageTag(language)
                var message = text.take(500)
                if (engine.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                    locale = Locale.US; message = fallback.take(500)
                    audioEvent("Selected voice unavailable; using English. Install its voice in Android text-to-speech settings.")
                }
                if (engine.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) { audioEvent("No supported voice installed. Check Android text-to-speech settings."); return@runOnUiThread }
                // Prefer an installed offline voice; otherwise use the engine's selected voice.
                engine.voices?.firstOrNull { it.locale.language == locale.language && !it.isNetworkConnectionRequired }?.let { engine.voice = it }
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.toFloat().coerceIn(0f, 1f)) }
                audioEvent("speaking")
                if (engine.speak(message, TextToSpeech.QUEUE_FLUSH, params, "auction-" + System.nanoTime()) == TextToSpeech.ERROR) audioEvent("Voice playback failed. Check installed voice data.")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        webView.addJavascriptInterface(VoiceBridge(), "CinemaVoice")
        webView.addJavascriptInterface(ScreenBridge(), "CinemaScreen")
        webView.addJavascriptInterface(BatteryBridge(), "CinemaBatteryNative")
        speech = TextToSpeech(this) { status -> speechReady = status == TextToSpeech.SUCCESS }
        speech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { audioEvent("speaking") }
            override fun onDone(id: String?) { audioEvent("idle") }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { audioEvent("Voice playback failed.") }
        })
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return true
                if (uri.scheme == "file" && uri.path?.startsWith("/android_asset/") == true) return false
                if (uri.scheme == "https" || uri.scheme == "http") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { }
                }
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    if (request.origin.scheme != "file" || !request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        request.deny()
                    } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    } else {
                        pendingMicRequest?.deny()
                        pendingMicRequest = request
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingMicRequest === request) pendingMicRequest = null
            }
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Cinema Tycoon Bid")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Cinema Tycoon Bid")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html")
        } else {
            webView.restoreState(savedInstanceState)
        }

        setContentView(webView)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume(); foreground = true; syncScreenAwake()
        if (!batteryReceiverRegistered) { registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); batteryReceiverRegistered = true }
        if (::webView.isInitialized) { webView.onResume(); webView.evaluateJavascript("window.CinemaAudio && window.CinemaAudio.resume()", null) }
    }
    override fun onPause() {
        foreground = false
        if (batteryReceiverRegistered) { unregisterReceiver(batteryReceiver); batteryReceiverRegistered = false }
        syncScreenAwake()
        speech?.stop()
        if (::webView.isInitialized) { webView.evaluateJavascript("window.CinemaAudio && window.CinemaAudio.pause()", null); webView.onPause() }
        super.onPause()
    }
    override fun onDestroy() {
        pendingMicRequest?.deny(); pendingMicRequest = null
        speech?.stop(); speech?.shutdown(); speech = null
        webView.destroy()
        super.onDestroy()
    }
}
