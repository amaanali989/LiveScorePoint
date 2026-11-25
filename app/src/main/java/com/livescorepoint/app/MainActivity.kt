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

private fun isMatchStumps(status: String): Boolean {
    return status.lowercase().contains("stumps")
}

private fun isMatchLive(status: String): Boolean {
    if (isMatchFinished(status) || isMatchStumps(status)) {
        return false
    }

    val value = status.lowercase()

    return value.contains("live") ||
        value.contains("in progress") ||
        value.contains("innings") ||
        value.contains("1st half") ||
        value.contains("2nd half") ||
        value.contains("half time")
}
private fun classifyCricketMatch(
    competition: String,
    home: String,
    away: String
): String {

    val value = "$competition $home $away".lowercase()

    val women = value.contains("women") ||
        value.contains("wpl") ||
        value.contains("wbbl")

    val league = listOf(
        "ipl", "wpl", "bbl", "wbbl", "psl",
        "sa20", "cpl", "hundred",
        "premier league", "super league"
    ).any { value.contains(it) }

    if (women && league) return "Women's Leagues"

    if (women) return "Women International"

    if (league) return "Men's Leagues"

    val international = listOf(
        "international", "test match", "odi",
        "t20i", "world cup", "asia cup",
        "champions trophy", "wtc", "tour of",
        "tri-series"
    ).any { value.contains(it) }

    return if (international) {
        "International"
    } else {
        "Domestic"
    }
}

private fun parseCricketMatches(
    json: String
): List<MatchItem> {

    val root = JSONObject(json)

    val array = root.optJSONArray("data")
        ?: root.optJSONArray("matches")
        ?: JSONArray()

    val result = mutableListOf<MatchItem>()

    for (i in 0 until array.length()) {

        val item = array.optJSONObject(i) ?: continue

        val teams = item.optJSONArray("teams")
            ?: JSONArray()

        val home = teams.stringAt(0)
        val away = teams.stringAt(1)

        val teamInfo = item.optJSONArray("teamInfo")
            ?: JSONArray()

        var homeLogo = ""
        var awayLogo = ""

        for (j in 0 until teamInfo.length()) {

            val info = teamInfo.optJSONObject(j)
                ?: continue

            val name = info.readString("name", "shortname")
            val logo = info.readString("img", "logo")

            if (name == home) homeLogo = logo
            if (name == away) awayLogo = logo
        }

        val scores = item.optJSONArray("score")
            ?: JSONArray()

        var homeScore = ""
        var awayScore = ""

        for (j in 0 until scores.length()) {

            val score = scores.optJSONObject(j)
                ?: continue

            val inning = score.readString("inning")
            val scoreText = score.readScore()

            if (inning.contains(home, true)) {
                homeScore = scoreText
            } else if (inning.contains(away, true)) {
                awayScore = scoreText
            } else if (homeScore.isBlank()) {
                homeScore = scoreText
            } else {
                awayScore = scoreText
            }
        }

        val status = item.readString(
            "status",
            "matchStatus"
        )

        val finished =
            item.optBoolean("matchEnded", false) ||
                isMatchFinished(status)

        val stumps = isMatchStumps(status)

        val started =
            item.optBoolean("matchStarted", false)

        val live =
            !finished &&
                !stumps &&
                (started || isMatchLive(status))

        val competition = item.readString(
            "name",
            "series",
            "competition"
        )

        val category = classifyCricketMatch(
            competition,
            home,
            away
        )

        result.add(
            MatchItem(
                id = item.readString("id", "matchId"),
                sport = "Cricket",
                home = home,
                away = away,
                homeLogo = homeLogo,
                awayLogo = awayLogo,
                homeScore = homeScore,
                awayScore = awayScore,
                status = status,
                date = item.readString(
                    "date",
                    "dateTimeGMT"
                ),
                time = item.readString("time"),
                competition = competition,
                venue = item.readString("venue"),
                category = category,
                isLive = live,
                isFinished = finished
            )
        )
    }

    return result
}
private fun parseFootballMatches(
    json: String
): List<MatchItem> {

    val root = JSONObject(json)

    val array = root.optJSONArray("matches")
        ?: root.optJSONArray("data")
        ?: JSONArray()

    val result = mutableListOf<MatchItem>()

    for (i in 0 until array.length()) {

        val item = array.optJSONObject(i) ?: continue

        val home = item.readString(
            "home_team",
            "home",
            "homeTeam"
        )

        val away = item.readString(
            "away_team",
            "away",
            "awayTeam"
        )

        val status = item.readString(
            "status_text",
            "status"
        )

        val finished =
            status.equals("finished", true) ||
                isMatchFinished(status)

        val live =
            !finished &&
                !isMatchStumps(status) &&
                (
                    status.equals("live", true) ||
                    isMatchLive(status)
                )

        result.add(
            MatchItem(
                id = item.readString("id", "url"),
                sport = "Football",
                home = home,
                away = away,
                homeLogo = item.readString("home_logo"),
                awayLogo = item.readString("away_logo"),
                homeScore = item.readString("home_score"),
                awayScore = item.readString("away_score"),
                status = status,
                date = item.readString("date"),
                time = item.readString("time"),
                competition = item.readString("competition"),
                venue = item.readString("venue"),
                isLive = live,
                isFinished = finished
            )
        )
    }

    return result
}

private suspend fun fetchApi(
    endpoint: String
): String = withContext(Dispatchers.IO) {

    val connection =
        URL("$API_BASE/$endpoint")
            .openConnection() as HttpURLConnection

    try {

        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )

        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response =
            stream.bufferedReader().use {
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

    LaunchedEffect(
        selectedSport,
        selectedFilter,
        refreshKey
    ) {

        loading = true
        errorMessage = ""

        try {

            val endpoint = when (selectedSport) {

                "Cricket" -> when (selectedFilter) {
                    "Live" -> "currentMatches"
                    "Upcoming" -> "cricketMatches"
                    else -> "dbMatches"
                }

                "Football" -> when (selectedFilter) {
                    "Live" -> "footballLive"
                    "Upcoming" -> "footballUpcoming"
                    else -> "footballMatches"
                }

                else -> ""
            }

            if (endpoint.isBlank()) {

                matches = emptyList()

            } else {

                val response = fetchApi(endpoint)

                val allMatches = when (selectedSport) {
                    "Cricket" -> parseCricketMatches(response)
                    "Football" -> parseFootballMatches(response)
                    else -> emptyList()
                }

                matches = when (selectedFilter) {

                    "Live" -> allMatches.filter {
                        it.isLive && !it.isFinished
                    }

                    "Upcoming" -> allMatches.filter {
                        !it.isLive && !it.isFinished
                    }

                    "Recent" -> allMatches.filter {
                        it.isFinished
                    }

                    else -> allMatches
                }
            }

        } catch (e: Exception) {

            matches = emptyList()
            errorMessage = e.message ?: "Unable to load matches"

        } finally {

            loading = false
        }
    }

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
                    selectedFilter = "Live"
                    selectedMatch = null
                }
            )
        }

        // Main content ko remaining space milegi.
        // Bottom navigation iske bahar fixed rahegi.

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
                    QuizScreen(selectedSport)
                }

                selectedSection == AppSection.STATS -> {
                    StatsScreen(selectedSport)
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
            }
        }

        // FIXED BOTTOM NAVIGATION

        if (selectedMatch == null) {

            BottomNavigationBar(
                selectedSection = selectedSection,
                onSectionChange = {
                    selectedSection = it

                    if (it == AppSection.LIVE) {
                        selectedFilter = "Live"
                    }
                }
            )
        }
    }
}
@Composable
private fun SportexXHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            Text(
                "SPORTEXX",
                color = PrimaryText,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )

            Text(
                "SPORTS • NEWS • STATS",
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
                .padding(horizontal = 11.dp, vertical = 8.dp)
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

@Composable
private fun SportSelectorBar(
    selectedSport: String,
    onSportChange: (String) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        contentPadding = PaddingValues(horizontal = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        items(sportsList) { sport ->

            val selected = selectedSport == sport

            Surface(
                modifier = Modifier.clickable {
                    onSportChange(sport)
                },
                shape = RoundedCornerShape(9.dp),
                color = if (selected) SoftBlue else SurfaceColor
            ) {

                Text(
                    sport,
                    modifier = Modifier.padding(
                        horizontal = 15.dp,
                        vertical = 10.dp
                    ),
                    color = if (selected) PrimaryText else SecondaryText,
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
            contentPadding = PaddingValues(horizontal = 3.dp)
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
                                if (selected) AccentBlue else Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        when (item.first) {
                            "Home" -> "⌂"
                            "Live" -> "●"
                            "News" -> "▤"
                            "Quiz" -> "?"
                            "Stats" -> "▥"
                            else -> "•••"
                        },
                        color = if (selected) AccentBlue else SecondaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        item.first,
                        color = if (selected) PrimaryText else SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
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

        MatchFilterBar(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange
        )

        if (selectedSport == "Cricket") {
            CricketCategoryBar(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }

        when {

            selectedSport !in listOf("Cricket", "Football") -> {
                ComingSoonScreen(selectedSport)
            }

            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }

            errorMessage.isNotBlank() -> {
                ErrorScreen(errorMessage, onRefresh)
            }

            filteredMatches.isEmpty() -> {
                EmptyMatchesScreen(selectedFilter, onRefresh)
            }

            else -> {
                Column(Modifier.fillMaxSize()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            when (selectedFilter) {
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

@Composable
private fun MatchFilterBar(
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        listOf("Live", "Upcoming", "Recent").forEach { filter ->

            val selected = selectedFilter == filter

            Surface(
                modifier = Modifier.clickable {
                    onFilterChange(filter)
                },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) SoftBlue else SurfaceColor
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
                        color = if (selected) PrimaryText else SecondaryText,
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

@Composable
private fun CricketCategoryBar(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        contentPadding = PaddingValues(horizontal = 13.dp),
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
                    color = if (selectedCategory == category) {
                        PrimaryText
                    } else {
                        SecondaryText
                    },
                    fontSize = 11.sp,
                    fontWeight = if (selectedCategory == category) {
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
                            if (selectedCategory == category) {
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
}

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

            SportMatchCard(match) {
                onMatchClick(match)
            }
        }
    }
}
@Composable
private fun SportMatchCard(
    match: MatchItem,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = CardColor,
        shape = RoundedCornerShape(13.dp)
    ) {

        Column(
            modifier = Modifier.padding(13.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    match.competition.ifBlank {
                        match.sport
                    },
                    color = SecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                if (match.isLive) {
                    Text(
                        "● LIVE",
                        color = LiveGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (match.isFinished) {
                    Text(
                        "FINISHED",
                        color = SecondaryText,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TeamMatchRow(
                name = match.home,
                logo = match.homeLogo,
                score = match.homeScore
            )

            Spacer(Modifier.height(10.dp))

            TeamMatchRow(
                name = match.away,
                logo = match.awayLogo,
                score = match.awayScore
            )

            if (match.status.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    match.status,
                    color = if (match.isLive) LiveGreen else SecondaryText,
                    fontSize = 11.sp
                )
            } else if (match.date.isNotBlank() || match.time.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    listOf(match.date, match.time)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            if (match.venue.isNotBlank()) {
                Spacer(Modifier.height(5.dp))

                Text(
                    match.venue,
                    color = SecondaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

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

        TeamLogoView(logo, name)

        Spacer(Modifier.width(10.dp))

        Text(
            name.ifBlank { "Team" },
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Text(
            score,
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TeamLogoView(
    logo: String,
    teamName: String
) {

    if (logo.startsWith("http")) {

        AsyncImage(
            model = logo,
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
                teamName.firstOrNull()?.uppercase() ?: "?",
                color = PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyMatchesScreen(
    filter: String,
    onRefresh: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "No $filter matches",
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(7.dp))

        Text(
            "Matches will appear here when available.",
            color = SecondaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onRefresh) {
            Text("Refresh", color = AccentBlue)
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRefresh: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "Unable to load matches",
            color = PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            message,
            color = SecondaryText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onRefresh) {
            Text("Try Again", color = AccentBlue)
        }
    }
}

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
            sport,
            color = PrimaryText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(9.dp))

        Text(
            "This feature is coming soon.",
            color = SecondaryText,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MatchCentreScreen(
    match: MatchItem,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(onClick = onBack) {
                Text("‹ Back", color = AccentBlue)
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "MATCH CENTRE",
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        MatchCentreScoreHeader(match)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(13.dp)
        ) {

            MatchInfoTab(match)

            Spacer(Modifier.height(13.dp))

            MatchOverviewTab(match)

            Spacer(Modifier.height(13.dp))

            MatchUnavailableTab(
                "Scorecard",
                "Detailed scorecard will appear when supported data is available."
            )

            Spacer(Modifier.height(13.dp))

            MatchUnavailableTab(
                "Squad",
                "Squad information is not available yet."
            )

            Spacer(Modifier.height(13.dp))

            MatchUnavailableTab(
                "Stats",
                "Verified match statistics are not available yet."
            )

            Spacer(Modifier.height(13.dp))

            MatchUnavailableTab(
                "Head to Head",
                "Head-to-head data is not available yet."
            )

            Spacer(Modifier.height(13.dp))

            MatchUnavailableTab(
                "Highlights",
                "Highlights are not available yet."
            )
        }
    }
}

@Composable
private fun MatchCentreScoreHeader(
    match: MatchItem
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        color = CardColor,
        shape = RoundedCornerShape(14.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                match.competition.ifBlank { match.sport },
                color = SecondaryText,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    TeamLogoView(match.homeLogo, match.home)

                    Spacer(Modifier.height(7.dp))

                    Text(
                        match.home,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        match.homeScore.ifBlank { "—" },
                        color = PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "VS",
                    color = SecondaryText,
                    fontSize = 12.sp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    TeamLogoView(match.awayLogo, match.away)

                    Spacer(Modifier.height(7.dp))

                    Text(
                        match.away,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        match.awayScore.ifBlank { "—" },
                        color = PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            Text(
                match.status.ifBlank { match.time },
                color = if (match.isLive) LiveGreen else SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun MatchInfoTab(match: MatchItem) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {

        MatchSectionTitle("Match Information")

        MatchInfoRow("Sport", match.sport)
        MatchInfoRow("Competition", match.competition)
        MatchInfoRow("Date", match.date)
        MatchInfoRow("Time", match.time)
        MatchInfoRow("Venue", match.venue)
        MatchInfoRow("Status", match.status)
    }
}

@Composable
private fun MatchOverviewTab(match: MatchItem) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {

        MatchSectionTitle("Overview")

        Text(
            "${match.home} vs ${match.away}",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Match information is shown above. Verified commentary and additional match data will appear when supported by the API.",
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MatchSectionTitle(title: String) {

    Text(
        title,
        color = PrimaryText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MatchInfoRow(
    label: String,
    value: String
) {

    if (value.isNotBlank()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {

            Text(
                label,
                color = SecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                value,
                color = PrimaryText,
                fontSize = 12.sp,
                modifier = Modifier.weight(1.4f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun MatchUnavailableTab(
    title: String,
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {

        MatchSectionTitle(title)

        Text(
            message,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MatchUnavailableContent(
    title: String,
    message: String
) {
    MatchUnavailableTab(title, message)
}

@Composable
private fun NewsScreen() {
    FeaturePlaceholder(
        title = "Sports News",
        message = "Verified sports news will appear here."
    )
}

@Composable
private fun QuizScreen(sport: String) {
    FeaturePlaceholder(
        title = "$sport Quiz",
        message = "Free quizzes and points are coming soon. No cash rewards or paid entry."
    )
}

@Composable
private fun StatsScreen(sport: String) {
    FeaturePlaceholder(
        title = "$sport Stats",
        message = "Verified player and team statistics will appear here when data is available."
    )
}

@Composable
private fun MoreScreen(
    onOpenRankings: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(15.dp)
    ) {

        Text(
            "More",
            color = PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        MoreMenuItem(
            title = "Football Rankings",
            subtitle = "League tables and player rankings",
            onClick = onOpenRankings
        )

        MoreMenuItem(
            title = "About SportexX",
            subtitle = "Sports, news and statistics"
        )

        MoreMenuItem(
            title = "Coming Soon",
            subtitle = "More features are under development"
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        color = CardColor,
        shape = RoundedCornerShape(12.dp)
    ) {

        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    title,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    subtitle,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            Text(
                "›",
                color = AccentBlue,
                fontSize = 23.sp
            )
        }
    }
}

@Composable
private fun RankingsScreen(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("‹ Back", color = AccentBlue)
        }

        FeaturePlaceholder(
            title = "Football Rankings",
            message = "League-wise team standings and separate player rankings will appear here when verified data is supported."
        )
    }
}

@Composable
private fun FeaturePlaceholder(
    title: String,
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            title,
            color = PrimaryText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            message,
            color = SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}