
package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
private val DividerColor = Color(0xFF304057)

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

private val matchFilters = listOf(
    "Live",
    "Upcoming",
    "Recent"
)

private enum class AppSection {
    HOME,
    LIVE,
    NEWS,
    QUIZ,
    STATS,
    MORE,
    RANKINGS,
    EVENTS,
    MATCH_DETAILS
}

private data class GoalEvent(
    val minute: String = "",
    val scorer: String = "",
    val assist: String = "",
    val type: String = "Goal"
)

private data class MatchItem(
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
    val competitionLogo: String = "",
    val venue: String = "",
    val category: String = "",
    val result: String = "",
    val matchUrl: String = "",
    val isLive: Boolean = false,
    val isFinished: Boolean = false,
    val isUpcoming: Boolean = false,
    val goalEvents: List<GoalEvent> = emptyList()
)

private data class TeamRanking(
    val position: Int = 0,
    val team: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDifference: Int = 0,
    val points: Int = 0,
    val logo: String = ""
)

private data class PlayerRanking(
    val position: Int = 0,
    val name: String = "",
    val team: String = "",
    val value: String = "",
    val category: String = "",
    val photo: String = ""
)

private data class NewsItem(
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val source: String = "",
    val publishedAt: String = "",
    val url: String = ""
)

private data class EventItem(
    val name: String = "",
    val sport: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = ""
)

private fun JSONObject.readString(vararg keys: String): String {
    for (key in keys) {
        val value = optString(key, "")
        if (value.isNotBlank() && value != "null") {
            return value
        }
    }
    return ""
}

private fun JSONObject.readInt(vararg keys: String): Int {
    for (key in keys) {
        if (has(key)) {
            val value = optString(key, "")
            value.toIntOrNull()?.let { return it }
        }
    }
    return 0
}

private fun JSONObject.readArray(vararg keys: String): JSONArray {
    for (key in keys) {
        val array = optJSONArray(key)
        if (array != null) return array
    }
    return JSONArray()
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
        value.contains("full time") ||
        value == "ft"
}

private fun isMatchStumps(status: String): Boolean {
    return status.lowercase().contains("stumps")
}

private fun isMatchLive(status: String): Boolean {
    val value = status.lowercase()

    if (isMatchFinished(value) || isMatchStumps(value)) {
        return false
    }

    return value.contains("live") ||
        value.contains("in progress") ||
        value.contains("innings") ||
        value.contains("1st half") ||
        value.contains("2nd half") ||
        value.contains("half time") ||
        value.contains("break")
}

private fun isMatchUpcoming(status: String): Boolean {
    val value = status.lowercase()

    return value.contains("upcoming") ||
        value.contains("scheduled") ||
        value.contains("not started") ||
        value.contains("yet to begin") ||
        value.contains("starts")
}

private suspend fun fetchApi(path: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL("$API_BASE$path")
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode = connection.responseCode

            if (responseCode in 200..299) {
                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

private fun JSONObject.extractMatches(): JSONArray {
    val possibleKeys = listOf(
        "data",
        "matches",
        "results",
        "response",
        "items"
    )

    for (key in possibleKeys) {
        val value = opt(key)

        if (value is JSONArray) {
            return value
        }

        if (value is JSONObject) {
            val nested = value.extractMatches()
            if (nested.length() > 0) return nested
        }
    }

    return JSONArray()
}

private fun JSONObject.extractText(
    vararg keys: String
): String {
    return readString(*keys)
}

private fun JSONArray.toMatchList(
    sport: String
): List<MatchItem> {
    val result = mutableListOf<MatchItem>()

    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue

        val status = item.readString(
            "status",
            "matchStatus",
            "state",
            "statusText"
        )

        val home = item.readString(
            "home",
            "homeTeam",
            "team1",
            "teamA",
            "localteam"
        )

        val away = item.readString(
            "away",
            "awayTeam",
            "team2",
            "teamB",
            "visitorteam"
        )

        val homeObject = item.optJSONObject("homeTeam")
            ?: item.optJSONObject("team1")

        val awayObject = item.optJSONObject("awayTeam")
            ?: item.optJSONObject("team2")

        val homeName = if (home.isNotBlank()) {
            home
        } else {
            homeObject?.readString("name", "title") ?: ""
        }

        val awayName = if (away.isNotBlank()) {
            away
        } else {
            awayObject?.readString("name", "title") ?: ""
        }

        if (homeName.isBlank() && awayName.isBlank()) {
            continue
        }

        result.add(
            MatchItem(
                id = item.readString(
                    "id",
                    "matchId",
                    "fixtureId"
                ),
                sport = sport,
                home = homeName,
                away = awayName,
                homeLogo = item.readString(
                    "homeLogo",
                    "team1Logo"
                ),
                awayLogo = item.readString(
                    "awayLogo",
                    "team2Logo"
                ),
                homeScore = item.readString(
                    "homeScore",
                    "team1Score",
                    "score1"
                ),
                awayScore = item.readString(
                    "awayScore",
                    "team2Score",
                    "score2"
                ),
                status = status,
                date = item.readString(
                    "date",
                    "matchDate"
                ),
                time = item.readString(
                    "time",
                    "matchTime"
                ),
                competition = item.readString(
                    "competition",
                    "series",
                    "tournament",
                    "league"
                ),
                venue = item.readString(
                    "venue",
                    "ground",
                    "stadium"
                ),
                category = item.readString(
                    "category",
                    "matchType",
                    "format"
                ),
                result = item.readString(
                    "result",
                    "resultText"
                ),
                isLive = isMatchLive(status),
                isFinished = isMatchFinished(status),
                isUpcoming = isMatchUpcoming(status)
            )
        )
    }

    return result
}

private fun JSONObject.extractNews(): List<NewsItem> {
    val array = extractMatches()
    val result = mutableListOf<NewsItem>()

    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue

        result.add(
            NewsItem(
                title = item.readString(
                    "title",
                    "headline",
                    "name"
                ),
                description = item.readString(
                    "description",
                    "summary",
                    "content"
                ),
                image = item.readString(
                    "image",
                    "imageUrl",
                    "thumbnail"
                ),
                source = item.readString(
                    "source",
                    "publisher"
                ),
                publishedAt = item.readString(
                    "publishedAt",
                    "date",
                    "time"
                ),
                url = item.readString(
                    "url",
                    "link"
                )
            )
        )
    }

    return result.filter { it.title.isNotBlank() }
}

private fun JSONObject.extractEvents(): List<EventItem> {
    val array = extractMatches()
    val result = mutableListOf<EventItem>()

    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue

        result.add(
            EventItem(
                name = item.readString(
                    "name",
                    "title",
                    "event"
                ),
                sport = item.readString(
                    "sport",
                    "category"
                ),
                location = item.readString(
                    "location",
                    "venue",
                    "host"
                ),
                startDate = item.readString(
                    "startDate",
                    "start",
                    "date"
                ),
                endDate = item.readString(
                    "endDate",
                    "end"
                ),
                status = item.readString(
                    "status",
                    "state"
                )
            )
        )
    }

    return result.filter { it.name.isNotBlank() }
}

private fun JSONObject.extractGoalEvents(): List<GoalEvent> {
    val possibleKeys = listOf(
        "events",
        "goals",
        "incidents",
        "timeline"
    )

    var eventArray = JSONArray()

    for (key in possibleKeys) {
        val array = optJSONArray(key)
        if (array != null) {
            eventArray = array
            break
        }
    }

    val result = mutableListOf<GoalEvent>()

    for (index in 0 until eventArray.length()) {
        val event = eventArray.optJSONObject(index) ?: continue

        val type = event.readString(
            "type",
            "eventType",
            "detail",
            "incident"
        )

        val normalizedType = type.lowercase()

        val isGoal = normalizedType.contains("goal") ||
            normalizedType.contains("penalty") ||
            normalizedType.contains("own goal")

        if (!isGoal) continue

        result.add(
            GoalEvent(
                minute = event.readString(
                    "minute",
                    "time",
                    "elapsed",
                    "displayTime"
                ),
                scorer = event.readString(
                    "player",
                    "scorer",
                    "playerName",
                    "name"
                ),
                assist = event.readString(
                    "assist",
                    "assistPlayer",
                    "assistName"
                ),
                type = type.ifBlank { "Goal" }
            )
        )
    }

    return result
}

private fun JSONObject.extractFootballMatch(): MatchItem {
    val homeObject = optJSONObject("homeTeam")
        ?: optJSONObject("team1")
        ?: JSONObject()

    val awayObject = optJSONObject("awayTeam")
        ?: optJSONObject("team2")
        ?: JSONObject()

    val status = readString(
        "status",
        "matchStatus",
        "state"
    )

    return MatchItem(
        id = readString(
            "id",
            "matchId",
            "fixtureId"
        ),
        sport = "Football",
        home = readString(
            "home",
            "homeName"
        ).ifBlank {
            homeObject.readString("name", "title")
        },
        away = readString(
            "away",
            "awayName"
        ).ifBlank {
            awayObject.readString("name", "title")
        },
        homeLogo = readString(
            "homeLogo"
        ).ifBlank {
            homeObject.readString("logo", "image")
        },
        awayLogo = readString(
            "awayLogo"
        ).ifBlank {
            awayObject.readString("logo", "image")
        },
        homeScore = readString(
            "homeScore",
            "score1"
        ),
        awayScore = readString(
            "awayScore",
            "score2"
        ),
        status = status,
        date = readString("date", "matchDate"),
        time = readString("time", "matchTime"),
        competition = readString(
            "competition",
            "league",
            "tournament"
        ),
        venue = readString(
            "venue",
            "stadium",
            "ground"
        ),
        result = readString("result", "resultText"),
        isLive = isMatchLive(status),
        isFinished = isMatchFinished(status),
        isUpcoming = isMatchUpcoming(status),
        goalEvents = extractGoalEvents()
    )
}

private fun formatGoalEvent(event: GoalEvent): String {
    val minute = event.minute
        .takeIf { it.isNotBlank() }
        ?.let { "$it'" }
        ?: ""

    val scorer = event.scorer.ifBlank {
        "Unknown scorer"
    }

    val assist = event.assist
        .takeIf { it.isNotBlank() }
        ?.let { " • Assist: $it" }
        ?: ""

    val type = event.type
        .takeIf { it.isNotBlank() }
        ?: "Goal"

    return "$minute  $scorer • $type$assist"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.rgb(16, 24, 39)

        window.navigationBarColor =
            android.graphics.Color.rgb(16, 24, 39)

        setContent {
            SportexXApp()
        }
    }
}

@Composable
private fun SportexXApp() {
    var selectedSport by remember {
        mutableStateOf("Cricket")
    }

    var selectedNavigation by remember {
        mutableStateOf("Home")
    }

    var selectedFilter by remember {
        mutableStateOf("Live")
    }

    var selectedCricketCategory by remember {
        mutableStateOf("International")
    }

    var selectedSection by remember {
        mutableStateOf(AppSection.HOME)
    }

    var selectedMatch by remember {
        mutableStateOf<MatchItem?>(null)
    }

    var cricketMatches by remember {
        mutableStateOf<List<MatchItem>>(emptyList())
    }

    var footballMatches by remember {
        mutableStateOf<List<MatchItem>>(emptyList())
    }

    var newsItems by remember {
        mutableStateOf<List<NewsItem>>(emptyList())
    }

    var eventItems by remember {
        mutableStateOf<List<EventItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var lastUpdated by remember {
        mutableStateOf("")
    }

    var refreshCounter by remember {
        mutableIntStateOf(0)
    }

    val currentMatches = if (selectedSport == "Football") {
        footballMatches
    } else {
        cricketMatches
    }

    val filteredMatches = when (selectedFilter) {
        "Live" -> currentMatches.filter { it.isLive }

        "Upcoming" -> currentMatches.filter {
            it.isUpcoming
        }

        "Recent" -> currentMatches.filter {
            it.isFinished
        }

        else -> currentMatches
    }

    LaunchedEffect(
        selectedSport,
        selectedNavigation,
        refreshCounter
    ) {
        if (selectedSport == "Cricket" ||
            selectedSport == "Football"
        ) {
            isLoading = true
            errorMessage = ""

            try {
                if (selectedSport == "Cricket") {
                    val response = fetchApi(
                        "/api/currentMatches"
                    )

                    if (response != null) {
                        cricketMatches =
                            response.extractMatches()
                                .toMatchList("Cricket")
                    }
                } else {
                    val response = fetchApi(
                        "/api/widget/matches/?sport=football&limit=10"
                    )

                    if (response != null) {
                        footballMatches =
                            response.extractMatches()
                                .toMatchList("Football")
                    }
                }

                lastUpdated = java.text.SimpleDateFormat(
                    "hh:mm a",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                if (currentMatches.isEmpty()) {
                    errorMessage =
                        "Match data is not available right now."
                }
            } catch (e: Exception) {
                errorMessage =
                    "Unable to load data. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SportexXHeader(
                    selectedSport = selectedSport,
                    onSportChange = {
                        selectedSport = it
                        selectedFilter = "Live"
                    }
                )

                when (selectedSection) {
                    AppSection.MATCH_DETAILS -> {
                        MatchDetailsScreen(
                            match = selectedMatch,
                            onBack = {
                                selectedSection =
                                    AppSection.HOME
                            }
                        )
                    }

                    else -> {
                        SportexXMainContent(
                            selectedNavigation =
                                selectedNavigation,
                            selectedSport = selectedSport,
                            selectedFilter = selectedFilter,
                            selectedCategory =
                                selectedCricketCategory,
                            matches = filteredMatches,
                            allMatches = currentMatches,
                            newsItems = newsItems,
                            eventItems = eventItems,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            lastUpdated = lastUpdated,
                            onFilterChange = {
                                selectedFilter = it
                            },
                            onCategoryChange = {
                                selectedCricketCategory = it
                            },
                            onMatchClick = {
                                selectedMatch = it
                                selectedSection =
                                    AppSection.MATCH_DETAILS
                            },
                            onRefresh = {
                                refreshCounter++
                            }
                        )
                    }
                }

                SportexXBottomNavigation(
                    selectedItem = selectedNavigation,
                    onItemSelected = {
                        selectedNavigation = it

                        selectedSection = when (it) {
                            "Home" -> AppSection.HOME
                            "Live" -> AppSection.LIVE
                            "News" -> AppSection.NEWS
                            "Quiz" -> AppSection.QUIZ
                            "Stats" -> AppSection.STATS
                            else -> AppSection.MORE
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SportexXHeader(
    selectedSport: String,
    onSportChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 10.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "SPORTEXX",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "SPORTS • NEWS • STATS",
                    color = SecondaryText,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        SoftBlue,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(
                        horizontal = 11.dp,
                        vertical = 7.dp
                    )
            ) {
                Text(
                    text = "SPORTS HUB",
                    color = PrimaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            sportsList.forEach { sport ->
                val selected = selectedSport == sport

                Box(
                    modifier = Modifier
                        .background(
                            color = if (selected) {
                                AccentBlue
                            } else {
                                SurfaceColor
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            onSportChange(sport)
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        )
                ) {
                    Text(
                        text = sport,
                        color = if (selected) {
                            Color.White
                        } else {
                            SecondaryText
                        },
                        fontSize = 12.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                }
            }
        }

        if (selectedSport !in listOf("Cricket", "Football")) {
            Spacer(modifier = Modifier.height(12.dp))

            ComingSoonMessage(
                title = "$selectedSport",
                description = "This feature is coming soon"
            )
        }
    }
}

@Composable
private fun ComingSoonMessage(
    title: String,
    description: String = "This feature is coming soon"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SurfaceColor,
                RoundedCornerShape(12.dp)
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = description,
            color = SecondaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SportexXMainContent(
    selectedNavigation: String,
    selectedSport: String,
    selectedFilter: String,
    selectedCategory: String,
    matches: List<MatchItem>,
    allMatches: List<MatchItem>,
    newsItems: List<NewsItem>,
    eventItems: List<EventItem>,
    isLoading: Boolean,
    errorMessage: String,
    lastUpdated: String,
    onFilterChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMatchClick: (MatchItem) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (selectedNavigation) {
            "Home", "Live" -> {
                MatchHomeScreen(
                    selectedSport = selectedSport,
                    selectedFilter = selectedFilter,
                    selectedCategory = selectedCategory,
                    matches = matches,
                    allMatches = allMatches,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    lastUpdated = lastUpdated,
                    onFilterChange = onFilterChange,
                    onCategoryChange = onCategoryChange,
                    onMatchClick = onMatchClick,
                    onRefresh = onRefresh
                )
            }

            "News" -> {
                NewsScreen(
                    newsItems = newsItems
                )
            }

            "Quiz" -> {
                QuizScreen()
            }

            "Stats" -> {
                StatsScreen(
                    selectedSport = selectedSport
                )
            }

            else -> {
                MoreScreen(
                    eventItems = eventItems
                )
            }
        }
    }
}

@Composable
private fun MatchHomeScreen(
    selectedSport: String,
    selectedFilter: String,
    selectedCategory: String,
    matches: List<MatchItem>,
    allMatches: List<MatchItem>,
    isLoading: Boolean,
    errorMessage: String,
    lastUpdated: String,
    onFilterChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMatchClick: (MatchItem) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedSport == "Cricket") {
                    "Cricket Matches"
                } else {
                    "Football Matches"
                },
                color = PrimaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Refresh",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onRefresh()
                }
            )
        }

        if (selectedSport == "Cricket") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cricketCategories.forEach { category ->
                    FilterChipItem(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = {
                            onCategoryChange(category)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            matchFilters.forEach { filter ->
                FilterChipItem(
                    text = filter,
                    selected = selectedFilter == filter,
                    onClick = {
                        onFilterChange(filter)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (lastUpdated.isNotBlank()) {
            Text(
                text = "Last updated: $lastUpdated",
                color = SecondaryText,
                fontSize = 10.sp,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentBlue
                    )
                }
            }

            matches.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage.ifBlank {
                                "No ${selectedFilter.lowercase()} matches available."
                            },
                            color = SecondaryText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tap Refresh to try again",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                onRefresh()
                            }
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = matches,
                        key = {
                            it.id.ifBlank {
                                "${it.home}_${it.away}_${it.date}"
                            }
                        }
                    ) { match ->
                        MatchCard(
                            match = match,
                            onClick = {
                                onMatchClick(match)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) {
                    SoftBlue
                } else {
                    SurfaceColor
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 13.dp,
                vertical = 9.dp
            )
    ) {
        Text(
            text = text,
            color = if (selected) {
                PrimaryText
            } else {
                SecondaryText
            },
            fontSize = 11.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

@Composable
private fun MatchCard(
    match: MatchItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                CardColor,
                RoundedCornerShape(14.dp)
            )
            .clickable {
                onClick()
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = match.competition.ifBlank {
                    match.sport
                },
                color = SecondaryText,
                fontSize = 11.sp,
                maxLines = 1
            )

            Text(
                text = match.status.ifBlank {
                    match.date
                },
                color = if (match.isLive) {
                    LiveGreen
                } else {
                    SecondaryText
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                if (match.homeLogo.isNotBlank()) {
                    AsyncImage(
                        model = match.homeLogo,
                        contentDescription = match.home,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = match.home.ifBlank { "Team 1" },
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (
                        match.homeScore.isNotBlank() ||
                        match.awayScore.isNotBlank()
                    ) {
                        "${match.homeScore} - ${match.awayScore}"
                    } else {
                        "VS"
                    },
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (match.time.isNotBlank()) {
                    Text(
                        text = match.time,
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                if (match.awayLogo.isNotBlank()) {
                    AsyncImage(
                        model = match.awayLogo,
                        contentDescription = match.away,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = match.away.ifBlank { "Team 2" },
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        if (match.sport == "Football" &&
            match.goalEvents.isNotEmpty()
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = DividerColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GOAL TIMELINE",
                color = AccentBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            match.goalEvents.forEach { event ->
                Text(
                    text = formatGoalEvent(event),
                    color = PrimaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        top = 5.dp
                    )
                )
            }
        }

        if (match.result.isNotBlank()) {
            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = match.result,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        if (match.venue.isNotBlank()) {
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = match.venue,
                color = SecondaryText,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "View Match Details  →",
            color = AccentBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatchDetailsScreen(
    match: MatchItem?,
    onBack: () -> Unit
) {
    var selectedTab by remember(match?.id) {
        mutableStateOf("Info")
    }

    if (match == null) {
        ComingSoonMessage(
            title = "Match details unavailable"
        )
        return
    }

    val tabs = if (match.sport == "Cricket") {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = "MATCH CENTRE",
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        MatchCard(
            match = match,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                FilterChipItem(
                    text = tab,
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            "Info" -> MatchInfoContent(match)

            "Overview" -> MatchOverviewContent(match)

            "Scorecard" -> {
                ComingSoonMessage(
                    title = "Scorecard",
                    description = "This feature is coming soon"
                )
            }

            "Squad" -> {
                ComingSoonMessage(
                    title = "Squad",
                    description = "This feature is coming soon"
                )
            }

            "Lineups" -> {
                ComingSoonMessage(
                    title = "Lineups",
                    description = "This feature is coming soon"
                )
            }

            "Stats" -> {
                ComingSoonMessage(
                    title = "Match Stats",
                    description = "This feature is coming soon"
                )
            }

            "H2H" -> {
                ComingSoonMessage(
                    title = "Head to Head",
                    description = "This feature is coming soon"
                )
            }

            "Highlights" -> {
                ComingSoonMessage(
                    title = "Highlights",
                    description = "This feature is coming soon"
                )
            }
        }
    }
}

@Composable
private fun MatchInfoContent(
    match: MatchItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                SurfaceColor,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        MatchInfoRow("Sport", match.sport)
        MatchInfoRow("Competition", match.competition)
        MatchInfoRow("Date", match.date)
        MatchInfoRow("Time", match.time)
        MatchInfoRow("Venue", match.venue)
        MatchInfoRow("Status", match.status)
        MatchInfoRow("Result", match.result)
    }
}

@Composable
private fun MatchInfoRow(
    label: String,
    value: String
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }

    HorizontalDivider(
        color = DividerColor
    )
}

@Composable
private fun MatchOverviewContent(
    match: MatchItem
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Text(
                text = "Match Overview",
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            MatchInfoContent(match)
        }

        if (match.sport == "Football") {
            item {
                Text(
                    text = "Goal Timeline",
                    color = PrimaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (match.goalEvents.isEmpty()) {
                item {
                    ComingSoonMessage(
                        title = "Goal Timeline",
                        description = "Goal events are not available from the API yet."
                    )
                }
            } else {
                items(match.goalEvents) { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                CardColor,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(13.dp)
                    ) {
                        Text(
                            text = formatGoalEvent(event),
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsScreen(
    newsItems: List<NewsItem>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Sports News",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (newsItems.isEmpty()) {
            ComingSoonMessage(
                title = "Sports News",
                description = "News feed is not connected yet."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsItems) { news ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                CardColor,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        if (news.image.isNotBlank()) {
                            AsyncImage(
                                model = news.image,
                                contentDescription = news.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                        }

                        Text(
                            text = news.title,
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = news.description,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )

                        if (news.source.isNotBlank()) {
                            Text(
                                text = news.source,
                                color = AccentBlue,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Sports Quiz",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    SurfaceColor,
                    RoundedCornerShape(14.dp)
                )
                .padding(18.dp)
        ) {
            Text(
                text = "Test Your Sports Knowledge",
                color = PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Play free quizzes and collect points.",
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No entry fee • No deposits • No cash rewards",
                color = AccentBlue,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(15.dp))

            ComingSoonMessage(
                title = "Quiz Coming Soon"
            )
        }
    }
}

@Composable
private fun StatsScreen(
    selectedSport: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "$selectedSport Stats",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        ComingSoonMessage(
            title = "Sports Statistics",
            description = "Verified statistics and rankings will appear here when supported by the API."
        )
    }
}

@Composable
private fun MoreScreen(
    eventItems: List<EventItem>
) {
    var selectedEventSection by remember {
        mutableStateOf("Live Events")
    }

    val eventSections = listOf(
        "Live Events",
        "Schedules",
        "Results",
        "Medal Tally",
        "Country Performance",
        "Upcoming Events"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "More",
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            eventSections.forEach { section ->
                FilterChipItem(
                    text = section,
                    selected = selectedEventSection == section,
                    onClick = {
                        selectedEventSection = section
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Olympics",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                EventSectionContent(
                    eventItems = eventItems,
                    selectedSection = selectedEventSection,
                    eventType = "Olympics"
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Commonwealth Games",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                EventSectionContent(
                    eventItems = eventItems,
                    selectedSection = selectedEventSection,
                    eventType = "Commonwealth Games"
                )
            }
        }
    }
}

@Composable
private fun EventSectionContent(
    eventItems: List<EventItem>,
    selectedSection: String,
    eventType: String
) {
    val matchingEvents = eventItems.filter {
        it.name.contains(eventType, ignoreCase = true) ||
            it.sport.contains(eventType, ignoreCase = true)
    }

    if (matchingEvents.isEmpty()) {
        ComingSoonMessage(
            title = selectedSection,
            description = "Verified $eventType data is not available yet."
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            matchingEvents.forEach { event ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CardColor,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = event.name,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Text(
                        text = event.location,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "${event.startDate} - ${event.endDate}",
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SportexXBottomNavigation(
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceColor,
        contentColor = PrimaryText
    ) {
        navigationItems.forEach { item ->
            val selected = selectedItem == item

            NavigationBarItem(
                selected = selected,
                onClick = {
                    onItemSelected(item)
                },
                icon = {
                    Text(
                        text = when (item) {
                            "Home" -> "⌂"
                            "Live" -> "●"
                            "News" -> "▤"
                            "Quiz" -> "?"
                            "Stats" -> "▥"
                            else -> "⋯"
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                label = {
                    Text(
                        text = item,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = PrimaryText,
                    indicatorColor = SoftBlue,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText
                )
            )
        }
    }
}
