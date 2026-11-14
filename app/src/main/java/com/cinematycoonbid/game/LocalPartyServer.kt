package com.cinematycoonbid.game

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** In-memory LAN-only party server. Host device and all players must share a reachable LAN. */
class LocalPartyServer(private val context: Context) {
    private val random = SecureRandom()
    private val players = linkedMapOf<String, Player>()
    private val sessions = ConcurrentHashMap<WebSocket, String>()
    private var host: WebSocket? = null
    private var room = ""
    private var mode = "manual"
    private var seconds = 30
    private var phase = "auction"
    private var lot: JSONObject? = null
    private var highId: String? = null
    private var highAmount = 0
    private var deadline = 0L
    private val history = JSONArray()
    private var results: JSONArray? = null
    private var running = false

    private var codeHost:WebSocket?=null
    private var codeRoom=""
    private val codeGuests=linkedMapOf<WebSocket,String>()
    private var codeCards=JSONArray()
    private var codeTurn="red"
    private var codeWinner=""
    private var codeNote=""
    private fun codeReset(){codeHost=null;codeRoom="";codeGuests.clear();codeCards=JSONArray();codeTurn="red";codeWinner="";codeNote=""}
    private fun codeState(secret:Boolean):JSONObject {
        val cards=JSONArray()
        for(i in 0 until codeCards.length()){
            val c=codeCards.getJSONObject(i)
            cards.put(JSONObject().put("name",c.getString("name")).put("open",c.getBoolean("open"))
                .put("color",if(secret||c.getBoolean("open"))c.getString("color") else JSONObject.NULL))
        }
        return JSONObject().put("type","code_state").put("code",codeRoom).put("cards",cards)
            .put("turn",codeTurn).put("winner",codeWinner).put("note",codeNote)
    }
    private fun codeBroadcast(){
        send(codeHost,codeState(false))
        codeGuests.forEach { (c,role)->send(c,codeState(role=="redCaptain"||role=="blueCaptain")) }
    }
    private fun codeHandle(c:WebSocket,m:JSONObject):Boolean {
        val type=m.optString("type")
        if(!type.startsWith("code_"))return false
        when(type){
            "code_create"->{
                if(codeHost!=null)error("Codewords room already active")
                if(sessions.containsKey(c)||codeGuests.containsKey(c))error("Already in a room")
                val names=m.optJSONArray("names")?:error("Missing words")
                if(names.length()!=25)error("Exactly 25 words required")
                val clean=mutableSetOf<String>()
                for(i in 0 until 25){
                    val word=names.optString(i).trim()
                    if(word.isBlank()||word.length>60||!clean.add(word))error("Invalid or duplicate word")
                }
                codeReset();codeHost=c;codeRoom=(100000+random.nextInt(900000)).toString()
                val colors=(List(9){"red"}+List(8){"blue"}+List(7){"neutral"}+listOf("assassin")).shuffled(random)
                clean.forEachIndexed { i,name->codeCards.put(JSONObject().put("name",name).put("color",colors[i]).put("open",false)) }
                codeNote="Red starts. Captains give clues."
                send(c,JSONObject().put("type","code_created").put("code",codeRoom))
                codeBroadcast()
            }
            "code_join"->{
                if(codeHost==null||m.optString("code")!=codeRoom)error("Room not found")
                if(codeGuests.size>=24)error("Room full")
                if(sessions.containsKey(c)||codeGuests.containsKey(c)||c==codeHost)error("Already joined")
                val role=m.optString("role")
                if(role !in listOf("redCaptain","blueCaptain","player"))error("Invalid role")
                if(role!="player"&&codeGuests.values.contains(role))error("Captain already joined")
                codeGuests[c]=role
                send(c,JSONObject().put("type","code_joined").put("role",role))
                codeBroadcast()
            }
            "code_guess","code_end"->{
                if(codeHost==null||codeWinner.isNotEmpty())error("No active game")
                if(c!=codeHost&&codeGuests[c]!="player")error("Only players can guess")
                if(type=="code_end"){codeTurn=if(codeTurn=="red")"blue" else "red";codeNote="Turn ended"}
                else{
                    val index=m.optInt("index",-1)
                    if(index !in 0..24)error("Invalid card")
                    val card=codeCards.getJSONObject(index)
                    if(card.getBoolean("open"))error("Card already revealed")
                    card.put("open",true)
                    val color=card.getString("color")
                    if(color=="assassin"){codeWinner=if(codeTurn=="red")"blue" else "red";codeNote="Black card! "+codeWinner+" wins"}
                    else if((0 until 25).none { val x=codeCards.getJSONObject(it);x.getString("color")==codeTurn&&!x.getBoolean("open") }){
                        codeWinner=codeTurn;codeNote=codeWinner+" wins!"
                    }else if(color!=codeTurn){codeTurn=if(codeTurn=="red")"blue" else "red";codeNote="Wrong card. Turn switched"}
                    else codeNote="Correct! Guess again or end turn"
                }
                codeBroadcast()
            }
            else->error("Unknown Codewords action")
        }
        return true
    }

    private data class Player(val id:String,val name:String,var budget:Int=100,val contracts:JSONArray=JSONArray(),var movie:JSONObject?=null,var ws:WebSocket?=null)
    private val http = object : NanoHTTPD(8080) {
        override fun serve(session: IHTTPSession): Response {
            val path = session.uri.substringBefore('?')
            val filename = when(path) { "/", "/join", "/multiplayer.html" -> "multiplayer.html"; "/codewords", "/codewords-lan.html" -> "codewords-lan.html"; else -> "" }
            if(filename.isEmpty()) return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","Not found")
            return try {
                val body=context.assets.open(filename).use { it.readBytes() }
                newFixedLengthResponse(Response.Status.OK,"text/html; charset=utf-8",body.inputStream(),body.size.toLong()).apply {
                    addHeader("Cache-Control","no-store")
                    addHeader("X-Content-Type-Options","nosniff")
                }
            } catch (_:Exception) { newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/plain","Page unavailable") }
        }
    }
    private val socket = object: WebSocketServer(InetSocketAddress("0.0.0.0",3000)) {
        override fun onOpen(conn:WebSocket, handshake:ClientHandshake) {}
        override fun onClose(conn:WebSocket, code:Int, reason:String, remote:Boolean) = synchronized(this@LocalPartyServer) {
            if(conn==codeHost){codeReset();return@synchronized}
            if(codeGuests.remove(conn)!=null){codeBroadcast();return@synchronized}
            val id=sessions.remove(conn)
            if(id=="HOST") { reset(); broadcast() }
            else if(id!=null) { players[id]?.ws=null; broadcast() }
        }
        override fun onMessage(conn:WebSocket, message:String) = synchronized(this@LocalPartyServer) {
            try { if(message.length>8192) error("Message too large"); val payload=JSONObject(message);if(!codeHandle(conn,payload))handle(conn,payload) }
            catch(e:Exception) { send(conn,JSONObject().put("type","error").put("message",e.message?:"Invalid request")) }
        }
        override fun onError(conn:WebSocket?, ex:Exception) {}
        override fun onStart() { connectionLostTimeout=30 }
    }
    fun start():String = synchronized(this) {
        if(!running) { http.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); socket.start(); running=true }
        return address() ?: ""
    }
    fun stop() = synchronized(this) { if(running){socket.stop(1000);http.stop();running=false};reset();codeReset() }
    fun address():String? {
        val interfaces=NetworkInterface.getNetworkInterfaces() ?: return null
        for(n in interfaces) {
            if(!n.isUp || n.isLoopback)continue
            for(a in n.inetAddresses) if(a is java.net.Inet4Address && a.isSiteLocalAddress) return a.hostAddress
        }
        return null
    }
    private fun reset(){players.clear();sessions.clear();host=null;room="";lot=null;highId=null;highAmount=0;deadline=0;phase="auction";results=null;while(history.length()>0)history.remove(0)}
    private fun send(c:WebSocket?,o:JSONObject){try{if(c?.isOpen==true)c.send(o.toString())}catch(_:Exception){}}
    private fun state():JSONObject {
        val list=JSONArray()
        players.values.forEach { p->list.put(JSONObject().put("id",p.id).put("name",p.name).put("budget",p.budget).put("contracts",p.contracts).put("movie",if(p.movie!=null)JSONObject().put("submitted",true) else JSONObject.NULL)) }
        val high=highId?.let{players[it]}?.let{JSONObject().put("name",it.name).put("amount",highAmount)}
        return JSONObject().put("code",room).put("mode",mode).put("seconds",seconds).put("phase",phase).put("lot",lot?:JSONObject.NULL).put("high",high?:JSONObject.NULL).put("remaining",if(deadline>0) ((deadline-System.currentTimeMillis()+999)/1000).coerceAtLeast(0) else JSONObject.NULL).put("players",list).put("history",history).put("results",results?:JSONObject.NULL)
    }
    private fun broadcast(){val s=state();send(host,JSONObject().put("type","state").put("state",s));players.values.forEach{p->send(p.ws,JSONObject().put("type","state").put("state",s).put("you",p.id))}}
    private fun endLot(){
        if(lot==null)return
        var outcome="Unsold"
        val p=highId?.let{players[it]}
        if(p!=null && p.budget>=highAmount) {p.budget-=highAmount;p.contracts.put(JSONObject(lot.toString()).put("price",highAmount));outcome=p.name+" · ₹"+highAmount+" Cr"}
        history.put(JSONObject().put("lot",lot!!.getString("name")).put("outcome",outcome))
        lot=null;highId=null;highAmount=0;deadline=0;broadcast()
    }
    private fun handle(c:WebSocket,m:JSONObject) {
        val type=m.optString("type")
        val id=sessions[c]
        if(id==null) {
            if(type=="create"){
                if(host!=null)error("This device already hosts a room")
                reset();room=(100000+random.nextInt(900000)).toString();host=c;sessions[c]="HOST";mode=if(m.optString("mode")=="auto")"auto" else "manual";seconds=m.optInt("seconds",30).takeIf{it in listOf(15,30,45,60)}?:30
                send(c,JSONObject().put("type","created").put("code",room));broadcast();return
            }
            if(type=="join"){
                if(room.isEmpty()||m.optString("code")!=room)error("Room not found")
                if(players.size>=20)error("Room full")
                if(phase=="results")error("Game ended")
                val pid=UUID.randomUUID().toString();val name=m.optString("name").trim().take(40).ifBlank{"Player "+(players.size+1)}
                players[pid]=Player(pid,name,ws=c);sessions[c]=pid
                send(c,JSONObject().put("type","joined").put("code",room).put("id",pid));broadcast();return
            }
            error("Create or join first")
        }
        val isHost=id=="HOST";val p=players[id]
        when(type) {
            "lot" -> {if(!isHost||phase!="auction"||lot!=null)error("Host must finish current auction")
                val kind=m.optString("kind");val name=m.optString("name").trim().take(40)
                if(kind !in listOf("director","actor","actress")||name.isEmpty())error("Enter a valid celebrity")
                lot=JSONObject().put("kind",kind).put("name",name);highId=null;highAmount=0
                deadline=if(mode=="auto")System.currentTimeMillis()+seconds*1000L else 0;broadcast()
            }
            "bid" -> {if(isHost||p==null||phase!="auction"||lot==null)error("Bidding unavailable")
                if(deadline>0&&System.currentTimeMillis()>=deadline){endLot();error("Auction closed")}
                val amount=highAmount+1;if(p.budget<amount)error("Insufficient budget")
                highId=p.id;highAmount=amount
                if(mode=="auto"&&deadline-System.currentTimeMillis()<5000)deadline=System.currentTimeMillis()+5000
                broadcast()
            }
            "close" -> {if(!isHost)error("Host only");endLot()}
            "movies" -> {if(!isHost||lot!=null)error("Close the auction first");phase="movies";broadcast()}
            "mode" -> {if(!isHost||lot!=null)error("Host only, between lots");mode=if(m.optString("mode")=="auto")"auto" else "manual";seconds=m.optInt("seconds",30).takeIf{it in listOf(15,30,45,60)}?:30;broadcast()}
            "submit" -> {
                if(isHost||p==null||phase!="movies")error("Movie submissions unavailable")
                val ids=m.optJSONArray("ids")?:JSONArray();val selected=mutableListOf<JSONObject>()
                for(i in 0 until ids.length()) {val n=ids.optInt(i,-1);if(n<0||n>=p.contracts.length()||selected.any{it===p.contracts.optJSONObject(n)})error("Invalid cast");selected.add(p.contracts.getJSONObject(n))}
                if(!listOf("director","actor","actress").all{k->selected.any{it.optString("kind")==k}})error("Select director, actor and actress")
                val cast=selected.sumOf{it.optInt("price")};val production=m.optInt("production",-1)
                if(production<kotlin.math.ceil(cast*.3).toInt()||production>p.budget)error("Invalid production budget")
                p.movie=JSONObject().put("castCost",cast).put("production",production);broadcast()
            }
            "results" -> {if(!isHost||phase!="movies")error("Host only during movie making");if(players.values.any{it.movie==null})error("Wait for all players")
                phase="results";results=JSONArray();players.values.forEach{p->results!!.put(JSONObject().put("name",p.name).put("totalCost",p.movie!!.getInt("castCost")+p.movie!!.getInt("production")).put("profit",JSONObject.NULL))};broadcast()
            }
            else -> error("Unknown action")
        }
    }
    init { Thread {
        while(true) {Thread.sleep(250);synchronized(this@LocalPartyServer) {if(running&&deadline>0&&System.currentTimeMillis()>=deadline)endLot()} }
    }.apply {isDaemon=true;start()} }
}
