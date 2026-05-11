package ch.lottomatchapp.app

import ch.lottomatchapp.app.util.getStatsDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StatsDateUtilTest {

    @Test
    fun `before cutoff returns previous day`() {
        val now = LocalDateTime.of(2025, 3, 15, 3, 30) // 3:30 AM
        val result = getStatsDate(now)
        assertEquals(LocalDate.of(2025, 3, 14), result)
    }

    @Test
    fun `after cutoff returns same day`() {
        val now = LocalDateTime.of(2025, 3, 15, 10, 0) // 10:00 AM
        val result = getStatsDate(now)
        assertEquals(LocalDate.of(2025, 3, 15), result)
    }

    @Test
    fun `exactly at cutoff returns same day`() {
        val now = LocalDateTime.of(2025, 3, 15, 5, 0) // exactly 5:00 AM
        val result = getStatsDate(now)
        assertEquals(LocalDate.of(2025, 3, 15), result)
    }

    @Test
    fun `custom cutoff hour works`() {
        val now = LocalDateTime.of(2025, 3, 15, 6, 0) // 6:00 AM
        // With cutoff at 7, this is still "yesterday"
        val result = getStatsDate(now, cutoffHour = 7)
        assertEquals(LocalDate.of(2025, 3, 14), result)
    }

    @Test
    fun `midnight is before cutoff`() {
        val now = LocalDateTime.of(2025, 3, 15, 0, 0) // midnight
        val result = getStatsDate(now)
        assertEquals(LocalDate.of(2025, 3, 14), result)
    }
}
