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

private data class Save(val career: Career = Career(seed = kotlin.random.Random.nextInt()), val history: List<FilmResult> = emptyList())
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
        return Save(Career(j.getInt("cash"), j.getInt("completed"), j.getInt("credibility"), j.getInt("audience"), j.getInt("seed")), history)
    }
    fun write(s: Save): Boolean {
        val c = s.career
        val a = JSONArray()
        s.history.forEach { f ->
            a.put(JSONObject().put("title", f.title).put("quality", f.quality).put("gross", f.gross)
                .put("rights", f.rights).put("revenue", f.revenue).put("cost", f.cost).put("delay", f.delay))
        }
        return prefs.edit().putString("career", JSONObject().put("version", 1).put("cash", c.cash)
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
    var pending by remember { mutableStateOf<Production?>(null) }
    var error by remember { mutableStateOf("") }
    val c = save.career
    fun finish(p: Production, response: Response? = null) {
        try {
            val (next, film) = EmpireEngine.finish(c, p, response)
            val updated = Save(next, save.history + film)
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
            TextButton(onClick = { pending = null }) { Text("Cancel uncommitted production") }
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
                    if (production.event) pending = production else finish(production)
                }) { Text("Produce and release movie") }
            if (c.cash < 115) Text("Insufficient capital for another film. Financing is not implemented yet.")
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("Studio history", style = MaterialTheme.typography.titleLarge)
        save.history.asReversed().forEach { Text("${it.title} • ₹${it.profit} lakh profit/loss") }
    }
}
