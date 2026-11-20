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
private fun isFinishedMatch(match: Match): Boolean {
    val status = match.status.lowercase()

    return match.matchEnded ||
        status.contains("won by") ||
        status.contains("tied") ||
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
    return !isLiveMatch(match) && !isFinishedMatch(match)
}

private fun parseMatches(
    response: String,
    sport: String
): List<Match> {
    val root = JSONObject(response)

    val array = when {
        root.has("matches") ->
            root.optJSONArray("matches") ?: JSONArray()

        root.has("data") ->
            root.optJSONArray("data") ?: JSONArray()

        else -> JSONArray()
    }

    val result = mutableListOf<Match>()

    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue

        val teamsArray = obj.optJSONArray("teams")
        val home = teamsArray?.optString(0, "") ?: ""
        val away = teamsArray?.optString(1, "") ?: ""

        val name = jsonString(obj, "name", "title")
        val split = name.split(
            Regex("\\s+vs\\s+", RegexOption.IGNORE_CASE)
        )

        val homeName = home.ifBlank {
            split.getOrNull(0)?.trim() ?: "Team 1"
        }

        val awayName = away.ifBlank {
            split.getOrNull(1)?.trim() ?: "Team 2"
        }

        val teamInfo = obj.optJSONArray("teamInfo")
        var homeLogo = ""
        var awayLogo = ""

        if (teamInfo != null) {
            for (j in 0 until teamInfo.length()) {
                val team = teamInfo.optJSONObject(j) ?: continue
                val teamName = jsonString(team, "name")
                val image = jsonString(team, "img", "logo")

                if (teamName.equals(homeName, true)) {
                    homeLogo = image
                }

                if (teamName.equals(awayName, true)) {
                    awayLogo = image
                }
            }
        }

        val scores = obj.optJSONArray("score")
        var homeScore = ""
        var awayScore = ""

        if (scores != null) {
            if (scores.length() > 0) {
                homeScore = scoreText(
                    scores.optJSONObject(0) ?: JSONObject()
                )
            }

            if (scores.length() > 1) {
                awayScore = scoreText(
                    scores.optJSONObject(1) ?: JSONObject()
                )
            }
        }

        val competition = jsonString(
            obj,
            "series",
            "competition",
            "tournament"
        )

        val status = jsonString(
            obj,
            "status",
            "matchStatus"
        )

        val started = obj.optBoolean("matchStarted", false)
        val ended = obj.optBoolean("matchEnded", false)

        result.add(
            Match(
                home = homeName,
                away = awayName,
                homeScore = homeScore,
                awayScore = awayScore,
                status = status,
                time = jsonString(obj, "date", "dateTime", "time"),
                competition = competition,
                venue = jsonString(obj, "venue"),
                sport = sport,
                homeLogo = homeLogo,
                awayLogo = awayLogo,
                matchId = jsonString(obj, "id", "matchId"),
                category = if (sport == "Cricket") {
                    classifyCricketMatch(name, competition)
                } else {
                    "All"
                },
                matchStarted = started,
                matchEnded = ended
            )
        )
    }

    return result
}

private suspend fun fetchMatches(
    endpoint: String,
    sport: String
): List<Match> = withContext(Dispatchers.IO) {
    val connection = URL("$API_BASE/$endpoint")
        .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val response = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        parseMatches(response, sport)
    } finally {
        connection.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LiveScorePointTheme {
                LiveScorePointApp()
            }
        }
    }
}

@Composable
private fun LiveScorePointTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SkyBlue,
            background = Background,
            surface = CardColor
        ),
        content = content
    )
}

@Composable
private fun LiveScorePointApp() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var selectedFilter by remember { mutableStateOf("Live") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPage by remember { mutableStateOf("Home") }
    var selectedMatch by remember { mutableStateOf<Match?>(null) }

    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedSport, refreshKey) {
        loading = true
        error = ""

        try {
            val endpoint = when (selectedSport) {
                "Cricket" -> "currentMatches"
                "Football" -> "footballLive"
                else -> ""
            }

            matches = if (endpoint.isNotBlank()) {
                fetchMatches(endpoint, selectedSport)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load matches"
            matches = emptyList()
        }

        loading = false
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = CardColor
            ) {
                val pages = listOf(
                    "Home", "Live", "Quiz", "Stats", "More"
                )

                pages.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = {
                            selectedPage = page
                            selectedMatch = null
                        },
                        icon = {},
                        label = {
                            Text(
                                page,
                                color = if (selectedPage == page) {
                                    SkyBlue
                                } else {
                                    Muted
                                }
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SkyBlue,
                            indicatorColor = Color(0xFF18334A)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                selectedMatch != null -> {
                    MatchCentre(
                        match = selectedMatch!!,
                        onBack = { selectedMatch = null }
                    )
                }

                selectedPage == "Home" ||
                    selectedPage == "Live" -> {
                    HomeContent(
                        selectedSport = selectedSport,
                        onSportChange = {
                            selectedSport = it
                            selectedCategory = "All"
                        },
                        selectedFilter = if (selectedPage == "Live") {
                            "Live"
                        } else {
                            selectedFilter
                        },
                        onFilterChange = {
                            selectedFilter = it
                        },
                        selectedCategory = selectedCategory,
                        onCategoryChange = {
                            selectedCategory = it
                        },
                        matches = matches,
                        loading = loading,
                        error = error,
                        onRefresh = {
                            refreshKey++
                        },
                        onMatchClick = {
                            selectedMatch = it
                        }
                    )
                }

                selectedPage == "Quiz" -> {
                    SimplePage("Quiz")
                }

                selectedPage == "Stats" -> {
                    SimplePage("Stats")
                }

                else -> {
                    SimplePage("More")
                }
            }
        }
    }
}
@Composable
private fun HomeContent(
    selectedSport: String,
    onSportChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    matches: List<Match>,
    loading: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onMatchClick: (Match) -> Unit
) {
    val filteredMatches = remember(
        matches,
        selectedFilter,
        selectedCategory,
        selectedSport
    ) {
        val byFilter = when (selectedFilter) {
            "Live" -> matches.filter { isLiveMatch(it) }
            "Upcoming" -> matches.filter { isUpcomingMatch(it) }
            "Recent" -> matches
                .filter { isFinishedMatch(it) }
                .sortedByDescending { it.time }
            else -> matches
        }

        if (selectedSport == "Cricket" &&
            selectedCategory != "All"
        ) {
            byFilter.filter {
                it.category == selectedCategory
            }
        } else {
            byFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Header()

        SportSelector(
            selectedSport = selectedSport,
            onSportChange = onSportChange
        )

        FilterSelector(
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange
        )

        if (selectedSport == "Cricket") {
            CricketCategorySelector(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SkyBlue)
                }
            }

            error.isNotBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Matches load nahi hue",
                        color = White
                    )
                    Text(
                        error,
                        color = Muted
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }

            selectedSport != "Cricket" &&
                selectedSport != "Football" -> {
                ComingSoonSection(selectedSport)
            }

            filteredMatches.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No matches found",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Is section mein abhi match available nahi hai",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            }

            else -> {
                MatchList(
                    matches = filteredMatches,
                    onMatchClick = onMatchClick
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "LIVE SCORE",
                color = SkyBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "LiveScorePoint",
                color = White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "LIVE",
            color = Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SportSelector(
    selectedSport: String,
    onSportChange: (String) -> Unit
) {
    val sports = listOf(
        "Cricket",
        "Football",
        "Kabaddi",
        "Tennis",
        "Basketball"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sports.forEach { sport ->
            FilterChip(
                selected = selectedSport == sport,
                onClick = { onSportChange(sport) },
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
private fun FilterSelector(
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF18334A),
                    selectedLabelColor = SkyBlue,
                    labelColor = Muted
                )
            )
        }
    }
}

@Composable
private fun CricketCategorySelector(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        cricketCategories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = {
                    Text(
                        category,
                        fontSize = 11.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBlue,
                    selectedLabelColor = Background,
                    labelColor = Muted
                )
            )
        }
    }
}

@Composable
private fun MatchList(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = matches,
            key = { it.matchId.ifBlank { it.home + it.away + it.time } }
        ) { match ->
            MatchCard(
                match = match,
                onClick = { onMatchClick(match) }
            )
        }
    }
}
@Composable
private fun MatchCard(
    match: Match,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.competition.ifBlank { match.sport },
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    when {
                        isLiveMatch(match) -> "LIVE"
                        isFinishedMatch(match) -> "FINISHED"
                        else -> "UPCOMING"
                    },
                    color = when {
                        isLiveMatch(match) -> Green
                        isFinishedMatch(match) -> Muted
                        else -> SkyBlue
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(match.homeLogo, match.home)

                Spacer(Modifier.width(9.dp))

                Text(
                    match.home,
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    match.homeScore.ifBlank { "-" },
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogo(match.awayLogo, match.away)

                Spacer(Modifier.width(9.dp))

                Text(
                    match.away,
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    match.awayScore.ifBlank { "-" },
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (match.status.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF26364B))
                Spacer(Modifier.height(8.dp))

                Text(
                    match.status,
                    color = if (isLiveMatch(match)) Green else Muted,
                    fontSize = 12.sp
                )
            }

            if (match.venue.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    match.venue,
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun TeamLogo(
    imageUrl: String,
    teamName: String
) {
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = teamName,
            modifier = Modifier.size(32.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color(0xFF26364B),
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                teamName.take(1).uppercase(),
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MatchCentre(
    match: Match,
    onBack: () -> Unit
) {
    var selectedTab by remember {
        mutableStateOf("Info")
    }

    val tabs = listOf(
        "Info",
        "Overview",
        "Scorecard",
        "Squad",
        "Commentary",
        "Stats",
        "H2H",
        "Highlights"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                color = SkyBlue,
                fontSize = 30.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(Modifier.width(12.dp))

            Text(
                "Match Centre",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardColor
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    match.competition,
                    color = Muted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TeamLogo(match.homeLogo, match.home)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            match.home,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            match.homeScore.ifBlank { "-" },
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "VS",
                        color = SkyBlue,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TeamLogo(match.awayLogo, match.away)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            match.away,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            match.awayScore.ifBlank { "-" },
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    match.status.ifBlank { match.time },
                    color = Green,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
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
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF18334A)
                    )
                )
            }
        }

        HorizontalDivider(color = Color(0xFF26364B))

        MatchCentreContent(
            match = match,
            selectedTab = selectedTab
        )
    }
}

@Composable
private fun MatchCentreContent(
    match: Match,
    selectedTab: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when (selectedTab) {
            "Info" -> {
                Text(
                    "Match Information",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                InfoRow("Competition", match.competition)
                InfoRow("Venue", match.venue)
                InfoRow("Date", match.time)
                InfoRow("Status", match.status)
                InfoRow("Sport", match.sport)
            }

            "Overview" -> {
                Text(
                    "Match Overview",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                InfoRow("Home Team", match.home)
                InfoRow("Away Team", match.away)
                InfoRow("Current Status", match.status)
            }

            "Scorecard" -> {
                Text(
                    "Scorecard",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                InfoRow(match.home, match.homeScore)
                InfoRow(match.away, match.awayScore)

                Text(
                    "Detailed batting and bowling scorecard will appear when the API provides it.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            else -> {
                ComingSoonSection(selectedTab)
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Muted,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )

            Text(
                value,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        HorizontalDivider(color = Color(0xFF26364B))
    }
}

@Composable
private fun ComingSoonSection(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "This feature is coming soon",
                color = Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SimplePage(
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "This feature is coming soon",
            color = Muted
        )
    }
}