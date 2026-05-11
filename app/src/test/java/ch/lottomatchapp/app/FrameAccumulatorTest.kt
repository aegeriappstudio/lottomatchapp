package ch.lottomatchapp.app

import androidx.compose.ui.geometry.Offset
import ch.lottomatchapp.app.scanner.FrameAccumulator
import ch.lottomatchapp.app.scanner.RecognizedNumber
import org.junit.Assert.*
import org.junit.Test

class FrameAccumulatorTest {

    private fun rn(value: Int) = RecognizedNumber(
        value = value,
        center = Offset(value * 10f, 100f),
        width = 30f,
        height = 30f
    )

    @Test
    fun `returns empty when not enough frames`() {
        val acc = FrameAccumulator(bufferSize = 5, requiredHits = 3)
        val result = acc.addFrame(listOf(rn(5), rn(10)))
        // Erst 1 Frame → kein Hit erreicht 3
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns stable numbers after enough consistent frames`() {
        val acc = FrameAccumulator(bufferSize = 5, requiredHits = 3)
        // Zahl 5 und 10 in 3 aufeinanderfolgenden Frames
        acc.addFrame(listOf(rn(5), rn(10)))
        acc.addFrame(listOf(rn(5), rn(10), rn(20)))
        val result = acc.addFrame(listOf(rn(5), rn(10), rn(20)))

        val stableValues = result.map { it.value }.toSet()
        assertTrue(5 in stableValues)
        assertTrue(10 in stableValues)
    }

    @Test
    fun `filters out flickering numbers`() {
        val acc = FrameAccumulator(bufferSize = 5, requiredHits = 3)
        // Zahl 99 erscheint nur in 1 von 5 Frames (Flicker)
        acc.addFrame(listOf(rn(5), rn(10)))
        acc.addFrame(listOf(rn(5), rn(10)))
        acc.addFrame(listOf(rn(5), rn(10), rn(99))) // 99 nur hier
        acc.addFrame(listOf(rn(5), rn(10)))
        val result = acc.addFrame(listOf(rn(5), rn(10)))

        val stableValues = result.map { it.value }.toSet()
        assertTrue(5 in stableValues)
        assertTrue(10 in stableValues)
        assertFalse(99 in stableValues)
    }

    @Test
    fun `ring buffer evicts old frames`() {
        val acc = FrameAccumulator(bufferSize = 3, requiredHits = 2)
        // Zahl 5 in Frame 1+2, dann 3 Frames ohne Zahl 5
        acc.addFrame(listOf(rn(5)))
        acc.addFrame(listOf(rn(5)))
        acc.addFrame(listOf(rn(10))) // 5 raus aus Buffer-Fenster
        acc.addFrame(listOf(rn(10)))
        val result = acc.addFrame(listOf(rn(10)))

        val stableValues = result.map { it.value }.toSet()
        assertFalse(5 in stableValues)
        assertTrue(10 in stableValues)
    }

    @Test
    fun `reset clears buffer`() {
        val acc = FrameAccumulator(bufferSize = 5, requiredHits = 3)
        acc.addFrame(listOf(rn(5)))
        acc.addFrame(listOf(rn(5)))
        acc.addFrame(listOf(rn(5)))
        acc.reset()
        val result = acc.addFrame(listOf(rn(5)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `currentStableCount returns correct count`() {
        val acc = FrameAccumulator(bufferSize = 5, requiredHits = 3)
        acc.addFrame(listOf(rn(5), rn(10), rn(20)))
        acc.addFrame(listOf(rn(5), rn(10), rn(20)))
        acc.addFrame(listOf(rn(5), rn(10)))

        assertEquals(2, acc.currentStableCount()) // 5 und 10 sind 3x, 20 nur 2x
    }
}
