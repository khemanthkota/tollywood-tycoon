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

private data class Save(val career: Career = Career(seed = kotlin.random.Random.nextInt()), val history: List<FilmResult> = emptyList(), val pending: Production? = null, val owned: List<StudioAsset> = emptyList(), val research: Set<String> = emptySet(), val stage: Int = 0, val response: Response? = null, val crafts: Map<FilmCraft,CraftInvestment> = emptyMap())
private class CareerStore(context: Context) {
    private val prefs = context.getSharedPreferences("cinema_empire_v1", Context.MODE_PRIVATE)
    fun load(): Save {
        val raw = prefs.getString("career", null) ?: return Save()
        val j = JSONObject(raw)
        require(j.getInt("version") == 1)
        val a = j.getJSONArray("history")
        val history = (0 until a.length()).map {
            val f = a.getJSONObject(it)
            FilmResult(f.getString("title"), f.getInt("quality"), f.getInt("gross"), f.getInt("rights"), f.getInt("revenue"), f.getInt("cost"), f.getInt("delay"))
        }
        val rivals = j.optJSONArray("rivals")?.let { arr ->
            (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                RivalStudio(s.getString("name"), s.getInt("cash"), Genre.valueOf(s.getString("genre")), s.getInt("ambition"))
            }
        } ?: EmpireMarket.initialRivals
        val pending = j.optJSONObject("pending")?.let { p ->
            Production(FilmPlan(p.getString("title"), Genre.valueOf(p.getString("genre")), Talent.valueOf(p.getString("cast")), Director.valueOf(p.getString("director")), p.getInt("budget")), p.getBoolean("event"), p.getInt("seed"))
        }
        return Save(Career(j.getInt("cash"), j.getInt("completed"), j.getInt("credibility"), j.getInt("audience"), j.getInt("seed"), rivals, Genre.values().associateWith { g -> j.optJSONObject("genreHits")?.optInt(g.name, 0) ?: 0 }), history, pending, j.optJSONArray("owned")?.let { arr -> (0 until arr.length()).map { i -> val x = arr.getJSONObject(i); StudioAsset(x.getString("id"), x.getString("technology"), x.optString("owner","player"), x.getInt("value"), x.optInt("debt",0), x.optString("renter","").takeIf { it.isNotEmpty() }, x.optInt("bookedUntil",0), x.optBoolean("auction",false)) } } ?: emptyList(), j.optJSONArray("research")?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet(), j.optInt("stage",0), j.optString("response","").takeIf { it.isNotEmpty() }?.let { Response.valueOf(it) }, FilmCraft.values().mapNotNull { craft -> j.optJSONObject("crafts")?.optString(craft.name)?.takeIf { it.isNotEmpty() }?.let { craft to CraftInvestment.valueOf(it) } }.toMap())
    }
    fun write(s: Save): Boolean {
        val c = s.career
        val a = JSONArray()
        s.history.forEach { f ->
            a.put(JSONObject().put("title", f.title).put("quality", f.quality).put("gross", f.gross)
                .put("rights", f.rights).put("revenue", f.revenue).put("cost", f.cost).put("delay", f.delay))
        }
        val rivals = JSONArray()
        c.rivals.forEach { s -> rivals.put(JSONObject().put("name", s.name).put("cash", s.cash).put("genre", s.preferredGenre.name).put("ambition", s.ambition)) }
        val pending = s.pending?.let { p -> JSONObject().put("title", p.plan.title).put("genre", p.plan.genre.name)
            .put("cast", p.plan.cast.name).put("director", p.plan.director.name).put("budget", p.plan.budget)
            .put("event", p.event).put("seed", p.seed) }
        val owned = JSONArray()
        s.owned.forEach { x -> owned.put(JSONObject().put("id",x.id).put("technology",x.technologyId).put("value",x.value).put("debt",x.debt).put("owner",x.owner).put("renter",x.renter ?: "").put("bookedUntil",x.bookedUntil).put("auction",x.auction)) }
        return prefs.edit().putString("career", JSONObject().put("crafts",JSONObject().apply { s.crafts.forEach { (k,v) -> put(k.name,v.name) } }).put("stage",s.stage).put("response",s.response?.name ?: "").put("research",JSONArray(s.research.toList())).put("owned",owned).put("genreHits", JSONObject().apply { c.genreHits.forEach { (g, hits) -> put(g.name, hits) } }).put("pending", pending ?: JSONObject.NULL).put("rivals", rivals).put("version", 1).put("cash", c.cash)
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
    val c = save.career
    fun finish(p: Production, response: Response? = null) {
        try {
            val (next, film) = EmpireEngine.finish(c, p, response, save.crafts)
            val updated = save.copy(career = next, history = save.history + film, pending = null, stage = 0, response = null, crafts = emptyMap())
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
        Text("Producer's Life • Offline prototype")
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
            Text("Craft upgrades: ₹$extra lakh")
            EmpireProductionCrafts.available(c.completed, CraftStage.values()[stage]).forEach { craft ->
                Text(craft.name.replace('_',' '))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CraftInvestment.values().forEach { investment ->
                        val choices = save.crafts + (craft to investment)
                        val total = p.plan.budget + EmpireProductionCrafts.assess(choices).extraCost + (save.response?.cost ?: 0)
                        FilterChip(selected = save.crafts[craft] == investment,
                            enabled = total <= c.cash,
                            onClick = {
                                val updated = save.copy(crafts = choices)
                                if (store.write(updated)) { save = updated; audio.cue() } else error = "Save failed."
                            }, label = { Text("${investment.name} +₹${investment.cost}") })
                    }
                }
            }
            fun advance(response: Response? = save.response) {
                val updated = save.copy(stage = stage+1, response = response)
                if (store.write(updated)) { save = updated; audio.event("clapper"); error = "" } else error = "Save failed."
            }
            if (stage == 1 && p.event && save.response == null) {
                Text("Equipment failure • choose a response")
                Response.values().forEach { response ->
                    Button(enabled = p.plan.budget + extra + response.cost <= c.cash,
                        onClick = { advance(response) }) { Text("${response.name} • ₹${response.cost} lakh") }
                }
            } else if (stage == 3) {
                Button(onClick = { finish(p,save.response) }) { Text("Premiere movie") }
            } else {
                Button(onClick = { advance() }) { Text("Complete ${stages[stage]}") }
            }
            if (stage == 0) TextButton(onClick = {
                val updated = save.copy(pending = null, stage = 0, response = null, crafts = emptyMap())
                if (store.write(updated)) { save = updated; pending = null } else error = "Save failed."
            }) { Text("Cancel uncommitted production") }
        } else {
            OutlinedTextField(title, { title = it }, label = { Text("Movie title") }, singleLine = true)
            Text("Genre")
            Genre.values().forEach { g -> FilterChip(genre == g, { genre = g }, { Text(g.name) }) }
            Text("Lead cast")
            Talent.values().forEach { t -> FilterChip(cast == t, { cast = t }, { Text("${t.name} • ₹${t.fee} lakh") }) }
            Text("Director")
            Director.values().forEach { d -> FilterChip(director == d, { director = d }, { Text("${d.name} • ₹${d.fee} lakh") }) }
            OutlinedTextField(budget, { budget = it.filter(Char::isDigit).take(8) }, label = { Text("Total budget (lakh)") }, singleLine = true)
            val minimum = cast.fee + director.fee + 60
            Text("Minimum budget: ₹$minimum lakh")
            Button(enabled = title.isNotBlank() && (budget.toIntOrNull() ?: 0) in minimum..c.cash,
                onClick = {
                    audio.event("clapper")
                    val production = EmpireEngine.start(c, FilmPlan(title.trim(), genre, cast, director, budget.toInt()))
                    val updated = save.copy(pending = production, stage = 0, response = null, crafts = emptyMap())
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
            }
            market.rivals.forEach { release -> Text("Upcoming rival release: ${release.studio} • ${release.genre.name}") }
        }
        if (tab == "Assets") {
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
        val economy = AssetEconomy(inventory, c.rivals.associate { it.name to it.cash } + ("player" to c.cash)).releaseBookings(c.completed*30)
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
            Button(enabled = pending == null && qualified && c.cash >= cost, onClick = {
                val result = EmpireTechnologyRoadmap.discover(industry,v.technology.id,v.generation.crewTraining*100)
                val updated = save.copy(career = c.copy(cash = c.cash-cost), research = save.research + (result.discovered-discovered))
                if (store.write(updated)) { save = updated; error = "" } else error = "Save failed."
            }) { Text("Develop technology") }
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
                Button(enabled = pending == null && eligible && c.cash >= t.price && available.isNotEmpty(),
                    onClick = { transact { economy.buy(available.first().id,"player",t.price,eligibility,industry) } }) { Text("Purchase") }
            } }
        }
        Text("Owned assets: ${save.owned.count { it.owner == "player" }}")
        economy.assets.filter { it.owner == "player" }.forEach { a ->
            Text(EmpireAssetCatalog.allTechnologies.first { it.id == a.technologyId }.name + " • ₹${a.value} lakh • Debt ₹${a.debt} lakh")
            if (a.renter != null) { Text("Booked by ${a.renter} until day ${a.bookedUntil}") }
            else if (a.debt == 0) {
                val renter = c.rivals.filter { it.cash >= a.value/10+100 }.maxByOrNull { it.cash }
                if (renter != null) Button(enabled = pending == null, onClick = {
                    transact { economy.rent(a.id,renter.name,c.completed*30,30,(a.value/10).coerceAtLeast(1)) }
                }) { Text("Rent to ${renter.name} • ₹${(a.value/10).coerceAtLeast(1)} lakh") }
                Button(enabled = pending == null && a.value >= 2, onClick = {
                    transact { economy.mortgage(a.id,"player",a.value/2) }
                }) { Text("Borrow ₹${a.value/2} lakh") }
                Button(enabled = pending == null, onClick = {
                    // Dealer buyback at a discount; returned stock is available for repurchase.
                    transact { economy.copy(assets = economy.assets.map { if (it.id == a.id) it.copy(owner = null) else it },
                        balances = economy.balances + ("player" to (c.cash + a.value*70/100))) }
                }) { Text("Sell to dealer ₹${a.value*70/100} lakh") }
            } else {
                Button(enabled = pending == null && c.cash >= a.debt, onClick = {
                    transact { economy.repay(a.id,"player",a.debt) }
                }) { Text("Repay principal ₹${a.debt} lakh") }
            }
        }
        }
        if (tab == "Career") {
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
        Text("Studio history", style = MaterialTheme.typography.titleLarge)
        save.history.asReversed().forEach { Text("${it.title} • ₹${it.profit} lakh profit/loss") }
        }
        }
        }
    }
    }
}
}
