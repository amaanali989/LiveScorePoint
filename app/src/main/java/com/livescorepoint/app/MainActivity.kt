
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
import coil.compose.AsyncImage
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
private val White = Color.White
private val Muted = Color(0xFF94A3B8)
private val Green = Color(0xFF22C55E)
private val Red = Color(0xFFEF4444)

private val cricketCategories = listOf(
    "All",
    "International",
    "Women International",
    "Men's Leagues",
    "Women's Leagues",
    "Domestic"
)

data class Match(
    val home: String,
    val away: String,
    val homeScore: String = "",
    val awayScore: String = "",
    val status: String = "",
    val time: String = "",
    val competition: String = "",
    val venue: String = "",
    val sport: String = "Cricket",
    val homeLogo: String = "",
    val awayLogo: String = "",
    val matchId: String = "",
    val category: String = "Domestic",
    val matchStarted: Boolean = false,
    val matchEnded: Boolean = false
)

private fun jsonString(
    obj: JSONObject,
    vararg keys: String
): String {
    for (key in keys) {
        val value = obj.optString(key, "")
        if (value.isNotBlank() && value != "null") {
            return value
        }
    }
    return ""
}

private fun scoreText(obj: JSONObject): String {
    val runs = obj.optString("r", "")
    val wickets = obj.optString("w", "")
    val overs = obj.optString("o", "")

    if (runs.isBlank()) return ""

    val wicketText =
        if (wickets.isNotBlank()) "/$wickets" else ""

    val overText =
        if (overs.isNotBlank()) " ($overs ov)" else ""

    return "$runs$wicketText$overText"
}

private fun classifyCricketMatch(
    name: String,
    competition: String
): String {
    val text = "$name $competition".lowercase()

    val womenLeagueCodes = listOf(
        "wpl", "wbbl", "wcpl", "women's premier",
        "women premier", "women big bash"
    )

    val leagueWords = listOf(
        "ipl", "bbl", "psl", "sa20", "cpl",
        "hundred", "t20 league", "premier league",
        "super league", "league"
    )

    val women = text.contains("women") ||
        text.contains("woman") ||
        text.contains("female") ||
        text.contains("girls")

    if (women) {
        return if (
            leagueWords.any { text.contains(it) } ||
            womenLeagueCodes.any { text.contains(it) }
        ) {
            "Women's Leagues"
        } else {
            "Women International"
        }
    }

    if (womenLeagueCodes.any { text.contains(it) }) {
        return "Women's Leagues"
    }

    if (leagueWords.any { text.contains(it) }) {
        return "Men's Leagues"
    }

    val internationalWords = listOf(
        "international", "test match", "odi",
        "t20i", "world cup", "asia cup",
        "champions trophy", "world test championship",
        "tour of", "tri-series"
    )

    return if (internationalWords.any { text.contains(it) }) {
        "International"
    } else {
        "Domestic"
    }
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

        val name = jsonString(item, "name", "match_name")

        val teams = item.optJSONArray("teams")

        var home = jsonString(
            item, "home", "home_team", "homeTeam"
        )

        var away = jsonString(
            item, "away", "away_team", "awayTeam"
        )

        if (teams != null && teams.length() >= 2) {
            if (home.isBlank()) home = teams.optString(0, "")
            if (away.isBlank()) away = teams.optString(1, "")
        }

        if (home.isBlank() || away.isBlank()) {
            val split = name.split(
                Regex("\\s+vs\\s+", RegexOption.IGNORE_CASE)
            )

            if (split.size >= 2) {
                if (home.isBlank()) home = split[0].trim()
                if (away.isBlank()) {
                    away = split[1].split(",").first().trim()
                }
            }
        }

        if (home.isBlank() || away.isBlank()) continue

        val teamInfo = item.optJSONArray("teamInfo")
        var homeLogo = ""
        var awayLogo = ""

        if (teamInfo != null) {
            for (j in 0 until teamInfo.length()) {
                val team = teamInfo.optJSONObject(j) ?: continue
                val teamName = jsonString(team, "name", "shortname")
                val logo = jsonString(team, "img", "logo", "image")

                if (
                    teamName.equals(home, true) ||
                    teamName.contains(home, true) ||
                    home.contains(teamName, true)
                ) {
                    homeLogo = logo
                }

                if (
                    teamName.equals(away, true) ||
                    teamName.contains(away, true) ||
                    away.contains(teamName, true)
                ) {
                    awayLogo = logo
                }
            }
        }

        val scoreArray = item.optJSONArray("score")
        var homeScore = jsonString(
            item, "home_score", "homeScore", "score_home"
        )
        var awayScore = jsonString(
            item, "away_score", "awayScore", "score_away"
        )

        if (scoreArray != null) {
            val homeScores = mutableListOf<String>()
            val awayScores = mutableListOf<String>()

            for (j in 0 until scoreArray.length()) {
                val score = scoreArray.optJSONObject(j) ?: continue
                val inning = jsonString(score, "inning")
                val formatted = scoreText(score)

                if (formatted.isBlank()) continue

                if (inning.contains(home, true)) {
                    homeScores.add(formatted)
                } else if (inning.contains(away, true)) {
                    awayScores.add(formatted)
                } else {
                    if (homeScores.isEmpty()) {
                        homeScores.add(formatted)
                    } else {
                        awayScores.add(formatted)
                    }
                }
            }

            if (homeScores.isNotEmpty()) {
                homeScore = homeScores.joinToString("\n")
            }

            if (awayScores.isNotEmpty()) {
                awayScore = awayScores.joinToString("\n")
            }
        }

        val status = jsonString(
            item, "status", "match_status", "state"
        )

        val competition = jsonString(
            item, "competition", "league", "tournament"
        ).ifBlank {
            if (name.contains(",")) {
                name.split(",").getOrNull(1)?.trim() ?: ""
            } else {
                ""
            }
        }

        val started = item.optBoolean("matchStarted", false) ||
            item.optBoolean("started", false)

        val ended = item.optBoolean("matchEnded", false) ||
            item.optBoolean("finished", false)

        val category = if (sport == "Cricket") {
            classifyCricketMatch(name, competition)
        } else {
            "All"
        }

        result.add(
            Match(
                home = home,
                away = away,
                homeScore = homeScore,
                awayScore = awayScore,
                status = status,
                time = jsonString(
                    item, "date", "time", "start_time"
                ),
                competition = competition,
                venue = jsonString(item, "venue", "stadium"),
                sport = sport,
                homeLogo = homeLogo,
                awayLogo = awayLogo,
                matchId = jsonString(item, "id", "match_id"),
                category = category,
                matchStarted = started,
                matchEnded = ended
            )
        )
    }

    return result
}

private fun isFinishedMatch(match: Match): Boolean {
    val status = match.status.lowercase()

    return match.matchEnded ||
        status.contains("won by") ||
        status.contains("match tied") ||
        status.contains("drawn") ||
        status.contains("abandoned") ||
        status.contains("no result") ||
        status.contains("completed") ||
        status.contains("finished") ||
        status.contains("result")
}

private fun isLiveMatch(match: Match): Boolean {
    if (isFinishedMatch(match)) return false

    val status = match.status.lowercase()

    return match.matchStarted ||
        status.contains("live") ||
        status.contains("in progress") ||
        status.contains("stumps") ||
        status.contains("innings")
}

private fun isUpcomingMatch(match: Match): Boolean {
    return !isFinishedMatch(match) && !isLiveMatch(match)
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
    var selectedCategory by remember { mutableStateOf("All") }
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
                            label = {
                                Text(
                                    tab,
                                    color = if (selectedTab == tab) {
                                        SkyBlue
                                    } else {
                                        Muted
                                    }
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SkyBlue,
                                selectedTextColor = SkyBlue,
                                indicatorColor = SkyBlue.copy(alpha = 0.12f),
                                unselectedTextColor = Muted
                            )
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
                            selected = selectedSport,
                            onSelect = {
                                selectedSport = it
                                selectedFilter = "Live"
                                selectedCategory = "All"
                            }
                        )

                        FilterSelector(
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it }
                        )

                        if (selectedSport == "Cricket") {
                            CricketCategorySelector(
                                selected = selectedCategory,
                                onSelect = { selectedCategory = it }
                            )
                        }

                        if (
                            selectedSport == "Cricket" ||
                            selectedSport == "Football"
                        ) {
                            MatchList(
                                sport = selectedSport,
                                filter = selectedFilter,
                                category = selectedCategory,
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

        Spacer(Modifier.height(5.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(7.dp)
                    .background(Green, RoundedCornerShape(50))
            )

            Spacer(Modifier.width(6.dp))

            Text(
                "LIVE SPORTS",
                color = Green,
                fontSize = 11.sp
            )
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
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Live", "Upcoming", "Recent").forEach {
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
fun CricketCategorySelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        cricketCategories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = {
                    Text(category, fontSize = 11.sp)
                },
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
    category: String,
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

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(sport, filter, refreshKey) {
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
          