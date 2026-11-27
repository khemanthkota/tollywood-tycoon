package com.khemanthkota.tollywoodtycoon.empire

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

// Original procedural audio; no external recordings or network dependency.
class EmpireAudio {
    private var music: AudioTrack? = null
    private var effect: AudioTrack? = null
    private var ambience: AudioTrack? = null
    var ambienceVolume = .18f
        set(value) { field = value.coerceIn(0f,1f); ambience?.setVolume(field) }
    private var currentScene: String? = null
    var musicVolume = .25f
        set(value) { field = value.coerceIn(0f,1f); music?.setVolume(field) }
    var effectsVolume = .45f
        set(value) { field = value.coerceIn(0f,1f); effect?.setVolume(field) }
    private fun track(samples: ShortArray, volume: Float): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(22050)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size*2).build()
        try {
            check(t.write(samples,0,samples.size) == samples.size)
            t.setVolume(volume)
            return t
        } catch (e: Exception) { t.release(); throw e }
    }
    fun scene(name: String) {
        if (currentScene == name && music != null) return
        music?.release(); music = null; currentScene = name
        ambience?.release(); ambience = null
        if (name == "Produce" || name == "Studio") {
            val random = kotlin.random.Random(42)
            var filtered = 0.0
            val room = ShortArray(22050*4) { i ->
                filtered = filtered*.94 + (random.nextDouble()-.5)*.06
                val fade = minOf(1.0,i/1000.0,(88200-i)/1000.0)
                ((filtered*3500 + sin(2*PI*60*i/22050.0)*120)*fade).toInt().toShort()
            }
            runCatching { ambience = track(room,ambienceVolume).also { it.setLoopPoints(0,room.size,-1); it.play() } }
        }
        val roots = when(name) {
            "Produce" -> intArrayOf(48,53,55,50)
            "Premiere" -> intArrayOf(48,53,55,60)
            "Crisis" -> intArrayOf(45,46,42,43)
            "Awards" -> intArrayOf(48,52,55,60)
            "Market" -> intArrayOf(45,48,50,43)
            "Assets" -> intArrayOf(50,53,57,48)
            else -> intArrayOf(48,55,53,50)
        }
        val rate = 22050
        val bar = rate*2
        val samples = ShortArray(bar*roots.size) { i ->
            val local = i%bar
            val time = local.toDouble()/rate
            val root = roots[i/bar]
            val envelope = minOf(1.0,local/2200.0,(bar-local)/4400.0)
            val chord = intArrayOf(0,if(name == "Premiere" || name == "Awards") 4 else 3,7,12).sumOf { step ->
                val f = 440.0*Math.pow(2.0,(root+step-69)/12.0)
                sin(2*PI*f*time)*.08 + sin(4*PI*f*time)*.015
            }
            val beat = time % .5
            val bass = sin(2*PI*55*time)*kotlin.math.exp(-beat*18)*.07
            val melodyNote = root + intArrayOf(12,19,15,24)[(time*2).toInt().coerceIn(0,3)]
            val melodyHz = 440.0*Math.pow(2.0,(melodyNote-69)/12.0)
            val melody = sin(2*PI*melodyHz*time)*kotlin.math.exp(-beat*5)*.045
            ((chord + bass + melody)*envelope*22000).toInt().coerceIn(-32768,32767).toShort()
        }
        runCatching { music = track(samples,musicVolume).also { it.setLoopPoints(0,samples.size,-1); it.play() } }
    }
    fun cue(success: Boolean = false) {
        effect?.release(); effect = null
        val count = if(success) 11025 else 2205
        val samples = ShortArray(count) { i ->
            val time = i/22050.0
            val f = if(success) doubleArrayOf(392.0,523.25,659.25)[(i*3/count).coerceAtMost(2)] else 720.0
            val envelope = minOf(1.0,i/150.0)*(1.0-i.toDouble()/count)
            (sin(2*PI*f*time)*envelope*10000).toInt().toShort()
        }
        runCatching { effect = track(samples,effectsVolume).also { it.play() } }
    }
    fun event(name: String) {
        effect?.release(); effect = null
        val random = kotlin.random.Random(71)
        val duration = if (name == "applause") 33075 else 6615
        val samples = ShortArray(duration) { i ->
            val t = i / 22050.0
            val noise = random.nextDouble()*2-1
            val signal = when(name) {
                "clapper" -> (noise*.65 + sin(2*PI*180*t)*.35)*kotlin.math.exp(-t*35)
                "purchase" -> sin(2*PI*(if(t<.12) 660 else 880)*t)*kotlin.math.exp(-t*9)*.6
                "applause" -> noise*(.2+.15*sin(2*PI*17*t))*(1-i.toDouble()/duration)
                "crisis" -> sin(2*PI*220*t)*kotlin.math.exp(-t*7)*.5
                else -> sin(2*PI*720*t)*kotlin.math.exp(-t*25)*.4
            }
            (signal*14000).toInt().toShort()
        }
        runCatching { effect = track(samples,effectsVolume).also { it.play() } }
    }
    fun pause() { runCatching { music?.pause(); effect?.pause(); ambience?.pause() } }
    fun resume() { runCatching { music?.play(); ambience?.play() } }
    fun release() { ambience?.release(); ambience=null; music?.release(); effect?.release(); music=null; effect=null; currentScene=null }
}
