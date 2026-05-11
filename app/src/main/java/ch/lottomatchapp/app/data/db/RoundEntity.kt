package ch.lottomatchapp.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundNumber: Int,
    val date: LocalDate,
    val drawnNumbers: List<Int>,
    val winnersCount: Int = 0,
    val maxCompletedRows: Int = 0,
    val cardStatesJson: String = "[]"
)
