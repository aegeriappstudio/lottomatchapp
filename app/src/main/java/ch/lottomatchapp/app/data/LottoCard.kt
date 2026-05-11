package ch.lottomatchapp.app.data

import android.os.Parcelable
import ch.lottomatchapp.app.stats.CardState
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cell(val number: Int = 0, val marked: Boolean = false) : Parcelable

@Parcelize
data class LottoCard(
    val name: String = "Lotto Card",
    val rows: Int = 3,
    val cols: Int = 9,
    val cells: List<Cell> = List(rows * cols) { Cell() }
) : Parcelable {

    fun setMarked(index: Int, marked: Boolean): LottoCard = copy(
        cells = cells.toMutableList().also { list ->
            val cur = list[index]
            if (cur.number != 0) list[index] = cur.copy(marked = marked)
        }
    )

    fun setNumber(index: Int, number: Int): LottoCard {
        val validated = if (number in 1..MAX_NUMBER) number else 0
        return copy(
            cells = cells.toMutableList().also { list ->
                list[index] = list[index].copy(number = validated, marked = false)
            }
        )
    }

    fun cleared(): LottoCard = copy(cells = cells.map { it.copy(marked = false) })

    fun wiped(): LottoCard = copy(cells = List(rows * cols) { Cell(0, false) })

    fun completedRows(): List<Int> {
        val done = mutableListOf<Int>()
        for (r in 0 until rows) {
            val row = cells.subList(r * cols, r * cols + cols)
            val numbered = row.filter { it.number != 0 }
            if (numbered.isNotEmpty() && numbered.all { it.marked }) done += r
        }
        return done
    }

    fun almostCompletedRows(): List<Int> {
        val result = mutableListOf<Int>()
        for (r in 0 until rows) {
            val row = cells.subList(r * cols, r * cols + cols)
            val numbered = row.filter { it.number != 0 }

            if (numbered.size == 5) {
                val markedCount = numbered.count { it.marked }
                if (markedCount == 4) {
                    result += r
                }
            }
        }
        return result
    }

    fun countNumbers(): Int = cells.count { it.number != 0 }

    fun isCompleteCard(): Boolean {
        for (r in 0 until rows) {
            val row = cells.subList(r * cols, r * cols + cols)
            if (row.count { it.number != 0 } != 5) return false
        }
        return true
    }

    fun markNumber(number: Int): LottoCard {
        return copy(cells = cells.map { if (it.number == number) it.copy(marked = true) else it })
    }

    fun unmarkNumber(number: Int): LottoCard {
        return copy(cells = cells.map { if (it.number == number) it.copy(marked = false) else it })
    }

    fun unmarkNumbers(numbers: List<Int>): LottoCard {
        val numbersToUnmark = numbers.toSet()
        return copy(cells = cells.map { if (it.number in numbersToUnmark) it.copy(marked = false) else it })
    }

    fun toCardState(): CardState {
        val numberedCells = cells.filter { it.number != 0 }
        val markedNumbers = numberedCells.count { it.marked }

        var minMissing = Int.MAX_VALUE
        for (r in 0 until rows) {
            val row = cells.subList(r * cols, r * cols + cols)
            val numbered = row.filter { it.number != 0 }
            if (numbered.isEmpty()) continue
            val missing = numbered.count { !it.marked }
            if (missing < minMissing) minMissing = missing
        }
        if (minMissing == Int.MAX_VALUE) minMissing = numberedCells.size

        return CardState(
            cardName = name,
            markedNumbers = markedNumbers,
            numbersToWin = minMissing
        )
    }

    companion object {
        const val MAX_NUMBER = 90
        fun empty(rows: Int = 3, cols: Int = 9, name: String = "Lotto Card"): LottoCard = LottoCard(name, rows, cols)
    }
}
