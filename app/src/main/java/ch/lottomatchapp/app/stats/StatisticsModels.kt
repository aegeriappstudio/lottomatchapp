package ch.lottomatchapp.app.stats

import java.time.LocalDate

data class RoundStatistics(
    val date: LocalDate,
    val roundNumber: Int,
    val drawnNumbers: List<Int>,
    val cardStates: List<CardState>,
    val winnersCount: Int = 0,
    val maxCompletedRows: Int = 0
)

data class CardState(
    val cardName: String,
    val markedNumbers: Int,
    val numbersToWin: Int
)

data class DailySummary(
    val date: LocalDate,
    val mostFrequentNumbers: List<Int>,
    val leastFrequentNumbers: List<Int>,
    val missingNumbers: List<Int>,
    val totalRounds: Int = 0,
    val totalDraws: Int = 0,
    val averageDrawsPerRound: Double = 0.0
)
