package ch.lottomatchapp.app

import ch.lottomatchapp.app.data.Cell
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.persistence.PersistedState
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

    private fun roundTripName(name: String): PersistedState {
        val card = LottoCard.empty(name = name).setNumber(0, 7)
        val other = LottoCard.empty(name = "Other").setNumber(1, 8)
        val encoded = encodeState(winRows = 2, cards = listOf(card, other), history = listOf(7), lastNumber = 7)
        val decoded = decodeState(encoded)
        assertNotNull("decode failed for name '$name'", decoded)
        return decoded!!
    }

    @Test
    fun `card names containing delimiters survive roundtrip`() {
        val names = listOf(
            "Oma: Karte 1",
            "A|B",
            "Semi;colon",
            "Comma, separated",
            "All |;:, at once"
        )
        for (name in names) {
            val decoded = roundTripName(name)
            assertEquals(2, decoded.cards.size)
            assertEquals(name, decoded.cards[0].name)
            assertEquals(7, decoded.cards[0].cells[0].number)
            assertEquals("Other", decoded.cards[1].name)
            assertEquals(8, decoded.cards[1].cells[1].number)
            assertEquals(2, decoded.winRows)
            assertEquals(listOf(7), decoded.history)
        }
    }

    @Test
    fun `card name containing percent survives roundtrip`() {
        val name = "100% Glück %7C nicht"
        val decoded = roundTripName(name)
        assertEquals(name, decoded.cards[0].name)
        assertEquals("Other", decoded.cards[1].name)
    }

    @Test
    fun `card name containing newline survives roundtrip`() {
        val name = "Line1\nLine2\r\nLine3"
        val decoded = roundTripName(name)
        assertEquals(name, decoded.cards[0].name)
        assertEquals(2, decoded.cards.size)
        assertEquals(listOf(7), decoded.history)
        assertEquals(7, decoded.lastNumber)
    }

    @Test
    fun `empty card name survives roundtrip`() {
        val decoded = roundTripName("")
        assertEquals("", decoded.cards[0].name)
        assertEquals(7, decoded.cards[0].cells[0].number)
        assertEquals("Other", decoded.cards[1].name)
    }

    @Test
    fun `legacy encoded state without escaping still decodes`() {
        // Format wie von älteren App-Versionen geschrieben (Name unescaped, ohne '%')
        val legacy = "2\n5,12\n12\nOma Karte;3x9:5:1,12:0|Card 2;3x9:33:0"
        val decoded = decodeState(legacy)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.winRows)
        assertEquals(listOf(5, 12), decoded.history)
        assertEquals(12, decoded.lastNumber)
        assertEquals(2, decoded.cards.size)
        assertEquals("Oma Karte", decoded.cards[0].name)
        assertEquals(5, decoded.cards[0].cells[0].number)
        assertTrue(decoded.cards[0].cells[0].marked)
        assertEquals(12, decoded.cards[0].cells[1].number)
        assertFalse(decoded.cards[0].cells[1].marked)
        assertEquals("Card 2", decoded.cards[1].name)
        assertEquals(33, decoded.cards[1].cells[0].number)
    }

    @Test
    fun `legacy name with literal percent that is not an escape stays unchanged`() {
        val legacy = "1\n\n\n50% Karte;3x9:"
        val decoded = decodeState(legacy)
        assertNotNull(decoded)
        assertEquals("50% Karte", decoded!!.cards[0].name)
    }
}
