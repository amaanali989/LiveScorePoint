
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE =
    "https://lsp-cricket-api.uddinamaan619.workers.dev"

private val Background = Color(0xFF080D18)
private val CardColor = Color(0xFF121C2D)
private val SkyBlue = Color(0xFF38BDF8)
private val White = Color(0xFFFFFFFF)
private val Muted = Color(0xFF94A3B8)
private val Green = Color(0xFF22C55E)

data class Match(
    val home: String,
    val away: String,
    val homeScore: String = "",
    val awayScore: String = "",
    val status: String = "",
    val time: String = "",
    val competition: String = "",
    val venue: String = "",
    val sport: String = "Cricket"
)

private fun jsonString(
    obj: JSONObject,
    vararg keys: String
): String {
    for (key in keys) {
        val value = obj.optString(key, "")
        if (value.isNotBlank() && value != "null") return value
    }
    return ""
}

private fun parseMatches(
    json: String,
    sport: String
): List<Match> {
    val root = JSONObject(json)
    val array = root.optJSONArray("matches")
        ?: root.optJSONArray("data")
        ?: JSONArray()

    val result = mutableListOf<Match>()

    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue

        val home = jsonString(
            item, "home", "home_team", "homeTeam"
        )
        val away = jsonString(
            item, "away", "away_team", "awayTeam"
        )

        if (home.isBlank() || away.isBlank()) continue

        val homeScore = jsonString(
            item, "home_score", "homeScore", "score_home"
        )
        val awayScore = jsonString(
            item, "away_score", "awayScore", "score_away"
        )

        result.add(
            Match(
                home = home,
                away = away,
                homeScore = homeScore,
                awayScore = awayScore,
                status = jsonString(
                    item, "status", "match_status", "state"
                ),
                time = jsonString(
                    item, "time", "date", "start_time"
                ),
                competition = jsonString(
                    item, "competition", "league", "tournament"
                ),
                venue = jsonString(
                    item, "venue", "stadium"
                ),
                sport = sport
            )
        )
    }

    return result
}

private suspend fun fetchMatches(
    endpoint: String,
    sport: String
): List<Match> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null

    try {
        connection = URL("$API_BASE/$endpoint")
            .openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty(
            "Accept", "application/json"
        )

        val responseCode = connection.responseCode

        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader().use {
            it.readText()
        }

        if (responseCode !in 200..299) {
            throw Exception("Server error: $responseCode")
        }

        parseMatches(response, sport)

    } finally {
        connection?.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.rgb(8, 13, 24)

        window.navigationBarColor =
            android.graphics.Color.rgb(8, 13, 24)

        setContent {
            LiveScorePointTheme {
                LiveScorePointApp()
            }
        }
    }
}

@Composable
fun LiveScorePointTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SkyBlue,
            secondary = SkyBlue,
            background = Background,
            surface = CardColor,
            onPrimary = Background,
            onBackground = White,
            onSurface = White
        ),
        content = content
    )
}

@Composable
fun LiveScorePointApp() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var selectedTab by remember { mutableStateOf("Home") }
    var selectedFilter by remember { mutableStateOf("Live") }
    var selectedMatch by remember { mutableStateOf<Match?>(null) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = CardColor) {
                listOf("Home", "Live", "Quiz", "Stats", "More")
                    .forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                selectedMatch = null
                            },
                            icon = {},
                            label = { Text(tab) }
                        )
                    }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            if (selectedMatch != null) {
                MatchCentre(
                    match = selectedMatch!!,
                    onBack = { selectedMatch = null }
                )
            } else {
                Header()

                when (selectedTab) {
                    "Home", "Live" -> {
                        SportSelector(
                            selectedSport,
                            onSelect = {
                                selectedSport = it
                                selectedFilter = "Live"
                            }
                        )

                        FilterSelector(
                            selectedFilter,
                            onSelect = { selectedFilter = it }
                        )

                        if (selectedSport == "Cricket" ||
                            selectedSport == "Football") {
                            MatchList(
                                sport = selectedSport,
                                filter = selectedFilter,
                                onMatchClick = {
                                    selectedMatch = it
                                }
                            )
                        } else {
                            ComingSoon()
                        }
                    }

                    "Quiz" -> SimplePage(
                        "Sports Quiz",
                        "Quiz feature is coming soon"
                    )

                    "Stats" -> SimplePage(
                        "Statistics",
                        "Detailed sports statistics coming soon"
                    )

                    else -> SimplePage(
                        "More",
                        "More features coming soon"
                    )
                }
            }
        }
    }
}

@Composable
fun Header() {
    Column(
        Modifier.fillMaxWidth().padding(18.dp)
    ) {
        Text(
            "LiveScorePoint",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "ALL SPORTS • ONE PLACE",
            color = Muted,
            fontSize = 11.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(7.dp)
                    .background(Green, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(6.dp))
            Text("LIVE SPORTS", color = Green, fontSize = 11.sp)
        }
    }
}

@Composable
fun SportSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val sports = listOf(
        "Cricket", "Football", "Hockey",
        "Kabaddi", "eSports", "F1", "Olympics"
    )

    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sports.forEach { sport ->
            FilterChip(
                selected = selected == sport,
                onClick = { onSelect(sport) },
                label = { Text(sport) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBlue,
                    selectedLabelColor = Background,
                    labelColor = White
                )
            )
        }
    }
}

@Composable
fun FilterSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Live", "Upcoming", "Results").forEach {
            FilterChip(
                selected = selected == it,
                onClick = { onSelect(it) },
                label = { Text(it) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBlue,
                    selectedLabelColor = Background,
                    labelColor = White
                )
            )
        }
    }
}

@Composable
fun MatchList(
    sport: String,
    filter: String,
    onMatchClick: (Match) -> Unit
) {
    var matches by remember(sport, filter) {
        mutableStateOf<List<Match>>(emptyList())
    }
    var loading by remember(sport, filter) {
        mutableStateOf(true)
    }
    var error by remember(sport, filter) {
        mutableStateOf("")
    }

    LaunchedEffect(sport, filter) {
        loading = true
        error = ""

        try {
            val endpoint = when (sport) {
                "Football" -> when (filter) {
                    "Live" -> "footballLive"
                    "Upcoming" -> "footballUpcoming"
                    else -> "footballMatches"
                }

                else -> when (filter) {
                    "Live" -> "currentMatches"
                    "Upcoming" -> "cricketMatches"
                    else -> "dbMatches"
                }
            }

            matches = fetchMatches(endpoint, sport)

            if (filter == "Results") {
                matches = matches.filter {
                    val status = it.status.lowercase()
                    status.contains("finished") ||
                        status.contains("complete") ||
                        status.contains("result")
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load matches"
        }

        loading = false
    }

    when {
        loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SkyBlue)
        }

        error.isNotEmpty() -> Text(
            error,
            color = Color.Red,
            modifier = Modifier.padding(18.dp)
        )

        matches.isEmpty() -> Text(
            "No matches available",
            color = Muted,
            modifier = Modifier.padding(18.dp)
        )

        else -> LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(matches) { match ->
                MatchCard(match) {
                    onMatchClick(match)
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                match.competition.ifBlank { match.sport },
                color = SkyBlue,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(match.home, color = White)
                    Text(match.away, color = White)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(match.homeScore, color = White)
                    Text(match.awayScore, color = White)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                match.status.ifBlank { match.time },
                color = Green,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MatchCentre(
    match: Match,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(
            rememberScrollState()
        ).padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = SkyBlue)
        }

        Text(
            match.competition,
            color = SkyBlue,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            match.home,
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "${match.homeScore}  -  ${match.awayScore}",
            color = SkyBlue,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            match.away,
            color = White,
            fontSize = 21.sp
        )

        Spacer(Modifier.height(20.dp))

        listOf(
            "Overview",
            "Scorecard",
            "Commentary",
            "Stats",
            "H2H",
            "Highlights"
        ).forEach { section ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                ),
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        section,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (section == "Highlights")
                            "Highlights coming soon"
                        else
                            "$section data coming soon",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ComingSoon() {
    Box(
        Modifier.fillMaxSize().padding(25.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "This feature is coming soon",
            color = Muted,
            fontSize = 17.sp
        )
    }
}

@Composable
fun SimplePage(
    title: String,
    message: String
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            title,
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = Muted)
    }
}
