package com.livescorepoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Background = Color(0xFF080D18)
private val CardColor = Color(0xFF151F30)
private val SkyBlue = Color(0xFF39BDF8)
private val White = Color(0xFFFFFFFF)
private val Muted = Color(0xFF9AA8BA)
private val Green = Color(0xFF35D07F)

private data class SportInfo(val name: String, val icon: String)
private data class CricketMatch(
    val name: String,
    val status: String,
    val score: String,
    val venue: String
)
private data class QuizQuestion(
    val type: String,
    val question: String,
    val options: List<String>,
    val answers: Set<String>,
    val explanation: String
)

private val sports = listOf(
    SportInfo("Cricket", "🏏"),
    SportInfo("Football", "⚽"),
    SportInfo("Hockey", "🏑"),
    SportInfo("Kabaddi", "🤼"),
    SportInfo("eSports", "🎮"),
    SportInfo("F1", "🏎️"),
    SportInfo("Olympics", "🏅"),
    SportInfo("Wrestling", "🥇")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LiveScorePointApp() } }
    }
}

@Composable
private fun LiveScorePointApp() {
    var selectedSport by remember { mutableStateOf("Cricket") }
    var selectedTab by remember { mutableStateOf("Home") }

    Column(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("LiveScorePoint", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("ALL SPORTS • ONE PLACE", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Text("● LIVE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "SPORTS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                sports.forEach { sport ->
                    SportChip(sport, selectedSport == sport.name) {
                        selectedSport = sport.name
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (selectedTab) {
                "Home", "Live" -> SportsHome(selectedSport)
                "Quiz" -> QuizScreen(selectedSport)
                "Stats" -> StatsScreen(selectedSport)
                else -> MoreScreen()
            }
        }

        Row(
            Modifier.fillMaxWidth().background(CardColor).padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Home", "Live", "Quiz", "Stats", "More").forEach { tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedTab = tab }.padding(horizontal = 5.dp)
                ) {
                    Text(
                        when (tab) {
                            "Home" -> "⌂"
                            "Live" -> "◉"
                            "Quiz" -> "?"
                            "Stats" -> "▥"
                            else -> "•••"
                        },
                        color = if (selectedTab == tab) SkyBlue else Muted,
                        fontSize = 18.sp
                    )
                    Text(tab, color = if (selectedTab == tab) SkyBlue else Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SportChip(sport: SportInfo, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (selected) SkyBlue else CardColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sport.icon, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            sport.name,
            color = if (selected) Background else White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SportsHome(sport: String) {
    var filter by remember(sport) { mutableStateOf("Live") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Live", "Upcoming", "Results").forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyBlue,
                        selectedLabelColor = Background,
                        labelColor = Muted
                    )
                )
            }
        }

        if (sport == "Cricket") {
            CricketMatches(filter)
        } else {
            OtherSportMatches(sport, filter)
        }
    }
}

@Composable
private fun CricketMatches(filter: String) {
    var matches by remember { mutableStateOf<List<CricketMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(filter) {
        if (filter == "Live") {
            loading = true
            try {
                matches = getMatches()
                error = false
            } catch (_: Exception) {
                error = true
            } finally {
                loading = false
            }
        } else {
            matches = emptyList()
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Text("CRICKET", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            when (filter) {
                "Live" -> "Live scorecards"
                "Upcoming" -> "Upcoming fixtures"
                else -> "Recent results"
            },
            color = Muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))

        when {
            filter != "Live" -> EmptyState(
                "Cricket $filter",
                "Is section ke liye fixture/results feed abhi connect nahi hai."
            )
            loading -> Text(
                "Loading live scores…",
                color = Muted,
                modifier = Modifier.padding(16.dp)
            )
            error -> EmptyState(
                "Scores load nahi hue",
                "Internet check karke dobara try karein."
            )
            matches.isEmpty() -> EmptyState(
                "Abhi live match nahi mila",
                "Naya live match aane par yahan dikhega."
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matches) { match ->
                    CricketMatchCard(match)
                }
            }
        }
    }
}

@Composable
private fun CricketMatchCard(match: CricketMatch) {
    Column(
        Modifier.fillMaxWidth()
            .background(CardColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("● LIVE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("CRICKET", color = Muted, fontSize = 9.sp)
        }
        Spacer(Modifier.height(9.dp))
        Text(match.name, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(
            match.score.ifBlank { "Score details updating" },
            color = SkyBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(match.status, color = Green, fontSize = 12.sp)
        if (match.venue.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text("📍 ${match.venue}", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun OtherSportMatches(sport: String, filter: String) {
    val details = when (sport) {
        "Football" -> listOf("⚽", "Football match centre", "Score • match time • line-ups • possession • shots • corners")
        "Hockey" -> listOf("🏑", "Hockey match centre", "Team score • periods • penalty corners • shots")
        "Kabaddi" -> listOf("🤼", "Kabaddi match centre", "Team score • raid points • tackle points • all-outs")
        "eSports" -> listOf("🎮", "eSports match centre", "Teams • maps • rounds • match results")
        "F1" -> listOf("🏎️", "Formula 1 race centre", "Driver positions • laps • timing • race status")
        "Olympics" -> listOf("🏅", "Olympics & Asian Games", "Country medals • events • athlete results")
        else -> listOf("🥇", "Wrestling match centre", "Wrestlers • weight category • bout status • results")
    }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text(sport.uppercase(), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(CardColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(details[0], fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(details[1], color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(details[2], color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                when (filter) {
                    "Live" -> "Live data feed abhi connect nahi hai."
                    "Upcoming" -> "Upcoming fixtures feed abhi connect nahi hai."
                    else -> "Results feed abhi connect nahi hai."
                },
                color = SkyBlue,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Yahan demo score nahi dikhaya jayega.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp)
            .background(CardColor, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun StatsScreen(sport: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("$sport Stats", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Player stats, team comparisons aur tournament records yahan add honge jab data feed connect hoga.",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MoreScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("More", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        listOf("Profile", "Saved teams", "Notifications", "Settings", "About LiveScorePoint").forEach {
            Text(
                it,
                color = White,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
            HorizontalDivider(color = CardColor)
        }
    }
}
@Composable
private fun QuizScreen(sport: String) {
    val questions = remember(sport) { quizQuestionsFor(sport) }
    var index by remember(sport) { mutableIntStateOf(0) }
    var selected by remember(sport) { mutableStateOf(setOf<String>()) }
    var submitted by remember(sport) { mutableStateOf(false) }
    var points by remember(sport) { mutableIntStateOf(0) }
    val question = questions[index]
    val correct = question.answers.isNotEmpty() && selected == question.answers

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("$sport Quiz", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Question ${index + 1} / ${questions.size}     •     Points: $points",
                color = Muted,
                fontSize = 11.sp
            )
        }

        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(CardColor, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    question.type.uppercase(),
                    color = SkyBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    question.question,
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                question.options.forEach { option ->
                    val isSelected = option in selected

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(
                                if (isSelected) SkyBlue.copy(alpha = 0.16f) else Background,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable(enabled = !submitted) {
                                selected = if (question.type == "Multiple Select") {
                                    if (isSelected) selected - option else selected + option
                                } else {
                                    setOf(option)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSelected) "●" else "○", color = SkyBlue)
                        Spacer(Modifier.width(10.dp))
                        Text(option, color = White, fontSize = 13.sp)
                    }
                }

                if (submitted) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            question.answers.isEmpty() ->
                                "Prediction recorded on this device."
                            correct -> "Correct! +10 points"
                            else -> "Not quite. Correct answer: ${question.answers.joinToString()}"
                        },
                        color = if (correct) Green else SkyBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(question.explanation, color = Muted, fontSize = 11.sp)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!submitted) {
                            if (selected.isNotEmpty()) {
                                if (correct) points += 10
                                submitted = true
                            }
                        } else {
                            if (index == questions.lastIndex) {
                                index = 0
                                points = 0
                            } else {
                                index++
                            }
                            selected = emptySet()
                            submitted = false
                        }
                    },
                    enabled = submitted || selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyBlue,
                        contentColor = Background
                    )
                ) {
                    Text(
                        if (submitted) {
                            if (index == questions.lastIndex) "Play Again"
                            else "Next Question"
                        } else {
                            "Submit Answer"
                        }
                    )
                }

                if (!submitted) {
                    TextButton(
                        onClick = {
                            index = if (index == questions.lastIndex) 0 else index + 1
                            selected = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Skip question", color = Muted)
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(CardColor, RoundedCornerShape(14.dp))
                    .padding(15.dp)
            ) {
                Text(
                    "Leaderboard",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("Your score: $points points", color = SkyBlue, fontSize = 14.sp)
                Text(
                    "Online leaderboard abhi connect nahi hai. Score filhaal isi session mein rahega.",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun quizQuestionsFor(sport: String): List<QuizQuestion> {
    return when (sport) {
        "Cricket" -> listOf(
            QuizQuestion("MCQ", "A standard cricket team has how many players on the field?", listOf("9", "10", "11", "12"), setOf("11"), "A playing side has 11 players."),
            QuizQuestion("Yes / No", "Can a batter score runs by running between the wickets?", listOf("Yes", "No"), setOf("Yes"), "Running between the wickets is one way to score."),
            QuizQuestion("True / False", "A six is scored when the ball clears the boundary without bouncing.", listOf("True", "False"), setOf("True"), "A boundary six is worth six runs."),
            QuizQuestion("Multiple Select", "Which are common bowling styles?", listOf("Fast", "Spin", "Seam", "Goalkeeping"), setOf("Fast", "Spin", "Seam"), "Fast, spin and seam describe bowling styles."),
            QuizQuestion("Match Prediction", "Who do you think will win the next cricket match?", listOf("Team A", "Team B", "Tie / No result"), emptySet(), "Prediction questions do not have a fixed correct answer.")
        )

        "Football" -> listOf(
            QuizQuestion("MCQ", "How many players from one team normally start a football match on the field?", listOf("9", "10", "11", "12"), setOf("11"), "A team starts with 11 players, including the goalkeeper."),
            QuizQuestion("Yes / No", "Can a goalkeeper use hands inside their own penalty area?", listOf("Yes", "No"), setOf("Yes"), "Goalkeepers have special handling rights in their own penalty area."),
            QuizQuestion("True / False", "A regular match has two 45-minute halves.", listOf("True", "False"), setOf("True"), "A standard match has two halves of 45 minutes."),
            QuizQuestion("Multiple Select", "Which can be awarded during a match?", listOf("Corner", "Penalty", "Throw-in", "Home run"), setOf("Corner", "Penalty", "Throw-in"), "Corner, penalty and throw-in are football restarts."),
            QuizQuestion("Match Prediction", "What will be the match result?", listOf("Home win", "Draw", "Away win"), emptySet(), "Prediction only; no fixed answer.")
        )

        "Hockey" -> listOf(
            QuizQuestion("MCQ", "How many players per team are on the field in standard field hockey?", listOf("9", "10", "11", "12"), setOf("11"), "A field hockey team has 11 players on the field."),
            QuizQuestion("Yes / No", "Is a penalty corner a set-piece opportunity?", listOf("Yes", "No"), setOf("Yes"), "Penalty corners are set-piece situations."),
            QuizQuestion("True / False", "Field hockey is played using a stick and a ball.", listOf("True", "False"), setOf("True"), "Players use sticks to play the ball."),
            QuizQuestion("Multiple Select", "Which are field hockey match elements?", listOf("Penalty corner", "Field goal", "Shoot-out", "Wicket"), setOf("Penalty corner", "Field goal", "Shoot-out"), "These are used in hockey; wicket belongs to cricket."),
            QuizQuestion("Match Prediction", "Who will win the next hockey match?", listOf("Team A", "Team B", "Draw"), emptySet(), "Prediction only.")
        )

        "Kabaddi" -> listOf(
            QuizQuestion("MCQ", "How many players from one team are on court at the start in standard kabaddi?", listOf("5", "6", "7", "8"), setOf("7"), "A side starts with seven players."),
            QuizQuestion("Yes / No", "Does a raider try to score points in the opponents' half?", listOf("Yes", "No"), setOf("Yes"), "The raider enters the opponents' half to score."),
            QuizQuestion("True / False", "A successful tackle can earn the defending team a point.", listOf("True", "False"), setOf("True"), "A successful tackle earns a point."),
            QuizQuestion("Multiple Select", "Which are kabaddi scoring terms?", listOf("Raid point", "Tackle point", "All-out", "Corner kick"), setOf("Raid point", "Tackle point", "All-out"), "These are common kabaddi terms."),
            QuizQuestion("Match Prediction", "Who will win the next kabaddi match?", listOf("Team A", "Team B", "Tie"), emptySet(), "Prediction only.")
        )

        "eSports" -> listOf(
            QuizQuestion("MCQ", "What does FPS commonly stand for in gaming?", listOf("Fast Player Score", "First Person Shooter", "Final Play Stage", "Free Point System"), setOf("First Person Shooter"), "FPS commonly means First Person Shooter."),
            QuizQuestion("Yes / No", "Can an esports match be played in multiple maps?", listOf("Yes", "No"), setOf("Yes"), "Many esports formats use multiple maps."),
            QuizQuestion("True / False", "A tournament bracket can include elimination rounds.", listOf("True", "False"), setOf("True"), "Knockout brackets use elimination rounds."),
            QuizQuestion("Multiple Select", "Which can be part of esports match data?", listOf("Maps", "Rounds", "Kills", "Overs"), setOf("Maps", "Rounds", "Kills"), "Maps, rounds and kills are common game stats."),
            QuizQuestion("Match Prediction", "Which side will win the next esports match?", listOf("Team A", "Team B", "Not sure"), emptySet(), "Prediction only.")
        )

        "F1" -> listOf(
            QuizQuestion("MCQ", "What does F1 stand for?", listOf("Formula One", "Fast One", "First One", "Final One"), setOf("Formula One"), "F1 is short for Formula One."),
            QuizQuestion("Yes / No", "Does a Formula 1 race use a starting grid?", listOf("Yes", "No"), setOf("Yes"), "Drivers line up on the starting grid."),
            QuizQuestion("True / False", "Pit stops can include changing tyres.", listOf("True", "False"), setOf("True"), "Tyre changes are a key part of pit strategy."),
            QuizQuestion("Multiple Select", "Which are tracked during an F1 race?", listOf("Lap time", "Pit stop", "Position", "Raid points"), setOf("Lap time", "Pit stop", "Position"), "Lap time, pit stops and positions are tracked."),
            QuizQuestion("Match Prediction", "Who will win the next Grand Prix?", listOf("Driver A", "Driver B", "Another driver"), emptySet(), "Prediction only.")
        )

        "Olympics" -> listOf(
            QuizQuestion("MCQ", "How often are the Summer Olympic Games normally held?", listOf("Every 2 years", "Every 3 years", "Every 4 years", "Every 5 years"), setOf("Every 4 years"), "The Summer Olympics are normally held every four years."),
            QuizQuestion("Yes / No", "Are medals awarded for first, second and third places in many Olympic events?", listOf("Yes", "No"), setOf("Yes"), "Gold, silver and bronze medals are awarded."),
            QuizQuestion("True / False", "The Olympic rings are a symbol of the Olympic movement.", listOf("True", "False"), setOf("True"), "The rings are a major Olympic symbol."),
            QuizQuestion("Multiple Select", "Which are Olympic medal types?", listOf("Gold", "Silver", "Bronze", "Copper"), setOf("Gold", "Silver", "Bronze"), "The standard podium medals are gold, silver and bronze."),
            QuizQuestion("Match Prediction", "Which country will top the next medal table?", listOf("Country A", "Country B", "Another country"), emptySet(), "Prediction only.")
        )

        else -> listOf(
            QuizQuestion("MCQ", "What is a wrestling match area commonly called?", listOf("Mat", "Pitch", "Court", "Track"), setOf("Mat"), "Wrestling contests are held on a mat."),
            QuizQuestion("Yes / No", "Are there different weight categories in wrestling?", listOf("Yes", "No"), setOf("Yes"), "Wrestling competitions use weight categories."),
            QuizQuestion("True / False", "A pin can end a wrestling bout.", listOf("True", "False"), setOf("True"), "A successful pin can decide a bout."),
            QuizQuestion("Multiple Select", "Which can describe wrestling competition formats?", listOf("Freestyle", "Greco-Roman", "Weight class", "Powerplay"), setOf("Freestyle", "Greco-Roman", "Weight class"), "These are wrestling styles or competition groupings."),
            QuizQuestion("Match Prediction", "Who will win the next wrestling bout?", listOf("Wrestler A", "Wrestler B", "Not sure"), emptySet(), "Prediction only.")
        )
    }
}

private suspend fun getMatches(): List<CricketMatch> = withContext(Dispatchers.IO) {
    val connection = URL(
        "https://lsp-cricket-api.uddinamaan619.workers.dev/currentMatches"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val data = JSONObject(response).optJSONArray("data")
            ?: return@withContext emptyList()

        val result = mutableListOf<CricketMatch>()

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val scores = item.optJSONArray("score")
            val scoreText = StringBuilder()

            if (scores != null) {
                for (j in 0 until scores.length()) {
                    val s = scores.optJSONObject(j) ?: continue
                    if (scoreText.isNotEmpty()) scoreText.append("   •   ")
                    scoreText.append(s.optString("r", "0"))
                    scoreText.append("/")
                    scoreText.append(s.optString("w", "0"))
                    scoreText.append("  (")
                    scoreText.append(s.optString("o", "0"))
                    scoreText.append(" ov)")
                }
            }

            result.add(
                CricketMatch(
                    item.optString("name", "Cricket Match"),
                    item.optString("status", "Status updating"),
                    scoreText.toString(),
                    item.optString("venue", "")
                )
            )
        }
        result
    } finally {
        connection.disconnect()
    }
}