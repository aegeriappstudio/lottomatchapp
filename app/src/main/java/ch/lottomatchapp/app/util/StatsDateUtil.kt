package ch.lottomatchapp.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Determines the logical date for statistics, allowing sessions to extend past midnight.
 *
 * If the time is before the cutoff (e.g., 5 AM), the round is considered part of the previous day's stats.
 *
 * @param now The actual timestamp of the event.
 * @return The logical [LocalDate] for statistical grouping.
 */
const val DEFAULT_STATS_CUTOFF_HOUR = 5

fun getStatsDate(now: LocalDateTime, cutoffHour: Int = DEFAULT_STATS_CUTOFF_HOUR): LocalDate {
    val cutoff = LocalTime.of(cutoffHour.coerceIn(0, 23), 0)
    return if (now.toLocalTime().isBefore(cutoff)) {
        now.toLocalDate().minusDays(1)
    } else {
        now.toLocalDate()
    }
}
