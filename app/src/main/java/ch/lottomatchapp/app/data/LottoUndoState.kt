package ch.lottomatchapp.app.data

import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics

/**
 * Snapshot des aktuellen Spielzustands plus Verweis auf den vorherigen Zustand
 * für Undo-Kette.
 */
data class LottoUndoState(
    val description: String,
    val cards: List<LottoCard>,
    val winRows: Int,
    val history: List<Int>,
    val lastNumber: Int?,
    val undoState: LottoUndoState?
)
