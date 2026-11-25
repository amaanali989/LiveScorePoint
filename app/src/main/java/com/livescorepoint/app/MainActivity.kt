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
    "Cricket", "Football", "Hockey",
    "Esports", "F1", "Wrestling", "Kabaddi"
)

private val navigationItems = listOf(
    "Home", "Live", "News", "Quiz", "Stats", "More"
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
    HOME, LIVE, NEWS, QUIZ, STATS, MORE,
    RANKINGS, MATCH_DETAILS
}

private fun JSONObject.readString(vararg keys: String): String {
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

// Stumps wale match ko LIVE nahi dikhana
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
                id = item.readString(
                    "id",
                    "matchId"
                ),
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

        val statusCode = status.lowercase()

        val finished =
            statusCode == "finished" ||
                isMatchFinished(status)

        val live =
            !finished &&
                !isMatchStumps(status) &&
                (
                    statusCode == "live" ||
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

        if (selectedMatch == null) {

            Spacer(Modifier.weight(1f))

            BottomNavigationBar(
                selectedSection = selectedSection,
                onSectionChange = {
                    selectedSection = it
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(Modifier.padding(13.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    match.competition.ifBlank { match.sport },
                    color = SecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Text(
                    when {
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

            TeamMatchRow(match.home, match.homeLogo, match.homeScore)

            Spacer(Modifier.height(10.dp))

            TeamMatchRow(match.away, match.awayLogo, match.awayScore)

            if (match.status.isNotBlank() || match.venue.isNotBlank()) {
                Spacer(Modifier.height(11.dp))

                HorizontalDivider(
                    color = Color(0xFF304056),
                    thickness = 0.6.dp
                )

                Spacer(Modifier.height(8.dp))

                if (match.status.isNotBlank()) {
                    Text(
                        match.status,
                        color = if (match.isLive) LiveGreen else SecondaryText,
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
                .background(SoftBlue, RoundedCornerShape(50)),
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
            Icons.Default.Sports,
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

        OutlinedButton(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

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
            Icons.Default.CloudOff,
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

        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
private fun ComingSoonScreen(sport: String) {
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

// MATCH CENTRE

@Composable
private fun MatchCentreScreen(
    match: MatchItem,
    onBack: () -> Unit
) {
    val tabs = if (match.sport.equals("Cricket", true)) {
        listOf(
            "Info", "Overview", "Scorecard",
            "Squad", "Stats", "H2H", "Highlights"
        )
    } else {
        listOf(
            "Info", "Overview", "Lineups",
            "Stats", "H2H", "Highlights"
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    match.competition.ifBlank { match.sport },
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

            if (match.isLive) {
                Text(
                    "LIVE",
                    color = LiveGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        MatchCentreScoreHeader(match)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(tabs) { tab ->
                Column(
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab,
                        color = if (selectedTab == tab) PrimaryText else SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == tab) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )

                    Spacer(Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .width(25.dp)
                            .height(2.dp)
                            .background(
                                if (selectedTab == tab) AccentBlue else Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            }
        }

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

@Composable
private fun MatchCentreScoreHeader(match: MatchItem) {
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
            color = if (match.isLive) LiveGreen else SecondaryText,
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
                TeamLogoView(match.homeLogo, match.home)

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
                TeamLogoView(match.awayLogo, match.away)

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
// MATCH INFO

@Composable
private fun MatchInfoTab(match: MatchItem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { MatchSectionTitle("Match Information") }
        item { MatchInfoRow("Sport", match.sport) }
        item { MatchInfoRow("Competition", match.competition) }
        item { MatchInfoRow("Match Date", match.date) }
        item { MatchInfoRow("Match Time", match.time) }
        item { MatchInfoRow("Venue", match.venue) }
        item { MatchInfoRow("Match Status", match.status) }
        item { MatchInfoRow("Home Team", match.home) }
        item { MatchInfoRow("Away Team", match.away) }
    }
}

// MATCH OVERVIEW

@Composable
private fun MatchOverviewTab(match: MatchItem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MatchSectionTitle("Match Overview") }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
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

        item { MatchSectionTitle("Commentary") }

        item {
            MatchUnavailableContent(
                "Commentary is not available yet.",
                "Live commentary will appear here when the API provides verified commentary data."
            )
        }
    }
}

@Composable
private fun MatchSectionTitle(title: String) {
    Text(
        title,
        color = PrimaryText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MatchInfoRow(title: String, value: String) {
    if (value.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor),
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

        MatchUnavailableContent("Data not available", description)
    }
}

@Composable
private fun MatchUnavailableContent(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Info,
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

// NEWS

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
            "News Coming Soon",
            "Verified sports news and updates will appear here when a news source is connected."
        )
    }
}

// QUIZ

@Composable
private fun QuizScreen(selectedSport: String) {
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
            "Quiz Coming Soon",
            "Match-based questions and sports trivia will appear here when verified quiz data is available."
        )

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(15.dp)) {
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

// STATS

@Composable
private fun StatsScreen(selectedSport: String) {
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
            "Statistics Coming Soon",
            "Player statistics, team performance and detailed records will appear here when verified data is available."
        )
    }
}

// MORE

@Composable
private fun MoreScreen(onOpenRankings: () -> Unit) {
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
            "Football Rankings",
            "League tables and player rankings",
            onOpenRankings
        )

        Spacer(Modifier.height(10.dp))

        MoreMenuItem(
            "About SportexX",
            "Sports • News • Stats",
            {}
        )

        Spacer(Modifier.height(10.dp))

        MoreMenuItem(
            "App Version",
            "Version 1.0",
            {}
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
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
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
}

// RANKINGS

@Composable
private fun RankingsScreen(onBack: () -> Unit) {
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
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
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
                "Rankings Coming Soon",
                "League-wise team standings, top scorers, assists, appearances, goalkeeper rankings and clean sheets will appear when supported by the API."
            )
        }
    }
}

// COMMON PLACEHOLDER

@Composable
private fun FeaturePlaceholder(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SportsScore,
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