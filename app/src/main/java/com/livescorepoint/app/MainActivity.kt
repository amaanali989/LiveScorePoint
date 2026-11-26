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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE =
    "https://lsp-cricket-api.uddinamaan619.workers.dev"

private val BackgroundColor = Color(0xFF10141D)
private val SurfaceColor = Color(0xFF1A2230)
private val PrimaryBlue = Color(0xFF3978C8)
private val PrimaryText = Color(0xFFF2F5FA)
private val SecondaryText = Color(0xFF9AA8BA)
private val LiveRed = Color(0xFFE45B65)
private val SuccessGreen = Color(0xFF39B982)

private val sportsList = listOf(
    "Cricket",
    "Football",
    "Basketball",
    "Tennis",
    "Kabaddi",
    "Hockey",
    "Volleyball",
    "Badminton",
    "More"
)

private val navigationItems = listOf(
    "Home",
    "Matches",
    "Rankings",
    "News",
    "More"
)

private val cricketFilters = listOf(
    "Live",
    "Upcoming",
    "Recent"
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
    val result: String = "",
    val isLive: Boolean = false,
    val isFinished: Boolean = false,
    val isUpcoming: Boolean = false
)

data class GoalEvent(
    val minute: String = "",
    val team: String = "",
    val player: String = "",
    val type: String = ""
)

data class TeamRanking(
    val position: Int = 0,
    val name: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0
)

data class PlayerRanking(
    val position: Int = 0,
    val name: String = "",
    val team: String = "",
    val value: String = "",
    val category: String = ""
)

data class NewsItem(
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val url: String = "",
    val source: String = ""
)

data class EventItem(
    val title: String = "",
    val date: String = "",
    val venue: String = "",
    val sport: String = ""
)

enum class MatchFilter {
    LIVE,
    UPCOMING,
    RECENT
}
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

            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            if (response.isBlank()) {
                return@withContext null
            }

            val clean = response.trim()

            if (clean.startsWith("[")) {
                JSONObject().put(
                    "matches",
                    JSONArray(clean)
                )
            } else {
                JSONObject(clean)
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

            if (result.isNotBlank() && result != "null") {
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

        if (value != null) {
            return value
        }
    }

    return null
}

private fun JSONObject.firstArray(
    vararg keys: String
): JSONArray? {
    for (key in keys) {
        val value = optJSONArray(key)

        if (value != null) {
            return value
        }
    }

    return null
}

private fun JSONObject.firstInt(
    vararg keys: String
): Int {
    for (key in keys) {
        val value = opt(key)

        if (value is Number) {
            return value.toInt()
        }

        val parsed = value?.toString()?.toIntOrNull()

        if (parsed != null) {
            return parsed
        }
    }

    return 0
}

private fun JSONObject.firstBoolean(
    vararg keys: String
): Boolean {
    for (key in keys) {
        val value = opt(key)

        if (value is Boolean) {
            return value
        }

        if (value != null) {
            when (value.toString().lowercase()) {
                "true", "1", "yes" -> return true
                "false", "0", "no" -> return false
            }
        }
    }

    return false
}

private fun unwrapData(
    response: JSONObject
): JSONObject {
    return response.optJSONObject("data") ?: response
}

private fun toObjectList(
    array: JSONArray?
): List<JSONObject> {
    if (array == null) return emptyList()

    val result = mutableListOf<JSONObject>()

    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i)

        if (item != null) {
            result.add(item)
        }
    }

    return result
}private fun parseMatch(
    json: JSONObject,
    sport: String
): MatchItem {

    val teams = json.optJSONArray("teams")
    val teamInfo = json.optJSONArray("teamInfo")
    val scoreArray = json.optJSONArray("score")

    val home = teams?.optString(0, "") ?: ""
    val away = teams?.optString(1, "") ?: ""

    fun logoFor(teamName: String): String {
        if (teamInfo == null) return ""

        for (i in 0 until teamInfo.length()) {
            val info = teamInfo.optJSONObject(i) ?: continue

            if (info.firstString("name").equals(teamName, true)) {
                return info.firstString("img", "logo", "image")
            }
        }

        return ""
    }

    fun formatScore(score: JSONObject): String {
        val runs = score.opt("r")?.toString() ?: ""
        val wickets = score.opt("w")?.toString() ?: ""
        val overs = score.opt("o")?.toString() ?: ""

        if (runs.isBlank() || runs == "null") return ""

        return "$runs/$wickets" +
            if (overs.isNotBlank() && overs != "null") {
                " ($overs)"
            } else {
                ""
            }
    }

    fun scoreFor(teamName: String): String {
        if (scoreArray == null) return ""

        for (i in 0 until scoreArray.length()) {
            val item = scoreArray.optJSONObject(i) ?: continue
            val inning = item.firstString("inning")

            val mentionsHome = inning.contains(home, true)
            val mentionsAway = inning.contains(away, true)

            if (
                inning.contains(teamName, true) &&
                !(mentionsHome && mentionsAway)
            ) {
                return formatScore(item)
            }
        }

        return ""
    }

    val firstScore = scoreArray?.optJSONObject(0)
    val secondScore = scoreArray?.optJSONObject(1)

    val homeScore = scoreFor(home)
        .ifBlank { firstScore?.let { formatScore(it) } ?: "" }

    val awayScore = scoreFor(away)
        .ifBlank { secondScore?.let { formatScore(it) } ?: "" }

    val status = json.firstString("status")
    val statusLower = status.lowercase()

    val matchStarted = json.firstBoolean("matchStarted")
    val matchEnded = json.firstBoolean("matchEnded")

    val finished = matchEnded ||
        statusLower.contains("won by") ||
        statusLower.contains("draw") ||
        statusLower.contains("tied") ||
        statusLower.contains("abandoned") ||
        statusLower.contains("no result")

    val upcoming = !matchStarted && !finished
    val live = matchStarted && !finished

    val name = json.firstString("name")
    val competition = name.substringAfter(",", "")
        .trim()
        .ifBlank {
            json.firstString("series", "league", "tournament")
        }

    return MatchItem(
        id = json.firstString("id", "matchId", "match_id"),
        sport = sport,
        home = home,
        away = away,
        homeLogo = logoFor(home),
        awayLogo = logoFor(away),
        homeScore = homeScore,
        awayScore = awayScore,
        status = status,
        date = json.firstString("date", "matchDate"),
        time = json.firstString("dateTimeGMT", "time", "matchTime"),
        competition = competition,
        venue = json.firstString("venue", "ground"),
        category = json.firstString("matchType", "format"),
        result = if (finished) status else "",
        isLive = live,
        isFinished = finished,
        isUpcoming = upcoming
    )
}

private fun parseMatches(
    response: JSONObject,
    sport: String
): List<MatchItem> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "matches",
        "data",
        "results",
        "items"
    ) ?: return emptyList()

    return toObjectList(array)
        .mapNotNull { item ->
            try {
                val match = parseMatch(item, sport)

                if (match.home.isNotBlank() &&
                    match.away.isNotBlank()
                ) {
                    match
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
}
private fun parseGoalEvents(
    response: JSONObject
): List<GoalEvent> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "events",
        "goals",
        "incidents",
        "matchEvents"
    ) ?: return emptyList()

    return toObjectList(array).mapNotNull { item ->

        val type = item.firstString(
            "type",
            "event",
            "incidentType"
        )

        val player = item.firstString(
            "player",
            "playerName",
            "scorer",
            "name"
        )

        val team = item.firstString(
            "team",
            "teamName"
        )

        val minute = item.firstString(
            "minute",
            "time",
            "elapsed",
            "matchTime"
        )

        if (
            player.isBlank() &&
            team.isBlank() &&
            minute.isBlank()
        ) {
            null
        } else {
            GoalEvent(
                minute = minute,
                team = team,
                player = player,
                type = type
            )
        }
    }
}

private fun goalLabel(event: GoalEvent): String {
    val type = event.type.lowercase()

    return when {
        type.contains("own") -> "Own Goal"
        type.contains("penalty") -> "Penalty"
        type.contains("goal") -> "Goal"
        type.contains("missed") -> "Missed Chance"
        else -> event.type.ifBlank { "Match Event" }
    }
}

private fun goalSummary(
    events: List<GoalEvent>
): String {

    val goals = events.filter {
        val type = it.type.lowercase()
        type.contains("goal") &&
            !type.contains("missed")
    }

    if (goals.isEmpty()) {
        return "No goal events available"
    }

    return goals.joinToString("  •  ") { event ->
        val time = event.minute
            .let { if (it.isNotBlank()) "$it' " else "" }

        "$time${event.player.ifBlank { "Unknown player" }}"
    }
}
private suspend fun loadMatches(
    sport: String,
    filter: String
): List<MatchItem> {

    if (sport.equals("Cricket", ignoreCase = true)) {

        val response = apiGet("/currentMatches")
            ?: return emptyList()

        val allMatches = parseMatches(response, sport)

        val filtered = when (filter.lowercase()) {
            "live" -> allMatches.filter { it.isLive }

            "upcoming" -> allMatches.filter { it.isUpcoming }

            "recent" -> allMatches.filter { it.isFinished }

            else -> allMatches
        }

        return sortMatches(filtered, filter)
    }

    // Other sports will be connected when their API endpoints are verified.
    return emptyList()
}

private suspend fun loadMatchDetails(
    matchId: String
): MatchItem? {

    if (matchId.isBlank()) return null

    val response = apiGet("/currentMatches")
        ?: return null

    return parseMatches(response, "Cricket")
        .firstOrNull { it.id == matchId }
}

private fun sortMatches(
    matches: List<MatchItem>,
    filter: String
): List<MatchItem> {

    return when (filter.lowercase()) {

        "live" -> matches.sortedByDescending {
            it.isLive
        }

        "upcoming" -> matches.sortedBy {
            it.date + it.time
        }

        "recent" -> matches.sortedByDescending {
            it.date + it.time
        }

        else -> matches
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SportexXApp()
        }
    }
}

@Composable
fun SportexXApp() {

    var selectedSport by remember {
        mutableStateOf("Cricket")
    }

    var selectedSection by remember {
        mutableStateOf("Home")
    }

    var selectedFilter by remember {
        mutableStateOf("Live")
    }

    var matches by remember {
        mutableStateOf<List<MatchItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var selectedMatch by remember {
        mutableStateOf<MatchItem?>(null)
    }

    LaunchedEffect(selectedSport, selectedFilter, selectedSection) {

        if (selectedSection == "Home" ||
            selectedSection == "Matches"
        ) {
            isLoading = true

            matches = loadMatches(
                sport = selectedSport,
                filter = selectedFilter
            )

            isLoading = false
        }
    }

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
        ) {

            if (selectedMatch == null) {

                MainHeader()

                SportsSelector(
                    selectedSport = selectedSport,
                    onSportSelected = {
                        selectedSport = it
                    }
                )

                MatchFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = {
                        selectedFilter = it
                    }
                )

                MainSectionContent(
                    selectedSection = selectedSection,
                    selectedSport = selectedSport,
                    selectedFilter = selectedFilter,
                    matches = matches,
                    isLoading = isLoading,
                    onMatchClick = {
                        selectedMatch = it
                    }
                )

                BottomNavigationBar(
                    selectedSection = selectedSection,
                    onSectionSelected = {
                        selectedSection = it
                    }
                )

            } else {

                MatchCentre(
                    match = selectedMatch!!,
                    onBack = {
                        selectedMatch = null
                    }
                )
            }
        }
    }
}
@Composable
private fun MainHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SportexX",
                color = PrimaryText,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "ALL SPORTS. ONE PLACE.",
                color = SecondaryText,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }

        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = PrimaryText,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun SportsSelector(
    selectedSport: String,
    onSportSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(sportsList) { sport ->
            val selected = selectedSport == sport

            Box(
                modifier = Modifier
                    .background(
                        if (selected) PrimaryBlue else SurfaceColor,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        onSportSelected(sport)
                    }
                    .padding(horizontal = 17.dp, vertical = 10.dp)
            ) {
                Text(
                    text = sport,
                    color = PrimaryText,
                    fontSize = 13.sp,
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

@Composable
private fun MatchFilters(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(
            top = 12.dp,
            bottom = 14.dp
        )
    ) {
        items(cricketFilters) { filter ->
            Column(
                modifier = Modifier.clickable {
                    onFilterSelected(filter)
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = filter,
                    color = if (selectedFilter == filter) {
                        PrimaryBlue
                    } else {
                        SecondaryText
                    },
                    fontSize = 14.sp,
                    fontWeight = if (selectedFilter == filter) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (selectedFilter == filter) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(3.dp)
                            .background(
                                PrimaryBlue,
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            }
        }
    }
}
@Composable
private fun MainSectionContent(
    selectedSection: String,
    selectedSport: String,
    selectedFilter: String,
    matches: List<MatchItem>,
    isLoading: Boolean,
    onMatchClick: (MatchItem) -> Unit
) {
    when (selectedSection) {

        "Home", "Matches" -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundColor)
            ) {

                Text(
                    text = "$selectedSport ${selectedFilter} Matches",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryBlue
                            )
                        }
                    }

                    matches.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matches available right now",
                                color = SecondaryText,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(matches) { match ->
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

        "Rankings" -> {
            ComingSoonScreen("Rankings")
        }

        "News" -> {
            ComingSoonScreen("News")
        }

        "More" -> {
            MoreScreen()
        }

        else -> {
            ComingSoonScreen(selectedSection)
        }
    }
}

@Composable
private fun MatchCard(
    match: MatchItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.competition.ifBlank {
                        match.category.ifBlank { match.sport }
                    },
                    color = SecondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                if (match.isLive) {
                    Text(
                        text = "● LIVE",
                        color = LiveRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (match.isFinished) {
                    Text(
                        text = "FINISHED",
                        color = SuccessGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "UPCOMING",
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            MatchTeamRow(
                team = match.home,
                score = match.homeScore
            )

            Spacer(modifier = Modifier.height(12.dp))

            MatchTeamRow(
                team = match.away,
                score = match.awayScore
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = SecondaryText.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = match.status.ifBlank {
                    match.date + "  " + match.time
                },
                color = SecondaryText,
                fontSize = 12.sp
            )

            if (match.venue.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = match.venue,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MatchTeamRow(
    team: String,
    score: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = team.ifBlank { "Team" },
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = score,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryText,
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(15.dp))

            Text(
                text = "Match Centre",
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    SurfaceColor,
                    RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {

            Text(
                text = match.competition.ifBlank {
                    match.sport
                },
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = match.home,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = match.homeScore.ifBlank { "-" },
                        color = PrimaryText,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "VS",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = match.away,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = match.awayScore.ifBlank { "-" },
                        color = PrimaryText,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = match.status.ifBlank { "Match information" },
                color = if (match.isLive) LiveRed else SecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = "Match Information",
            color = PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    SurfaceColor,
                    RoundedCornerShape(15.dp)
                )
                .padding(16.dp)
        ) {
            DetailRow("Sport", match.sport)
            DetailRow("Format", match.category)
            DetailRow("Date", match.date)
            DetailRow("Time", match.time)
            DetailRow("Venue", match.venue)
            DetailRow("Competition", match.competition)
            DetailRow("Result", match.result)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}
@Composable
private fun BottomNavigationBar(
    selectedSection: String,
    onSectionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->

            val selected = selectedSection == item

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onSectionSelected(item)
                    }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (item) {
                        "Home" -> "⌂"
                        "Matches" -> "⚽"
                        "Rankings" -> "▤"
                        "News" -> "▣"
                        else -> "•••"
                    },
                    color = if (selected) PrimaryBlue else SecondaryText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item,
                    color = if (selected) PrimaryBlue else SecondaryText,
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

@Composable
private fun MoreScreen() {

    val options = listOf(
        "All Sports",
        "Events",
        "Points System",
        "Settings",
        "About SportexX"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {

        Text(
            text = "More",
            color = PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        options.forEach { option ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(
                        SurfaceColor,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "›",
                    color = SecondaryText,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(
    section: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = section,
                color = PrimaryText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This feature is coming soon",
                color = SecondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
private fun parseTeamRankings(
    response: JSONObject
): List<TeamRanking> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "standings",
        "rankings",
        "teams",
        "data",
        "results"
    ) ?: return emptyList()

    return toObjectList(array).mapIndexed { index, item ->

        TeamRanking(
            position = item.firstInt("position", "rank")
                .takeIf { it > 0 } ?: index + 1,

            name = item.firstString(
                "team",
                "teamName",
                "name"
            ),

            played = item.firstInt("played", "matches"),
            won = item.firstInt("won", "wins"),
            drawn = item.firstInt("drawn", "draws"),
            lost = item.firstInt("lost", "losses"),
            goalsFor = item.firstInt("goalsFor", "gf"),
            goalsAgainst = item.firstInt("goalsAgainst", "ga"),
            points = item.firstInt("points", "pts")
        )
    }.filter { it.name.isNotBlank() }
}

private fun parsePlayerRankings(
    response: JSONObject,
    category: String
): List<PlayerRanking> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "players",
        "rankings",
        "data",
        "results"
    ) ?: return emptyList()

    return toObjectList(array).mapIndexed { index, item ->

        PlayerRanking(
            position = item.firstInt("position", "rank")
                .takeIf { it > 0 } ?: index + 1,

            name = item.firstString(
                "player",
                "playerName",
                "name"
            ),

            team = item.firstString(
                "team",
                "teamName"
            ),

            value = item.firstString(
                "value",
                "goals",
                "assists",
                "appearances",
                "rating",
                "wickets"
            ),

            category = category
        )
    }.filter { it.name.isNotBlank() }
}

private fun parseNews(
    response: JSONObject
): List<NewsItem> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "news",
        "articles",
        "data",
        "results"
    ) ?: return emptyList()

    return toObjectList(array).map { item ->

        NewsItem(
            title = item.firstString(
                "title",
                "headline",
                "name"
            ),

            description = item.firstString(
                "description",
                "summary",
                "content"
            ),

            image = item.firstString(
                "image",
                "imageUrl",
                "thumbnail",
                "urlToImage"
            ),

            url = item.firstString(
                "url",
                "link",
                "articleUrl"
            ),

            source = item.firstString(
                "source",
                "author",
                "publisher"
            )
        )
    }.filter { it.title.isNotBlank() }
}
private fun parseEvents(
    response: JSONObject
): List<EventItem> {

    val data = unwrapData(response)

    val array = data.firstArray(
        "events",
        "tournaments",
        "competitions",
        "data",
        "results"
    ) ?: return emptyList()

    return toObjectList(array).map { item ->

        EventItem(
            title = item.firstString(
                "title",
                "name",
                "eventName"
            ),
            date = item.firstString(
                "date",
                "startDate",
                "eventDate"
            ),
            venue = item.firstString(
                "venue",
                "location",
                "stadium"
            ),
            sport = item.firstString(
                "sport",
                "category"
            )
        )
    }.filter { it.title.isNotBlank() }
}

@Composable
private fun RankingTeamCard(
    team: TeamRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${team.position}. ${team.name}",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    text = "${team.points} PTS",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "P ${team.played}    W ${team.won}    D ${team.drawn}    L ${team.lost}",
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "GF ${team.goalsFor}    GA ${team.goalsAgainst}    GD ${team.goalsFor - team.goalsAgainst}",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RankingPlayerCard(
    player: PlayerRanking
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${player.position}",
                color = PrimaryBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(15.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = player.team.ifBlank {
                        player.category
                    },
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            Text(
                text = player.value.ifBlank { "-" },
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun EventCard(
    event: EventItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(
                text = event.title,
                color = PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (event.sport.isNotBlank()) {
                Text(
                    text = event.sport,
                    color = PrimaryBlue,
                    fontSize = 12.sp
                )
            }

            if (event.date.isNotBlank()) {
                Text(
                    text = event.date,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            if (event.venue.isNotBlank()) {
                Text(
                    text = event.venue,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}
