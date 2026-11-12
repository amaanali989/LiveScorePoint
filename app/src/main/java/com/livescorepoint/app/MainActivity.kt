package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

val Background = Color(0xFF080D18)
val CardColor = Color(0xFF151F30)
val SkyBlue = Color(0xFF39BDF8)
val White = Color(0xFFFFFFFF)
val Muted = Color(0xFF9AA8BA)
val Dark = Color(0xFF080D18)

data class CricketMatch(
    val name: String,
    val status: String,
    val score: String,
    val venue: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswers: Set<String>,
    val explanation: String,
    val type: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var selectedTab by remember { mutableStateOf("Home") }

    val sports = listOf(
        "Cricket",
        "Football",
        "Hockey",
        "Kabaddi",
        "eSports",
        "F1",
        "Olympics",
        "Wrestling"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                "LiveScorePoint",
                color = White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(18.dp)
            )

            Text(
                "SPORTS",
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sports.take(4).forEach { sport ->
                    SportItem(
                        sport = sport,
                        selected = selectedSport == sport,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSport = sport
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sports.drop(4).forEach { sport ->
                    SportItem(
                        sport = sport,
                        selected = selectedSport == sport,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedSport = sport
                    }
                }
            }

            when (selectedTab) {
                "Home", "Live" -> MatchList()
                "Quiz" -> QuizScreen(selectedSport)
                "Stats" -> SimpleSection("Stats", "Sports statistics will appear here.")
                else -> SimpleSection("More", "More features coming soon.")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Home", "Live", "Quiz", "Stats", "More").forEach { tab ->
                Text(
                    text = tab,
                    color = if (selectedTab == tab) SkyBlue else Muted,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == tab) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    modifier = Modifier.clickable {
                        selectedTab = tab
                    }
                )
            }
        }
    }
}

@Composable
fun SportItem(
    sport: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) SkyBlue else CardColor,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            sport,
            color = if (selected) Dark else White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SimpleSection(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        Text(title, color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(message, color = Muted, fontSize = 14.sp)
    }
}

@Composable
fun MatchList() {
    var matches by remember { mutableStateOf<List<CricketMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            matches = getMatches()
            error = false
        } catch (e: Exception) {
            error = true
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            "Live Matches",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            loading -> Text("Loading matches...", color = Muted)
            error -> Text("Matches load nahi ho paaye. Baad mein try karein.", color = Muted)
            matches.isEmpty() -> Text("Abhi koi live match nahi hai.", color = Muted)
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matches) { match ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(match.name, color = White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(match.score, color = SkyBlue, fontSize = 14.sp)
                        Text(match.status, color = Muted, fontSize = 12.sp)
                        Text(match.venue, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
@Composable
fun QuizScreen(sport: String) {
    val questions = remember(sport) { quizQuestionsFor(sport) }

    var questionIndex by remember(sport) { mutableIntStateOf(0) }
    var selectedAnswers by remember(sport) {
        mutableStateOf(setOf<String>())
    }
    var submitted by remember(sport) { mutableStateOf(false) }
    var points by remember { mutableIntStateOf(0) }

    val question = questions[questionIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Text(
            "$sport Quiz",
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Question ${questionIndex + 1} of ${questions.size}",
            color = Muted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                question.type,
                color = SkyBlue,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                question.question,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            question.options.forEach { option ->
                val selected = option in selectedAnswers

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            if (selected) SkyBlue.copy(alpha = 0.18f)
                            else Background,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable(enabled = !submitted) {
                            selectedAnswers =
                                if (question.type == "Multiple Select") {
                                    if (selected) {
                                        selectedAnswers - option
                                    } else {
                                        selectedAnswers + option
                                    }
                                } else {
                                    setOf(option)
                                }
                        }
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selected) "●" else "○",
                        color = SkyBlue
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        option,
                        color = White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (submitted) {
                val correct =
                    question.correctAnswers.isNotEmpty() &&
                    selectedAnswers == question.correctAnswers

                Text(
                    if (correct) "Correct answer!" else "Answer submitted",
                    color = if (correct) SkyBlue else Muted,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    question.explanation,
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (!submitted) {
                        if (selectedAnswers.isNotEmpty()) {
                            if (
                                question.correctAnswers.isNotEmpty() &&
                                selectedAnswers == question.correctAnswers
                            ) {
                                points += 10
                            }
                            submitted = true
                        }
                    } else {
                        if (questionIndex == questions.lastIndex) {
                            questionIndex = 0
                            points = 0
                        } else {
                            questionIndex++
                        }
                        selectedAnswers = emptySet()
                        submitted = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBlue,
                    contentColor = Dark
                )
            ) {
                Text(
                    if (submitted) {
                        if (questionIndex == questions.lastIndex) {
                            "Play Again"
                        } else {
                            "Next Question"
                        }
                    } else {
                        "Submit Answer"
                    }
                )
            }

            if (!submitted) {
                TextButton(
                    onClick = {
                        questionIndex =
                            if (questionIndex == questions.lastIndex) 0
                            else questionIndex + 1
                        selectedAnswers = emptySet()
                        submitted = false
                    }
                ) {
                    Text("Skip Question", color = Muted)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                "Leaderboard",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text("Demo leaderboard", color = Muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("1. Player One       120 pts", color = White, fontSize = 13.sp)
            Text("2. Player Two       100 pts", color = White, fontSize = 13.sp)
            Text("3. You                    $points pts", color = SkyBlue, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Points are for fun only. No cash rewards or paid entry.",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

fun quizQuestionsFor(sport: String): List<QuizQuestion> {
    val options = when (sport) {
        "Cricket" -> listOf("Yes", "No", "True", "False")
        "Football" -> listOf("Home team", "Away team", "Draw")
        "Hockey" -> listOf("Team A", "Team B", "Draw")
        "Kabaddi" -> listOf("Team A", "Team B", "Tie")
        "eSports" -> listOf("Team A", "Team B", "Not sure")
        "F1" -> listOf("Driver A", "Driver B", "Another driver")
        "Olympics" -> listOf("Country A", "Country B", "Another country")
        else -> listOf("Wrestler A", "Wrestler B", "Not sure")
    }

    return listOf(
        QuizQuestion(
            "$sport: Select the correct answer.",
            listOf("Option A", "Option B", "Option C", "Option D"),
            setOf("Option A"),
            "This is a demo question.",
            "Multiple Choice"
        ),
        QuizQuestion(
            "$sport: Is this statement correct?",
            listOf("Yes", "No"),
            setOf("Yes"),
            "This is a demo Yes/No question.",
            "Yes / No"
        ),
        QuizQuestion(
            "$sport: Is this statement true?",
            listOf("True", "False"),
            setOf("True"),
            "This is a demo True/False question.",
            "True / False"
        ),
        QuizQuestion(
            "$sport: Select all correct options.",
            listOf("Option A", "Option B", "Option C"),
            setOf("Option A", "Option B"),
            "This is a demo Multiple Select question.",
            "Multiple Select"
        ),
        QuizQuestion(
            "$sport: Who will win the next event?",
            options,
            emptySet(),
            "Prediction saved in this demo.",
            "Prediction"
        )
    )
}

suspend fun getMatches(): List<CricketMatch> =
    withContext(Dispatchers.IO) {
        val connection = URL(
            "https://lsp-cricket-api.uddinamaan619.workers.dev/currentMatches"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)
            val data = json.optJSONArray("data")
                ?: return@withContext emptyList()

            val result = mutableListOf<CricketMatch>()

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val scoreArray = item.optJSONArray("score")
                val scoreText = StringBuilder()

                if (scoreArray != null) {
                    for (j in 0 until scoreArray.length()) {
                        val scoreItem = scoreArray.optJSONObject(j)
                        if (scoreItem != null) {
                            if (scoreText.isNotEmpty()) {
                                scoreText.append(" | ")
                            }
                            scoreText.append(scoreItem.optString("inning", ""))
                            scoreText.append(": ")
                            scoreText.append(scoreItem.optString("r", "0"))
                            scoreText.append("/")
                            scoreText.append(scoreItem.optString("w", "0"))
                            scoreText.append(" (")
                            scoreText.append(scoreItem.optString("o", "0"))
                            scoreText.append(" ov)")
                        }
                    }
                }

                result.add(
                    CricketMatch(
                        name = item.optString("name", "Cricket Match"),
                        status = item.optString("status", ""),
                        score = scoreText.toString(),
                        venue = item.optString("venue", "")
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }