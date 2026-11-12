
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LiveScorePointApp()
            }
        }
    }
}

@Composable
fun LiveScorePointApp() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var selectedTab by remember { mutableStateOf("Live") }
    var matches by remember { mutableStateOf<List<CricketMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = ""
        try {
            matches = getMatches()
        } catch (e: Exception) {
            error = "Matches load nahi ho paaye. Refresh karke dekhein."
        }
        loading = false
    }

    val sports = listOf(
        "🏏" to "Cricket",
        "⚽" to "Football",
        "🏑" to "Hockey",
        "🤼" to "Kabaddi",
        "🎮" to "eSports",
        "🏎️" to "F1",
        "🏅" to "Olympics",
        "🥋" to "Wrestling"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dark)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LIVESCOREPOINT",
                    color = White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Every Match. Every Detail.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Text(
                "LSP",
                color = SkyBlue,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "SPORTS",
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sports.forEach { sport ->
                val selected = selectedSport == sport.second
                Column(
                    modifier = Modifier
                        .width(76.dp)
                        .background(
                            if (selected) CardColor else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSport = sport.second }
                        .padding(vertical = 9.dp, horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(sport.first, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        sport.second,
                        color = if (selected) SkyBlue else Muted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Live", "Upcoming", "Results").forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) SkyBlue else CardColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                ) {
                    Text(
                        tab,
                        color = if (selected) Dark else Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "↻",
                color = SkyBlue,
                fontSize = 22.sp,
                modifier = Modifier.clickable { refreshKey++ }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            if (selectedTab == "Live") "LIVE MATCHES" else "${selectedTab.uppercase()} MATCHES",
            color = White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, bottom = 12.dp)
        )

        when {
            selectedSport != "Cricket" -> {
                EmptyMessage(
                    "${selectedSport} scores coming soon",
                    "Is sport ka live data agle updates mein add hoga."
                )
            }

            selectedTab != "Live" -> {
                EmptyMessage(
                    "$selectedTab coming soon",
                    "Abhi cricket ke live matches connected hain."
                )
            }

            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(35.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SkyBlue)
                }
            }

            error.isNotEmpty() -> {
                EmptyMessage("Unable to load matches", error)
                TextButton(onClick = { refreshKey++ }) {
                    Text("Try again", color = SkyBlue)
                }
            }

            matches.isEmpty() -> {
                EmptyMessage(
                    "No live matches",
                    "Filhaal koi live cricket match nahi mila."
                )
            }

            else -> {
                matches.forEach { match ->
                    MatchCard(match)
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))
        HorizontalDivider(color = CardColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Home", "Live", "Stats", "More").forEach { item ->
                Text(
                    item,
                    color = if (item == "Home") SkyBlue else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MatchCard(match: CricketMatch) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                match.name,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                match.score.ifBlank { "Score not available" },
                color = SkyBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(match.status, color = Muted, fontSize = 12.sp)

            if (match.venue.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(match.venue, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun EmptyMessage(title: String, message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                title,
                color = White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(message, color = Muted, fontSize = 12.sp)
        }
    }
}

suspend fun getMatches(): List<CricketMatch> = withContext(Dispatchers.IO) {
    val url = URL("https://lsp-cricket-api.uddinamaan619.workers.dev/currentMatches")
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val data = json.optJSONArray("data") ?: return@withContext emptyList()

        val result = mutableListOf<CricketMatch>()

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val scores = item.optJSONArray("score")
            val scoreText = StringBuilder()

            if (scores != null) {
                for (j in 0 until scores.length()) {
                    val score = scores.optJSONObject(j) ?: continue
                    if (scoreText.isNotEmpty()) scoreText.append("  |  ")
                    scoreText.append(score.optString("inning", ""))
                    scoreText.append(": ")
                    scoreText.append(score.optString("r", ""))
                    scoreText.append("/")
                    scoreText.append(score.optString("w", ""))
                    scoreText.append(" (")
                    scoreText.append(score.optString("o", ""))
                    scoreText.append(" ov)")
                }
            }

            result.add(
                CricketMatch(
                    name = item.optString("name", "Cricket Match"),
                    status = item.optString("status", "Live"),
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
