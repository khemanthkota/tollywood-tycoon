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

    private val codeScreens=linkedSetOf<WebSocket>()
    private val bidScreens=linkedSetOf<WebSocket>()
    private var codeHost:WebSocket?=null
    private var codeRoom=""
    private val codeGuests=linkedMapOf<WebSocket,String>()
    private var codeCards=JSONArray()
    private var codeTurn="red"
    private var codeWinner=""
    private var codeNote=""
    private fun codeReset(){codeHost=null;codeRoom="";codeGuests.clear();codeScreens.clear();codeCards=JSONArray();codeTurn="red";codeWinner="";codeNote=""}
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
        codeScreens.forEach { send(it,codeState(false)) }
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
                val colors=(List(9){"red"}+List(8){"blue"}+List(7){"neutral"}+listOf("assassin")).toMutableList()
                java.util.Collections.shuffle(colors,random)
                clean.forEachIndexed { i,name->codeCards.put(JSONObject().put("name",name).put("color",colors[i]).put("open",false)) }
                codeNote="Red starts. Captains give clues."
                send(c,JSONObject().put("type","code_created").put("code",codeRoom))
                codeBroadcast()
            }
            "code_watch"->{
                if(codeHost==null||m.optString("code")!=codeRoom)error("Room not found")
                if(codeScreens.size>=5)error("Too many TV displays")
                codeScreens.add(c);send(c,codeState(false))
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


    private data class TriviaPlayer(val name:String,val token:String=UUID.randomUUID().toString(),var score:Int=0,var answer:Int=-1)
    private val triviaPlayers=linkedMapOf<WebSocket,TriviaPlayer>()
    private val triviaScreens=linkedSetOf<WebSocket>()
    private var triviaHost:WebSocket?=null
    private var triviaRoom=""
    private var triviaIndex=-1
    private var triviaRevealed=false
    private var triviaFinished=false
    private var triviaDeadline=0L
    private var triviaAdvanceAt=0L
    private var triviaDuration=30
    private var triviaDeck=listOf<Int>()
    private var triviaQuestions=listOf<List<String>>()
    private val triviaFullBank:List<Pair<String,List<String>>> by lazy {
        val raw=context.assets.open("trivia-lan-bank.json").bufferedReader().use{it.readText()}
        val arr=JSONArray(raw)
        (0 until arr.length()).map { i->
            val item=arr.getJSONObject(i);val options=item.getJSONArray("options")
            item.getString("industry") to (listOf(item.getString("question"))+(0 until 4).map{options.getString(it)}+listOf(item.getInt("correct").toString()))
        }
    }
    // Question, four options, correct answer index. Curated starter set; expand using the existing trivia dataset later.
    private val triviaBank=listOf(
        listOf("Who directed RRR?","S. S. Rajamouli","Sukumar","Trivikram","Shankar","0"),
        listOf("Which movie features the character Pushpa Raj?","Salaar","Pushpa: The Rise","Jersey","Devara","1"),
        listOf("Which film features the song Naatu Naatu?","Baahubali","RRR","Pushpa","Magadheera","1"),
        listOf("Who directed Inception?","James Cameron","Christopher Nolan","Steven Spielberg","Ridley Scott","1"),
        listOf("Which movie is set aboard an ocean liner?","Avatar","Titanic","Interstellar","Gladiator","1"),
        listOf("Who played the lead role in Dangal?","Shah Rukh Khan","Ranveer Singh","Aamir Khan","Hrithik Roshan","2"),
        listOf("Which film features the character Rancho?","3 Idiots","PK","Lagaan","Dangal","0"),
        listOf("Which film features the fictional world Pandora?","Star Wars","Avatar","The Matrix","Jurassic Park","1"),
        listOf("Who directed Jurassic Park (1993)?","James Cameron","Christopher Nolan","Steven Spielberg","George Lucas","2"),
        listOf("Which movie features the character Jack Dawson?","Titanic","Avatar","The Matrix","Oppenheimer","0"),
        listOf("Who directed Baahubali: The Beginning?","S. S. Rajamouli","Shankar","Sukumar","Mani Ratnam","0"),
        listOf("Which superhero uses a shield made of vibranium?","Iron Man","Captain America","Thor","Batman","1"),
        listOf("Who played the title character in Barbie (2023)?","Emma Stone","Margot Robbie","Anne Hathaway","Gal Gadot","1"),
        listOf("Which film features the character Mahavir Singh Phogat?","Dangal","Lagaan","PK","Sultan","0"),
        listOf("Which film stars Keanu Reeves as a retired assassin?","The Matrix","John Wick","Speed","Constantine","1")
    )
    private fun triviaReset(){triviaHost=null;triviaRoom="";triviaPlayers.clear();triviaScreens.clear();triviaIndex=-1;triviaRevealed=false;triviaFinished=false;triviaDeadline=0L;triviaAdvanceAt=0L;triviaDeck=emptyList();triviaQuestions=emptyList()}
    private fun triviaState():JSONObject{
        val q=if(triviaIndex in triviaQuestions.indices)triviaQuestions[triviaIndex] else null
        val players=JSONArray()
        triviaPlayers.values.sortedWith(compareByDescending<TriviaPlayer>{it.score}.thenBy{it.name}).forEach{players.put(JSONObject().put("name",it.name).put("score",it.score).put("answered",it.answer>=0))}
        return JSONObject().put("type","trivia_state").put("code",triviaRoom).put("index",triviaIndex).put("total",triviaDeck.size)
            .put("finished",triviaFinished).put("revealed",triviaRevealed).put("deadline",if(triviaDeadline>0)triviaDeadline else JSONObject.NULL).put("question",q?.get(0)?:JSONObject.NULL)
            .put("options",if(q==null)JSONArray() else JSONArray(q.subList(1,5)))
            .put("correct",if(triviaRevealed&&q!=null)q[5].toInt() else JSONObject.NULL).put("players",players)
    }
    private fun triviaBroadcast(){
        val public=triviaState()
        send(triviaHost,public)
        triviaScreens.forEach{send(it,public)}
        triviaPlayers.forEach{(c,p)->send(c,JSONObject(public.toString()).put("yourAnswer",p.answer))}
    }
    private fun triviaHandle(c:WebSocket,m:JSONObject):Boolean{
        val type=m.optString("type")
        if(!type.startsWith("trivia_"))return false
        when(type){
            "trivia_create"->{
                if(triviaHost!=null)error("Trivia room already active")
                triviaReset();triviaHost=c;triviaRoom=(100000+random.nextInt(900000)).toString()
                val industry=m.optString("industry","all")
                val candidates=triviaFullBank.filter{industry=="all"||it.first==industry}.map{it.second}.toMutableList()
                if(candidates.size<10)error("Not enough questions for this industry")
                java.util.Collections.shuffle(candidates,random)
                triviaQuestions=candidates.take(10).map{q->
                    val answer=q[q[5].toInt()+1]
                    val options=q.subList(1,5).toMutableList();java.util.Collections.shuffle(options,random)
                    listOf(q[0])+options+listOf(options.indexOf(answer).toString())
                }
                triviaDeck=triviaQuestions.indices.toList()
                triviaDuration=m.optInt("seconds",30).takeIf{it in listOf(15,30,45,60)}?:30
                send(c,JSONObject().put("type","trivia_created").put("code",triviaRoom));triviaBroadcast()
            }
            "trivia_watch"->{
                if(triviaHost==null||m.optString("code")!=triviaRoom)error("Room not found")
                if(triviaScreens.size>=5)error("Too many TV displays")
                triviaScreens.add(c);send(c,triviaState())
            }
            "trivia_join"->{
                if(triviaHost==null||m.optString("code")!=triviaRoom||triviaFinished)error("Room not found or game finished")
                if(triviaPlayers.containsKey(c)||c==triviaHost)error("Already joined")
                val resume=m.optString("token")
                val old=triviaPlayers.entries.firstOrNull{it.value.token==resume&&resume.isNotBlank()}
                if(old!=null){val player=old.value;triviaPlayers.remove(old.key);triviaPlayers[c]=player;send(c,JSONObject().put("type","trivia_joined").put("token",player.token));triviaBroadcast();return}
                if(triviaPlayers.size>=20)error("Room full")
                val name=m.optString("name").trim().take(24)
                if(name.isBlank())error("Enter your name")
                if(triviaPlayers.values.any{it.name.equals(name,true)})error("Name already in use")
                val player=TriviaPlayer(name);triviaPlayers[c]=player
                send(c,JSONObject().put("type","trivia_joined").put("token",player.token));triviaBroadcast()
            }
            "trivia_next"->{
                if(c!=triviaHost)error("Host only")
                if(triviaFinished)error("Round finished")
                if(triviaIndex>=0&&!triviaRevealed)error("Reveal answers first")
                triviaAdvanceAt=0L
                if(triviaIndex+1>=triviaDeck.size){triviaFinished=true;triviaDeadline=0L}
                else{triviaIndex++;triviaRevealed=false;triviaDeadline=System.currentTimeMillis()+triviaDuration*1000L;triviaPlayers.values.forEach{it.answer=-1}}
                triviaBroadcast()
            }
            "trivia_answer"->{
                val player=triviaPlayers[c]?:error("Join as a player first")
                if(triviaIndex !in triviaDeck.indices||triviaRevealed||triviaFinished||player.answer>=0||System.currentTimeMillis()>=triviaDeadline)error("Answer unavailable")
                val answer=m.optInt("answer",-1)
                if(answer !in 0..3)error("Invalid answer")
                player.answer=answer;triviaBroadcast()
            }
            "trivia_reveal"->{
                if(c!=triviaHost)error("Host only")
                if(triviaIndex !in triviaDeck.indices||triviaRevealed)error("No active unanswered question")
                triviaRevealed=true;triviaDeadline=0L;triviaAdvanceAt=System.currentTimeMillis()+8000L
                val correct=triviaQuestions[triviaIndex][5].toInt()
                triviaPlayers.values.forEach{if(it.answer==correct)it.score+=100}
                triviaBroadcast()
            }
            else->error("Unknown Trivia action")
        }
        return true
    }

    private data class Player(val id:String,val name:String,var budget:Int=100,val contracts:JSONArray=JSONArray(),var movie:JSONObject?=null,var ws:WebSocket?=null)
    private val http = object : NanoHTTPD(8080) {
        override fun serve(session: IHTTPSession): Response {
            val path = session.uri.substringBefore('?')
            val filename = when(path) { "/", "/join", "/multiplayer.html" -> "multiplayer.html"; "/codewords", "/codewords-lan.html" -> "codewords-lan.html"; "/tv.html" -> "tv.html"; "/trivia-lan.html" -> "trivia-lan.html"; else -> "" }
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
            if(conn==triviaHost){triviaReset();return@synchronized}
            if(triviaScreens.remove(conn))return@synchronized
            if(triviaPlayers.containsKey(conn)){triviaBroadcast();return@synchronized}
            if(codeScreens.remove(conn))return@synchronized
            if(bidScreens.remove(conn))return@synchronized
            if(conn==codeHost){codeReset();return@synchronized}
            if(codeGuests.remove(conn)!=null){codeBroadcast();return@synchronized}
            val id=sessions.remove(conn)
            if(id=="HOST") { reset(); broadcast() }
            else if(id!=null) { players[id]?.ws=null; broadcast() }
        }
        override fun onMessage(conn:WebSocket, message:String) = synchronized(this@LocalPartyServer) {
            try { if(message.length>8192) error("Message too large"); val payload=JSONObject(message);if(!triviaHandle(conn,payload)&&!codeHandle(conn,payload))handle(conn,payload) }
            catch(e:Exception) { send(conn,JSONObject().put("type","error").put("message",e.message?:"Invalid request")) }
        }
        override fun onError(conn:WebSocket?, ex:Exception) {}
        override fun onStart() { connectionLostTimeout=30 }
    }
    fun start():String = synchronized(this) {
        if(!running) { http.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); socket.start(); running=true }
        return address() ?: ""
    }
    fun stop() = synchronized(this) { if(running){socket.stop(1000);http.stop();running=false};reset();codeReset();triviaReset() }
    fun address():String? {
        val interfaces=NetworkInterface.getNetworkInterfaces() ?: return null
        for(n in interfaces) {
            if(!n.isUp || n.isLoopback)continue
            for(a in n.inetAddresses) if(a is java.net.Inet4Address && a.isSiteLocalAddress) return a.hostAddress
        }
        return null
    }
    private fun reset(){bidScreens.clear();players.clear();sessions.clear();host=null;room="";lot=null;highId=null;highAmount=0;deadline=0;phase="auction";results=null;while(history.length()>0)history.remove(0)}
    private fun send(c:WebSocket?,o:JSONObject){try{if(c?.isOpen==true)c.send(o.toString())}catch(_:Exception){}}
    private fun state():JSONObject {
        val list=JSONArray()
        players.values.forEach { p->list.put(JSONObject().put("id",p.id).put("name",p.name).put("budget",p.budget).put("contracts",p.contracts).put("movie",if(p.movie!=null)JSONObject().put("submitted",true) else JSONObject.NULL)) }
        val high=highId?.let{players[it]}?.let{JSONObject().put("name",it.name).put("amount",highAmount)}
        return JSONObject().put("code",room).put("mode",mode).put("seconds",seconds).put("phase",phase).put("lot",lot?:JSONObject.NULL).put("high",high?:JSONObject.NULL).put("remaining",if(deadline>0) ((deadline-System.currentTimeMillis()+999)/1000).coerceAtLeast(0) else JSONObject.NULL).put("players",list).put("history",history).put("results",results?:JSONObject.NULL)
    }
    private fun broadcast(){val s=state();send(host,JSONObject().put("type","state").put("state",s));players.values.forEach{p->send(p.ws,JSONObject().put("type","state").put("state",s).put("you",p.id))};bidScreens.forEach{send(it,JSONObject().put("type","state").put("state",s))}}
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
            if(type=="watch"){
                if(room.isEmpty()||m.optString("code")!=room)error("Room not found")
                if(bidScreens.size>=5)error("Too many TV displays")
                bidScreens.add(c);send(c,JSONObject().put("type","state").put("state",state()));return
            }
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
                val ids=m.optJSONArray("ids")?:JSONArray();val selected=mutableListOf<JSONObject>();val used=mutableSetOf<Int>()
                for(i in 0 until ids.length()) {val n=ids.optInt(i,-1);if(n<0||n>=p.contracts.length()||!used.add(n))error("Invalid cast");selected.add(p.contracts.getJSONObject(n))}
                if(selected.count{it.optString("kind")=="director"}!=1||selected.none{it.optString("kind")=="actor"}||selected.none{it.optString("kind")=="actress"})error("Select exactly one director and at least one actor and actress")
                val cast=selected.sumOf{it.optInt("price")};val production=m.optInt("production",-1)
                if(production<kotlin.math.ceil(cast*.3).toInt()||production>p.budget)error("Invalid production budget")
                p.movie=JSONObject().put("castCost",cast).put("production",production).put("title",m.optString("title","Untitled Film").trim().take(70));broadcast()
            }
            "results" -> {if(!isHost||phase!="movies")error("Host only during movie making");if(players.values.any{it.movie==null})error("Wait for all players")
                phase="results";results=JSONArray()
                val scored=players.values.map{p->
                    val cast=p.movie!!.getInt("castCost");val production=p.movie!!.getInt("production");val total=cast+production
                    // Fictional deterministic LAN game economy, not actual box office.
                    val collections=kotlin.math.round((production*2.0+cast*1.25+22.0)*100.0)/100.0
                    val profit=kotlin.math.round((collections-total)*100.0)/100.0
                    JSONObject().put("name",p.name).put("title",p.movie!!.optString("title","Untitled Film"))
                        .put("totalCost",total).put("collections",collections).put("profit",profit)
                        .put("roi",kotlin.math.round((profit/total*100.0)*100.0)/100.0)
                }.sortedByDescending{it.getDouble("profit")}
                scored.forEach{results!!.put(it)}
                broadcast()
            }
            else -> error("Unknown action")
        }
    }
    init { Thread {
        while(true) {Thread.sleep(250);synchronized(this@LocalPartyServer) {if(running&&deadline>0&&System.currentTimeMillis()>=deadline)endLot()
            if(running&&triviaDeadline>0&&System.currentTimeMillis()>=triviaDeadline&&!triviaRevealed){
                triviaRevealed=true;triviaDeadline=0L;triviaAdvanceAt=System.currentTimeMillis()+8000L
                val correct=triviaBank[triviaDeck[triviaIndex]][5].toInt()
                triviaPlayers.values.forEach{if(it.answer==correct)it.score+=100}
                triviaBroadcast()
            }
            if(running&&triviaAdvanceAt>0&&System.currentTimeMillis()>=triviaAdvanceAt){
                triviaAdvanceAt=0L
                if(triviaIndex+1>=triviaDeck.size){triviaFinished=true;triviaDeadline=0L}
                else{triviaIndex++;triviaRevealed=false;triviaDeadline=System.currentTimeMillis()+triviaDuration*1000L;triviaPlayers.values.forEach{it.answer=-1}}
                triviaBroadcast()
            }} }
    }.apply {isDaemon=true;start()} }
}
