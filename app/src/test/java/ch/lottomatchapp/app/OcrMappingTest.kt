package ch.lottomatchapp.app

import androidx.compose.ui.geometry.Offset
import ch.lottomatchapp.app.scanner.RecognizedNumber
import ch.lottomatchapp.app.scanner.columnRanges90
import ch.lottomatchapp.app.scanner.mapRecognizedToLottoCard
import ch.lottomatchapp.app.scanner.splitConcatenatedNumbers
import org.junit.Assert.*
import org.junit.Test

class OcrMappingTest {

    @Test
    fun `columnRanges90 has 9 ranges covering 1-90`() {
        val ranges = columnRanges90()
        assertEquals(9, ranges.size)
        assertEquals(1, ranges.first().first)
        assertEquals(90, ranges.last().last)
    }

    @Test
    fun `mapRecognizedToLottoCard returns empty card for empty input`() {
        val card = mapRecognizedToLottoCard(emptyList())
        assertEquals(0, card.countNumbers())
    }

    @Test
    fun `mapRecognizedToLottoCard places numbers in correct columns`() {
        // Numbers at X-positions consistent with their column ranges
        val recognized = listOf(
            RecognizedNumber(5, Offset(50f, 100f), 30f, 30f),    // col 0, left
            RecognizedNumber(25, Offset(250f, 100f), 30f, 30f),  // col 2, middle-left
            RecognizedNumber(67, Offset(650f, 100f), 30f, 30f),  // col 6, right
        )

        val card = mapRecognizedToLottoCard(recognized)
        assertEquals(3, card.countNumbers())

        val numberPositions = card.cells.mapIndexed { idx, cell -> idx to cell }
            .filter { it.second.number != 0 }

        numberPositions.forEach { (idx, cell) ->
            val col = idx % 9
            val range = columnRanges90()[col]
            assertTrue("Number ${cell.number} should be in range $range for col $col",
                cell.number in range)
        }
    }

    @Test
    fun `mapRecognizedToLottoCard limits to 5 numbers per row`() {
        // 7 numbers all at same Y, spread across X proportionally to their columns
        val recognized = (1..7).map { i ->
            RecognizedNumber(i * 10, Offset(i * 100f, 100f), 30f, 30f)
        }

        val card = mapRecognizedToLottoCard(recognized)

        for (r in 0 until 3) {
            val rowCells = card.cells.subList(r * 9, r * 9 + 9)
            assertTrue(rowCells.count { it.number != 0 } <= 5)
        }
    }

    @Test
    fun `mapRecognizedToLottoCard distributes across 3 rows by Y position`() {
        // Same X to avoid skew correction issues with only 3 points
        val recognized = listOf(
            RecognizedNumber(5, Offset(100f, 50f), 30f, 30f),
            RecognizedNumber(15, Offset(100f, 200f), 30f, 30f),
            RecognizedNumber(25, Offset(100f, 350f), 30f, 30f),
        )

        val card = mapRecognizedToLottoCard(recognized)
        assertEquals(3, card.countNumbers())

        val occupiedRows = card.cells.mapIndexed { idx, cell -> idx / 9 to cell }
            .filter { it.second.number != 0 }
            .map { it.first }
            .distinct()

        assertEquals(3, occupiedRows.size)
    }

    @Test
    fun `mapRecognizedToLottoCard caps at 15 numbers total`() {
        val recognized = (1..20).map { i ->
            val value = ((i - 1) % 9 + 1) * if (i <= 10) 1 else 10
                .coerceIn(1, 90)
            RecognizedNumber(
                value.coerceIn(1, 90),
                Offset(((i % 9) * 100 + 50).toFloat(), ((i / 9) * 100 + 50).toFloat()),
                30f,
                30f
            )
        }

        val card = mapRecognizedToLottoCard(recognized)
        assertTrue(card.countNumbers() <= 15)
    }

    // ---- splitConcatenatedNumbers Tests ----

    @Test
    fun `splitConcatenatedNumbers splits even-length pairs`() {
        assertEquals(listOf(15, 20), splitConcatenatedNumbers("1520"))
        assertEquals(listOf(54, 65), splitConcatenatedNumbers("5465"))
        assertEquals(listOf(69, 70), splitConcatenatedNumbers("6970"))
        assertEquals(listOf(33, 47), splitConcatenatedNumbers("3347"))
    }

    @Test
    fun `splitConcatenatedNumbers splits odd-length strings`() {
        val result = splitConcatenatedNumbers("52367")
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it in 1..90 })
        // Should prefer 2-digit numbers: [52, 36, 7] or [5, 23, 67]
        assertTrue(result.size >= 2)
    }

    @Test
    fun `splitConcatenatedNumbers returns empty for single or two digits`() {
        assertEquals(emptyList<Int>(), splitConcatenatedNumbers("5"))
        assertEquals(emptyList<Int>(), splitConcatenatedNumbers("45"))
    }

    @Test
    fun `splitConcatenatedNumbers returns empty for all zeros`() {
        assertEquals(emptyList<Int>(), splitConcatenatedNumbers("0000"))
    }

    @Test
    fun `splitConcatenatedNumbers handles three-digit real cases`() {
        // "890" should split as [8, 90] (both valid)
        val result = splitConcatenatedNumbers("890")
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it in 1..90 })
    }

    @Test
    fun `splitConcatenatedNumbers prefers two-digit splits`() {
        // "1234" should prefer [12, 34] over [1, 2, 3, 4]
        val result = splitConcatenatedNumbers("1234")
        assertEquals(listOf(12, 34), result)
    }

    // ---- X-Position validation ----

    @Test
    fun `skewed numbers are placed in correct rows`() {
        // Numbers of a "row" are skewed (Y increases with X)
        val row1 = listOf(
            RecognizedNumber(5, Offset(50f, 100f), 30f, 30f),
            RecognizedNumber(25, Offset(250f, 120f), 30f, 30f),
            RecognizedNumber(45, Offset(450f, 140f), 30f, 30f),
            RecognizedNumber(65, Offset(650f, 130f), 30f, 30f),
            RecognizedNumber(85, Offset(850f, 110f), 30f, 30f),
        )
        val row2 = listOf(
            RecognizedNumber(8, Offset(50f, 300f), 30f, 30f),
            RecognizedNumber(28, Offset(250f, 320f), 30f, 30f),
            RecognizedNumber(38, Offset(350f, 330f), 30f, 30f),
            RecognizedNumber(58, Offset(550f, 340f), 30f, 30f),
            RecognizedNumber(78, Offset(750f, 310f), 30f, 30f),
        )
        val row3 = listOf(
            RecognizedNumber(3, Offset(50f, 500f), 30f, 30f),
            RecognizedNumber(13, Offset(150f, 520f), 30f, 30f),
            RecognizedNumber(43, Offset(450f, 530f), 30f, 30f),
            RecognizedNumber(63, Offset(650f, 540f), 30f, 30f),
            RecognizedNumber(83, Offset(850f, 510f), 30f, 30f),
        )

        val card = mapRecognizedToLottoCard(row1 + row2 + row3)
        assertEquals(15, card.countNumbers())

        for (r in 0 until 3) {
            val rowCells = card.cells.subList(r * 9, r * 9 + 9)
            assertEquals("Row $r should have 5 numbers", 5, rowCells.count { it.number != 0 })
        }
    }

    // ---- Real-world card test ----

    @Test
    fun `simulated real card maps correctly`() {
        // Simuliert die Karte aus dem Screenshot:
        // Zeile 1: 8, 28, 38, 54, 65
        // Zeile 2: 16, 33, 47, 73, 89
        // Zeile 3: 17, 29, 51, 69, 70
        // X-Positionen proportional zu Spalten auf einer 900px breiten Karte
        val card = listOf(
            // Row 1 (Y ≈ 100)
            RecognizedNumber(8, Offset(50f, 100f), 40f, 50f),
            RecognizedNumber(28, Offset(250f, 100f), 40f, 50f),
            RecognizedNumber(38, Offset(350f, 100f), 40f, 50f),
            RecognizedNumber(54, Offset(550f, 100f), 40f, 50f),
            RecognizedNumber(65, Offset(650f, 100f), 40f, 50f),
            // Row 2 (Y ≈ 200)
            RecognizedNumber(16, Offset(150f, 200f), 40f, 50f),
            RecognizedNumber(33, Offset(350f, 200f), 40f, 50f),
            RecognizedNumber(47, Offset(450f, 200f), 40f, 50f),
            RecognizedNumber(73, Offset(750f, 200f), 40f, 50f),
            RecognizedNumber(89, Offset(850f, 200f), 40f, 50f),
            // Row 3 (Y ≈ 300)
            RecognizedNumber(17, Offset(150f, 300f), 40f, 50f),
            RecognizedNumber(29, Offset(250f, 300f), 40f, 50f),
            RecognizedNumber(51, Offset(550f, 300f), 40f, 50f),
            RecognizedNumber(69, Offset(650f, 300f), 40f, 50f),
            RecognizedNumber(70, Offset(750f, 300f), 40f, 50f),
        )

        val result = mapRecognizedToLottoCard(card)
        assertTrue(result.isCompleteCard())
        assertEquals(15, result.countNumbers())

        // Verify specific numbers are present
        val allNumbers = result.cells.map { it.number }.filter { it != 0 }.toSet()
        val expected = setOf(8, 28, 38, 54, 65, 16, 33, 47, 73, 89, 17, 29, 51, 69, 70)
        assertEquals(expected, allNumbers)
    }
}
