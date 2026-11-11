
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
    var selectedTab by remember { mutableIntStateOf(0) }

    val bg = if (darkMode) Color(0xFF0B1220) else Color(0xFFF3F6FA)
    val card = if (darkMode) Color(0xFF172338) else Color.White
    val text = if (darkMode) Color.White else Color(0xFF172338)
    val green = Color(0xFF36D69A)
    val blue = Color(0xFF3987FF)

    MaterialTheme {
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = card) {
                    val tabs = listOf("Home", "Matches", "Quiz", "Profile")
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {},
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LSP",
                                color = green,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "LIVE SCORE POINT",
                                color = text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            if (darkMode) "☀️" else "🌙",
                            fontSize = 24.sp,
                            modifier = Modifier.clickable {
                                darkMode = !darkMode
                            }
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(blue, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            "SPORTS, STATS & QUIZ",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            "Every Match.\nEvery Detail.",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Follow your favourite sports",
                            color = Color.White
                        )
                    }
                }

                item {
                    when (selectedTab) {
                        0 -> SectionTitle("Sports", text)
                        1 -> SectionTitle("Matches", text)
                        2 -> SectionTitle("Quiz & Rewards", text)
                        else -> SectionTitle("Your Profile", text)
                    }
                }

                if (selectedTab == 0) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SportCard("🏏", "Cricket", green, Modifier.weight(1f))
                                SportCard("⚽", "Football", blue, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SportCard("🏑", "Hockey", blue, Modifier.weight(1f))
                                SportCard("🏎️", "Formula 1", green, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SportCard("🤼", "Kabaddi", green, Modifier.weight(1f))
                                SportCard("🎮", "Esports", blue, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SportCard("🏅", "Olympics", blue, Modifier.weight(1f))
                                SportCard("🌏", "Asian Games", green, Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (selectedTab == 0 || selectedTab == 1) {
                    item {
                        InfoCard(card, text) {
                            Text(
                                "Match Centre",
                                color = green,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Live match data will appear here after API integration.",
                                color = text
                            )
                        }
                    }
                }

                if (selectedTab == 0 || selectedTab == 2) {
                    item {
                        InfoCard(card, text) {
                            Text("🏆 Points & Leaderboards — 60%", color = text)
                            Text("⭐ Achievements — 20%", color = text)
                            Text("🎯 Quiz Accuracy — 20%", color = text)
                            Text(
                                "Quiz opens 24 hours before a match.",
                                color = green
                            )
                        }
                    }
                }

                if (selectedTab == 0 || selectedTab == 3) {
                    item {
                        InfoCard(card, text) {
                            Text("Your Profile", color = green)
                            Text("Favourite sports and achievements will appear here.", color = text)
                        }
                    }
                }

                if (selectedTab == 0) {
                    item {
                        InfoCard(card, text) {
                            Text("Advertisement Space", color = green)
                            Text(
                                "AdMob integration will be added in a later update.",
                                color = text
                            )
                        }
                    }
                }

                item {
                    Text(
                        "LIVE SCORE POINT • 2026",
                        color = text.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, color: Color) {
    Text(
        title,
        color = color,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SportCard(
    emoji: String,
    name: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 27.sp)
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
fun InfoCard(
    background: Color,
    textColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}
