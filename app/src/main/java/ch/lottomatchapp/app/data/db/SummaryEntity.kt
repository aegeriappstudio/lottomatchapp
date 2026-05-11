package ch.lottomatchapp.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_summaries")
data class SummaryEntity(
    @PrimaryKey val date: LocalDate,
    val mostFrequentNumbers: List<Int>,
    val leastFrequentNumbers: List<Int>,
    val missingNumbers: List<Int>,
    val totalRounds: Int = 0,
    val totalDraws: Int = 0,
    val averageDrawsPerRound: Double = 0.0
)
