package ch.lottomatchapp.app

import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.data.LottoUndoState
import ch.lottomatchapp.app.data.UndoAction
import ch.lottomatchapp.app.logic.GameReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReducerTest {

    private var token = 0L
    private fun nextToken() = ++token

    private fun root(
        cards: List<LottoCard> = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7).setNumber(1, 12)),
        winRows: Int = 1,
        history: List<Int> = emptyList(),
        lastNumber: Int? = null
    ) = LottoUndoState(
        action = null,
        token = 0L,
        savedRoundId = null,
        cards = cards,
        winRows = winRows,
        history = history,
        lastNumber = lastNumber,
        undoState = null
    )

    @Test
    fun `duplicate number is rejected`() {
        val base = root(history = listOf(3), lastNumber = 7)
        assertNull(GameReducer.markNumber(base, 7, nextToken()))
        assertNull(GameReducer.markNumber(base, 3, nextToken()))
    }

    @Test
    fun `number outside 1 to 90 is rejected`() {
        val base = root()
        assertNull(GameReducer.markNumber(base, 0, nextToken()))
        assertNull(GameReducer.markNumber(base, 91, nextToken()))
    }

    @Test
    fun `mark promotes previous lastNumber into history and marks cards`() {
        val first = GameReducer.markNumber(root(), 7, nextToken())!!
        assertEquals(emptyList<Int>(), first.history)
        assertEquals(7, first.lastNumber)
        assertTrue(first.cards[0].cells[0].marked)
        assertEquals(UndoAction.NumberMarked(7), first.action)

        val second = GameReducer.markNumber(first, 12, nextToken())!!
        assertEquals(listOf(7), second.history)
        assertEquals(12, second.lastNumber)
        assertTrue(second.cards[0].cells[1].marked)
        // trimUndoChain kopiert die Kette, daher Gleichheit statt Identität
        assertEquals(first, second.undoState)
    }

    @Test
    fun `remove lastNumber promotes previous history entry`() {
        val base = root(history = listOf(3, 7), lastNumber = 12)
            .copy(cards = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7).setNumber(1, 12).markNumber(7).markNumber(12)))

        val result = GameReducer.removeNumberFromHistory(base, 12, alsoUnmarkOnCards = true, token = nextToken())

        assertEquals(listOf(3), result.history)
        assertEquals(7, result.lastNumber)
        assertFalse(result.cards[0].cells[1].marked)
        assertTrue(result.cards[0].cells[0].marked)
        assertEquals(UndoAction.NumberRemoved(12), result.action)
    }

    @Test
    fun `remove lastNumber with empty history leaves no lastNumber`() {
        val base = root(history = emptyList(), lastNumber = 12)
        val result = GameReducer.removeNumberFromHistory(base, 12, alsoUnmarkOnCards = false, token = nextToken())
        assertEquals(emptyList<Int>(), result.history)
        assertNull(result.lastNumber)
    }

    @Test
    fun `remove number from middle of history keeps lastNumber`() {
        val base = root(history = listOf(3, 7, 9), lastNumber = 12)
        val result = GameReducer.removeNumberFromHistory(base, 7, alsoUnmarkOnCards = false, token = nextToken())
        assertEquals(listOf(3, 9), result.history)
        assertEquals(12, result.lastNumber)
    }

    @Test
    fun `clear history unmarks drawn numbers on cards`() {
        val base = root(history = listOf(7), lastNumber = 12)
            .copy(cards = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7).setNumber(1, 12).markNumber(7).markNumber(12)))

        val result = GameReducer.clearHistory(base, alsoUnmarkOnCards = true, token = nextToken())

        assertEquals(emptyList<Int>(), result.history)
        assertNull(result.lastNumber)
        assertFalse(result.cards[0].cells[0].marked)
        assertFalse(result.cards[0].cells[1].marked)
        assertEquals(UndoAction.HistoryCleared, result.action)
    }

    @Test
    fun `new round resets marks, history and win rows but keeps numbers`() {
        val base = root(winRows = 3, history = listOf(7), lastNumber = 12)
            .copy(cards = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7).markNumber(7)))

        val result = GameReducer.newRound(base, nextToken(), savedRoundId = 99L)

        assertEquals(1, result.winRows)
        assertEquals(emptyList<Int>(), result.history)
        assertNull(result.lastNumber)
        assertEquals(7, result.cards[0].cells[0].number)
        assertFalse(result.cards[0].cells[0].marked)
        assertEquals(UndoAction.NewRound, result.action)
        assertEquals(99L, result.savedRoundId)
    }

    @Test
    fun `undo pops exactly one state and restores cards, history, lastNumber and winRows`() {
        val base = root(winRows = 2, history = listOf(3), lastNumber = 7)
        val marked = GameReducer.markNumber(base, 12, nextToken())!!
        val rowsChanged = GameReducer.setWinRows(marked, 3, nextToken())

        val afterUndo = GameReducer.undo(rowsChanged)
        assertNotNull(afterUndo)
        assertEquals(2, afterUndo!!.winRows)
        assertEquals(listOf(3, 7), afterUndo.history)
        assertEquals(12, afterUndo.lastNumber)
        assertEquals(marked.cards, afterUndo.cards)
        assertEquals(UndoAction.NumberMarked(12), afterUndo.action)

        val afterSecondUndo = GameReducer.undo(afterUndo)
        assertEquals(base.cards, afterSecondUndo!!.cards)
        assertEquals(listOf(3), afterSecondUndo.history)
        assertEquals(7, afterSecondUndo.lastNumber)
        assertNull(afterSecondUndo.action)
        assertNull(GameReducer.undo(afterSecondUndo))
    }

    @Test
    fun `chain is trimmed at MAX_UNDO_DEPTH`() {
        var state = root()
        repeat(GameReducer.MAX_UNDO_DEPTH * 2) { i ->
            state = GameReducer.setWinRows(state, (i % 3) + 1, nextToken())
        }
        assertEquals(GameReducer.MAX_UNDO_DEPTH, GameReducer.chainLength(state))

        // Der älteste verbleibende Zustand zeigt auf nichts mehr
        var oldest = state
        while (true) {
            oldest = oldest.undoState ?: break
        }
        assertNull(oldest.undoState)
    }

    @Test
    fun `tokens are assigned to pushed states`() {
        val a = GameReducer.setWinRows(root(), 2, nextToken())
        val b = GameReducer.setWinRows(a, 3, nextToken())
        assertEquals(1L, a.token)
        assertEquals(2L, b.token)
        assertEquals(0L, b.undoState!!.undoState!!.token)
    }

    @Test
    fun `undo with expected token refuses on stale token`() {
        val deleted = GameReducer.deleteCard(root(), 0, nextToken())
        val deleteToken = deleted.token
        val marked = GameReducer.markNumber(deleted.copy(cards = root().cards), 7, nextToken())!!

        // Snackbar-Undo für "Karte gelöscht" darf die Zahl nicht rückgängig machen
        assertNull(GameReducer.undo(marked, deleteToken))
        assertNull(GameReducer.undo(null, deleteToken))

        // Mit dem aktuellen Token klappt es
        val undone = GameReducer.undo(marked, marked.token)
        assertNotNull(undone)
        assertEquals(deleteToken, undone!!.token)
    }

    @Test
    fun `undo of new round exposes savedRoundId on popped state`() {
        val base = root(history = listOf(7), lastNumber = 12)
        val top = GameReducer.newRound(base, nextToken(), savedRoundId = 4711L)

        assertEquals(UndoAction.NewRound, top.action)
        assertEquals(4711L, top.savedRoundId)
        val restored = GameReducer.undo(top)!!
        assertEquals(listOf(7), restored.history)
        assertEquals(12, restored.lastNumber)
        assertNull(restored.savedRoundId)
    }
}
