
                            "Play Again" else "Next Question"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(CardColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                "Leaderboard",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Demo leaderboard", color = Muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("1. Player One       120 pts", color = White, fontSize = 13.sp)
            Text("2. Player Two       100 pts", color = White, fontSize = 13.sp)
            Text("3. You                    $points pts", color = SkyBlue, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Points are for fun only. No cash rewards or paid entry.",
                color = Muted,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

fun quizQuestionsFor(sport: String): List<QuizQuestion> {
    val options = when (sport) {
        "Cricket" -> listOf("Yes", "No", "True", "False")
        "Football" -> listOf("Home team", "Away team", "Draw")
        "Hockey" -> listOf("Team A", "Team B", "Draw")
        "Kabaddi" -> listOf("Team A", "Team B", "Tie")
        "eSports" -> listOf("Team A", "Team B", "Not sure")
        "F1" -> listOf("Driver A", "Driver B", "Another driver")
        "Olympics" -> listOf("Country A", "Country B", "Another country")
        else -> listOf("Wrestler A", "Wrestler B", "Not sure")
    }

    return listOf(
        QuizQuestion(
            "$sport: Select the correct answer.",
            listOf("Option A", "Option B", "Option C", "Option D"),
            setOf("Option A"),
            "Option A is the correct answer for this demo question.",
            "Multiple Choice"
        ),
        QuizQuestion(
            "$sport: Is this statement correct?",
            listOf("Yes", "No"),
            setOf("Yes"),
            "This is a demo Yes/No question.",
            "Yes / No"
        ),
        QuizQuestion(
            "$sport: Is this statement true?",
            listOf("True", "False"),
            setOf("True"),
            "This is a demo True/False question.",
            "True / False"
        ),
        QuizQuestion(
            "$sport: Select all correct options.",
            listOf("Option A", "Option B", "Option C"),
            setOf("Option A", "Option B"),
            "This is a demo Multiple Select question.",
            "Multiple Select"
        ),
        QuizQuestion(
            "$sport: Who will win the next event?",
            options,
            emptySet(),
            "Prediction saved in this demo.",
            "Prediction"
        )
    )
}
suspend fun getMatches(): List<CricketMatch> =
    withContext(Dispatchers.IO) {
        val connection = URL(
            "https://lsp-cricket-api.uddinamaan619.workers.dev/currentMatches"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)
            val data = json.optJSONArray("data")
                ?: return@withContext emptyList()

            val result = mutableListOf<CricketMatch>()

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val scoreArray = item.optJSONArray("score")
                val scoreText = StringBuilder()

                if (scoreArray != null) {
                    for (j in 0 until scoreArray.length()) {
                        val scoreItem = scoreArray.optJSONObject(j)
                        if (scoreItem != null) {
                            if (scoreText.isNotEmpty()) {
                                scoreText.append(" | ")
                            }
                            scoreText.append(scoreItem.optString("inning", ""))
                            scoreText.append(": ")
                            scoreText.append(scoreItem.optString("r", "0"))
                            scoreText.append("/")
                            scoreText.append(scoreItem.optString("w", "0"))
                            scoreText.append(" (")
                            scoreText.append(scoreItem.optString("o", "0"))
                            scoreText.append(" ov)")
                        }
                    }
                }

                result.add(
                    CricketMatch(
                        name = item.optString("name", "Cricket Match"),
                        status = item.optString("status", ""),
                        score = scoreText.toString(),
                        venue = item.optString("venue", "")
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }