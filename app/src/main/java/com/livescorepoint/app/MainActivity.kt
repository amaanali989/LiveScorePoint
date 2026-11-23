
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ======================================
// SPORTEXX CONFIGURATION
// ======================================

private const val API_BASE =
    "https://lsp-cricket-api.uddinamaan619.workers.dev"

private val AppBackground = Color(0xFF101827)
private val SurfaceColor = Color(0xFF192438)
private val CardColor = Color(0xFF202E43)
private val AccentBlue = Color(0xFF3B82C4)
private val SoftBlue = Color(0xFF263B5B)
private val PrimaryText = Color(0xFFE2E8F0)
private val SecondaryText = Color(0xFF94A3B8)
private val LiveGreen = Color(0xFF39B878)
private val ErrorRed = Color(0xFFE16B6B)

// ======================================
// SPORTS AND NAVIGATION
// ======================================

private val sportsList = listOf(
    "Cricket",
    "Football",
    "Hockey",
    "Esports",
    "F1",
    "Wrestling",
    "Kabaddi"
)

private val navigationItems = listOf(
    "Home",
    "Live",
    "News",
    "Quiz",
    "Stats",
    "More"
)

private val cricketCategories = listOf(
    "International",
    "Women International",
    "Men's Leagues",
    "Women's Leagues",
    "Domestic"
)

// ======================================
// DATA MODELS
// ======================================

data class MatchItem(
    val id: String = "",
    val sport: String = "Cricket",
    val home: String = "",
    val away: String = "",
    val homeLogo: String = "",
    val awayLogo: String = "",
    val homeScore: String = "",
    val awayScore: String = "",
    val status: String = "",
    val date: String = "",
    val time: String = "",
    val competition: String = "",
    val venue: String = "",
    val category: String = "",
    val isLive: Boolean = false,
    val isFinished: Boolean = false
)

data class NewsItem(
    val title: String,
    val description: String = "",
    val image: String = "",
    val source: String = "",
    val url: String = ""
)

data class QuizEvent(
    val id: String,
    val sport: String,
    val title: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String = ""
)

enum class AppSection {
    HOME,
    LIVE,
    NEWS,
    QUIZ,
    STATS,
    MORE,
    RANKINGS,
    MATCH_DETAILS
}

// ======================================
// JSON HELPERS
// ======================================

private fun JSONObject.readString(
    vararg keys: String
): String {
    for (key in keys) {
        val value = optString(key, "")
        if (value.isNotBlank() && value != "null") {
            return value
        }
    }
    return ""
}

private fun JSONArray.stringAt(index: Int): String {
    return if (index in 0 until length()) {
        optString(index, "")
    } else {
        ""
    }
}

private fun JSONObject.readScore(): String {
    val runs = optString("r", "")
    val wickets = optString("w", "")
    val overs = optString("o", "")

    if (runs.isBlank()) return ""

    val wicketPart =
        if (wickets.isNotBlank()) "/$wickets" else ""

    val overPart =
        if (overs.isNotBlank()) " ($overs ov)" else ""

    return "$runs$wicketPart$overPart"
}

// ======================================
// MATCH STATUS
// ======================================

private fun isMatchFinished(status: String): Boolean {
    val value = status.lowercase()

    return value.contains("won by") ||
        value.contains("drawn") ||
        value.contains("tied") ||
        value.contains("abandoned") ||
        value.contains("no result") ||
        value.contains("finished") ||
        value == "ft" ||
        value.contains("full time")
}

private fun isMatchLive(status: String): Boolean {
    if (isMatchFinished(status)) return false

    val value = status.lowercase()

    return value.contains("live") ||
        value.contains("in progress") ||
        value.contains("innings") ||
        value.contains("1st half") ||
        value.contains("2nd half") ||
        value.contains("half time")
}

// ======================================
// CRICKET CATEGORY DETECTION
// ======================================

private fun classifyCricketMatch(
    name: String,
    competition: String
): String {
    val value = "$name $competition".lowercase()

    val isWomen =
        value.contains("women") ||
        value.contains("woman") ||
        value.contains("female")

    val leagueWords = listOf(
        "ipl", "wpl", "bbl", "wbbl", "psl",
        "sa20", "cpl", "hundred",
        "premier league", "super league"
    )

    val isLeague = leagueWords.any {
        value.contains(it)
    }

    if (isWomen && isLeague) {
        return "Women's Leagues"
    }

    if (isWomen) {
        return "Women International"
    }

    if (isLeague) {
        return "Men's Leagues"
    }

    val internationalWords = listOf(
        "international",
        "test match",
        "odi",
        "t20i",
        "world cup",
        "asia cup",
        "champions trophy",
        "world test championship",
        "tour of",
        "tri-series"
    )

    return if (internationalWords.any {
        value.contains(it)
    }) {
        "International"
    } else {
        "Domestic"
    }
}

// ======================================
// CRICKET API PARSER
// ======================================

private fun parseCricketMatches(
    response: String
): List<MatchItem> {

    val root = JSONObject(response)

    val array = root.optJSONArray("data")
        ?: root.optJSONArray("matches")
        ?: JSONArray()

    val result = mutableListOf<MatchItem>()

    for (i in 0 until array.length()) {

        val obj = array.optJSONObject(i) ?: continue

        val teams = obj.optJSONArray("teams")
            ?: JSONArray()

        val home = teams.stringAt(0)
        val away = teams.stringAt(1)

        if (home.isBlank() || away.isBlank()) continue

        val name = obj.readString("name", "title")

        val competition = obj.readString(
            "series",
            "competition",
            "tournament"
        )

        val teamInfo = obj.optJSONArray("teamInfo")
            ?: JSONArray()

        var homeLogo = ""
        var awayLogo = ""

        for (j in 0 until teamInfo.length()) {
            val team = teamInfo.optJSONObject(j)
                ?: continue

            val teamName = team.readString("name")
            val logo = team.readString("img", "logo")

            if (teamName.equals(home, true)) {
                homeLogo = logo
            }

            if (teamName.equals(away, true)) {
                awayLogo = logo
            }
        }

        val scores = obj.optJSONArray("score")
            ?: JSONArray()

        var homeScore = ""
        var awayScore = ""

        for (j in 0 until scores.length()) {
            val score = scores.optJSONObject(j)
                ?: continue

            val inning = score.readString("inning")
                .lowercase()

            val value = score.readScore()

            when {
                inning.contains(home.lowercase()) &&
                    homeScore.isBlank() -> {
                    homeScore = value
                }

                inning.contains(away.lowercase()) &&
                    awayScore.isBlank() -> {
                    awayScore = value
                }
            }
        }

        val status = obj.readString(
            "status",
            "matchStatus"
        )

        val finished =
            obj.optBoolean("matchEnded", false) ||
                isMatchFinished(status)

        val live =
            !finished &&
                (
                    obj.optBoolean("matchStarted", false) ||
                        isMatchLive(status)
                    )

        result.add(
            MatchItem(
                id = obj.readString("id", "matchId"),
                sport = "Cricket",
                home = home,
                away = away,
                homeLogo = homeLogo,
                awayLogo = awayLogo,
                homeScore = homeScore,
                awayScore = awayScore,
                status = status,
                date = obj.readString("date"),
                time = obj.readString(
                    "dateTimeGMT",
                    "dateTime",
                    "time"
                ),
                competition = competition.ifBlank { name },
                venue = obj.readString("venue"),
                category = classifyCricketMatch(
                    name,
                    competition
                ),
                isLive = live,
                isFinished = finished
            )
        )
    }

    return result
}

// ======================================
// FOOTBALL API PARSER
// ======================================

private fun parseFootballMatches(
    response: String
): List<MatchItem> {

    val root = JSONObject(response)

    val array = root.optJSONArray("matches")
        ?: root.optJSONArray("data")
        ?: JSONArray()

    val result = mutableListOf<MatchItem>()

    for (i in 0 until array.length()) {

        val obj = array.optJSONObject(i) ?: continue

        val home = obj.readString("home")
        val away = obj.readString("away")

        if (home.isBlank() || away.isBlank()) continue

        val status = obj.readString(
            "status_text",
            "status"
        )

        val statusCode = obj.readString("status")
            .lowercase()

        val finished =
            statusCode == "finished" ||
                isMatchFinished(status)

        val live =
            !finished &&
                (
                    statusCode == "live" ||
                        isMatchLive(status)
                    )

        result.add(
            MatchItem(
                id = obj.readString("id", "url"),
                sport = "Football",
                home = home,
                away = away,
                homeLogo = obj.readString("home_logo"),
                awayLogo = obj.readString("away_logo"),
                homeScore = obj.readString("home_score"),
                awayScore = obj.readString("away_score"),
                status = status,
                date = obj.readString("date"),
                time = obj.readString("time"),
                competition = obj.readString("competition"),
                isLive = live,
                isFinished = finished
            )
        )
    }

    return result
}

// ======================================
// API FETCH SYSTEM
// ======================================

private suspend fun fetchApi(
    endpoint: String
): String = withContext(Dispatchers.IO) {

    val connection = URL(
        "$API_BASE/$endpoint"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )

        val code = connection.responseCode

        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader().use {
            it.readText()
        }

        if (code !in 200..299) {
            throw Exception("Server error: $code")
        }

        response
    } finally {
        connection.disconnect()
    }
}

// ======================================
// MAIN ACTIVITY
// ======================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SportexXTheme {
                SportexXApp()
            }
        }
    }
}

// ======================================
// SPORTEXX THEME
// ======================================

@Composable
private fun SportexXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentBlue,
            onPrimary = PrimaryText,
            background = AppBackground,
            onBackground = PrimaryText,
            surface = SurfaceColor,
            onSurface = PrimaryText,
            secondary = SoftBlue
        ),
        content = content
    )
}

// ======================================
// MAIN APP
// ======================================

@Composable
private fun SportexXApp() {

    var selectedSport by remember {
        mutableStateOf("Cricket")
    }

    var selectedSection by remember {
        mutableStateOf(AppSection.HOME)
    }

    var selectedFilter by remember {
        mutableStateOf("Live")
    }

    var selectedCategory by remember {
        mutableStateOf("International")
    }

    var selectedMatch by remember {
        mutableStateOf<MatchItem?>(null)
    }

    var matches by remember {
        mutableStateOf<List<MatchItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    // MATCH DATA LOAD

    LaunchedEffect(
        selectedSport,
        selectedFilter,
        refreshKey
    ) {
        val endpoint = when (selectedSport) {
            "Cricket" -> when (selectedFilter) {
                "Live" -> "currentMatches"
                "Upcoming" -> "cricketMatches"
                "Recent" -> "dbMatches"
                else -> "currentMatches"
            }

            "Football" -> when (selectedFilter) {
                "Live" -> "footballLive"
                "Upcoming" -> "footballUpcoming"
                "Recent" -> "footballMatches"
                else -> "footballLive"
            }

            else -> ""
        }

        if (endpoint.isBlank()) {
            matches = emptyList()
            errorMessage = ""
            loading = false
            return@LaunchedEffect
        }

        loading = true
        errorMessage = ""

        try {
            val response = fetchApi(endpoint)

            val parsed = when (selectedSport) {
                "Cricket" -> parseCricketMatches(response)
                "Football" -> parseFootballMatches(response)
                else -> emptyList()
            }

            matches = when (selectedFilter) {
                "Live" -> parsed.filter { it.isLive }

                "Recent" -> parsed.filter {
                    it.isFinished
                }

                "Upcoming" -> parsed.filter {
                    !it.isLive && !it.isFinished
                }

                else -> parsed
            }

        } catch (e: Exception) {
            matches = emptyList()
            errorMessage = e.message
                ?: "Unable to load matches"
        }

        loading = false
    }

    // MAIN SCREEN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        if (selectedMatch == null) {

            SportexXHeader()

            SportSelectorBar(
                selectedSport = selectedSport,
                onSportChange = {
                    selectedSport = it
                    selectedCategory = "International"
                    selectedFilter = "Live"
                    selectedSection = AppSection.HOME
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when {
                selectedMatch != null -> {
                    MatchCentreScreen(
                        match = selectedMatch!!,
                        onBack = {
                            selectedMatch = null
                        }
                    )
                }

                selectedSection == AppSection.HOME ||
                    selectedSection == AppSection.LIVE -> {

                    SportHomeScreen(
                        selectedSport = selectedSport,
                        selectedFilter = selectedFilter,
                        selectedCategory = selectedCategory,
                        matches = matches,
                        loading = loading,
                        errorMessage = errorMessage,
                        onFilterChange = {
                            selectedFilter = it
                        },
                        onCategoryChange = {
                            selectedCategory = it
                        },
                        onRefresh = {
                            refreshKey++
                        },
                        onMatchClick = {
                            selectedMatch = it
                        }
                    )
                }

                selectedSection == AppSection.NEWS -> {
                    NewsScreen()
                }

                selectedSection == AppSection.QUIZ -> {
                    QuizScreen(
                        selectedSport = selectedSport
                    )
                }

                selectedSection == AppSection.STATS -> {
                    StatsScreen(
                        selectedSport = selectedSport
                    )
                }

                selectedSection == AppSection.MORE -> {
                    MoreScreen(
                        onOpenRankings = {
                            selectedSection = AppSection.RANKINGS
                        }
                    )
                }

                selectedSection == AppSection.RANKINGS -> {
                    RankingsScreen(
                        onBack = {
                            selectedSection = AppSection.MORE
                        }
                    )
                }

                else -> {
                    SportHomeScreen(
                        selectedSport = selectedSport,
                        selectedFilter = selectedFilter,
                        selectedCategory = selectedCategory,
                        matches = matches,
                        loading = loading,
                        errorMessage = errorMessage,
                        onFilterChange = {
                            selectedFilter = it
                        },
                        onCategoryChange = {
                            selectedCategory = it
                        },
                        onRefresh = {
                            refreshKey++
                        },
                        onMatchClick = {
                            selectedMatch = it
                        }
                    )
                }
            }
        }

        if (selectedMatch == null) {
            BottomNavigationBar(
                selectedSection = selectedSection,
                onSectionChange = {
                    selectedSection = it
                }
            )
        }
    }
}

// ======================================
// SPORTEXX HEADER
// ======================================

@Composable
private fun SportexXHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 13.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            Text(
                text = "SPORTEXX",
                color = PrimaryText,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )

            Text(
                text = "SPORTS • NEWS • STATS",
                color = SecondaryText,
                fontSize = 9.sp,
                letterSpacing = 1.1.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .background(
                    SurfaceColor,
                    RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 11.dp,
                    vertical = 8.dp
                )
        ) {
            Text(
                "S",
                color = AccentBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

// ======================================
// TOP SPORTS SLIDEBAR
// ======================================

@Composable
private fun SportSelectorBar(
    selectedSport: String,
    onSportChange: (String) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        contentPadding = PaddingValues(
            horizontal = 13.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        items(sportsList) { sport ->

            val selected = selectedSport == sport

            Surface(
                modifier = Modifier.clickable {
                    onSportChange(sport)
                },
                shape = RoundedCornerShape(9.dp),
                color = if (selected) {
                    SoftBlue
                } else {
                    SurfaceColor
                }
            ) {

                Text(
                    text = sport,
                    modifier = Modifier.padding(
                        horizontal = 15.dp,
                        vertical = 10.dp
                    ),
                    color = if (selected) {
                        PrimaryText
                    } else {
                        SecondaryText
                    },
                    fontSize = 13.sp,
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
                )
            }
        }
    }
}

// ======================================
// BOTTOM NAVIGATION SLIDEBAR
// ======================================

@Composable
private fun BottomNavigationBar(
    selectedSection: AppSection,
    onSectionChange: (AppSection) -> Unit
) {

    val items = listOf(
        "Home" to AppSection.HOME,
        "Live" to AppSection.LIVE,
        "News" to AppSection.NEWS,
        "Quiz" to AppSection.QUIZ,
        "Stats" to AppSection.STATS,
        "More" to AppSection.MORE
    )

    Surface(
        color = SurfaceColor,
        shadowElevation = 8.dp
    ) {

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(
                horizontal = 3.dp
            )
        ) {

            items(items) { item ->

                val selected =
                    selectedSection == item.second ||
                        (
                            selectedSection == AppSection.RANKINGS &&
                                item.second == AppSection.MORE
                            )

                Column(
                    modifier = Modifier
                        .width(88.dp)
                        .fillMaxHeight()
                        .clickable {
                            onSectionChange(item.second)
                        }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(3.dp)
                            .background(
                                if (selected) AccentBlue
                                else Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = when (item.first) {
                            "Home" -> "⌂"
                            "Live" -> "●"
                            "News" -> "▤"
                            "Quiz" -> "?"
                            "Stats" -> "▥"
                            else -> "•••"
                        },
                        color = if (selected) {
                            AccentBlue
                        } else {
                            SecondaryText
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = item.first,
                        color = if (selected) {
                            PrimaryText
                        } else {
                            SecondaryText
                        },
                        fontSize = 10.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}

// ======================================
// SPORT HOME SCREEN
// ======================================

@Composable
private fun SportHomeScreen(
    selectedSport: String,
    selectedFilter: String,
    selectedCategory: String,
    matches: List<MatchItem>,
    loading: Boolean,
    errorMessage: String,
    onFilterChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onMatchClick: (MatchItem) -> Unit
) {

    val filteredMatches = remember(
        matches,
        selectedSport,
        selectedCategory
    ) {
        if (selectedSport == "Cricket") {
            matches.filter {
                it.category == selectedCategory
            }
        } else {
            matches
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        // MATCH FILTERS

        MatchFilterBar(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange
        )

        // CRICKET CATEGORIES

        if (selectedSport == "Cricket") {
            CricketCategoryBar(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }

        when {

            selectedSport !in listOf(
                "Cricket",
                "Football"
            ) -> {

                ComingSoonScreen(
                    sport = selectedSport
                )
            }

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentBlue
                    )
                }
            }

            errorMessage.isNotBlank() -> {

                ErrorScreen(
                    message = errorMessage,
                    onRetry = onRefresh
                )
            }

            filteredMatches.isEmpty() -> {

                EmptyMatchesScreen(
                    selectedFilter = selectedFilter,
                    onRefresh = onRefresh
                )
            }

            else -> {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 15.dp,
                                vertical = 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = when (selectedFilter) {
                                "Live" -> "Live Matches"
                                "Upcoming" -> "Upcoming Matches"
                                "Recent" -> "Recent Matches"
                                else -> "Matches"
                            },
                            color = PrimaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "${filteredMatches.size} matches",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }

                    MatchListView(
                        matches = filteredMatches,
                        onMatchClick = onMatchClick
                    )
                }
            }
        }
    }
}

// ======================================
// MATCH FILTER BAR
// ======================================

@Composable
private fun MatchFilterBar(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {

    val filters = listOf(
        "Live",
        "Upcoming",
        "Recent"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 13.dp,
                vertical = 5.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        filters.forEach { filter ->

            val selected = selectedFilter == filter

            Surface(
                modifier = Modifier.clickable {
                    onFilterChange(filter)
                },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    SoftBlue
                } else {
                    SurfaceColor
                }
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 15.dp,
                        vertical = 9.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (filter == "Live") {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    LiveGreen,
                                    RoundedCornerShape(50)
                                )
                        )

                        Spacer(Modifier.width(7.dp))
                    }

                    Text(
                        filter,
                        color = if (selected) {
                            PrimaryText
                        } else {
                            SecondaryText
                        },
                        fontSize = 12.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}

// ======================================
// CRICKET CATEGORY BAR
// ======================================

@Composable
private fun CricketCategoryBar(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        contentPadding = PaddingValues(
            horizontal = 13.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {

        items(cricketCategories) { category ->

            Column(
                modifier = Modifier
                    .clickable {
                        onCategoryChange(category)
                    }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    category,
                    color = if (
                        selectedCategory == category
                    ) {
                        PrimaryText
                    } else {
                        SecondaryText
                    },
                    fontSize = 11.sp,
                    fontWeight = if (
                        selectedCategory == category
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )

                Spacer(Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp)
                        .background(
                            if (
                                selectedCategory == category
                            ) AccentBlue
                            else Color.Transparent,
                            RoundedCornerShape(5.dp)
                        )
                )
            }
        }
    }
}

// ======================================
// MATCH LIST
// ======================================

@Composable
private fun MatchListView(
    matches: List<MatchItem>,
    onMatchClick: (MatchItem) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 7.dp
        ),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        items(
            items = matches,
            key = {
                it.id.ifBlank {
                    "${it.sport}_${it.home}_${it.away}_${it.date}"
                }
            }
        ) { match ->

            SportMatchCard(
                match = match,
                onClick = {
                    onMatchClick(match)
                }
            )
        }
    }
}

// ======================================
// MATCH CARD
// ======================================

@Composable
private fun SportMatchCard(
    match: MatchItem,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Column(
            modifier = Modifier.padding(13.dp)
        ) {

            // COMPETITION AND STATUS

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    match.competition.ifBlank {
                        match.sport
                    },
                    color = SecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Text(
                    text = when {
                        match.isLive -> "LIVE"
                        match.isFinished -> "FINISHED"
                        else -> "UPCOMING"
                    },
                    color = when {
                        match.isLive -> LiveGreen
                        match.isFinished -> SecondaryText
                        else -> AccentBlue
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(13.dp))

            // HOME TEAM

            TeamMatchRow(
                name = match.home,
                logo = match.homeLogo,
                score = match.homeScore
            )

            Spacer(Modifier.height(10.dp))

            // AWAY TEAM

            TeamMatchRow(
                name = match.away,
                logo = match.awayLogo,
                score = match.awayScore
            )

            // STATUS

            if (
                match.status.isNotBlank() ||
                match.venue.isNotBlank()
            ) {

                Spacer(Modifier.height(11.dp))

                HorizontalDivider(
                    color = Color(0xFF304056),
                    thickness = 0.6.dp
                )

                Spacer(Modifier.height(8.dp))

                if (match.status.isNotBlank()) {
                    Text(
                        match.status,
                        color = if (match.isLive) {
                            LiveGreen
                        } else {
                            SecondaryText
                        },
                        fontSize = 11.sp
                    )
                }

                if (match.venue.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        match.venue,
                        color = SecondaryText,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ======================================
// TEAM MATCH ROW
// ======================================

@Composable
private fun TeamMatchRow(
    name: String,
    logo: String,
    score: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TeamLogoView(
            imageUrl = logo,
            teamName = name
        )

        Spacer(Modifier.width(10.dp))

        Text(
            name,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Text(
            score.ifBlank { "-" },
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ======================================
// TEAM LOGO
// ======================================

@Composable
private fun TeamLogoView(
    imageUrl: String,
    teamName: String
) {

    if (imageUrl.isNotBlank()) {

        AsyncImage(
            model = imageUrl,
            contentDescription = teamName,
            modifier = Modifier.size(29.dp)
        )

    } else {

        Box(
            modifier = Modifier
                .size(29.dp)
                .background(
                    SoftBlue,
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                teamName.take(1).uppercase(),
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

// ======================================
// EMPTY MATCHES
// ======================================

@Composable
private fun EmptyMatchesScreen(
    selectedFilter: String,
    onRefresh: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.Sports,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(42.dp)
        )

        Spacer(Modifier.height(13.dp))

        Text(
            when (selectedFilter) {
                "Live" -> "No live matches"
                "Upcoming" -> "No upcoming matches"
                "Recent" -> "No recent matches"
                else -> "No matches available"
            },
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Abhi is section mein match available nahi hai.",
            color = SecondaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRefresh
        ) {
            Text("Refresh")
        }
    }
}

// ======================================
// ERROR SCREEN
// ======================================

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(40.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Unable to load matches",
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(7.dp))

        Text(
            message,
            color = SecondaryText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onRetry
        ) {
            Text("Try Again")
        }
    }
}

// ======================================
// COMING SOON SCREEN
// ======================================

@Composable
private fun ComingSoonScreen(
    sport: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "SPORTEXX",
            color = AccentBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(15.dp))

        Text(
            sport,
            color = PrimaryText,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(9.dp))

        Text(
            "We're Working on It",
            color = PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "This feature is coming soon.",
            color = SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ======================================
// MATCH CENTRE
// ======================================

@Composable
private fun MatchCentreScreen(
    match: MatchItem,
    onBack: () -> Unit
) {

    val tabs = if (match.sport.equals("Cricket", true)) {
        listOf(
            "Info",
            "Overview",
            "Scorecard",
            "Squad",
            "Stats",
            "H2H",
            "Highlights"
        )
    } else {
        listOf(
            "Info",
            "Overview",
            "Lineups",
            "Stats",
            "H2H",
            "Highlights"
        )
    }

    var selectedTab by remember {
        mutableStateOf("Overview")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        // TOP BAR

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .padding(
                    horizontal = 12.dp,
                    vertical = 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    match.competition.ifBlank {
                        match.sport
                    },
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    match.sport,
                    color = SecondaryText,
                    fontSize = 10.sp
                )
            }

            Text(
                if (match.isLive) "LIVE" else "",
                color = LiveGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // MATCH SUMMARY

        MatchCentreScoreHeader(match)

        // TABS

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor),
            contentPadding = PaddingValues(
                horizontal = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            items(tabs) { tab ->

                Column(
                    modifier = Modifier
                        .clickable {
                            selectedTab = tab
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        tab,
                        color = if (
                            selectedTab == tab
                        ) PrimaryText
                        else SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (
                            selectedTab == tab
                        ) FontWeight.Bold
                        else FontWeight.Normal
                    )

                    Spacer(Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .width(25.dp)
                            .height(2.dp)
                            .background(
                                if (selectedTab == tab) {
                                    AccentBlue
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            }
        }

        // SELECTED TAB CONTENT

        when (selectedTab) {

            "Info" -> MatchInfoTab(match)

            "Overview" -> MatchOverviewTab(match)

            "Scorecard" -> MatchUnavailableTab(
                "Detailed scorecard",
                "Scorecard data is not available from the current API response."
            )

            "Squad" -> MatchUnavailableTab(
                "Squad information",
                "Squad data is not available from the current API response."
            )

            "Lineups" -> MatchUnavailableTab(
                "Team lineups",
                "Lineup data is not available from the current API response."
            )

            "Stats" -> MatchUnavailableTab(
                "Match statistics",
                "Detailed statistics are not available from the current API response."
            )

            "H2H" -> MatchUnavailableTab(
                "Head to Head",
                "Head-to-head data is not available from the current API response."
            )

            "Highlights" -> MatchUnavailableTab(
                "Match highlights",
                "No verified highlights link is available yet."
            )
        }
    }
}

// ======================================
// MATCH SCORE HEADER
// ======================================

@Composable
private fun MatchCentreScoreHeader(
    match: MatchItem
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor)
            .padding(17.dp)
    ) {

        Text(
            match.status.ifBlank {
                if (match.isFinished) "Match finished"
                else if (match.isLive) "Live match"
                else match.date
            },
            color = if (match.isLive) {
                LiveGreen
            } else {
                SecondaryText
            },
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(17.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TeamLogoView(
                    imageUrl = match.homeLogo,
                    teamName = match.home
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    match.home,
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    match.homeScore.ifBlank { "-" },
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "VS",
                color = SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TeamLogoView(
                    imageUrl = match.awayLogo,
                    teamName = match.away
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    match.away,
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    match.awayScore.ifBlank { "-" },
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ======================================
// MATCH INFO TAB
// ======================================

@Composable
private fun MatchInfoTab(
    match: MatchItem
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            MatchSectionTitle("Match Information")
        }

        item {
            MatchInfoRow(
                "Sport",
                match.sport
            )
        }

        item {
            MatchInfoRow(
                "Competition",
                match.competition
            )
        }

        item {
            MatchInfoRow(
                "Match Date",
                match.date
            )
        }

        item {
            MatchInfoRow(
                "Match Time",
                match.time
            )
        }

        item {
            MatchInfoRow(
                "Venue",
                match.venue
            )
        }

        item {
            MatchInfoRow(
                "Match Status",
                match.status
            )
        }

        item {
            MatchInfoRow(
                "Home Team",
                match.home
            )
        }

        item {
            MatchInfoRow(
                "Away Team",
                match.away
            )
        }
    }
}

// ======================================
// MATCH OVERVIEW TAB
// ======================================

@Composable
private fun MatchOverviewTab(
    match: MatchItem
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            MatchSectionTitle("Match Overview")
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Text(
                        "Match Summary",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "${match.home} vs ${match.away}",
                        color = PrimaryText,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        "Competition: ${match.competition.ifBlank { "Not available" }}",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Venue: ${match.venue.ifBlank { "Not available" }}",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            MatchSectionTitle("Commentary")
        }

        item {
            MatchUnavailableContent(
                "Commentary is not available yet.",
                "Live commentary will appear here when the API provides verified commentary data."
            )
        }
    }
}

// ======================================
// COMMON MATCH SECTION TITLE
// ======================================

@Composable
private fun MatchSectionTitle(
    title: String
) {

    Text(
        title,
        color = PrimaryText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            vertical = 4.dp
        )
    )
}

// ======================================
// MATCH INFORMATION ROW
// ======================================

@Composable
private fun MatchInfoRow(
    title: String,
    value: String
) {

    if (value.isNotBlank()) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            ),
            shape = RoundedCornerShape(8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    title,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    value,
                    color = PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ======================================
// UNAVAILABLE TAB
// ======================================

@Composable
private fun MatchUnavailableTab(
    title: String,
    description: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            title,
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(9.dp))

        MatchUnavailableContent(
            "Data not available",
            description
        )
    }
}

// ======================================
// UNAVAILABLE CONTENT
// ======================================

@Composable
private fun MatchUnavailableContent(
    title: String,
    description: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(10.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(27.dp)
            )

            Spacer(Modifier.height(9.dp))

            Text(
                title,
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                description,
                color = SecondaryText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ======================================
// NEWS SCREEN
// ======================================

@Composable
private fun NewsScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {

        Text(
            "Sports News",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        FeaturePlaceholder(
            title = "News Coming Soon",
            description = "Verified sports news and updates will appear here when a news source is connected."
        )
    }
}

// ======================================
// QUIZ SCREEN
// ======================================

@Composable
private fun QuizScreen(
    selectedSport: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {

        Text(
            "Sports Quiz",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            selectedSport,
            color = AccentBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(18.dp))

        FeaturePlaceholder(
            title = "Quiz Coming Soon",
            description = "Match-based questions and sports trivia will appear here when verified quiz data is available."
        )

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            ),
            shape = RoundedCornerShape(10.dp)
        ) {

            Column(
                modifier = Modifier.padding(15.dp)
            ) {

                Text(
                    "Points System",
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    "Quiz participation will use free, non-cash points only. No deposits, staking or cash rewards.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ======================================
// STATS SCREEN
// ======================================

@Composable
private fun StatsScreen(
    selectedSport: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {

        Text(
            "Sports Statistics",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            selectedSport,
            color = AccentBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(18.dp))

        FeaturePlaceholder(
            title = "Statistics Coming Soon",
            description = "Player statistics, team performance and detailed records will appear here when verified data is available."
        )
    }
}

// ======================================
// MORE SCREEN
// ======================================

@Composable
private fun MoreScreen(
    onOpenRankings: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {

        Text(
            "More",
            color = PrimaryText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        MoreMenuItem(
            title = "Football Rankings",
            subtitle = "League tables and player rankings",
            onClick = onOpenRankings
        )

        Spacer(Modifier.height(10.dp))

        MoreMenuItem(
            title = "About SportexX",
            subtitle = "Sports • News • Stats",
            onClick = {}
        )

        Spacer(Modifier.height(10.dp))

        MoreMenuItem(
            title = "App Version",
            subtitle = "Version 1.0",
            onClick = {}
        )
    }
}

// ======================================
// MORE MENU ITEM
// ======================================

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(10.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    title,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    subtitle,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}

// ======================================
// RANKINGS SCREEN
// ======================================

@Composable
private fun RankingsScreen(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }

            Text(
                "Football Rankings",
                color = PrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            FeaturePlaceholder(
                title = "Rankings Coming Soon",
                description = "League-wise team standings, top scorers, assists, appearances, goalkeeper rankings and clean sheets will appear when supported by the API."
            )
        }
    }
}

// ======================================
// COMMON FEATURE PLACEHOLDER
// ======================================

@Composable
private fun FeaturePlaceholder(
    title: String,
    description: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.SportsScore,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(38.dp)
            )

            Spacer(Modifier.height(13.dp))

            Text(
                title,
                color = PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                description,
                color = SecondaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
