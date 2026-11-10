
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var darkMode by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("Home") }

    val bg = if (darkMode) Color(0xFF101820) else Color(0xFFF3F5F7)
    val card = if (darkMode) Color(0xFF1D2A35) else Color.White
    val txt = if (darkMode) Color.White else Color(0xFF17212B)
    val accent = Color(0xFF00C896)

    MaterialTheme {
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = card) {
                    listOf("Home", "Matches", "Tournaments", "Quiz", "Settings")
                        .forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {},
                                label = { Text(tab, fontSize = 10.sp) }
                            )
                        }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "LIVE SCORE POINT",
                        color = accent,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Every Game. Every Moment.",
                        color = txt,
                        fontSize = 13.sp
                    )
                }

                item {
                    when (selectedTab) {
                        "Home" -> {
                            Text("Sports", color = txt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            val sports = listOf(
                                "🏏 Cricket", "⚽ Football",
                                "🏑 Hockey", "🏎️ Formula 1",
                                "🤼 Kabaddi", "🎮 Esports",
                                "🏅 Olympics", "🌏 Asian Games"
                            )
                            sports.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { sport ->
                                        SportCard(sport, card, txt, Modifier.weight(1f))
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }

                        "Matches" -> {
                            Text("Matches", color = txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            MatchCard(card, txt, accent, "India", "Australia", "Match data coming soon")
                        }

                        "Tournaments" -> {
                            Text("Tournaments", color = txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            SportCard("Cricket Tournaments", card, txt)
                            SportCard("Football Tournaments", card, txt)
                            SportCard("Olympics & Asian Games", card, txt)
                        }

                        "Quiz" -> {
                            Text("Quiz Zone", color = txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Compete. Earn Points. Climb the Leaderboard.", color = accent)
                            ScoreCard(card, txt, "Points + Leaderboards", "60%")
                            ScoreCard(card, txt, "Achievements", "20%")
                            ScoreCard(card, txt, "Accuracy", "20%")
                            Text("Quizzes coming soon", color = txt)
                        }

                        "Settings" -> {
                            Text("Settings", color = txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dark Theme", color = txt, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = darkMode,
                                    onCheckedChange = { darkMode = it }
                                )
                            }
                            Text("Live Score Point v1.0", color = txt)
                        }
                    }
                }

                if (selectedTab == "Home") {
                    item {
                        Text("Featured Match", color = txt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        MatchCard(card, txt, accent, "Team A", "Team B", "Demo scoreboard")
                    }
                }
            }
        }
    }
}

@Composable
fun SportCard(
    name: String,
    card: Color,
    txt: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        modifier = modifier.padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = card),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            name,
            color = txt,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MatchCard(
    card: Color,
    txt: Color,
    accent: Color,
    team1: String,
    team2: String,
    status: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = card),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(status, color = accent, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text("$team1  VS  $team2", color = txt, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Live scores will appear here", color = txt, fontSize = 12.sp)
        }
    }
}

@Composable
fun ScoreCard(card: Color, txt: Color, title: String, percentage: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = card),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = txt, modifier = Modifier.weight(1f))
            Text(percentage, color = Color(0xFF00C896), fontWeight = FontWeight.Bold)
        }
    }
}
