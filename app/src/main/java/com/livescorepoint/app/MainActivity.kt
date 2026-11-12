
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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

private val SkyBlue = Color(0xFF29B6F6)
private val Dark = Color(0xFF080F18)
private val CardColor = Color(0xFF142231)
private val Muted = Color(0xFFAAB8C5)
private val White = Color(0xFFF4F7FA)

data class CricketMatch(
    val name: String,
    val status: String,
    val score: String,
    val venue: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answers: Set<String>,
    val explanation: String,
    val type: String
)

private val sports = listOf(
    "🏏 Cricket",
    "⚽ Football",
    "🏑 Hockey",
    "🤼 Kabaddi",
    "🎮 eSports",
    "🏎️ F1",
    "🏅 Olympics",
    "🤼 Wrestling"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveScorePointApp()
        }
    }
}

@Composable
fun LiveScorePointApp() {
    var selectedPage by remember { mutableStateOf("Home") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SkyBlue,
            background = Dark,
            surface = CardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Dark)
        ) {
            when (selectedPage) {
                "Home" -> HomeScreen()
                "Live" -> LiveScreen()
                "Quiz" -> QuizScreen()
                "Stats" -> SimplePage(
                    "Stats",
                    "Match statistics yahan show honge."
                )
                else -> SimplePage(
                    "More",
                    "More features coming soon."
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardColor)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Home", "Live", "Quiz", "Stats", "More")
                    .forEach { page ->
                        Column(
                            modifier = Modifier
                                .clickable { selectedPage = page }
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                page,
                                color = if (selectedPage == page)
                                    SkyBlue else Muted,
                                fontSize = 13.sp,
                                fontWeight = if (selectedPage == page)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            "LiveScorePoint",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Every sport. Every moment.",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SportsRow() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            sports.forEach { sport ->
                Column(
                    modifier = Modifier
                        .width(76.dp)
                        .background(
                            CardColor,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 11.dp, horizontal = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        sport.substringBefore(" "),
                        fontSize = 19.sp
                    )
                    Text(
                        sport.substringAfter(" "),
                        color = White,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf("Live") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader()
        SportsRow()

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Live", "Upcoming", "Results").forEach { tab ->
                val selected = selectedTab == tab
                Text(
                    tab,
                    color = if (selected) Dark else Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(
                            if (selected) SkyBlue else CardColor,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 17.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            "Live" -> MatchList()
            "Upcoming" -> EmptyMessage(
                "Upcoming matches",
                "Upcoming match data abhi connect nahi hai."
            )
            "Results" -> EmptyMessage(
                "Recent results",
                "Results data abhi connect nahi hai."
            )
        }
    }
}

@Composable
fun LiveScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader()
        SportsRow()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "Live Matches",
            color = White,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        MatchList()
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
        }
        loading = false
    }

    when {
        loading -> Text(
            "Loading live matches...",
            color = Muted,
            modifier = Modifier.padding(20.dp)
        )

        matches.isEmpty() -> EmptyMessage(
            "No live matches",
            if (error) "Live data load nahi ho saka. Thodi der baad try karein."
            else "Abhi koi live cricket match available nahi hai."
        )

        else -> {
            matches.forEach { match ->
                MatchCard(match)
            }
        }
    }
}

@Composable
fun MatchCard(match: CricketMatch) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .background(CardColor, RoundedCornerShape(14.dp))
            .padding(15.dp)
    ) {
        Text(
            "● LIVE",
            color = SkyBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            match.name,
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            match.score.ifBlank { "Score unavailable" },
            color = SkyBlue,
            fontSize = 14.sp
        )
        Text(
            match.status,
            color = Muted,
            fontSize = 12.sp
        )
        if (match.venue.isNotBlank()) {
            Text(
                match.venue,
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun EmptyMessage(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
            .background(CardColor, RoundedCornerShape(13.dp))
            .padding(18.dp)
    ) {
        Text(
            title,
            color = White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            message,
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SimplePage(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader()
        Text(
            title,
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(18.dp)
        )
        EmptyMessage(title, message)
    }
}

@Composable
fun QuizScreen() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var questionIndex by remember { mutableIntStateOf(0) }
    var points by remember { mutableIntStateOf(0) }
    var submitted by remember { mutableStateOf(false) }
    var skipped by remember { mutableStateOf(false) }

    val questions = remember(selectedSport) {
        quizQuestionsFor(selectedSport)
    }

    val question = questions[
        questionIndex.coerceIn(0, questions.lastIndex)
    ]

    val selectedAnswers = remember(selectedSport, questionIndex) {
        mutableStateListOf<String>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader()

        Text(
            "Sports Quiz",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            "Play. Answer. Earn points.",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 4.dp,
                bottom = 14.dp
            )
        )

        Text(
            "Choose a sport",
            color = White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sports.forEach { item ->
                val name = item.substringAfter(" ")
                Text(
                    name,
                    color = if (selectedSport == name) Dark else White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            if (selectedSport == name) SkyBlue else CardColor,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable {
                            selectedSport = name
                            questionIndex = 0
                            points = 0
                            submitted = false
                            skipped = false
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(CardColor, RoundedCornerShape(15.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Daily Quiz",
                    color = SkyBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Points: $points",
                    color = White,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Question ${questionIndex + 1} of ${questions.size}",
                color = Muted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                question.question,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                question.type,
                color = SkyBlue,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (question.type == "Prediction") {
                Text(
                    "Apna prediction select karein:",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            question.options.forEach { option ->
                val isSelected = selectedAnswers.contains(option)
                val isCorrect = question.answers.contains(option)

                val backgroundColor = when {
                    submitted && isCorrect -> Color(0xFF174B3A)
                    submitted && isSelected && !isCorrect -> Color(0xFF59302F)
                    isSelected -> Color(0xFF164C6A)
                    else -> Dark
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            backgroundColor,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable(
                            enabled = !submitted
                        ) {
                            if (question.type == "Multiple Select") {
                                if (selectedAnswers.contains(option)) {
                                    selectedAnswers.remove(option)
                                } else {
                                    selectedAnswers.add(option)
                                }
                            } else {
                                selectedAnswers.clear()
                                selectedAnswers.add(option)
                            }
                        }
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isSelected) "●" else "○",
                        color = SkyBlue,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        option,
                        color = White,
                        fontSize = 13.sp
                    )
                }
            }

            if (submitted) {
                Spacer(modifier = Modifier.height(12.dp))

                val correct = question.type != "Prediction" &&
                    selectedAnswers.toSet() == question.answers

                Text(
                    when {
                        question.type == "Prediction" ->
                            "Prediction saved in this demo."
                        correct -> "Correct answer!"
                        else -> "Not quite. Keep learning!"
                    },
                    color = if (correct) Color(0xFF65D6A3) else SkyBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    question.explanation,
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            if (!submitted && !skipped) {
                Button(
                    onClick = {
                        if (selectedAnswers.isNotEmpty()) {
                            if (
                                question.type != "Prediction" &&
                                selectedAnswers.toSet() == question.answers
                            ) {
                                points += 10
                            }
                            submitted = true
                        }
                    },
                    enabled = selectedAnswers.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyBlue,
                        contentColor = Dark
                    )
                ) {
                    Text("Submit Answer")
                }

                TextButton(
                    onClick = {
                        skipped = true
                        submitted = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Skip question", color = Muted)
                }
            } else {
                Button(
                    onClick = {
                        if (questionIndex < questions.lastIndex) {
                            questionIndex++
                            submitted = false
                            skipped = false
                        } else {
                            questionIndex = 0
                            submitted = false
                            skipped = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyBlue,
                        contentColor = Dark
                    )
                ) {
                    Text(
                        if (questionIndex == questions.lastIndex)
              