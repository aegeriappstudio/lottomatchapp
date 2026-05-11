package ch.lottomatchapp.app.persistence

import android.content.Context
import ch.lottomatchapp.app.data.Cell
import ch.lottomatchapp.app.data.LottoCard

data class PersistedState(
    val winRows: Int,
    val cards: List<LottoCard>,
    val history: List<Int>,
    val lastNumber: Int?
)

internal fun encodeState(winRows: Int, cards: List<LottoCard>, history: List<Int>, lastNumber: Int?): String {
    val sb = StringBuilder()
    sb.append(winRows).append('\n')
    sb.append(history.joinToString(",")).append('\n')
    sb.append(lastNumber?.toString() ?: "").append('\n')
    sb.append(
        cards.joinToString("|") { card ->
            buildString {
                append(card.name).append(';')
                append(card.rows).append('x').append(card.cols).append(':')
                append(
                    card.cells.joinToString(",") { cell ->
                        "${cell.number}:${if (cell.marked) 1 else 0}"
                    }
                )
            }
        }
    )
    return sb.toString()
}

internal fun decodeState(data: String): PersistedState? {
    return runCatching {
        val lines = data.split('\n')
        if (lines.size < 4) return null

        val winRows = lines[0].toIntOrNull() ?: 1

        val historyLine = lines[1]
        val history = if (historyLine.isBlank()) {
            emptyList()
        } else {
            historyLine.split(',').mapNotNull { it.toIntOrNull() }
        }

        val lastNumber = lines[2].toIntOrNull()

        val cardsLine = lines[3]
        val cards: List<LottoCard> =
            if (cardsLine.isBlank()) {
                listOf(LottoCard.empty())
            } else {
                cardsLine.split('|').mapNotNull { cardStr ->
                    val headerBody = cardStr.split(':', limit = 2)
                    if (headerBody.size != 2) return@mapNotNull null
                    val header = headerBody[0]
                    val body = headerBody[1]

                    val nameDim = header.split(';')
                    val name = if (nameDim.size > 1) nameDim[0] else "Lotto Card"
                    val dims = if (nameDim.size > 1) nameDim[1] else nameDim[0]

                    val rc = dims.split('x')
                    val rows = rc.getOrNull(0)?.toIntOrNull() ?: 3
                    val cols = rc.getOrNull(1)?.toIntOrNull() ?: 9

                    val cellTokens = if (body.isBlank()) emptyList() else body.split(',')
                    val totalCells = rows * cols
                    val cells = MutableList(totalCells) { Cell() }
                    for (i in 0 until minOf(totalCells, cellTokens.size)) {
                        val token = cellTokens[i]
                        val nm = token.split(':')
                        val n = nm.getOrNull(0)?.toIntOrNull() ?: 0
                        val m = nm.getOrNull(1)?.toIntOrNull() ?: 0
                        cells[i] = Cell(number = n, marked = m == 1)
                    }
                    LottoCard(name = name, rows = rows, cols = cols, cells = cells)
                }
            }

        PersistedState(winRows, cards, history, lastNumber)
    }.getOrNull()
}
