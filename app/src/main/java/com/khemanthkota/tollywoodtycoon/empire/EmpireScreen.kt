package com.khemanthkota.tollywoodtycoon.empire

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import org.json.JSONArray

internal fun productionFundsAvailable(cash: Int, budget: Int, payroll: Int, craft: Int = 0, contingency: Int = 0): Long =
    cash.toLong() - budget - payroll - craft - contingency

private fun decodeProduction(p: JSONObject): Production =
Production(FilmPlan(p.getString("title"), Genre.valueOf(p.getString("genre")), Talent.valueOf(p.getString("cast")), Director.valueOf(p.getString("director")), p.getInt("budget"), p.optString("primary").takeIf { it.isNotEmpty() }?.let { HybridFilm(StoryGenre.valueOf(it),p.optString("secondary").takeIf { it.isNotEmpty() }?.let { g -> StoryGenre.valueOf(g) },scale = ProductionScale.valueOf(p.optString("scale","INDEPENDENT"))) }), p.getBoolean("event"), p.getInt("seed"), p.optInt("potential",-1).takeIf { it >= 0 }?.let { CapabilityAssessment(emptyMap(),emptyMap(),it,0,0,false) })

private fun encodeProduction(p: Production): JSONObject = JSONObject().put("title", p.plan.title).put("genre", p.plan.genre.name)
            .put("cast", p.plan.cast.name).put("director", p.plan.director.name).put("budget", p.plan.budget)
            .put("event", p.event).put("seed", p.seed).put("primary",p.plan.hybrid?.primary?.name ?: "").put("secondary",p.plan.hybrid?.secondary?.name ?: "").put("scale",p.plan.hybrid?.scale?.name ?: "INDEPENDENT").put("potential",p.capability?.qualityPotential ?: -1)

private fun encodeOtt(offer: RightsOffer?): Any = offer?.let { JSONObject().put("buyer",it.buyer).put("guarantee",it.guaranteedLakh).put("bonus",it.bonusLakh).put("threshold",it.grossThresholdLakh).put("window",it.theatricalWindowDays).put("direct",it.directStreaming) } ?: JSONObject.NULL
private fun decodeOtt(j: JSONObject?): RightsOffer? = j?.let { RightsOffer(it.getString("buyer"),RightsCategory.OTT,it.getInt("guarantee"),it.optInt("bonus"),it.optInt("threshold"),it.optInt("window"),it.optBoolean("direct")) }

private fun encodeNegotiations(offers: List<RightsNegotiation>): JSONArray = JSONArray().apply {
    offers.forEach { put(JSONObject().put("offer",encodeOtt(it.offer)).put("rounds",it.rounds).put("status",it.status.name)) }
}
private fun decodeNegotiations(arr: JSONArray?): List<RightsNegotiation> = if(arr == null) emptyList() else (0 until arr.length()).mapNotNull { i ->
    val j = arr.getJSONObject(i)
    decodeOtt(j.optJSONObject("offer"))?.let { RightsNegotiation(it,j.optInt("rounds"),DealStatus.valueOf(j.optString("status","OPEN"))) }
}
private fun decodeSlots(j: JSONObject): ProductionSlots {
    val arr = j.optJSONArray("movieSlots") ?: return ProductionSlots()
    val movies = (0 until arr.length()).map { i ->
        val x = arr.getJSONObject(i)
        val crafts = FilmCraft.values().mapNotNull { craft -> x.optJSONObject("crafts")?.optString(craft.name)?.takeIf { it.isNotEmpty() }?.let { craft to CraftInvestment.valueOf(it) } }.toMap()
        ProductionSlot(x.getString("id"), decodeProduction(x.getJSONObject("production")), x.getInt("stage"), x.optString("response").takeIf { it.isNotEmpty() }?.let { Response.valueOf(it) }, crafts, x.optInt("releaseDay",-1).takeIf { it >= 0 }, x.optInt("stageReadyDay",0), decodeOtt(x.optJSONObject("ottDeal")), decodeNegotiations(x.optJSONArray("ottOffers")))
    }
    return ProductionSlots(movies, j.optString("selectedMovie").takeIf { it.isNotEmpty() })
}

private fun encodeSlots(slots: ProductionSlots): JSONArray = JSONArray().apply {
    slots.movies.forEach { slot -> put(JSONObject().put("id",slot.id).put("ottOffers",encodeNegotiations(slot.ottOffers)).put("ottDeal",encodeOtt(slot.ottDeal)).put("stageReadyDay",slot.stageReadyDay).put("releaseDay",slot.releaseDay ?: -1).put("production",encodeProduction(slot.production)).put("stage",slot.stage).put("response",slot.response?.name ?: "").put("crafts",JSONObject().apply { slot.crafts.forEach { (k,v) -> put(k.name,v.name) } })) }
}

internal data class Save(val career: Career = Career(seed = kotlin.random.Random.nextInt()), val history: List<FilmResult> = emptyList(), val pending: Production? = null, val owned: List<StudioAsset> = emptyList(), val research: Set<String> = emptySet(), val stage: Int = 0, val response: Response? = null, val crafts: Map<FilmCraft,CraftInvestment> = emptyMap(), val contracts: List<TalentContract> = emptyList(), val loans: List<StudioLoan> = emptyList(), val overdue: Int = 0, val managers: List<StudioManager> = emptyList(), val slots: ProductionSlots = ProductionSlots(), val releaseDay: Int? = null, val gameDay: Int = 0, val stageReadyDay: Int = 0, val ottDeal: RightsOffer? = null, val ottOffers: List<RightsNegotiation> = emptyList(), val streamingPayments: List<StreamingPayment> = emptyList(), val accounts: BusinessAccounts = BusinessAccounts(), val theaters: List<StudioTheater> = emptyList(), val relationships: StudioRelationships = StudioRelationships())
internal class CareerStore(context: Context) {
    private val prefs = context.getSharedPreferences("cinema_empire_v1", Context.MODE_PRIVATE)
    fun load(): Save {
        val raw = prefs.getString("career", null) ?: return Save()
        val j = JSONObject(raw)
        require(j.getInt("version") == 1)
        val a = j.getJSONArray("history")
        val history = (0 until a.length()).map {
            val f = a.getJSONObject(it)
            FilmResult(f.getString("title"), f.getInt("quality"), f.getInt("gross"), f.getInt("rights"), f.getInt("revenue"), f.getInt("cost"), f.getInt("delay"), f.optInt("actualReleaseDay",-1), f.optBoolean("ottSold",true), f.optInt("ottRound",-1), f.optString("ottNegotiationJson",""))
        }
        val rivals = j.optJSONArray("rivals")?.let { arr ->
            (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                RivalStudio(s.getString("name"), s.getInt("cash"), Genre.valueOf(s.getString("genre")), s.getInt("ambition"))
            }
        } ?: EmpireMarket.initialRivals
        val pending = j.optJSONObject("pending")?.let { p ->
            decodeProduction(p)
        }
        return Save(Career(j.getInt("cash"), j.getInt("completed"), j.getInt("credibility"), j.getInt("audience"), j.getInt("seed"), rivals, Genre.values().associateWith { g -> j.optJSONObject("genreHits")?.optInt(g.name, 0) ?: 0 }), history, pending, j.optJSONArray("owned")?.let { arr -> (0 until arr.length()).map { i -> val x = arr.getJSONObject(i); StudioAsset(x.getString("id"), x.getString("technology"), x.optString("owner","player"), x.getInt("value"), x.optInt("debt",0), x.optString("renter","").takeIf { it.isNotEmpty() }, x.optInt("bookedUntil",0), x.optBoolean("auction",false)) } } ?: emptyList(), j.optJSONArray("research")?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet(), j.optInt("stage",0), j.optString("response","").takeIf { it.isNotEmpty() }?.let { Response.valueOf(it) }, FilmCraft.values().mapNotNull { craft -> j.optJSONObject("crafts")?.optString(craft.name)?.takeIf { it.isNotEmpty() }?.let { craft to CraftInvestment.valueOf(it) } }.toMap(), j.optJSONArray("contracts")?.let { arr -> (0 until arr.length()).map { i -> val t = arr.getJSONObject(i); TalentContract(t.getString("id"),t.getString("talent"),"player",ContractType.valueOf(t.getString("type")),t.getInt("remaining"),t.getInt("fee"),t.getInt("advance"),t.getInt("share"),t.getInt("expires")) } } ?: emptyList(), j.optJSONArray("loans")?.let { arr -> (0 until arr.length()).map { i -> val l=arr.getJSONObject(i); StudioLoan(l.getString("id"),l.getInt("principal"),l.getInt("outstanding"),l.getInt("rate"),l.getInt("months")) } } ?: emptyList(), j.optInt("overdue",0), j.optJSONArray("managers")?.let { arr -> (0 until arr.length()).map { i -> val m=arr.getJSONObject(i); StudioManager(m.getString("id"),Department.valueOf(m.getString("department")),m.getInt("skill"),m.getInt("reliability"),m.getInt("salary"),m.optInt("experience")) } } ?: emptyList(), decodeSlots(j), j.optInt("releaseDay",-1).takeIf { it >= 0 }, j.optInt("gameDay", j.getInt("completed")*30), j.optInt("stageReadyDay",0), decodeOtt(j.optJSONObject("ottDeal")), decodeNegotiations(j.optJSONArray("ottOffers")), j.optJSONArray("streamingPayments")?.let { arr -> (0 until arr.length()).map { i -> val t=arr.getJSONObject(i); StreamingPayment(t.getString("film"),t.getString("buyer"),t.getInt("day"),t.getInt("amount")) } } ?: emptyList(), BusinessAccounts(j.optLong("taxableProfit"),j.optLong("businessArrears"),j.optLong("lastTax")), j.optJSONArray("theaters")?.let { a -> (0 until a.length()).map { i -> val t=a.getJSONObject(i); StudioTheater(t.getString("id"),TheaterAcquisition.valueOf(t.getString("type")),t.getInt("opening")) } } ?: emptyList(), StudioRelationships(j.optJSONObject("relationships")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } ?: emptyMap()))
    }
    fun write(s: Save): Boolean {
        val c = s.career
        val a = JSONArray()
        s.history.forEach { f ->
            a.put(JSONObject().put("actualReleaseDay",f.actualReleaseDay).put("ottSold",f.ottSold).put("ottRound",f.ottRound).put("ottNegotiationJson",f.ottNegotiationJson).put("title", f.title).put("quality", f.quality).put("gross", f.gross)
                .put("rights", f.rights).put("revenue", f.revenue).put("cost", f.cost).put("delay", f.delay))
        }
        val rivals = JSONArray()
        c.rivals.forEach { s -> rivals.put(JSONObject().put("name", s.name).put("cash", s.cash).put("genre", s.preferredGenre.name).put("ambition", s.ambition)) }
        val pending = s.pending?.let { encodeProduction(it) }
        val owned = JSONArray()
        s.owned.forEach { x -> owned.put(JSONObject().put("id",x.id).put("technology",x.technologyId).put("value",x.value).put("debt",x.debt).put("owner",x.owner).put("renter",x.renter ?: "").put("bookedUntil",x.bookedUntil).put("auction",x.auction)) }
        return prefs.edit().putString("career", JSONObject().put("theaters",JSONArray().apply { s.theaters.forEach { t -> put(JSONObject().put("id",t.id).put("type",t.acquisition.name).put("opening",t.openingDay)) } }).put("relationships",JSONObject().apply { s.relationships.scores.forEach { (k,v) -> put(k,v) } }).put("taxableProfit",s.accounts.taxableProfit).put("businessArrears",s.accounts.arrears).put("lastTax",s.accounts.lastTax).put("streamingPayments",JSONArray().apply { s.streamingPayments.forEach { t -> put(JSONObject().put("film",t.film).put("buyer",t.buyer).put("day",t.dueDay).put("amount",t.amount)) } }).put("ottOffers",encodeNegotiations(s.ottOffers)).put("ottDeal",encodeOtt(s.ottDeal)).put("stageReadyDay",s.stageReadyDay).put("gameDay",s.gameDay).put("releaseDay",s.releaseDay ?: -1).put("movieSlots",encodeSlots(s.slots)).put("selectedMovie",s.slots.selectedId ?: "").put("managers",JSONArray().apply { s.managers.forEach { m -> put(JSONObject().put("id",m.id).put("department",m.department.name).put("skill",m.skill).put("reliability",m.reliability).put("salary",m.salary).put("experience",m.experience)) } }).put("overdue",s.overdue).put("loans",JSONArray().apply { s.loans.forEach { l -> put(JSONObject().put("id",l.id).put("principal",l.principal).put("outstanding",l.outstanding).put("rate",l.annualRateBps).put("months",l.monthsRemaining)) } }).put("contracts",JSONArray().apply { s.contracts.forEach { t -> put(JSONObject().put("id",t.id).put("talent",t.talent).put("type",t.type.name).put("remaining",t.remainingFilms).put("fee",t.fee).put("advance",t.advance).put("share",t.profitShare).put("expires",t.expiresDay)) } }).put("crafts",JSONObject().apply { s.crafts.forEach { (k,v) -> put(k.name,v.name) } }).put("stage",s.stage).put("response",s.response?.name ?: "").put("research",JSONArray(s.research.toList())).put("owned",owned).put("genreHits", JSONObject().apply { c.genreHits.forEach { (g, hits) -> put(g.name, hits) } }).put("pending", pending ?: JSONObject.NULL).put("rivals", rivals).put("version", 1).put("cash", c.cash)
            .put("completed", c.completed).put("credibility", c.credibility).put("audience", c.audience)
            .put("seed", c.seed).put("history", a).toString()).commit()
    }
}

@Composable
fun EmpireScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { CareerStore(context) }
    val audio = remember { EmpireAudio() }
    val audioPrefs = remember { context.getSharedPreferences("empire_audio", Context.MODE_PRIVATE) }
    var musicVolume by remember { mutableStateOf(audioPrefs.getFloat("music", .25f)) }
    var effectsVolume by remember { mutableStateOf(audioPrefs.getFloat("effects", .45f)) }
    var ambienceVolume by remember { mutableStateOf(audioPrefs.getFloat("ambience", .18f)) }
    var audioSettings by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(true) }
    DisposableEffect(audio, context) {
        val owner = context as? androidx.lifecycle.LifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when(event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> { foreground = false; audio.pause() }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> { foreground = true; audio.resume() }
                else -> Unit
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer); audio.release() }
    }
    LaunchedEffect(musicVolume, effectsVolume, ambienceVolume) {
        audio.musicVolume = musicVolume
        audio.effectsVolume = effectsVolume
        audio.ambienceVolume = ambienceVolume
        audioPrefs.edit().putFloat("music",musicVolume).putFloat("effects",effectsVolume).putFloat("ambience",ambienceVolume).apply()
    }
    val loaded = remember { runCatching { store.load() } }
    var save by remember { mutableStateOf(loaded.getOrNull() ?: Save()) }
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(Genre.ACTION) }
    var secondary by remember { mutableStateOf<StoryGenre?>(null) }
    var scale by remember { mutableStateOf(ProductionScale.INDEPENDENT) }
    var genrePopup by remember { mutableStateOf(false) }
    var cast by remember { mutableStateOf(Talent.NEWCOMER) }
    var director by remember { mutableStateOf(Director.DEBUT) }
    var budget by remember { mutableStateOf("200") }
    var pending by remember { mutableStateOf<Production?>(save.pending) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Studio") }
    var resultPopup by remember { mutableStateOf<FilmResult?>(null) }
    LaunchedEffect(tab, foreground, resultPopup, pending, save.stage, save.response) {
        if (foreground) audio.scene(when {
            resultPopup != null -> if ((resultPopup?.profit ?: 0) > 0) "Premiere" else "Crisis"
            pending?.event == true && save.stage == 1 && save.response == null && tab == "Produce" -> "Crisis"
            else -> tab
        })
    }
    var clockSpeed by remember { mutableStateOf(0) }
    LaunchedEffect(clockSpeed, foreground) {
        while (clockSpeed > 0 && foreground && loaded.isSuccess) {
            kotlinx.coroutines.delay(4000L / clockSpeed)
            val day = save.gameDay + 1
            val settlement = settleStreaming(save.streamingPayments,day)
            val reserved = save.slots.movies.sumOf { it.commitment().total } + (save.pending?.plan?.budget?.toLong() ?: 0L)
            val ownFilm = save.history.any { it.actualReleaseDay >= 0 && day-it.actualReleaseDay in 1..28 && it.gross > 0 }
            val forecast = ReleaseSeasonPlanner.year(day/365,"Telugu",0).forecast(day,"Telugu",60,100)
            val occupancy = StudioEvents.occupancy(forecast.occupancy,ownFilm,forecast.rivals.size)
            val birthdays = StudioEvents.birthdays(day)
            var relations = save.relationships
            var gifts = 0
            if(save.managers.isNotEmpty()) birthdays.forEach { talent ->
                if(StudioEvents.managerGift(save.career.cash-gifts,reserved,relations.score(talent.name))) {
                    gifts += 5
                    relations = relations.gift(talent.name,5)
                }
            }
            val theaterNet = save.theaters.sumOf { t -> t.dailyMargin(day,occupancy,ownFilm) - if(BusinessCalendar.monthBoundary(day) && t.operating(day)) t.acquisition.monthly else 0 }
            val business = BusinessCalendar.closeDay(day,Math.addExact(Math.addExact(save.career.cash,settlement.received),theaterNet-gifts),save.accounts.record(theaterNet.toLong()),save.managers.sumOf { it.salary },reserved)
            val finance = FinanceLedger(business.cash,save.loans,save.overdue)
            val monthly = if(BusinessCalendar.monthBoundary(day)) {
                val available = (business.cash.toLong()-reserved).coerceAtLeast(0).toInt()
                val paid = FinanceLedger(available,save.loans,save.overdue).month()
                FinanceLedger(Math.addExact(business.cash-available,paid.cash),paid.loans,paid.overdue)
            } else finance
            val updated = save.copy(relationships=relations, gameDay = day, streamingPayments = settlement.remaining, accounts = business.accounts, loans = monthly.loans, overdue = monthly.overdue, career = save.career.copy(cash = monthly.cash, rivals = if(BusinessCalendar.monthBoundary(day)) StudioEvents.rivalMonth(save.career.rivals,day,save.career.seed) else save.career.rivals))
            if (store.write(updated)) save = updated else { clockSpeed = 0; error = "Clock save failed." }
        }
    }
    val c = save.career
    val monthlyPayroll = BusinessCalendar.employeeCost(save.managers.sumOf { it.salary },false)
    val loanReserve = (c.cash.toLong() - FinanceLedger(c.cash,save.loans,save.overdue).month().cash).coerceAtLeast(0L)
    val otherMovieReserve = save.slots.movies.sumOf { it.commitment().total + monthlyPayroll.toLong() + loanReserve }
    val productionCash = (c.cash.toLong() - loanReserve - otherMovieReserve).coerceIn(0L,Int.MAX_VALUE.toLong()).toInt()
    fun finish(p: Production, response: Response? = null) {
        try {
            require(productionFundsAvailable(productionCash, p.plan.budget, monthlyPayroll, EmpireProductionCrafts.assess(save.crafts).extraCost, response?.cost ?: 0) >= 0) { "Insufficient reserved funds for production and payroll." }
            val (rawNext, rawFilm) = EmpireEngine.finishWithRights(c, p, response, save.crafts, save.ottDeal?.let { RightsBook().accept(it) } ?: RightsBook(), save.releaseDay?.let { day -> ReleaseSeasonPlanner.year(day/365,"Telugu",0).forecast(day,"Telugu",p.plan.cast.appeal,100) })
            val agreement = save.contracts.firstOrNull { it.talent == p.plan.cast.name && it.remainingFilms > 0 && it.expiresDay > save.gameDay }
            val discount = agreement?.let { (p.plan.cast.fee-it.fee).coerceAtLeast(0) } ?: 0
            val film = rawFilm.copy(cost=rawFilm.cost-discount, actualReleaseDay=save.gameDay, ottSold=save.ottDeal != null)
            val payment = save.ottDeal?.let { StreamingPayment.from(p.plan.title,it,save.gameDay,film.gross) }?.takeIf { it.dueDay > save.gameDay }
            val next = rawNext.copy(cash=rawNext.cash+discount-(payment?.amount ?: 0))
            val updatedContracts = save.contracts.map { if(it.id==agreement?.id) it.copy(remainingFilms=it.remainingFilms-1) else it }
            val payroll = monthlyPayroll
            val finance = FinanceLedger(next.cash,save.loans,save.overdue)
            val reserved = save.slots.movies.sumOf { it.commitment().total + payroll.toLong() + loanReserve }
            require(finance.cash.toLong() >= reserved) { "Settlement would consume funds reserved for other productions." }
            val updated = save.copy(relationships = save.ottDeal?.let { save.relationships.change(it.buyer,3) } ?: save.relationships, accounts = save.accounts.record(film.profit.toLong()), career = next.copy(cash=finance.cash), loans=finance.loans, overdue=finance.overdue, history = save.history + film, streamingPayments = save.streamingPayments + listOfNotNull(payment), contracts = updatedContracts, pending = null, stage = 0, response = null, crafts = emptyMap(), releaseDay = null, ottDeal = null, ottOffers = emptyList())
            if (!store.write(updated)) { error = "Save failed. Please retry."; return }
            save = updated
            pending = null
            title = ""
            resultPopup = film
            audio.event(if (film.profit > 0) "applause" else "crisis")
            tab = "Studio"
            error = ""
        } catch (e: Exception) { error = e.message ?: "Cannot complete film." }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFFD080), secondary = Color(0xFF75D9E8), background = Color(0xFF101522), surface = Color(0xFF192235))) {
    Surface(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back to games") }
            TextButton(onClick = { audio.cue(); audioSettings = true }) { Text("Audio settings") }
        }
        if (audioSettings) AlertDialog(
            onDismissRequest = { audioSettings = false },
            title = { Text("Studio audio") },
            text = { Column {
                Text("Music • ${(musicVolume*100).toInt()}%")
                Slider(value = musicVolume, onValueChange = { musicVolume = it })
                Text("Sound effects • ${(effectsVolume*100).toInt()}%")
                Slider(value = effectsVolume, onValueChange = { effectsVolume = it }, onValueChangeFinished = { audio.cue() })
                Text("Studio ambience • ${(ambienceVolume*100).toInt()}%")
                Slider(value = ambienceVolume, onValueChange = { ambienceVolume = it })
                TextButton(onClick = { musicVolume = 0f; effectsVolume = 0f; ambienceVolume = 0f }) { Text("Mute all") }
            } },
            confirmButton = { TextButton(onClick = { audioSettings = false }) { Text("Done") } }
        )
        Text("Cinema Empire", style = MaterialTheme.typography.headlineMedium)
        Text("Game day ${save.gameDay+1} • Year ${2026+save.gameDay/365}")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0,1,2,5,10).forEach { speed -> FilterChip(selected = clockSpeed == speed, onClick = { clockSpeed = speed }, label = { Text(if (speed == 0) "Pause" else "${speed}×") }) }
        }
        if (loaded.isFailure) {
            Text("Career save could not be read. Existing data has not been overwritten.")
            return@Column
        }
        Text("Cash: ₹${c.cash} lakh | Films: ${c.completed}")
        Text("Credibility: ${c.credibility} | Audience: ${c.audience} | Level: ${c.unlocked}")
        val tabs = listOf("Studio","Produce","Market","Assets","Career")
        ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab), edgePadding = 0.dp) {
            tabs.forEach { name -> Tab(selected = tab == name, onClick = { audio.cue(); tab = name }, text = { Text(name) }) }
        }
        if (pending != null) TextButton(onClick = { audio.cue(); tab = "Produce" }) { Text("Active production • resume") }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        resultPopup?.let { film ->
            val animatedGross by animateIntAsState(targetValue = film.gross, animationSpec = tween(1200))
            AlertDialog(onDismissRequest = { resultPopup = null }, title = { Text("Premiere • ${film.title}") },
                text = { Text("Box office ₹${animatedGross} lakh\nProfit/loss ₹${film.profit} lakh\nQuality ${film.quality}/100") },
                confirmButton = { Button(onClick = { resultPopup = null }) { Text("Continue") } })
        }
        key(tab) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (tab == "Studio") {
        Text("Studio command center", style = MaterialTheme.typography.headlineSmall)
        StudioEvents.birthdays(save.gameDay).forEach { Text("Birthday today: ${it.name}") }
        Text("Business arrears: ₹${save.accounts.arrears}L • Last annual tax: ₹${save.accounts.lastTax}L")
        if(StudioEvents.distressed(c.cash,save.accounts.arrears+save.overdue.toLong()) || c.cash<0) {
            Text("Financial distress: sell assets or arrange financing.",color=MaterialTheme.colorScheme.error)
            var confirmBankruptcy by remember { mutableStateOf(false) }
            OutlinedButton(onClick={confirmBankruptcy=true}) { Text("Declare bankruptcy") }
            if(confirmBankruptcy) AlertDialog(onDismissRequest={confirmBankruptcy=false},title={Text("Close studio and restart?")},text={Text("This ends your current career, cancels productions and removes assets, debts and film history. Start a new studio.")},confirmButton={TextButton(onClick={
                val updated=Save()
                if(store.write(updated)) { clockSpeed=0; save=updated; pending=null; confirmBankruptcy=false; error="" } else error="Save failed."
            }) { Text("Restart career") }},dismissButton={TextButton(onClick={confirmBankruptcy=false}) { Text("Cancel") }})
        }
        if(save.managers.isNotEmpty()) Text("Managers automatically send affordable birthday gifts.")
        Card(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFF101522))
                for (i in 0..7) {
                    val x = w*i/8
                    drawLine(Color(0xFF29405A), Offset(x,0f), Offset(w/2+(x-w/2)*1.8f,h), 2f)
                }
                drawRect(Color(0xFF263A50), Offset(w*.22f,h*.15f), Size(w*.56f,h*.58f))
                drawRect(Color(0xFF75D9E8), Offset(w*.25f,h*.19f), Size(w*.5f,h*.45f))
                drawRect(Color(0xFF101522), Offset(w*.28f,h*.22f), Size(w*.44f,h*.39f))
                drawCircle(Color(0xFFFFD080), h*.07f, Offset(w*.5f,h*.41f))
                drawLine(Color(0xFFFFD080),Offset(w*.1f,h*.1f),Offset(w*.35f,h*.65f),4f)
                drawLine(Color(0xFFFFD080),Offset(w*.9f,h*.1f),Offset(w*.65f,h*.65f),4f)
            }
            Text("STAGE 01  •  STUDIO OPERATIONS", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.secondary)
        }
        Button(onClick = { tab = "Produce" }, modifier = Modifier.fillMaxWidth()) { Text("Start / resume production") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { audio.cue(); tab = "Assets" }) { Text("Equipment") }
            OutlinedButton(onClick = { audio.cue(); tab = "Market" }) { Text("Industry") }
        }
        val last = save.history.lastOrNull()
        if (last != null) {
            Card { Column(Modifier.padding(12.dp)) {
                Text(last.title, style = MaterialTheme.typography.titleLarge)
                Text("Quality: ${last.quality}/100 | Box office: ₹${last.gross} lakh")
                Text("Producer revenue: ₹${last.revenue} lakh | Cost: ₹${last.cost} lakh")
                Text("Profit/loss: ₹${last.profit} lakh | Delay: ${last.delay} days")
            } }
        }
        }
        if (tab == "Produce") {
            Text("Production portfolio", style = MaterialTheme.typography.titleMedium)
            if (pending != null) {
                OutlinedButton(onClick = {
                    val current = pending ?: return@OutlinedButton
                    val slot = ProductionSlot(java.util.UUID.randomUUID().toString(), current, save.stage, save.response, save.crafts, save.releaseDay, save.stageReadyDay, save.ottDeal, save.ottOffers)
                    val parked = ProductionSlots(save.slots.movies + slot)
                    val updated = save.copy(slots = parked, pending = null, stage = 0, response = null, crafts = emptyMap(), releaseDay = null, ottDeal = null, ottOffers = emptyList())
                    if (store.write(updated)) { save = updated; pending = null; title = ""; error = "" } else error = "Save failed."
                }) { Text("Park movie / start another") }
            }
            save.slots.movies.forEach { slot ->
                OutlinedButton(onClick = {
                    val current = pending
                    val remaining = save.slots.movies.filterNot { it.id == slot.id }
                    val parked = if (current == null) remaining else remaining + ProductionSlot(java.util.UUID.randomUUID().toString(), current, save.stage, save.response, save.crafts, save.releaseDay, save.stageReadyDay, save.ottDeal, save.ottOffers)
                    val updated = save.copy(slots = ProductionSlots(parked), pending = slot.production, stage = slot.stage, response = slot.response, crafts = slot.crafts, releaseDay = slot.releaseDay, stageReadyDay = slot.stageReadyDay, ottDeal = slot.ottDeal, ottOffers = slot.ottOffers)
                    if (store.write(updated)) { save = updated; pending = slot.production; error = "" } else error = "Save failed."
                }) { Text("Resume ${slot.production.plan.title} • Stage ${slot.stage + 1}") }
            }

        val p = pending
        if (p != null) {
            val stages = listOf("Pre-production","Principal photography","Post-production","Premiere")
            val stage = save.stage.coerceIn(0,3)
            Text(p.plan.title, style = MaterialTheme.typography.headlineSmall)
            Text("Stage ${stage+1} of 4 • ${stages[stage]}")
            LinearProgressIndicator(progress = (stage+1)/4f, modifier = Modifier.fillMaxWidth())
            Card { Column(Modifier.padding(16.dp)) {
                Text(listOf("Finalize cast and production budget.","Shoot the movie on set.","Complete editing and prepare the release.","Release the film and reveal box office results.")[stage])
                Text("Committed budget: ₹${p.plan.budget} lakh")
            } }
            val extra = EmpireProductionCrafts.assess(save.crafts).extraCost
            val payrollReserve = save.managers.sumOf { it.salary }
            val responseReserve = save.response?.cost ?: if (p.event) Response.values().minOf { it.cost } else 0
            Text("Payroll reserve: ₹$payrollReserve lakh • Available: ₹${productionCash-p.plan.budget-extra-payrollReserve-responseReserve} lakh")
            Text("Craft upgrades: ₹$extra lakh")
            val assessment = EmpireProductionCrafts.assess(save.crafts)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Production decision review", style = MaterialTheme.typography.titleMedium)
                    Text("Base film: ₹${p.plan.budget}L • Craft investment: ₹${assessment.extraCost}L")
                    Text("Payroll: ₹${payrollReserve}L • Crisis reserve: ₹${responseReserve}L")
                    Text("Uncommitted cash: ₹${productionFundsAvailable(productionCash,p.plan.budget,payrollReserve,extra,responseReserve)}L")
                    Text("Selected craft decisions: ${save.crafts.size}")
                    save.crafts.entries.forEach { (craft, investment) ->
                        Text("${craft.name.replace('_',' ')}: ${investment.name} (+₹${investment.cost}L)")
                    }
                    Text("Choices are saved with your production and affect the final film.")
                }
            }
            if(save.managers.isNotEmpty()) {
                Button(onClick={
                    var choices=save.crafts
                    val available=EmpireProductionCrafts.available(c.completed,CraftStage.values()[stage])
                    save.managers.forEach { manager ->
                        val remaining=(productionCash-p.plan.budget-responseReserve-payrollReserve-EmpireProductionCrafts.assess(choices).extraCost).coerceAtLeast(0)
                        val report=EmpireManagers.recommend(manager,ManagerPolicy(Delegation.AUTOMATIC,30),available.filter { it !in choices },remaining)
                        choices=choices+report.choices
                    }
                    val updated=save.copy(crafts=choices)
                    if(store.write(updated)) save=updated else error="Save failed."
                }) { Text("Apply manager recommendations • max ₹30L each") }
            }
            EmpireProductionCrafts.available(c.completed, CraftStage.values()[stage]).forEach { craft ->
                Text(craft.name.replace('_',' '))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CraftInvestment.values().forEach { investment ->
                        val choices = save.crafts + (craft to investment)
                        FilterChip(selected = save.crafts[craft] == investment,
                            enabled = productionFundsAvailable(productionCash, p.plan.budget, payrollReserve, EmpireProductionCrafts.assess(choices).extraCost, responseReserve) >= 0,
                            onClick = {
                                val updated = save.copy(crafts = choices)
                                if (store.write(updated)) { save = updated; audio.cue() } else error = "Save failed."
                            }, label = { Text("${investment.name} +₹${investment.cost}") })
                    }
                }
            }
            var showOtt by remember(p) { mutableStateOf(false) }
            OutlinedButton(onClick = {
                if(save.ottDeal == null && save.ottOffers.isEmpty()) {
                    val updated = save.copy(ottOffers = StreamingMarket.offers(p.plan,c.credibility,c.seed+stage).map { RightsNegotiation(it.copy(guaranteedLakh=it.guaranteedLakh*(100+save.relationships.platformPremium(it.buyer))/100)) })
                    if(store.write(updated)) { save=updated; showOtt=true } else error="Save failed."
                } else showOtt=true
            }) { Text(save.ottDeal?.let { "OTT signed: ${it.buyer}" } ?: "Explore OTT offers") }
            if (showOtt) {
                val signed = save.ottDeal
                AlertDialog(onDismissRequest = { showOtt = false }, title = { Text("Streaming rights") },
                    text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                        if (signed != null) Text("${signed.buyer}: ₹${signed.guaranteedLakh}L • ${if(signed.directStreaming) "Direct OTT" else "${signed.theatricalWindowDays}-day theatrical window"}")
                        else {
                            val offers = save.ottOffers
                            fun persistOffers(next: List<RightsNegotiation>) {
                                val updated = save.copy(ottOffers=next)
                                if(store.write(updated)) save=updated else error="Save failed."
                            }
                            offers.forEachIndexed { index, deal ->
                                Text("${deal.offer.buyer} • ₹${deal.offer.guaranteedLakh}L")
                                Text(if(deal.offer.directStreaming) "Direct OTT • no box office" else "${deal.offer.theatricalWindowDays}-day window • bonus ₹${deal.offer.bonusLakh}L")
                                if(deal.status == DealStatus.OPEN) {
                                    if(!deal.offer.directStreaming) Row(Modifier.horizontalScroll(rememberScrollState())) {
                                        listOf(14,28,56,90).forEach { window ->
                                            FilterChip(selected=deal.offer.theatricalWindowDays==window,onClick={
                                                val previous=deal.offer.theatricalWindowDays
                                                val revised=deal.offer.copy(theatricalWindowDays=window,guaranteedLakh=(deal.offer.guaranteedLakh.toLong()*(150-window)/(150-previous)).toInt())
                                                persistOffers(offers.mapIndexed { i,old -> if(i==index) old.copy(offer=revised) else old })
                                            },label={Text("$window days")})
                                        }
                                    }
                                    TextButton(onClick = { persistOffers(offers.mapIndexed { i, old -> if(i==index) old.counter(old.offer.guaranteedLakh*120/100,c.credibility) else old }) }) { Text("Counter +20%") }
                                    TextButton(onClick = {
                                        val updated = save.copy(ottDeal = deal.offer)
                                        if(store.write(updated)) { save=updated; showOtt=false } else error="Save failed."
                                    }) { Text("Accept ${deal.offer.buyer}") }
                                    TextButton(onClick = { persistOffers(offers.mapIndexed { i, old -> if(i==index) old.copy(status=DealStatus.WALKED_AWAY) else old }) }) { Text("Reject") }
                                } else Text("Offer closed")
                            }
                        }
                    } }, confirmButton = { TextButton(onClick = { showOtt=false }) { Text("Close") } })
            }
            val ready = save.gameDay >= save.stageReadyDay
            if (!ready) Text("Production in progress • ${save.stageReadyDay-save.gameDay} days remaining. Use the game clock to advance.")
            fun advance(response: Response? = save.response) {
                if (!ready) return
                val updated = save.copy(stage = stage+1, response = response, stageReadyDay = save.gameDay + when(stage) { 0 -> 45; 1 -> 20; else -> 0 })
                if (store.write(updated)) { save = updated; audio.event("clapper"); error = "" } else error = "Save failed."
            }
            if (stage == 1 && p.event && save.response == null) {
                Text("Equipment failure • choose a response")
                Text("Compare the cash remaining after each response before committing.")
                Response.values().forEach { response ->
                    Button(enabled = ready && productionFundsAvailable(productionCash, p.plan.budget, payrollReserve, extra, response.cost) >= 0,
                        onClick = { advance(response) }) { Text("${response.name} • ₹${response.cost}L • Left ₹${productionFundsAvailable(productionCash,p.plan.budget,payrollReserve,extra,response.cost)}L") }
                }
            } else if (stage == 3) {
                val earliest = save.gameDay
                val year = earliest / 365
                val first = ReleaseSeasonPlanner.year(year,"Telugu",0)
                val second = ReleaseSeasonPlanner.year(year+1,"Telugu",0)
                ReleaseDatePicker(ReleaseMarketCalendar(first.holidays+second.holidays,first.releases+second.releases),earliest,"Telugu",p.plan.cast.appeal,100,save.releaseDay) { day ->
                    val updated = save.copy(releaseDay=day)
                    if (store.write(updated)) save=updated else error="Save failed."
                }
                Button(enabled = save.releaseDay?.let { it <= save.gameDay } == true, onClick = { finish(p,save.response) }) { Text(if(save.ottDeal?.directStreaming == true) "Release on OTT" else "Premiere movie") }
            } else {
                Button(enabled = ready, onClick = { advance() }) { Text("Complete ${stages[stage]}") }
            }
            if (stage == 0) TextButton(onClick = {
                val updated = save.copy(pending = null, stage = 0, response = null, crafts = emptyMap(), releaseDay = null, ottDeal = null, ottOffers = emptyList())
                if (store.write(updated)) { save = updated; pending = null } else error = "Save failed."
            }) { Text("Cancel uncommitted production") }
        } else {
            OutlinedTextField(title, { title = it }, label = { Text("Movie title") }, singleLine = true)
            Text("Genre")
            Genre.values().forEach { g -> FilterChip(genre == g, { genre = g }, { Text(g.name) }) }
            if (c.completed >= 3) {
                TextButton(onClick = { genrePopup = true }) { Text("Secondary genre: ${secondary?.name ?: "None"}") }
                if (genrePopup) AlertDialog(onDismissRequest = { genrePopup = false }, title = { Text("Blend genres") },
                    text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        TextButton(onClick = { secondary = null; genrePopup = false }) { Text("None") }
                        StoryGenre.values().filter { it.name != genre.name }.forEach { g ->
                            TextButton(onClick = { secondary = g; genrePopup = false }) { Text(g.name) }
                        }
                    } }, confirmButton = { TextButton(onClick = { genrePopup = false }) { Text("Close") } })
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    ProductionScale.values().forEach { s -> FilterChip(scale == s,{ scale = s },{ Text(s.name) }) }
                }
            }
            Text("Lead cast")
            Talent.values().forEach { t -> FilterChip(cast == t, { cast = t }, { Text("${t.name} • ₹${t.fee} lakh") }) }
            Text("Director")
            Director.values().forEach { d -> FilterChip(director == d, { director = d }, { Text("${d.name} • ₹${d.fee} lakh") }) }
            OutlinedTextField(budget, { budget = it.filter(Char::isDigit).take(8) }, label = { Text("Total budget (lakh)") }, singleLine = true)
            val rivalBid = StudioEvents.rivalBid(c.rivals,cast,save.gameDay,c.seed)
            val minimum = maxOf(cast.fee,rivalBid) + director.fee + 60
            if(rivalBid>cast.fee) Text("Rival talent offer: ₹$rivalBid L • Higher production budget required.")
            val payrollReserve = save.managers.sumOf { it.salary }
            val contingency = Response.values().minOf { it.cost }
            Text("Minimum budget: ₹$minimum lakh")
            Text("Payroll: ₹$payrollReserve lakh • Contingency: ₹$contingency lakh")
            Text("Loan and other film reserves: ₹${loanReserve + otherMovieReserve} lakh")
            Button(enabled = title.isNotBlank() && (budget.toIntOrNull() ?: 0) >= minimum && productionFundsAvailable(productionCash, budget.toIntOrNull() ?: 0, payrollReserve, contingency = contingency) >= 0,
                onClick = {
                    audio.event("clapper")
                    val primary = StoryGenre.valueOf(genre.name)
                    val hybrid = if (c.completed >= 3) HybridFilm(primary,secondary?.takeIf { it != primary },scale = scale) else null
                    val hired = hybrid?.genres?.flatMap { it.needs }?.associateWith { 1 } ?: emptyMap()
                    val production = EmpireEngine.start(c, FilmPlan(title.trim(), genre, cast, director, budget.toInt(),hybrid),
                        AssetEconomy(save.owned,mapOf("player" to c.cash)),startDay = save.gameDay,endDay = save.gameDay+80,hiredCapabilities = hired)
                    val updated = save.copy(pending = production, stage = 0, response = null, crafts = emptyMap(), releaseDay = null, stageReadyDay = save.gameDay+15, ottDeal = null, ottOffers = emptyList())
                    if (store.write(updated)) { save = updated; pending = production } else error = "Save failed."
                }) { Text("Greenlight movie") }
            if (c.cash < 115) Text("Insufficient capital for another film. Financing is not implemented yet.")
        }
        }
        if (tab == "Market" && c.completed < 2) Text("Industry reports unlock after two films.")
        if (tab == "Market" && c.completed >= 2) {
            Text("Industry market", style = MaterialTheme.typography.titleLarge)
            val market = EmpireMarket.round(c.seed, c.rivals)
            market.demand.entries.sortedByDescending { it.value }.forEach { (g, d) ->
                Text("${g.name}: ${(d * 100).toInt()}% demand")
            }
            c.rivals.sortedByDescending { it.cash }.forEach { s ->
                Text("${s.name} • ₹${s.cash} lakh • ${s.preferredGenre.name}")
                Text("Target release: day ${StudioEvents.targetDay(s,save.gameDay)+1}")
            }
            market.rivals.forEach { release -> Text("Upcoming rival release: ${release.studio} • ${release.genre.name}") }
        }
        if (tab == "Assets" && (pending != null || save.slots.movies.isNotEmpty())) { Text("Finish or cancel all productions before changing assets.") }
        if (tab == "Assets" && pending == null && save.slots.movies.isEmpty()) {
        Text("Theater business", style = MaterialTheme.typography.titleLarge)
        save.theaters.forEach { t ->
            Text("${t.id} • ${t.acquisition.name} • ${if(t.operating(save.gameDay)) "Open" else "Opens day ${t.openingDay}"}")
            OutlinedButton(onClick={
                val updated=save.copy(theaters=save.theaters.filterNot { it.id==t.id },career=c.copy(cash=Math.addExact(c.cash,StudioEvents.resale(t))))
                if(store.write(updated)) save=updated else error="Save failed."
            }) { Text("Sell / exit • ₹${StudioEvents.resale(t)}L") }
        }
        TheaterAcquisition.values().forEach { mode ->
            OutlinedButton(enabled=c.cash >= mode.upfront,onClick={
                val theater=StudioTheater("Cinema ${java.util.UUID.randomUUID().toString().take(8)}",mode,save.gameDay+mode.buildDays)
                val updated=save.copy(theaters=save.theaters+theater,career=c.copy(cash=c.cash-mode.upfront))
                if(store.write(updated)) save=updated else error="Save failed."
            }) { Text("${mode.name} theater • ₹${mode.upfront}L") }
        }
        Text("Celebrity relationships", style = MaterialTheme.typography.titleLarge)
        Talent.values().forEach { talent ->
            OutlinedButton(enabled=c.cash>=5,onClick={
                val updated=save.copy(relationships=save.relationships.gift(talent.name,5),career=c.copy(cash=c.cash-5))
                if(store.write(updated)) save=updated else error="Save failed."
            }) { Text("Gift ${talent.name} • ₹5L • Trust ${save.relationships.score(talent.name)}") }
        }
        Text("Asset marketplace", style = MaterialTheme.typography.titleLarge)
        val year = 2026 + c.completed / 4
        val discovered = setOf("digital4k","largeformat","ledstage") + save.research +
            EmpireTechnologyRoadmap.variants.filter { it.generation.year == 2026 }.map { it.technology.id }
        val industry = IndustryTechnology(year, discovered)
        Text("Industry year: $year")
        var category by remember { mutableStateOf("camera") }
        var page by remember(category) { mutableStateOf(0) }
        Text("Browse asset categories")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmpireTechnologyRoadmap.families.forEach { f -> FilterChip(category == f.id, { category = f.id }, { Text(f.id) }) }
        }
        val achievements = buildSet {
            if (c.completed >= 8 && save.history.count { it.quality >= 75 } >= 3) add("cinematography")
            if (c.completed >= 10 && save.history.count { it.quality >= 80 } >= 3) add("technical")
        }
        val eligibility = AssetEligibility(c.completed, achievements)
        val inventory = EmpireAssetCatalog.initialAssets(industry).filter { a -> save.owned.none { it.id == a.id } } + save.owned
        val economy = AssetEconomy(inventory, c.rivals.associate { it.name to it.cash } + ("player" to c.cash)).releaseBookings(save.gameDay)
        fun transact(action: () -> AssetEconomy) {
            try {
                val result = action()
                val updated = save.copy(career = c.copy(cash = result.balances.getValue("player"), rivals = c.rivals.map { it.copy(cash = result.balances[it.name] ?: it.cash) }),
                    owned = result.assets.filter { it.owner != null })
                if (store.write(updated)) { save = updated; error = ""; audio.event("purchase") } else error = "Save failed."
            } catch (e: Exception) { error = e.message ?: "Asset transaction failed." }
        }
        Text("Research and development")
        val candidates = EmpireTechnologyRoadmap.variants.filter { v ->
            v.category == category && v.technology.id !in discovered &&
                v.technology.earliestYear <= year && discovered.containsAll(v.technology.prerequisites)
        }
        candidates.take(5).forEach { v ->
            val cost = v.generation.crewTraining * 20
            val qualified = c.completed >= v.technology.requiredFilms &&
                achievements.containsAll(v.technology.requiredAchievements)
            Text(v.technology.name + " • Research ₹$cost lakh")
            Button(enabled = pending == null && save.slots.movies.isEmpty() && qualified && c.cash >= cost, onClick = {
                val result = EmpireTechnologyRoadmap.discover(industry,v.technology.id,v.generation.crewTraining*100)
                val updated = save.copy(career = c.copy(cash = c.cash-cost), research = save.research + (result.discovered-discovered))
                if (store.write(updated)) { save = updated; error = "" } else error = "Save failed."
            }) { Text("Develop technology") }
        }
        var auctionAsset by remember { mutableStateOf<StudioAsset?>(null) }
        var bidText by remember { mutableStateOf("") }
        auctionAsset?.let { asset ->
            val technology = EmpireAssetCatalog.allTechnologies.first { it.id == asset.technologyId }
            // Prototype rival experience follows industry rounds until individual careers are implemented.
            val bidders = listOf(AssetBidder("player",eligibility,0,emptySet())) +
                c.rivals.map { rival -> AssetBidder(rival.name,AssetEligibility(c.completed,achievements),100,
                    when(rival.preferredGenre) {
                        Genre.ACTION -> setOf("stunt","camera")
                        Genre.DRAMA -> setOf("sets","lighting")
                        Genre.COMEDY -> setOf("sound","studio")
                        Genre.THRILLER -> setOf("camera","post")
                        Genre.ROMANCE -> setOf("music","costume")
                    }) }
            val amount = bidText.toIntOrNull() ?: 0
            AlertDialog(onDismissRequest = { auctionAsset = null },
                title = { Text("Sealed auction • ${technology.name}") },
                text = { Column {
                    Text("Reserve ₹${technology.price} lakh • Rival bids are hidden")
                    OutlinedTextField(bidText,{ bidText = it.filter(Char::isDigit).take(8) },label = { Text("Your bid (lakh)") })
                } },
                confirmButton = { Button(enabled = amount in technology.price..c.cash && pending == null,
                    onClick = {
                        val bids = bidders.drop(1).mapNotNull { EmpireAssetCompetition.rivalBid(economy,asset,it,industry) } + AssetBid("player",amount)
                        val result = EmpireAssetCompetition.settle(economy,asset.id,bidders,bids,industry,technology.price)
                        transact { result.economy }
                        error = result.winner?.let { "Auction winner: ${it.studio} • ₹${it.amount} lakh" } ?: "No qualifying bids"
                        auctionAsset = null
                    }) { Text("Submit bid") } },
                dismissButton = { TextButton(onClick = { auctionAsset = null }) { Text("Cancel") } })
        }
        val listings = EmpireAssetCatalog.technologies.filter { it.id in industry.discovered } +
            EmpireTechnologyRoadmap.variants.filter { it.category == category && it.technology.id in discovered }.map { it.technology }
        Row {
            TextButton(enabled = page > 0, onClick = { page-- }) { Text("Previous") }
            TextButton(enabled = (page+1)*10 < listings.size, onClick = { page++ }) { Text("Next") }
        }
        listings.drop(page*10).take(10).forEach { t ->
            val available = inventory.filter { it.technologyId == t.id && it.owner == null }
            val eligible = AssetEconomy.eligible(t,eligibility,industry)
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(t.name)
                Text("₹${t.price} lakh • ${available.size} available")
                Text("Requires ${t.requiredFilms} films • ${t.requiredAchievements.joinToString().ifEmpty { "No extra achievement" }}")
                Button(enabled = pending == null && save.slots.movies.isEmpty() && eligible && c.cash >= t.price && available.isNotEmpty(),
                    onClick = { transact { economy.buy(available.first().id,"player",t.price,eligibility,industry) } }) { Text("Purchase") }
                Button(enabled = pending == null && save.slots.movies.isEmpty() && eligible && c.cash >= t.price && available.isNotEmpty(), onClick = { auctionAsset = available.first(); bidText = t.price.toString() }) { Text("Enter auction") }
            } }
        }
        Text("Owned assets: ${save.owned.count { it.owner == "player" }}")
        economy.assets.filter { it.owner == "player" }.forEach { a ->
            Text(EmpireAssetCatalog.allTechnologies.first { it.id == a.technologyId }.name + " • ₹${a.value} lakh • Debt ₹${a.debt} lakh")
            if (a.renter != null) { Text("Booked by ${a.renter} until day ${a.bookedUntil}") }
            else if (a.debt == 0) {
                val renter = c.rivals.filter { it.cash >= a.value/10+100 }.maxByOrNull { it.cash }
                if (renter != null) Button(enabled = pending == null && save.slots.movies.isEmpty(), onClick = {
                    transact { economy.rent(a.id,renter.name,save.gameDay,30,(a.value/10).coerceAtLeast(1)) }
                }) { Text("Rent to ${renter.name} • ₹${(a.value/10).coerceAtLeast(1)} lakh") }
                Button(enabled = pending == null && save.slots.movies.isEmpty() && a.value >= 2, onClick = {
                    transact { economy.mortgage(a.id,"player",a.value/2) }
                }) { Text("Borrow ₹${a.value/2} lakh") }
                Button(enabled = pending == null && save.slots.movies.isEmpty(), onClick = {
                    // Dealer buyback at a discount; returned stock is available for repurchase.
                    transact { economy.copy(assets = economy.assets.map { if (it.id == a.id) it.copy(owner = null) else it },
                        balances = economy.balances + ("player" to (c.cash + a.value*70/100))) }
                }) { Text("Sell to dealer ₹${a.value*70/100} lakh") }
            } else {
                Button(enabled = pending == null && save.slots.movies.isEmpty() && c.cash >= a.debt, onClick = {
                    transact { economy.repay(a.id,"player",a.debt) }
                }) { Text("Repay principal ₹${a.debt} lakh") }
            }
        }
        }
        if (tab == "Career" && (pending != null || save.slots.movies.isNotEmpty())) { Text("Finish or cancel all productions before changing studio finances or staff.") }
        if (tab == "Career" && pending == null && save.slots.movies.isEmpty()) {
        Text("Department managers",style=MaterialTheme.typography.titleLarge)
        if(c.completed<5) Text("Managers unlock after five films.")
        else Department.values().forEach { department ->
            val manager=save.managers.firstOrNull { it.department==department }
            if(manager==null) Button(enabled=pending==null && c.cash>=20,onClick={
                val updated=save.copy(career=c.copy(cash=c.cash-20),managers=save.managers+StudioManager(department.name,department,75,80,5))
                if(store.write(updated)) save=updated else error="Save failed."
            }) { Text("Hire ${department.name} • ₹20L") }
            else Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${department.name} • Skill ${manager.skill} • ₹${manager.salary}L/month",modifier=Modifier.weight(1f))
                TextButton(enabled=pending==null && c.cash>=manager.salary*2,onClick={
                    val (trained,cash)=EmpireManagers.train(manager,c.cash)
                    val updated=save.copy(career=c.copy(cash=cash),managers=save.managers.map { if(it.id==manager.id) trained else it })
                    if(store.write(updated)) save=updated else error="Save failed."
                }) { Text("Train") }
                TextButton(enabled=pending==null,onClick={
                    val updated=save.copy(managers=save.managers.filterNot { it.id==manager.id })
                    if(store.write(updated)) save=updated else error="Save failed."
                }) { Text("Dismiss") }
            }
        }
        Text("Studio financing",style=MaterialTheme.typography.titleLarge)
        Text("Outstanding: ₹${save.loans.sumOf { it.outstanding }} lakh • Unpaid interest: ₹${save.overdue} lakh")
        save.loans.forEach { loan -> Text("₹${loan.outstanding} lakh • ${loan.annualRateBps/100.0}% APR • ${loan.monthsRemaining} months") }
        if(c.completed>=3) Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(50,100,250).forEach { amount ->
                Button(enabled=pending==null && c.credibility>=40 && amount<=c.cash.toLong()*c.credibility/100-save.loans.sumOf { it.outstanding.toLong() },onClick={
                    val ledger=FinanceLedger(c.cash,save.loans,save.overdue).borrow(java.util.UUID.randomUUID().toString(),amount,12,c.credibility)
                    val updated=save.copy(career=c.copy(cash=ledger.cash),loans=ledger.loans)
                    if(store.write(updated)) save=updated else error="Save failed."
                }) { Text("₹$amount L") }
            }
        }
        Text("Talent agreements", style = MaterialTheme.typography.titleLarge)
        if (c.completed < 5) Text("Multi-film agreements unlock after five films.")
        else {
            Talent.values().forEach { talent ->
                val existing = save.contracts.firstOrNull { it.talent == talent.name && it.remainingFilms > 0 && it.expiresDay > save.gameDay }
                if (existing != null) Text("${talent.name}: ${existing.remainingFilms} films remaining")
                else Button(enabled = pending == null && save.slots.movies.isEmpty() && c.cash >= talent.fee/2, onClick = {
                    val agreement = TalentContract("contract-${c.completed}-${talent.name}",talent.name,"player",ContractType.MULTI_FILM,3,talent.fee*80/100,talent.fee/2,expiresDay=save.gameDay+360)
                    val ledger = ContractLedger(mapOf("player" to c.cash),save.contracts).sign(agreement,save.gameDay)
                    val updated = save.copy(career=c.copy(cash=ledger.cash.getValue("player")),contracts=ledger.contracts)
                    if(store.write(updated)) save=updated else error="Save failed."
                }) { Text("Sign ${talent.name} • 3 films • ₹${talent.fee/2} lakh advance") }
            }
        }
        Text("Career mastery tree", style = MaterialTheme.typography.titleLarge)
        Text("Build trust in any genre. Hits require quality 60+ and at least 20% profit on cost.")
        Genre.values().forEach { g ->
            val hits = c.genreHits[g] ?: 0
            val level = EmpireMastery.level(hits)
            val next = EmpireMastery.next(hits)
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("${g.name} • ${level.name} • +${level.trust}% demand")
                Text(Mastery.values().filter { it.hits > 0 }.joinToString(" → ") { m -> (if (hits >= m.hits) "✓ " else "") + m.hits + " hits" })
                LinearProgressIndicator(progress = (hits / (next?.hits ?: 15).toFloat()).coerceIn(0f,1f), modifier = Modifier.fillMaxWidth())
                Text(if (next == null) "Mastery complete" else "$hits / ${next.hits} hits toward ${next.name}")
            } }
        }
        Text("Upcoming streaming payments", style = MaterialTheme.typography.titleMedium)
        if(save.streamingPayments.isEmpty()) Text("No outstanding streaming payments")
        save.streamingPayments.sortedBy { it.dueDay }.forEach { Text("${it.film} • ${it.buyer} • ₹${it.amount}L • Day ${it.dueDay}") }
        Text("Studio history", style = MaterialTheme.typography.titleLarge)
        save.history.indices.reversed().forEach { index ->
            val film = save.history[index]
            Text("${film.title} • ₹${film.profit} lakh profit/loss")
            if(film.actualReleaseDay >= 0 && !film.ottSold) {
                val market = PostReleaseStreaming.offers(film,film.actualReleaseDay,save.gameDay,false)
                val round = (save.gameDay-film.actualReleaseDay-7)/14
                val offers = if(market.isEmpty()) emptyList() else if(film.ottRound==round && film.ottNegotiationJson.isNotEmpty()) decodeNegotiations(JSONArray(film.ottNegotiationJson)) else market.map { RightsNegotiation(it) }
                fun reviseOffer(updatedOffer: RightsNegotiation) {
                    val revised = film.copy(ottRound=round,ottNegotiationJson=encodeNegotiations(offers.map { if(it.offer.buyer==updatedOffer.offer.buyer) updatedOffer else it }).toString())
                    val updated = save.copy(history=save.history.mapIndexed { i,f -> if(i==index) revised else f })
                    if(store.write(updated)) save=updated else error="Save failed."
                }
                offers.filter { it.status==DealStatus.OPEN }.forEach { negotiation ->
                    val offer = negotiation.offer
                    OutlinedButton(onClick={ reviseOffer(PostReleaseStreaming.counter(negotiation,film,save.career.credibility,offers.count { it.status==DealStatus.OPEN }-1,save.gameDay-film.actualReleaseDay)) }) { Text("Counter +20%: ${offer.buyer}") }
                    OutlinedButton(onClick={ reviseOffer(negotiation.copy(status=DealStatus.WALKED_AWAY)) }) { Text("Reject: ${offer.buyer}") }
                    OutlinedButton(onClick = {
                        val current = save.history[index]
                        if(!current.ottSold) {
                            val payment = PostReleaseStreaming.payment(current,offer,current.actualReleaseDay,save.gameDay)
                            val revised = current.copy(ottSold=true,rights=Math.addExact(current.rights,payment.amount),revenue=Math.addExact(current.revenue,payment.amount))
                            val immediate = payment.dueDay <= save.gameDay
                            val updated = save.copy(accounts=save.accounts.record(payment.amount.toLong()),history=save.history.mapIndexed { i,f -> if(i==index) revised else f },
                                streamingPayments=save.streamingPayments + if(immediate) emptyList() else listOf(payment),
                                career=save.career.copy(cash=Math.addExact(save.career.cash,if(immediate) payment.amount else 0)))
                            if(store.write(updated)) save=updated else error="Save failed."
                        }
                    }) { Text("Sell OTT: ${offer.buyer} • ₹${offer.guaranteedLakh}L") }
                }
            }
        }
        }
        }
        }
    }
    }
}
}
