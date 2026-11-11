
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

private val Green = Color(0xFF36D69A)
private val Blue = Color(0xFF3987FF)
private val Dark = Color(0xFF0B1220)
private val DarkCard = Color(0xFF172338)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiveScorePointApp() }
    }
}

@Composable
fun LiveScorePointApp() {
    var darkMode by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSport by remember { mutableStateOf("Cricket") }

    val bg = if (darkMode) Dark else Color(0xFFF3F6FA)
    val card = if (darkMode) DarkCard else Color.White
    val fg = if (darkMode) Color.White else Color(0xFF172338)

    MaterialTheme {
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = card) {
                    listOf("Home", "Matches", "Quiz", "Profile").forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            icon = {},
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LSP", color = Green, fontSize = 29.sp, fontWeight = FontWeight.Black)
                            Text("LIVE SCORE POINT", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(if (darkMode) "☀️" else "🌙", fontSize = 23.sp,
                            modifier = Modifier.clickable { darkMode = !darkMode })
                    }
                }
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Blue, RoundedCornerShape(18.dp)).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("SPORTS, STATS & QUIZ", color = Color.White, fontSize = 12.sp)
                        Text("Every Match. Every Detail.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Follow your favourite sports", color = Color.White)
                    }
                }
                item {
                    Text(
                        listOf("Sports", "Matches", "Quiz & Rewards", "Your Profile")[selectedTab],
                        color = fg, fontSize = 21.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (selectedTab == 0) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                listOf("🏏" to "Cricket", "⚽" to "Football"),
                                listOf("🏑" to "Hockey", "🏎️" to "Formula 1"),
                                listOf("🤼" to "Kabaddi", "🎮" to "Esports"),
                                listOf("🏅" to "Olympics", "🌏" to "Asian Games")
                            ).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { (emoji, name) ->
                                        SportTile(emoji, name, fg, Modifier.weight(1f)) { selectedSport = name }
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectedTab == 0 || selectedTab == 1) {
                    item {
                        Panel(card, fg) {
                            Text("$selectedSport Match Centre", color = Green, fontWeight = FontWeight.Bold)
                            Text("Select a sport to view its match centre.", color = fg)
                            if (selectedSport == "Cricket") CricketCentre(fg, card)
                            else Text("Match data will appear after a verified sports data feed is connected.", color = fg)
                        }
                    }
                }
                if (selectedTab == 0 || selectedTab == 2) {
                    item {
                        Panel(card, fg) {
                            Text("Quiz & Rewards", color = Green, fontWeight = FontWeight.Bold)
                            Text("Points & Leaderboards — 60%", color = fg)
                            Text("Achievements — 20%", color = fg)
                            Text("Quiz Accuracy — 20%", color = fg)
                            Text("Quiz opens 24 hours before a match. Cricket quiz closes at toss; other sports at match start.", color = fg)
                        }
                    }
                }
                if (selectedTab == 0 || selectedTab == 3) {
                    item { Panel(card, fg) {
                        Text("Your Profile", color = Green, fontWeight = FontWeight.Bold)
                        Text("Favourite sports and achievements will appear here.", color = fg)
                    } }
                }
                if (selectedTab == 0) {
                    item { Panel(card, fg) {
                        Text("Advertisement Space", color = Green, fontWeight = FontWeight.Bold)
                        Text("AdMob setup and ad units are still pending.", color = fg)
                    } }
                }
                item {
                    Text("LIVE SCORE POINT • 2026", color = fg.copy(alpha = 0.5f),
                        fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp))
                }
            }
        }
    }
}

@Composable
private fun SportTile(emoji: String, name: String, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.background(Blue.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick).padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 26.sp)
        Text(name, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun Panel(background: Color, fg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(background, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp), content = content
    )
}

@Composable
private fun CricketCentre(fg: Color, card: Color) {
    var tab by remember { mutableStateOf("Overview") }
    var format by remember { mutableStateOf("T20") }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Overview", "Stats", "Scorecard", "Commentary").forEach {
            FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) })
        }
    }
    if (tab == "Stats") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("T20", "ODI", "Test").forEach {
                FilterChip(selected = format == it, onClick = { format = it }, label = { Text(it) })
            }
        }
        Text("RUNS & DOT BALLS", color = Green, fontWeight = FontWeight.Bold)
        if (format == "Test") {
            Text("Session-wise breakdown", color = fg, fontWeight = FontWeight.SemiBold)
            listOf("Session 1", "Session 2", "Session 3").forEach {
                StatRow(it, "Runs — • Wickets — • Overs —", fg)
            }
        } else {
            val phases = if (format == "T20")
                listOf("Powerplay (1–6)", "Middle overs (7–15)", "Death overs (16–20)")
            else listOf("Powerplay 1 (1–10)", "Middle overs (11–40)", "Powerplay 2 (41–50)")
            phases.forEach { phase ->
                Text(phase, color = fg, fontWeight = FontWeight.SemiBold)
                StatRow("Runs", "—", fg)
                StatRow("Dot balls", "—", fg)
                StatRow("Overs", "—", fg)
            }
        }
        Divider(color = fg.copy(alpha = 0.2f))
        Text("PLAYER & TEAM DOT BALLS", color = Green, fontWeight = FontWeight.Bold)
        StatRow("Batter-wise dot balls", "—", fg)
        StatRow("Bowler-wise dot balls", "—", fg)
        StatRow("Team total dot balls", "—", fg)
        Divider(color = fg.copy(alpha = 0.2f))
        Text("RUNS BREAKDOWN", color = Green, fontWeight = FontWeight.Bold)
        listOf("Batter runs", "Bowler runs conceded", "Singles (1)", "Doubles (2)", "Triples (3)",
            "Fours (4)", "Sixes (6)", "Extras", "Team total runs").forEach { StatRow(it, "—", fg) }
        Divider(color = fg.copy(alpha = 0.2f))
        Text("BOWLING & BATTING", color = Green, fontWeight = FontWeight.Bold)
        listOf("Strike rate", "Balls faced", "Overs bowled", "Maidens", "Economy", "Wickets").forEach {
            StatRow(it, "—", fg)
        }
        Text("— = match feed connect hone ke baad data aayega. Abhi koi live data connected nahi hai.",
            color = fg.copy(alpha = 0.7f), fontSize = 11.sp)
    } else if (tab == "Scorecard") {
        Text("Batting scorecard", color = Green, fontWeight = FontWeight.Bold)
        listOf("Batter • Runs • Balls • 4s • 6s • SR", "Bowler • Overs • Runs • Wickets • Econ", "Extras • Total").forEach {
            Text(it, color = fg)
        }
    } else if (tab == "Commentary") {
        Text("Ball-by-ball commentary", color = Green, fontWeight = FontWeight.Bold)
        Text("Commentary and fall of wickets will appear when a live data feed is connected.", color = fg)
    } else {
        Text("Cricket overview", color = Green, fontWeight = FontWeight.Bold)
        Text("Live score, teams, innings, run rate and match status will appear here after API integration.", color = fg)
    }
}

@Composable
private fun StatRow(label: String, value: String, fg: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = fg, fontSize = 13.sp)
        Text(value, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
