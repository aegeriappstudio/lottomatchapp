package ch.lottomatchapp.app.logic

import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.data.LottoUndoState
import ch.lottomatchapp.app.data.UndoAction

/**
 * Reine Zustandsübergänge des Spiels (ohne Android-Abhängigkeiten), damit sie
 * per JUnit testbar sind. Jeder Übergang liefert einen neuen [LottoUndoState],
 * der auf den (getrimmten) Vorgänger zeigt; das ViewModel persistiert ihn nur noch.
 */
object GameReducer {

    const val MAX_UNDO_DEPTH = 30
    const val MIN_NUMBER = 1
    const val MAX_NUMBER = 90

    /** Kürzt die Kette auf höchstens [depth] Zustände (inkl. [state] selbst). */
    fun trimUndoChain(state: LottoUndoState?, depth: Int): LottoUndoState? {
        if (state == null || depth <= 0) return null
        return state.copy(undoState = trimUndoChain(state.undoState, depth - 1))
    }

    /** Anzahl Zustände in der Kette (inkl. [state]). */
    fun chainLength(state: LottoUndoState?): Int {
        var n = 0
        var cur = state
        while (cur != null) {
            n++
            cur = cur.undoState
        }
        return n
    }

    /**
     * Legt einen neuen Zustand auf die Kette: [transform] wird auf [base]
     * angewendet, das Ergebnis erhält [action]/[token]/[savedRoundId] und zeigt
     * auf [base] (Kette insgesamt auf [MAX_UNDO_DEPTH] Zustände begrenzt).
     */
    fun push(
        base: LottoUndoState,
        action: UndoAction,
        token: Long,
        savedRoundId: Long? = null,
        transform: (LottoUndoState) -> LottoUndoState
    ): LottoUndoState {
        val transformed = transform(base)
        return transformed.copy(
            action = action,
            token = token,
            savedRoundId = savedRoundId,
            undoState = trimUndoChain(base, MAX_UNDO_DEPTH - 1)
        )
    }

    /** Vorgänger von [top] oder `null`, wenn nichts rückgängig zu machen ist. */
    fun undo(top: LottoUndoState?): LottoUndoState? = top?.undoState

    /**
     * Wie [undo], greift aber nur, wenn [top] noch genau den Zustand mit
     * [expectedToken] darstellt (Snackbar-Undo darf nicht die falsche Aktion treffen).
     */
    fun undo(top: LottoUndoState?, expectedToken: Long): LottoUndoState? =
        if (top != null && top.token == expectedToken) top.undoState else null

    /**
     * Zahl ziehen: wird abgelehnt (null), wenn ausserhalb 1..90 oder bereits
     * gezogen. Die bisherige letzte Zahl wandert in die History.
     */
    fun markNumber(base: LottoUndoState, number: Int, token: Long): LottoUndoState? {
        if (number !in MIN_NUMBER..MAX_NUMBER) return null
        if (base.history.contains(number) || base.lastNumber == number) return null
        return push(base, UndoAction.NumberMarked(number), token) {
            it.copy(
                cards = it.cards.map { card -> card.markNumber(number) },
                history = it.lastNumber?.let { n -> it.history + n } ?: it.history,
                lastNumber = number
            )
        }
    }

    /**
     * Zahl aus dem Verlauf entfernen. Ist es die letzte Zahl, rückt der letzte
     * History-Eintrag nach (oder null, wenn keiner vorhanden).
     */
    fun removeNumberFromHistory(
        base: LottoUndoState,
        number: Int,
        alsoUnmarkOnCards: Boolean,
        token: Long
    ): LottoUndoState = push(base, UndoAction.NumberRemoved(number), token) {
        val updatedCards = if (alsoUnmarkOnCards) {
            it.cards.map { card -> card.unmarkNumber(number) }
        } else it.cards

        if (it.lastNumber == number) {
            it.copy(
                cards = updatedCards,
                history = it.history.dropLast(1),
                lastNumber = it.history.lastOrNull()
            )
        } else {
            it.copy(
                cards = updatedCards,
                history = it.history.filter { n -> n != number }
            )
        }
    }

    fun clearHistory(base: LottoUndoState, alsoUnmarkOnCards: Boolean, token: Long): LottoUndoState =
        push(base, UndoAction.HistoryCleared, token) {
            val updatedCards = if (alsoUnmarkOnCards) {
                val numbersToUnmark = it.history + listOfNotNull(it.lastNumber)
                it.cards.map { card -> card.unmarkNumbers(numbersToUnmark) }
            } else it.cards
            it.copy(cards = updatedCards, history = emptyList(), lastNumber = null)
        }

    /** Neue Runde: Markierungen weg, Verlauf leer, Gewinnreihen zurück auf 1. */
    fun newRound(base: LottoUndoState, token: Long, savedRoundId: Long?): LottoUndoState =
        push(base, UndoAction.NewRound, token, savedRoundId) {
            it.copy(
                cards = it.cards.map { card -> card.cleared() },
                history = emptyList(),
                lastNumber = null,
                winRows = 1
            )
        }

    fun wipeAll(base: LottoUndoState, token: Long): LottoUndoState =
        push(base, UndoAction.AllCleared, token) {
            it.copy(cards = emptyList(), history = emptyList(), lastNumber = null)
        }

    fun addCard(base: LottoUndoState, card: LottoCard, token: Long): LottoUndoState =
        push(base, UndoAction.CardAdded, token) { it.copy(cards = it.cards + card) }

    fun deleteCard(base: LottoUndoState, index: Int, token: Long): LottoUndoState =
        push(base, UndoAction.CardDeleted(index), token) {
            it.copy(cards = it.cards.filterIndexed { i, _ -> i != index })
        }

    fun replaceCard(base: LottoUndoState, index: Int, updated: LottoCard, token: Long): LottoUndoState =
        push(base, UndoAction.CellChanged(index), token) {
            it.copy(cards = it.cards.mapIndexed { i, card -> if (i == index) updated else card })
        }

    fun renameCard(base: LottoUndoState, index: Int, newName: String, token: Long): LottoUndoState =
        push(base, UndoAction.CardRenamed, token) {
            it.copy(cards = it.cards.mapIndexed { i, card -> if (i == index) card.copy(name = newName) else card })
        }

    fun setWinRows(base: LottoUndoState, rows: Int, token: Long): LottoUndoState =
        push(base, UndoAction.WinRowsChanged, token) { it.copy(winRows = rows) }

    fun clearMarksOnCard(base: LottoUndoState, index: Int, token: Long): LottoUndoState =
        push(base, UndoAction.MarksCleared(index), token) {
            it.copy(cards = it.cards.mapIndexed { i, card -> if (i == index) card.cleared() else card })
        }

    fun applyScanToCard(base: LottoUndoState, index: Int, scanned: LottoCard, token: Long): LottoUndoState =
        push(base, UndoAction.ScanApplied(index), token) {
            it.copy(cards = it.cards.mapIndexed { i, c -> if (i == index) c.copy(cells = scanned.cells) else c })
        }
}
