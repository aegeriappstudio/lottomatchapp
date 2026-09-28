package ch.lottomatchapp.app.data

/**
 * Snapshot des aktuellen Spielzustands plus Verweis auf den vorherigen Zustand
 * für Undo-Kette.
 *
 * @param action Aktion, die zu diesem Zustand geführt hat; `null` für den
 *   Wurzelzustand (nichts mehr rückgängig zu machen).
 * @param token Monoton steigende Kennung des Zustands. Erlaubt gezieltes Undo
 *   (z.B. aus einer Snackbar), das nur greift, wenn seither nichts anderes
 *   passiert ist.
 * @param savedRoundId Room-ID der Statistik-Zeile, die beim Start einer neuen
 *   Runde gespeichert wurde; nur bei [UndoAction.NewRound] gesetzt, damit ein
 *   Undo die Zeile wieder entfernen kann.
 */
data class LottoUndoState(
    val action: UndoAction?,
    val token: Long,
    val savedRoundId: Long?,
    val cards: List<LottoCard>,
    val winRows: Int,
    val history: List<Int>,
    val lastNumber: Int?,
    val undoState: LottoUndoState?
)
