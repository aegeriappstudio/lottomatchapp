package ch.lottomatchapp.app

import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.data.LottoUndoState
import ch.lottomatchapp.app.data.UndoAction
import ch.lottomatchapp.app.persistence.UndoChainCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UndoChainCodecTest {

    private fun state(
        action: UndoAction?,
        token: Long,
        savedRoundId: Long? = null,
        cards: List<LottoCard> = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7)),
        winRows: Int = 1,
        history: List<Int> = emptyList(),
        lastNumber: Int? = null,
        previous: LottoUndoState? = null
    ) = LottoUndoState(
        action = action,
        token = token,
        savedRoundId = savedRoundId,
        cards = cards,
        winRows = winRows,
        history = history,
        lastNumber = lastNumber,
        undoState = previous
    )

    @Test
    fun `chain of three states survives roundtrip`() {
        val root = state(action = null, token = 0)
        val marked = state(
            action = UndoAction.NumberMarked(7),
            token = 1,
            cards = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7).markNumber(7)),
            lastNumber = 7,
            previous = root
        )
        val newRound = state(
            action = UndoAction.NewRound,
            token = 2,
            savedRoundId = 42L,
            cards = listOf(LottoCard.empty(name = "Card 1").setNumber(0, 7)),
            winRows = 2,
            previous = marked
        )

        val decoded = UndoChainCodec.decode(UndoChainCodec.encode(newRound))

        assertEquals(newRound, decoded)
    }

    @Test
    fun `every action type roundtrips`() {
        val actions = listOf(
            UndoAction.NumberMarked(15),
            UndoAction.CardAdded,
            UndoAction.CardDeleted(2),
            UndoAction.CellChanged(0),
            UndoAction.CardRenamed,
            UndoAction.WinRowsChanged,
            UndoAction.MarksCleared(1),
            UndoAction.ScanApplied(3),
            UndoAction.NewRound,
            UndoAction.AllCleared,
            UndoAction.HistoryCleared,
            UndoAction.NumberRemoved(88)
        )
        var chain: LottoUndoState = state(action = null, token = 0)
        actions.forEachIndexed { i, action ->
            chain = state(action = action, token = (i + 1).toLong(), previous = chain)
        }

        val decoded = UndoChainCodec.decode(UndoChainCodec.encode(chain))

        assertEquals(chain, decoded)
        assertEquals(actions.size + 1, chainToList(decoded).size)
    }

    @Test
    fun `root only chain roundtrips`() {
        val root = state(action = null, token = 0, history = listOf(1, 2, 3), lastNumber = 4)
        val decoded = UndoChainCodec.decode(UndoChainCodec.encode(root))
        assertEquals(root, decoded)
        assertNull(decoded!!.undoState)
    }

    @Test
    fun `card names containing separators do not break the chain`() {
        val nasty = "A${UndoChainCodec.RECORD_SEPARATOR}B${UndoChainCodec.FIELD_SEPARATOR}C|;:,\n%"
        val root = state(action = null, token = 0, cards = listOf(LottoCard.empty(name = nasty)))
        val top = state(action = UndoAction.CardRenamed, token = 1, cards = listOf(LottoCard.empty(name = nasty)), previous = root)

        val decoded = UndoChainCodec.decode(UndoChainCodec.encode(top))

        assertEquals(top, decoded)
        assertEquals(nasty, decoded!!.cards[0].name)
        assertEquals(nasty, decoded.undoState!!.cards[0].name)
    }

    @Test
    fun `decode rejects empty and garbage input`() {
        assertNull(UndoChainCodec.decode(""))
        assertNull(UndoChainCodec.decode("garbage"))
    }

    @Test
    fun `decode rejects chain with one broken record`() {
        val root = state(action = null, token = 0)
        val top = state(action = UndoAction.CardAdded, token = 1, previous = root)
        val encoded = UndoChainCodec.encode(top)
        val broken = encoded + UndoChainCodec.RECORD_SEPARATOR + "XX" +
            UndoChainCodec.FIELD_SEPARATOR + UndoChainCodec.FIELD_SEPARATOR + "3" +
            UndoChainCodec.FIELD_SEPARATOR + UndoChainCodec.FIELD_SEPARATOR + "1\n\n\n"

        assertNotNull(UndoChainCodec.decode(encoded))
        assertNull(UndoChainCodec.decode(broken))
    }

    @Test
    fun `decode rejects action that is missing its argument`() {
        val record = "NM" + UndoChainCodec.FIELD_SEPARATOR + "" + UndoChainCodec.FIELD_SEPARATOR + "1" +
            UndoChainCodec.FIELD_SEPARATOR + "" + UndoChainCodec.FIELD_SEPARATOR + "1\n\n\nCard 1;3x9:"
        assertNull(UndoChainCodec.decode(record))
    }

    private fun chainToList(state: LottoUndoState?): List<LottoUndoState> {
        val list = mutableListOf<LottoUndoState>()
        var cur = state
        while (cur != null) {
            list += cur
            cur = cur.undoState
        }
        return list
    }
}
