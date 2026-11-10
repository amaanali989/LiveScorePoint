package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    var selectedSport by remember { mutableStateOf("All") }

    val sports = listOf(
        "Cricket", "Football", "Hockey", "F1",
        "Kabaddi", "Esports", "Olympics", "Asian Games"
    )

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf("Home", "Matches", "Tournaments", "Quiz", "Settings")
                        .forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Text("●") },
                                label = { Text(tab) }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LIVE SCORE POINT",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text("Every Game. Every Point.")
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { darkMode = it }
                        )
                    }
                }

                item {
                    if (selectedTab == "Home") {
                        Text("MATCH CENTRE", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Live", "Upcoming", "Results").forEach {
                                FilterChip(
                                    selected = selectedSport == it,
                                    onClick = { selectedSport = it },
                                    label = { Text(it) }
                                )
                            }
                        }
                    } else {
                        Text(selectedTab, style = MaterialTheme.typography.headlineSmall)
                    }
                }

                if (selectedTab == "Home" || selectedTab == "Matches") {
                    item {
                        Text("SPORTS", style = MaterialTheme.typography.titleLarge)
                    }

                    items(sports) { sport ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSport = sport }
                        ) {
                            Text(
                                sport,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    item {
                        Text("FEATURED MATCHES", style = MaterialTheme.typography.titleLarge)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Cricket")
                                Text("Team A  vs  Team B")
                                Text("Match information will appear here")
                                Text("LIVE DATA COMING SOON")
                            }
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Football")
                                Text("Team A  vs  Team B")
                                Text("Match information will appear here")
                                Text("LIVE DATA COMING SOON")
                            }
                        }
                    }
                }

                if (selectedTab == "Quiz") {
                    item {
                        Text("QUIZ ZONE", style = MaterialTheme.typography.headlineMedium)
                        Text("Play quizzes. Earn points. Climb the leaderboard.")
                        Text("Scoring System", style = MaterialTheme.typography.titleLarge)
                        Text("Points & Leaderboard: 60%")
                        Text("Achievements: 20%")
                        Text("Accuracy: 20%")
                        Button(onClick = {}) {
                            Text("Start Quiz")
                        }
                    }
                }

                if (selectedTab == "Tournaments") {
                    item {
                        Text("TOURNAMENTS")
                        Text("Cricket, Football, Olympics and Asian Games")
                    }
                }

                if (selectedTab == "Settings") {
                    item {
                        Text("SETTINGS")
                        Text("Dark / Light Theme")
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { darkMode = it }
                        )
                    }
                }
            }
        }
    }
}
