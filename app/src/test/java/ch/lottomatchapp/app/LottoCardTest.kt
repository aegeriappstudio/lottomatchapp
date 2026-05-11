package ch.lottomatchapp.app

import ch.lottomatchapp.app.data.Cell
import ch.lottomatchapp.app.data.LottoCard
import org.junit.Assert.*
import org.junit.Test

class LottoCardTest {

    @Test
    fun `setNumber validates range`() {
        val card = LottoCard.empty()
        val updated = card.setNumber(0, 42)
        assertEquals(42, updated.cells[0].number)

        val invalid = card.setNumber(0, 91)
        assertEquals(0, invalid.cells[0].number)

        val negative = card.setNumber(0, -5)
        assertEquals(0, negative.cells[0].number)

        val zero = card.setNumber(0, 0)
        assertEquals(0, zero.cells[0].number)
    }

    @Test
    fun `markNumber marks correct cell`() {
        val card = LottoCard.empty().setNumber(0, 5)
        val marked = card.markNumber(5)
        assertTrue(marked.cells[0].marked)
        assertFalse(marked.cells[1].marked)
    }

    @Test
    fun `unmarkNumber unmarks correct cell`() {
        val card = LottoCard.empty().setNumber(0, 5).markNumber(5)
        assertTrue(card.cells[0].marked)
        val unmarked = card.unmarkNumber(5)
        assertFalse(unmarked.cells[0].marked)
    }

    @Test
    fun `completedRows detects full row`() {
        val cells = MutableList(27) { Cell() }
        // Row 0: 5 numbers, all marked
        cells[0] = Cell(1, true)
        cells[1] = Cell(12, true)
        cells[2] = Cell(23, true)
        cells[3] = Cell(34, true)
        cells[4] = Cell(45, true)
        val card = LottoCard(cells = cells)
        assertEquals(listOf(0), card.completedRows())
    }

    @Test
    fun `completedRows does not include partial row`() {
        val cells = MutableList(27) { Cell() }
        cells[0] = Cell(1, true)
        cells[1] = Cell(12, true)
        cells[2] = Cell(23, false) // not marked
        cells[3] = Cell(34, true)
        cells[4] = Cell(45, true)
        val card = LottoCard(cells = cells)
        assertTrue(card.completedRows().isEmpty())
    }

    @Test
    fun `isCompleteCard requires 5 numbers per row`() {
        val cells = MutableList(27) { Cell() }
        for (r in 0 until 3) {
            for (i in 0 until 5) {
                cells[r * 9 + i] = Cell(r * 10 + i + 1)
            }
        }
        val card = LottoCard(cells = cells)
        assertTrue(card.isCompleteCard())
    }

    @Test
    fun `isCompleteCard returns false with incomplete rows`() {
        val card = LottoCard.empty()
        assertFalse(card.isCompleteCard())
    }

    @Test
    fun `countNumbers counts non-zero cells`() {
        val card = LottoCard.empty().setNumber(0, 5).setNumber(3, 34)
        assertEquals(2, card.countNumbers())
    }

    @Test
    fun `cleared resets marks but keeps numbers`() {
        val card = LottoCard.empty().setNumber(0, 5).markNumber(5)
        val cleared = card.cleared()
        assertEquals(5, cleared.cells[0].number)
        assertFalse(cleared.cells[0].marked)
    }

    @Test
    fun `toCardState calculates numbersToWin per row`() {
        val cells = MutableList(27) { Cell() }
        // Row 0: 5 numbers, 3 marked, 2 missing
        cells[0] = Cell(1, true)
        cells[1] = Cell(12, true)
        cells[2] = Cell(23, true)
        cells[3] = Cell(34, false)
        cells[4] = Cell(45, false)
        // Row 1: 5 numbers, 4 marked, 1 missing
        cells[9] = Cell(2, true)
        cells[10] = Cell(13, true)
        cells[11] = Cell(24, true)
        cells[12] = Cell(35, true)
        cells[13] = Cell(46, false)
        val card = LottoCard(cells = cells)
        val state = card.toCardState()
        assertEquals(1, state.numbersToWin) // best row has only 1 missing
        assertEquals(7, state.markedNumbers)
    }
}
