package com.livescorepoint.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Green = Color(0xFF36D69A)
private val Dark = Color(0xFF0B1220)
private val Card = Color(0xFF172338)

data class CricketMatch(
    val name: String,
    val status: String,
    val score: String,
    val venue: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private fun getMatches(callback: (List<CricketMatch>, String?) -> Unit) {
    Thread {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(
                "https://lsp-cricket-api.uddinamaan619.workers.dev/currentMatches"
            ).openConnection() as HttpURLConnection

            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"

            val code = connection.responseCode
            val stream = if (code in 200..299)
                connection.inputStream else connection.errorStream

            val response = stream.bufferedReader().use { it.readText() }

            if (code !in 200..299) {
                throw Exception("Server error: $code")
            }

            val json = JSONObject(response)
            val data = json.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<CricketMatch>()

            for (i in 0 until data.length()) {
                val match = data.optJSONObject(i) ?: continue
                val scores = match.optJSONArray("score") ?: JSONArray()

                val scoreText = (0 until scores.length()).mapNotNull { index ->
                    val s = scores.optJSONObject(index)
                        ?: return@mapNotNull null

                    val runs = s.optString("r", "")
                    if (runs.isBlank()) null
                    else "${s.optString("inning")}: $runs/${s.optString("w")} (${s.optString("o")} ov)"
                }.joinToString("\n")

                list.add(
                    CricketMatch(
                        match.optString("name", "Cricket Match"),
                        match.optString("status", "Status unavailable"),
                        scoreText,
                        match.optString("venue", "")
                    )
                )
            }

            Handler(Looper.getMainLooper()).post {
                callback(list, null)
            }

        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                callback(emptyList(), e.message ?: "Connection error")
            }
        } finally {
            connection?.disconnect()
        }
    }.start()
}

@Composable
fun App() {
    var matches by remember { mutableStateOf<List<CricketMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("Matches") }

    fun refresh() {
        loading = true
        error = null
        getMatches { result, problem ->
            matches = result
            error = problem
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    MaterialTheme {
        Scaffold(
            containerColor = Dark,
            bottomBar = {
                NavigationBar(containerColor = Card) {
                    listOf("Home", "Matches", "Quiz", "Profile").forEach {
                        NavigationBarItem(
                            selected = tab == it,
                            onClick = { tab = it },
                            icon = {},
                            label = { Text(it) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "LSP",
                        color = Green,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "LIVE SCORE POINT",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                item {
                    Text(
                        "Every Match. Every Detail.",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "CRICKET MATCHES",
                            color = Green,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { refresh() }) {
                            Text("Refresh")
                        }
                    }
                }

                if (loading) {
                    item {
                        CircularProgressIndicator(color = Green)
                        Text("Loading matches...", color = Color.White)
                    }
                }

                if (error != null) {
                    item {
                        Text("Error: $error", color = Color.White)
                        TextButton(onClick = { refresh() }) {
                            Text("Try Again")
                        }
                    }
                }

                if (!loading && error == null && matches.isEmpty()) {
                    item {
                        Text(
                            "No matches received from API.",
                            color = Color.White
                        )
                    }
                }

                items(matches) { match ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Card, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            match.name,
                            color = Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(match.status, color = Color.White)
                        if (match.score.isNotBlank()) {
                            Text(match.score, color = Color.White)
                        }
                        if (match.venue.isNotBlank()) {
                            Text(
                                match.venue,
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Text(
                        "LIVE SCORE POINT • 2026",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}