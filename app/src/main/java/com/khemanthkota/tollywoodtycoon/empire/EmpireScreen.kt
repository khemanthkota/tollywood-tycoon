package com.khemanthkota.tollywoodtycoon.empire

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import org.json.JSONArray

private data class Save(val career: Career = Career(seed = kotlin.random.Random.nextInt()), val history: List<FilmResult> = emptyList(), val pending: Production? = null, val owned: List<StudioAsset> = emptyList())
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
        return Save(Career(j.getInt("cash"), j.getInt("completed"), j.getInt("credibility"), j.getInt("audience"), j.getInt("seed"), rivals, Genre.values().associateWith { g -> j.optJSONObject("genreHits")?.optInt(g.name, 0) ?: 0 }), history, pending, j.optJSONArray("owned")?.let { arr -> (0 until arr.length()).map { i -> val x = arr.getJSONObject(i); StudioAsset(x.getString("id"), x.getString("technology"), "player", x.getInt("value"), x.optInt("debt",0)) } } ?: emptyList())
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
        s.owned.forEach { x -> owned.put(JSONObject().put("id",x.id).put("technology",x.technologyId).put("value",x.value).put("debt",x.debt)) }
        return prefs.edit().putString("career", JSONObject().put("owned",owned).put("genreHits", JSONObject().apply { c.genreHits.forEach { (g, hits) -> put(g.name, hits) } }).put("pending", pending ?: JSONObject.NULL).put("rivals", rivals).put("version", 1).put("cash", c.cash)
            .put("completed", c.completed).put("credibility", c.credibility).put("audience", c.audience)
            .put("seed", c.seed).put("history", a).toString()).commit()
    }
}

@Composable
fun EmpireScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { CareerStore(context) }
    val loaded = remember { runCatching { store.load() } }
    var save by remember { mutableStateOf(loaded.getOrNull() ?: Save()) }
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(Genre.ACTION) }
    var cast by remember { mutableStateOf(Talent.NEWCOMER) }
    var director by remember { mutableStateOf(Director.DEBUT) }
    var budget by remember { mutableStateOf("200") }
    var pending by remember { mutableStateOf<Production?>(save.pending) }
    var error by remember { mutableStateOf("") }
    val c = save.career
    fun finish(p: Production, response: Response? = null) {
        try {
            val (next, film) = EmpireEngine.finish(c, p, response)
            val updated = save.copy(career = next, history = save.history + film, pending = null)
            if (!store.write(updated)) { error = "Save failed. Please retry."; return }
            save = updated
            pending = null
            title = ""
            error = ""
        } catch (e: Exception) { error = e.message ?: "Cannot complete film." }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Back to games") }
        Text("Cinema Empire", style = MaterialTheme.typography.headlineMedium)
        Text("Producer's Life • Offline prototype")
        if (loaded.isFailure) {
            Text("Career save could not be read. Existing data has not been overwritten.")
            return@Column
        }
        Text("Cash: ₹${c.cash} lakh | Films: ${c.completed}")
        Text("Credibility: ${c.credibility} | Audience: ${c.audience} | Level: ${c.unlocked}")
        val last = save.history.lastOrNull()
        if (last != null) {
            Card { Column(Modifier.padding(12.dp)) {
                Text(last.title, style = MaterialTheme.typography.titleLarge)
                Text("Quality: ${last.quality}/100 | Box office: ₹${last.gross} lakh")
                Text("Producer revenue: ₹${last.revenue} lakh | Cost: ₹${last.cost} lakh")
                Text("Profit/loss: ₹${last.profit} lakh | Delay: ${last.delay} days")
            } }
        }
        val p = pending
        if (p != null) {
            Text("Equipment failure! Choose a response.")
            Response.values().forEach { response ->
                Button(enabled = p.plan.budget + response.cost <= c.cash,
                    onClick = { finish(p, response) }) { Text("${response.name} • ₹${response.cost} lakh") }
            }
            TextButton(onClick = { val updated = save.copy(pending = null); if (store.write(updated)) { save = updated; pending = null } else error = "Save failed." }) { Text("Cancel uncommitted production") }
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
                    val production = EmpireEngine.start(c, FilmPlan(title.trim(), genre, cast, director, budget.toInt()))
                    if (production.event) {
                        val updated = save.copy(pending = production)
                        if (store.write(updated)) { save = updated; pending = production } else error = "Save failed."
                    } else finish(production)
                }) { Text("Produce and release movie") }
            if (c.cash < 115) Text("Insufficient capital for another film. Financing is not implemented yet.")
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        if (c.completed >= 2) {
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
        Text("Asset marketplace", style = MaterialTheme.typography.titleLarge)
        val year = 2026 + c.completed / 4
        // Temporary calendar-based discovery for the prototype; research UI is not yet connected.
        val discovered = setOf("digital4k","largeformat","ledstage") +
            EmpireTechnologyRoadmap.variants.filter { it.technology.earliestYear <= year }.map { it.technology.id }
        val industry = IndustryTechnology(year, discovered)
        Text("Industry year: $year")
        var category by remember { mutableStateOf("camera") }
        var page by remember(category) { mutableStateOf(0) }
        Text("Browse asset categories")
        EmpireTechnologyRoadmap.families.forEach { f ->
            FilterChip(category == f.id, { category = f.id }, { Text(f.id) })
        }
        val achievements = buildSet {
            if (c.completed >= 8 && save.history.count { it.quality >= 75 } >= 3) add("cinematography")
            if (c.completed >= 10 && save.history.count { it.quality >= 80 } >= 3) add("technical")
        }
        val eligibility = AssetEligibility(c.completed, achievements)
        val inventory = EmpireAssetCatalog.initialAssets(industry).filter { a -> save.owned.none { it.id == a.id } } + save.owned
        val economy = AssetEconomy(inventory, mapOf("player" to c.cash))
        fun transact(action: () -> AssetEconomy) {
            try {
                val result = action()
                val updated = save.copy(career = c.copy(cash = result.balances.getValue("player")),
                    owned = result.assets.filter { it.owner == "player" })
                if (store.write(updated)) { save = updated; error = "" } else error = "Save failed."
            } catch (e: Exception) { error = e.message ?: "Asset transaction failed." }
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
        Text("Owned assets: ${save.owned.size}")
        save.owned.forEach { a ->
            Text(EmpireAssetCatalog.allTechnologies.first { it.id == a.technologyId }.name + " • ₹${a.value} lakh • Debt ₹${a.debt} lakh")
            if (a.debt == 0) {
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
        Text("Career mastery tree", style = MaterialTheme.typography.titleLarge)
        Text("Build trust in any genre. Hits require quality 60+ and at least 20% profit on cost.")
        Genre.values().forEach { g ->
            val hits = c.genreHits[g] ?: 0
            val level = EmpireMastery.level(hits)
            val next = EmpireMastery.next(hits)
            Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("${g.name} • ${level.name} • +${level.trust}% demand")
                Text(Mastery.values().filter { it.hits > 0 }.joinToString(" → ") { m -> (if (hits >= m.hits) "✓ " else "") + m.hits + " hits" })
                Text(if (next == null) "Mastery complete" else "$hits / ${next.hits} hits toward ${next.name}")
            } }
        }
        Text("Studio history", style = MaterialTheme.typography.titleLarge)
        save.history.asReversed().forEach { Text("${it.title} • ₹${it.profit} lakh profit/loss") }
    }
}
