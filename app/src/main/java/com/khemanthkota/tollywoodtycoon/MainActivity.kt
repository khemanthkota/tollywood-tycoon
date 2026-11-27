package com.khemanthkota.tollywoodtycoon

import android.content.Intent
import android.os.Bundle
import org.json.JSONArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.khemanthkota.tollywoodtycoon.ui.theme.TollywoodTycoonTheme

private data class TriviaQuestion(
    val id: String,
    val industry: String,
    val prompt: String,
    val options: List<String>,
    val correct: Int
)

private fun loadBetaQuestions(context: android.content.Context): List<TriviaQuestion> = try {
    val text = context.assets.open("trivia-beta-18000.json").bufferedReader().use { it.readText() }
    val json = JSONArray(text)
    (0 until json.length()).mapNotNull { i ->
        val item = json.getJSONObject(i)
        val choices = item.getJSONArray("options")
        val options = (0 until choices.length()).map { choices.getString(it) }
        val answerIndex = options.indexOf(item.getString("answer"))
        if (options.size != 4 || answerIndex < 0) null else TriviaQuestion(
            item.getString("id"), item.optString("industry", "Mixed"),
            item.getString("prompt"), options, answerIndex
        )
    }
} catch (_: Exception) { starterQuestions }


// Offline starter set. The bulk draft dataset is NOT yet shipped in this app.
private val starterQuestions = listOf(
    TriviaQuestion("starter-001", "Tollywood", "Who directed the Telugu film Eega (2012)?", listOf("Sukumar", "S. S. Rajamouli", "Trivikram Srinivas", "Koratala Siva"), 1),
    TriviaQuestion("starter-002", "Hollywood", "Which film won the first Academy Award for Best Animated Feature?", listOf("Toy Story", "Shrek", "Finding Nemo", "Monsters, Inc."), 1),
    TriviaQuestion("starter-003", "Bollywood", "Who directed the Hindi film Lagaan (2001)?", listOf("Ashutosh Gowariker", "Karan Johar", "Sanjay Leela Bhansali", "Farhan Akhtar"), 0),
    TriviaQuestion("starter-004", "Kollywood", "Who composed the music for Roja (1992)?", listOf("Ilaiyaraaja", "Harris Jayaraj", "A. R. Rahman", "Yuvan Shankar Raja"), 2),
    TriviaQuestion("starter-005", "Mollywood", "Who directed the Malayalam film Drishyam (2013)?", listOf("Jeethu Joseph", "Priyadarshan", "Lal Jose", "Sathyan Anthikad"), 0),
    TriviaQuestion("starter-006", "Sandalwood", "Who directed the Kannada film Kantara (2022)?", listOf("Prashanth Neel", "Rakshit Shetty", "Rishab Shetty", "Pawan Kumar"), 2)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TollywoodTycoonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    var mode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("home") }
                    when (mode) {
                        "empire" -> com.khemanthkota.tollywoodtycoon.empire.EmpireScreen({ mode = "home" }, Modifier.padding(padding))
                        "trivia" -> {
                            androidx.activity.compose.BackHandler { mode = "home" }
                            TriviaGame(Modifier.padding(padding))
                        }
                        else -> Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Cinema Tycoon", style = MaterialTheme.typography.headlineMedium)
                            Button(onClick = { mode = "empire" }) { Text("Cinema Empire – Producer's Life") }
                            Button(onClick = { mode = "trivia" }) { Text("Trivia") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriviaGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bank = remember { loadBetaQuestions(context) }
    var industry by remember { mutableStateOf("All") }
    var questions by remember { mutableStateOf(bank.shuffled()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var reportOpen by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Incorrect answer") }
    var reportNotes by remember { mutableStateOf("") }
    var started by remember { mutableStateOf(false) }

    fun startGame() {
        questions = bank.filter { industry == "All" || it.industry == industry }.shuffled().take(10)
        index = 0
        selected = -1
        score = 0
        started = true
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Cinema Tycoon • Trivia", style = MaterialTheme.typography.headlineMedium)
        Text("Offline beta • ${bank.size} questions • unverified", style = MaterialTheme.typography.labelMedium)
        if (!started) {
            Text("Choose an industry", style = MaterialTheme.typography.titleMedium)
            val industries = listOf("All") + bank.map { it.industry }.distinct().sorted()
            industries.forEach { choice ->
                FilterChip(selected = industry == choice, onClick = { industry = choice }, label = { Text(choice) })
            }
            Button(onClick = { startGame() }, modifier = Modifier.fillMaxWidth()) { Text("Start trivia") }
        } else if (index >= questions.size) {
            Text("Game complete!", style = MaterialTheme.typography.headlineSmall)
            Text("Score: $score / ${questions.size}")
            Button(onClick = { started = false }, modifier = Modifier.fillMaxWidth()) { Text("Play again") }
        } else {
            val q = questions[index]
            Text("Question ${index + 1} of ${questions.size} • ${q.industry}")
            LinearProgressIndicator(progress = { (index + 1).toFloat() / questions.size }, modifier = Modifier.fillMaxWidth())
            Text(q.prompt, style = MaterialTheme.typography.titleLarge)
            q.options.forEachIndexed { optionIndex, option ->
                OutlinedButton(
                    onClick = {
                        if (selected == -1) {
                            selected = optionIndex
                            if (optionIndex == q.correct) score++
                        }
                    },
                    enabled = selected == -1,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("${'A' + optionIndex}. $option") }
            }
            if (selected != -1) {
                Text(if (selected == q.correct) "Correct!" else "Correct answer: ${q.options[q.correct]}")
                Text("Score: $score")
                Button(onClick = { selected = -1; index++ }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (index == questions.lastIndex) "See results" else "Next question")
                }
            }
            TextButton(onClick = { reportOpen = true }) { Text("Report question") }

            if (reportOpen) {
                AlertDialog(
                    onDismissRequest = { reportOpen = false },
                    title = { Text("Report question") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Incorrect answer", "Multiple correct answers", "Confusing wording", "Other").forEach { reason ->
                                Row {
                                    RadioButton(selected = reportReason == reason, onClick = { reportReason = reason })
                                    TextButton(onClick = { reportReason = reason }) { Text(reason) }
                                }
                            }
                            OutlinedTextField(value = reportNotes, onValueChange = { reportNotes = it }, label = { Text("Details (optional)") })
                            Text("Reports are shared through another app; they are not uploaded automatically.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val message = "Cinema Tycoon question report\nID: ${q.id}\nIndustry: ${q.industry}\nQuestion: ${q.prompt}\nOptions: ${q.options.joinToString(" | ")}\nExpected: ${q.options[q.correct]}\nReason: $reportReason\nNotes: $reportNotes"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Cinema Tycoon trivia report: ${q.id}")
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(send, "Send question report"))
                            reportOpen = false
                            reportNotes = ""
                        }) { Text("Share report") }
                    },
                    dismissButton = { TextButton(onClick = { reportOpen = false }) { Text("Cancel") } }
                )
            }
        }
    }
}
