
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

private suspend fun apiGet(path: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val fullUrl = if (path.startsWith("http")) {
                path
            } else {
                API_BASE.trimEnd('/') + "/" + path.trimStart('/')
            }

            connection = URL(fullUrl)
                .openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode = connection.responseCode

            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            } ?: return@withContext null

            if (response.isBlank()) {
                null
            } else {
                JSONObject(response)
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

private fun JSONObject.firstString(
    vararg keys: String
): String {
    for (key in keys) {
        val value = opt(key)

        if (value != null && value != JSONObject.NULL) {
            val result = value.toString().trim()

            if (result.isNotEmpty() &&
                result != "null" &&
                result != "undefined"
            ) {
                return result
            }
        }
    }

    return ""
}

private fun JSONObject.firstObject(
    vararg keys: String
): JSONObject? {
    for (key in keys) {
        val value = optJSONObject(key)

        if (value != null) return value
    }

    return null
}

private fun JSONObject.firstArray(
    vararg keys: String
): JSONArray? {
    for (key in keys) {
        val value = optJSONArray(key)

        if (value != null) return value
    }

    return null
}

private fun JSONObject.firstInt(
    vararg keys: String
): Int {
    for (key in keys) {
        val value = opt(key)

        val number = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }

        if (number != null) return number
    }

    return 0
}

private fun JSONObject.firstBoolean(
    vararg keys: String
): Boolean {
    for (key in keys) {
        val value = opt(key)

        when (value) {
            is Boolean -> return value
            is String -> {
                if (value.equals("true", true)) return true
                if (value.equals("false", true)) return false
            }
        }
    }

    return false
}

private fun JSONObject.unwrapData(): JSONObject {
    return firstObject(
        "data",
        "response",
        "result"
    ) ?: this
}

private fun JSONArray.toObjectList(): List<JSONObject> {
    val result = mutableListOf<JSONObject>()

    for (index in 0 until length()) {
        val item = optJSONObject(index)

        if (item != null) {
            result.add(item)
        }
    }

    return result
}

private fun parseMatch(
    json: JSONObject,
    sport: String
): MatchItem {
    val homeObject = json.firstObject(
        "team1",
        "homeTeam",
        "home",
        "team_home"
    )

    val awayObject = json.firstObject(
        "team2",
        "awayTeam",
        "away",
        "team_away"
    )

    val homeName = homeObject?.firstString(
        "name",
        "teamName",
        "title",
        "shortName"
    ) ?: ""

    val awayName = awayObject?.firstString(
        "name",
        "teamName",
        "title",
        "shortName"
    ) ?: ""

    val home = if (homeName.isNotBlank()) {
        homeName
    } else {
        json.firstString(
            "team1Name",
            "homeTeamName",
            "home_name"
        )
    }

    val away = if (awayName.isNotBlank()) {
        awayName
    } else {
        json.firstString(
            "team2Name",
            "awayTeamName",
            "away_name"
        )
    }

    val homeScore = json.firstString(
        "team1Score",
        "homeScore",
        "home_score",
        "score1"
    ).ifBlank {
        homeObject?.firstString("score", "runs") ?: ""
    }

    val awayScore = json.firstString(
        "team2Score",
        "awayScore",
        "away_score",
        "score2"
    ).ifBlank {
        awayObject?.firstString("score", "runs") ?: ""
    }

    val status = json.firstString(
        "status",
        "matchStatus",
        "state",
        "statusText"
    )

    val statusLower = status.lowercase()

    val finished = statusLower.contains("finished") ||
        statusLower.contains("completed") ||
        statusLower.contains("result") ||
        statusLower.contains("ended")

    val upcoming = statusLower.contains("upcoming") ||
        statusLower.contains("scheduled") ||
        statusLower.contains("not started") ||
        statusLower.contains("fixture")

    val live = !finished && !upcoming &&
        (
            statusLower.contains("live") ||
            statusLower.contains("in progress") ||
            statusLower.contains("innings") ||
            statusLower.contains("playing")
        )

    val goals = parseGoalEvents(json)

    return MatchItem(
        id = json.firstString(
            "id",
            "matchId",
            "match_id",
            "fixtureId",
            "key"
        ),
        sport = sport,
        home = home,
        away = away,
        homeLogo = homeObject?.firstString(
            "logo",
            "image",
            "teamLogo",
            "crest"
        ) ?: json.firstString("team1Logo", "homeLogo"),
        awayLogo = awayObject?.firstString(
            "logo",
            "image",
            "teamLogo",
            "crest"
        ) ?: json.firstString("team2Logo", "awayLogo"),
        homeScore = homeScore,
        awayScore = awayScore,
        status = status,
        date = json.firstString(
            "date",
            "matchDate",
            "startDate",
            "utcDate"
        ),
        time = json.firstString(
            "time",
            "matchTime",
            "startTime"
        ),
        competition = json.firstString(
            "competition",
            "series",
            "league",
            "tournament",
            "seriesName"
        ),
        competitionLogo = json.firstString(
            "competitionLogo",
            "leagueLogo",
            "seriesLogo"
        ),
        venue = json.firstString(
            "venue",
            "ground",
            "stadium"
        ),
        category = json.firstString(
            "category",
            "format",
            "matchType"
        ),
        result = json.firstString(
            "result",
            "resultText",
            "resultInfo"
        ),
        matchUrl = json.firstString(
            "url",
            "matchUrl",
            "match_url"
        ),
        isLive = live,
        isFinished = finished,
        isUpcoming = upcoming,
        goalEvents = goals
    )
}

private fun parseMatches(
    response: JSONObject,
    sport: String
): List<MatchItem> {
    val root = response.unwrapData()

    val array = root.firstArray(
        "matches",
        "items",
        "fixtures",
        "events",
        "data"
    ) ?: response.firstArray(
        "matches",
        "items",
        "fixtures",
        "events"
    ) ?: return emptyList()

    return array.toObjectList()
        .map { parseMatch(it, sport) }
        .filter {
            it.home.isNotBlank() &&
            it.away.isNotBlank()
        }
        .distinctBy {
            it.id.ifBlank {
                it.home + "_" + it.away + "_" + it.date
            }
        }
}

private fun parseGoalEvents(
    json: JSONObject
): List<GoalEvent> {
    val events = json.firstArray(
        "goals",
        "goalEvents",
        "events",
        "incidents",
        "scorers"
    ) ?: return emptyList()

    return events.toObjectList()
        .mapNotNull { event ->
            val type = event.firstString(
                "type",
                "eventType",
                "detail"
            ).ifBlank { "Goal" }

            val typeLower = type.lowercase()

            if (
                !typeLower.contains("goal") &&
                !typeLower.contains("penalty") &&
                !typeLower.contains("own")
            ) {
                return@mapNotNull null
            }

            val player = event.firstObject(
                "player",
                "scorer",
                "playerIn"
            )

            val assistObject = event.firstObject(
                "assist",
                "assistPlayer"
            )

            val scorer = player?.firstString(
                "name",
                "playerName",
                "displayName"
            ) ?: event.firstString(
                "scorer",
                "playerName",
                "player",
                "name"
            )

            val assist = assistObject?.firstString(
                "name",
                "playerName"
            ) ?: event.firstString(
                "assistName",
                "assist",
                "assistedBy"
            )

            val minute = event.firstString(
                "minute",
                "time",
                "elapsed",
                "matchTime"
            )

            if (scorer.isBlank() || minute.isBlank()) {
                return@mapNotNull null
            }

            GoalEvent(
                minute = minute,
                scorer = scorer,
                assist = assist,
                type = type
            )
        }
        .sortedBy {
            it.minute.filter { char -> char.isDigit() }
                .toIntOrNull() ?: 0
        }
        .distinctBy {
            it.minute + "_" + it.scorer
        }
}

private fun goalLabel(goal: GoalEvent): String {
    val extra = when {
        goal.type.contains("own", true) -> " (OG)"
        goal.type.contains("penalty", true) -> " (P)"
        else -> ""
    }

    return "${goal.scorer}$extra ${goal.minute}'"
}

private fun goalSummary(match: MatchItem): String {
    if (match.goalEvents.isEmpty()) return ""

    return match.goalEvents
        .takeLast(3)
        .joinToString("  •  ") {
            goalLabel(it)
        }
}

private suspend fun loadMatches(
    sport: String,
    filter: String
): List<MatchItem> {
    val response = when (sport.lowercase()) {

        "cricket" -> {
            val path = when (filter) {
                "Recent" -> "/dbMatches"
                else -> "/currentMatches"
            }

            apiGet(path)
        }

        "football" -> {
            apiGet(
                "/api/widget/matches/?sport=football&limit=100"
            )
        }

        else -> null
    } ?: return emptyList()

    val allMatches = parseMatches(response, sport)

    return when (filter) {
        "Live" -> allMatches.filter { it.isLive }

        "Upcoming" -> allMatches.filter {
            it.isUpcoming
        }

        "Recent" -> allMatches.filter {
            it.isFinished
        }

        else -> allMatches
    }
}

private suspend fun loadMatchDetails(
    sport: String,
    match: MatchItem
): JSONObject? {
    return when (sport.lowercase()) {

        "cricket" -> {
            if (match.matchUrl.isNotBlank()) {
                apiGet(
                    "/dbScorecard?match=${match.matchUrl}"
                )
            } else if (match.id.isNotBlank()) {
                apiGet(
                    "/scorecard?id=${match.id}"
                )
            } else {
                null
            }
        }

        "football" -> {
            null
        }

        else -> null
    }
}

private fun sortMatches(
    matches: List<MatchItem>,
    filter: String
): List<MatchItem> {
    return when (filter) {
        "Live" -> matches.sortedByDescending {
            it.isLive
        }

        "Upcoming" -> matches.sortedBy {
            it.date + it.time
        }

        "Recent" -> matches.sortedByDescending {
            it.date + it.time
        }

        else -> matches
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SportexXApp()
            }
        }
    }
}

@Composable
private fun SportexXApp() {

    var selectedSport by remember {
        mutableStateOf("Cricket")
    }

    var selectedFilter by remember {
        mutableStateOf("Live")
    }

    var selectedCategory by remember {
        mutableStateOf("International")
    }

    var selectedSection by remember {
        mutableStateOf(AppSection.HOME)
    }

    var matches by remember {
        mutableStateOf<List<MatchItem>>(emptyList())
    }

    var selectedMatch by remember {
        mutableStateOf<MatchItem?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(
        selectedSport,
        selectedFilter,
        selectedSection
    ) {
        if (
            selectedSection == AppSection.HOME ||
            selectedSection == AppSection.LIVE
        ) {
            if (
                selectedSport == "Cricket" ||
                selectedSport == "Football"
            ) {
                isLoading = true
                errorMessage = ""

                val result = loadMatches(
                    selectedSport,
                    selectedFilter
                )

                matches = sortMatches(
                    result,
                    selectedFilter
                )

                if (matches.isEmpty()) {
                    errorMessage =
                        "Abhi is category ka match data available nahi hai."
                }

                isLoading = false
            } else {
                matches = emptyList()
                errorMessage = "This feature is coming soon."
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        MainHeader()

        if (selectedSection == AppSection.HOME ||
            selectedSection == AppSection.LIVE
        ) {

            SportsSelector(
                selectedSport = selectedSport,
                onSelect = {
                    selectedSport = it
                    selectedFilter = "Live"
                }
            )

            MatchFilters(
                selectedFilter = selectedFilter,
                onSelect = {
                    selectedFilter = it
                }
            )

            if (selectedSport == "Cricket") {
                CricketCategories(
                    selectedCategory = selectedCategory,
                    onSelect = {
                        selectedCategory = it
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MainSectionContent(
                section = selectedSection,
                sport = selectedSport,
                filter = selectedFilter,
                category = selectedCategory,
                matches = matches,
                selectedMatch = selectedMatch,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onMatchClick = {
                    selectedMatch = it
                    selectedSection = AppSection.MATCH_DETAILS
                },
                onBack = {
                    selectedSection = AppSection.HOME
                }
            )
        }

        BottomNavigationBar(
            selectedSection = selectedSection,
            onSelect = {
                selectedSection = it
                selectedMatch = null
            }
        )
    }
}

@Composable
private fun MainHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    AccentBlue,
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "S",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.width(11.dp))

        Column {
            Text(
                "SPORTEXX",
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            Text(
                "SPORTS  •  NEWS  •  STATS",
                color = SecondaryText,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "LIVE",
            color = LiveGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SportsSelector(
    selectedSport: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sportsList.forEach { sport ->
            val selected = selectedSport == sport

            Box(
                modifier = Modifier
                    .background(
                        if (selected) AccentBlue else CardColor,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        onSelect(sport)
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
            ) {
                Text(
                    sport,
                    color = if (selected) Color.White
                    else SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold
                    else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MatchFilters(
    selectedFilter: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        matchFilters.forEach { filter ->
            val selected = selectedFilter == filter

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) SoftBlue else SurfaceColor,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onSelect(filter)
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    filter,
                    color = if (selected) PrimaryText
                    else SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold
                    else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CricketCategories(
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        cricketCategories.forEach { category ->
            val selected = selectedCategory == category

            Column(
                modifier = Modifier
                    .clickable {
                        onSelect(category)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    category,
                    color = if (selected) AccentBlue
                    else SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold
                    else FontWeight.Normal
                )

                Spacer(Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp)
                        .background(
                            if (selected) AccentBlue
                            else Color.Transparent
                        )
                )
            }
        }
    }
}

@Composable
private fun MainSectionContent(
    section: AppSection,
    sport: String,
    filter: String,
    category: String,
    matches: List<MatchItem>,
    selectedMatch: MatchItem?,
    isLoading: Boolean,
    errorMessage: String,
    onMatchClick: (MatchItem) -> Unit,
    onBack: () -> Unit
) {
    when (section) {
        AppSection.HOME,
        AppSection.LIVE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    if (filter == "Live") "Live Matches"
                    else "$filter Matches",
                    color = PrimaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        top = 12.dp,
                        bottom = 10.dp
                    )
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(30.dp)
                    )
                } else if (matches.isEmpty()) {
                    Text(
                        errorMessage.ifBlank {
                            "No matches available right now."
                        },
                        color = SecondaryText,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    matches.forEach { match ->
                        MatchCard(
                            match = match,
                            onClick = {
                                onMatchClick(match)
                            }
                        )
                        Spacer(Modifier.height(11.dp))
                    }
                }

                if (sport == "Cricket") {
                    Text(
                        "Category: $category",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(
                            top = 8.dp,
                            bottom = 16.dp
                        )
                    )
                }
            }
        }

        AppSection.MATCH_DETAILS -> {
            if (selectedMatch != null) {
                MatchCentre(
                    match = selectedMatch,
                    onBack = onBack
                )
            } else {
                ComingSoonScreen("Match Centre")
            }
        }

        AppSection.NEWS -> ComingSoonScreen("Sports News")
        AppSection.QUIZ -> ComingSoonScreen("Sports Quiz")
        AppSection.STATS -> ComingSoonScreen("Sports Stats")
        AppSection.MORE -> MoreScreen()
        AppSection.RANKINGS -> ComingSoonScreen("Rankings")
        AppSection.EVENTS -> ComingSoonScreen("Major Events")
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
                RoundedCornerShape(15.dp)
            )
            .clickable {
                onClick()
            }
            .padding(14.dp)
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
            } else {
                Text(
                    match.status.ifBlank {
                        match.date
                    },
                    color = SecondaryText,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(13.dp))

        MatchTeamRow(
            name = match.home,
            logo = match.homeLogo,
            score = match.homeScore
        )

        Spacer(Modifier.height(10.dp))

        MatchTeamRow(
            name = match.away,
            logo = match.awayLogo,
            score = match.awayScore
        )

        if (match.sport.equals("Football", true) &&
            match.goalEvents.isNotEmpty()
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                goalSummary(match),
                color = AccentBlue,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            listOf(match.date, match.time, match.venue)
                .filter { it.isNotBlank() }
                .joinToString("  •  "),
            color = SecondaryText,
            fontSize = 10.sp
        )

        if (match.result.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                match.result,
                color = PrimaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MatchTeamRow(
    name: String,
    logo: String,
    score: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logo.isNotBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = name,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        SoftBlue,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    color = PrimaryText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            name,
            color = PrimaryText,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )

        Text(
            score.ifBlank { "—" },
            color = PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatchCentre(
    match: MatchItem,
    onBack: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf("Overview")
    }

    val tabs = if (match.sport.equals("Football", true)) {
        listOf(
            "Info",
            "Overview",
            "Lineups",
            "Stats",
            "H2H",
            "Highlights"
        )
    } else {
        listOf(
            "Info",
            "Overview",
            "Scorecard",
            "Squad",
            "Stats",
            "H2H",
            "Highlights"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹ Back",
                color = AccentBlue,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                "MATCH CENTRE",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CardColor,
                    RoundedCornerShape(15.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                match.competition.ifBlank {
                    match.sport
                },
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(15.dp))

            Text(
                match.home,
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "${match.homeScore.ifBlank { "—" }}  -  ${match.awayScore.ifBlank { "—" }}",
                color = AccentBlue,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                match.away,
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(9.dp))

            Text(
                match.status.ifBlank { match.date },
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(13.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            tabs.forEach { tab ->
                val selected = selectedTab == tab

                Column(
                    modifier = Modifier.clickable {
                        selectedTab = tab
                    }
                ) {
                    Text(
                        tab,
                        color = if (selected) AccentBlue
                        else SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold
                        else FontWeight.Normal
                    )

                    Spacer(Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .width(25.dp)
                            .height(2.dp)
                            .background(
                                if (selected) AccentBlue
                                else Color.Transparent
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                "Info" -> {
                    DetailRow("Sport", match.sport)
                    DetailRow("Competition", match.competition)
                    DetailRow("Date", match.date)
                    DetailRow("Time", match.time)
                    DetailRow("Venue", match.venue)
                    DetailRow("Result", match.result)
                }

                "Overview" -> {
                    Text(
                        "Match Overview",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    if (match.sport.equals("Football", true)) {
                        Text(
                            "Goal Timeline",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        if (match.goalEvents.isEmpty()) {
                            ComingSoonScreen(
                                "Goal events are not available yet"
                            )
                        } else {
                            match.goalEvents.forEach { goal ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        goalLabel(goal),
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (goal.assist.isNotBlank()) {
                                        Text(
                                            "Assist: ${goal.assist}",
                                            color = SecondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Divider(
                                    color = DividerColor
                                )
                            }
                        }
                    } else {
                        Text(
                            "Cricket match overview data will appear when supported by the API.",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )
                    }
                }

                "Lineups",
                "Scorecard",
                "Squad",
                "Stats",
                "H2H",
                "Highlights" -> {
                    ComingSoonScreen(
                        "$selectedTab data is not available yet"
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            value.ifBlank { "Not available" },
            color = PrimaryText,
            fontSize = 13.sp
        )
    }

    Divider(color = DividerColor)
}

@Composable
private fun BottomNavigationBar(
    selectedSection: AppSection,
    onSelect: (AppSection) -> Unit
) {
    val items = listOf(
        Triple("Home", "⌂", AppSection.HOME),
        Triple("Live", "◉", AppSection.LIVE),
        Triple("News", "▤", AppSection.NEWS),
        Triple("Quiz", "?", AppSection.QUIZ),
        Triple("Stats", "▥", AppSection.STATS),
        Triple("More", "⋯", AppSection.MORE)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .navigationBarsPadding()
            .padding(
                horizontal = 5.dp,
                vertical = 9.dp
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = selectedSection == item.third

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onSelect(item.third)
                    }
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    item.second,
                    color = if (selected) AccentBlue
                    else SecondaryText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    item.first,
                    color = if (selected) PrimaryText
                    else SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold
                    else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MoreScreen() {
    val options = listOf(
        "Football Rankings",
        "Cricket Rankings",
        "Major Events",
        "Olympics",
        "Commonwealth Games",
        "Points System",
        "About SportexX"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "More",
            color = PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(
                        CardColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    option,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "›",
                    color = AccentBlue,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            color = SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun parseTeamRankings(
    response: JSONObject
): List<TeamRanking> {
    val root = response.unwrapData()

    val array = root.firstArray(
        "standings",
        "table",
        "teams",
        "rankings"
    ) ?: return emptyList()

    return array.toObjectList().mapIndexedNotNull {
        index, item ->

        val teamObject = item.firstObject(
            "team",
            "participant"
        )

        val teamName = teamObject?.firstString(
            "name",
            "teamName"
        ) ?: item.firstString(
            "teamName",
            "name",
            "team"
        )

        if (teamName.isBlank()) {
            return@mapIndexedNotNull null
        }

        val played = item.firstInt(
            "played",
            "matches",
            "P"
        )

        val won = item.firstInt(
            "won",
            "wins",
            "W"
        )

        val drawn = item.firstInt(
            "drawn",
            "draws",
            "D"
        )

        val lost = item.firstInt(
            "lost",
            "losses",
            "L"
        )

        val goalsFor = item.firstInt(
            "goalsFor",
            "goals_for",
            "GF"
        )

        val goalsAgainst = item.firstInt(
            "goalsAgainst",
            "goals_against",
            "GA"
        )

        TeamRanking(
            position = item.firstInt(
                "position",
                "rank",
                "place"
            ).takeIf { it > 0 } ?: index + 1,
            team = teamName,
            played = played,
            won = won,
            drawn = drawn,
            lost = lost,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            goalDifference = item.firstInt(
                "goalDifference",
                "goal_difference",
                "GD"
            ).takeIf { it != 0 } ?: goalsFor - goalsAgainst,
            points = item.firstInt(
                "points",
                "pts",
                "Pts"
            ),
            logo = teamObject?.firstString(
                "logo",
                "crest",
                "image"
            ) ?: ""
        )
    }
}

private fun parsePlayerRankings(
    response: JSONObject,
    category: String
): List<PlayerRanking> {
    val root = response.unwrapData()

    val array = root.firstArray(
        "players",
        "rankings",
        "leaders",
        "topPlayers"
    ) ?: return emptyList()

    return array.toObjectList().mapIndexedNotNull {
        index, item ->

        val playerObject = item.firstObject(
            "player",
            "athlete"
        )

        val playerName = playerObject?.firstString(
            "name",
            "fullName"
        ) ?: item.firstString(
            "playerName",
            "name"
        )

        if (playerName.isBlank()) {
            return@mapIndexedNotNull null
        }

        PlayerRanking(
            position = item.firstInt(
                "position",
                "rank"
            ).takeIf { it > 0 } ?: index + 1,
            name = playerName,
            team = item.firstString(
                "teamName",
                "team",
                "club"
            ),
            value = item.firstString(
                "value",
                "total",
                "goals",
                "assists",
                "appearances",
                "cleanSheets"
            ),
            category = category,
            photo = playerObject?.firstString(
                "photo",
                "image"
            ) ?: ""
        )
    }
}

private fun parseNews(
    response: JSONObject
): List<NewsItem> {
    val root = response.unwrapData()

    val array = root.firstArray(
        "news",
        "articles",
        "items",
        "results"
    ) ?: return emptyList()

    return array.toObjectList().mapNotNull { item ->
        val title = item.firstString(
            "title",
            "headline"
        )

        if (title.isBlank()) {
            return@mapNotNull null
        }

        NewsItem(
            title = title,
            description = item.firstString(
                "description",
                "summary",
                "content"
            ),
            image = item.firstString(
                "image",
                "imageUrl",
                "thumbnail"
            ),
            source = item.firstString(
                "source",
                "publisher"
            ),
            publishedAt = item.firstString(
                "publishedAt",
                "date"
            ),
            url = item.firstString(
                "url",
                "link"
            )
        )
    }
}

private fun parseEvents(
    response: JSONObject
): List<EventItem> {
    val root = response.unwrapData()

    val array = root.firstArray(
        "events",
        "competitions",
        "tournaments",
        "items"
    ) ?: return emptyList()

    return array.toObjectList().mapNotNull { item ->
        val name = item.firstString(
            "name",
            "title",
            "eventName"
        )

        if (name.isBlank()) {
            return@mapNotNull null
        }

        EventItem(
            name = name,
            sport = item.firstString(
                "sport",
                "category"
            ),
            location = item.firstString(
                "location",
                "venue",
                "host"
            ),
            startDate = item.firstString(
                "startDate",
                "start_date",
                "date"
            ),
            endDate = item.firstString(
                "endDate",
                "end_date"
            ),
            status = item.firstString(
                "status",
                "state"
            )
        )
    }
}

@Composable
private fun RankingTeamCard(
    ranking: TeamRanking
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                CardColor,
                RoundedCornerShape(12.dp)
            )
            .padding(13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${ranking.position}",
                color = AccentBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(Modifier.width(10.dp))

            Text(
                ranking.team,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${ranking.points} Pts",
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "P ${ranking.played}  •  W ${ranking.won}  •  D ${ranking.drawn}  •  L ${ranking.lost}",
            color = SecondaryText,
            fontSize = 11.sp
        )

        Text(
            "GF ${ranking.goalsFor}  •  GA ${ranking.goalsAgainst}  •  GD ${ranking.goalDifference}",
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RankingPlayerCard(
    ranking: PlayerRanking
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                CardColor,
                RoundedCornerShape(12.dp)
            )
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${ranking.position}",
            color = AccentBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                ranking.name,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                ranking.team,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        Text(
            ranking.value,
            color = AccentBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EventCard(
    event: EventItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                CardColor,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            event.name,
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(7.dp))

        Text(
            listOf(
                event.sport,
                event.location,
                event.startDate,
                event.endDate
            ).filter { it.isNotBlank() }
                .joinToString(" • "),
            color = SecondaryText,
            fontSize = 11.sp
        )

        if (event.status.isNotBlank()) {
            Spacer(Modifier.height(5.dp))

            Text(
                event.status,
                color = AccentBlue,
                fontSize = 11.sp
            )
        }
    }
}
