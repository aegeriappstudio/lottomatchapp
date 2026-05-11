package ch.lottomatchapp.app

import ch.lottomatchapp.app.data.Cell
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.persistence.decodeState
import ch.lottomatchapp.app.persistence.encodeState
import org.junit.Assert.*
import org.junit.Test

class GameStateTest {

    @Test
    fun `encode and decode roundtrip preserves state`() {
        val cells = MutableList(27) { Cell() }
        cells[0] = Cell(5, true)
        cells[1] = Cell(12, false)
        val card = LottoCard(name = "Test Card", cells = cells)

        val encoded = encodeState(
            winRows = 2,
            cards = listOf(card),
            history = listOf(5, 12, 33),
            lastNumber = 42
        )

        val decoded = decodeState(encoded)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.winRows)
        assertEquals(listOf(5, 12, 33), decoded.history)
        assertEquals(42, decoded.lastNumber)
        assertEquals(1, decoded.cards.size)
        assertEquals("Test Card", decoded.cards[0].name)
        assertEquals(5, decoded.cards[0].cells[0].number)
        assertTrue(decoded.cards[0].cells[0].marked)
        assertEquals(12, decoded.cards[0].cells[1].number)
        assertFalse(decoded.cards[0].cells[1].marked)
    }

    @Test
    fun `decode returns null for invalid data`() {
        assertNull(decodeState("invalid"))
        assertNull(decodeState("1\n2"))
    }

    @Test
    fun `encode and decode with empty history`() {
        val card = LottoCard.empty(name = "Card 1")
        val encoded = encodeState(
            winRows = 1,
            cards = listOf(card),
            history = emptyList(),
            lastNumber = null
        )

        val decoded = decodeState(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.winRows)
        assertTrue(decoded.history.isEmpty())
        assertNull(decoded.lastNumber)
    }

    @Test
    fun `encode and decode with multiple cards`() {
        val card1 = LottoCard.empty(name = "Card 1").setNumber(0, 7)
        val card2 = LottoCard.empty(name = "Card 2").setNumber(4, 45)

        val encoded = encodeState(
            winRows = 3,
            cards = listOf(card1, card2),
            history = listOf(7, 45),
            lastNumber = 45
        )

        val decoded = decodeState(encoded)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.cards.size)
        assertEquals("Card 1", decoded.cards[0].name)
        assertEquals(7, decoded.cards[0].cells[0].number)
        assertEquals("Card 2", decoded.cards[1].name)
        assertEquals(45, decoded.cards[1].cells[4].number)
    }
}
