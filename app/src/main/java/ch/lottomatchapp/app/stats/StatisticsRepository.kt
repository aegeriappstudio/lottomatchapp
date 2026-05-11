package ch.lottomatchapp.app.stats

import android.content.Context
import android.util.Log
import ch.lottomatchapp.app.data.db.AppDatabase
import ch.lottomatchapp.app.data.db.RoundEntity
import ch.lottomatchapp.app.data.db.SummaryEntity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

private const val TAG = "StatisticsRepository"

class StatisticsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val roundDao = db.roundDao()
    private val summaryDao = db.summaryDao()

    init {
        try {
            migrateFromSharedPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed during init", e)
        }
    }

    fun saveRound(round: RoundStatistics) {
        val entity = RoundEntity(
            roundNumber = round.roundNumber,
            date = round.date,
            drawnNumbers = round.drawnNumbers,
            winnersCount = round.winnersCount,
            maxCompletedRows = round.maxCompletedRows,
            cardStatesJson = GsonBuilder().create().toJson(round.cardStates)
        )
        roundDao.insert(entity)

        val allRounds = getAllRounds()
        updateDailySummary(round.date, allRounds)
    }

    fun getAllRounds(): List<RoundStatistics> {
        return try {
            val gson = GsonBuilder().create()
            roundDao.getAll().map { entity ->
                val cardStates: List<CardState> = try {
                    val type = object : TypeToken<List<CardState>>() {}.type
                    gson.fromJson(entity.cardStatesJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                RoundStatistics(
                    date = entity.date,
                    roundNumber = entity.roundNumber,
                    drawnNumbers = entity.drawnNumbers,
                    cardStates = cardStates,
                    winnersCount = entity.winnersCount,
                    maxCompletedRows = entity.maxCompletedRows
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rounds from Room", e)
            emptyList()
        }
    }

    private fun updateDailySummary(date: LocalDate, allRounds: List<RoundStatistics>) {
        val roundsForDate = allRounds.filter { it.date == date }
        val summary = calculateDailySummary(date, roundsForDate)
        val entity = SummaryEntity(
            date = summary.date,
            mostFrequentNumbers = summary.mostFrequentNumbers,
            leastFrequentNumbers = summary.leastFrequentNumbers,
            missingNumbers = summary.missingNumbers,
            totalRounds = summary.totalRounds,
            totalDraws = summary.totalDraws,
            averageDrawsPerRound = summary.averageDrawsPerRound
        )
        summaryDao.upsert(entity)
    }

    fun getAllSummaries(): Map<String, DailySummary> {
        return try {
            summaryDao.getAll().associate { entity ->
                entity.date.toString() to DailySummary(
                    date = entity.date,
                    mostFrequentNumbers = entity.mostFrequentNumbers,
                    leastFrequentNumbers = entity.leastFrequentNumbers,
                    missingNumbers = entity.missingNumbers,
                    totalRounds = entity.totalRounds,
                    totalDraws = entity.totalDraws,
                    averageDrawsPerRound = entity.averageDrawsPerRound
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load summaries from Room", e)
            emptyMap()
        }
    }

    fun clearAll() {
        roundDao.deleteAll()
        summaryDao.deleteAll()
    }

    /**
     * Migriert alte SharedPreferences-Daten einmalig nach Room.
     */
    private fun migrateFromSharedPreferences() {
        val prefs = appContext.getSharedPreferences("ch.lottomatchapp.app.stats", Context.MODE_PRIVATE)
        val roundsJson = prefs.getString("rounds", null) ?: return

        // Nur migrieren wenn Room noch leer ist
        if (roundDao.getAll().isNotEmpty()) return

        try {
            val gson = GsonBuilder()
                .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
                .create()
            val type = object : TypeToken<List<RoundStatistics>>() {}.type
            val oldRounds: List<RoundStatistics> = gson.fromJson(roundsJson, type) ?: return

            val cardGson = GsonBuilder().create()
            oldRounds.forEach { round ->
                roundDao.insert(
                    RoundEntity(
                        roundNumber = round.roundNumber,
                        date = round.date,
                        drawnNumbers = round.drawnNumbers,
                        winnersCount = round.winnersCount,
                        maxCompletedRows = round.maxCompletedRows,
                        cardStatesJson = cardGson.toJson(round.cardStates)
                    )
                )
            }

            // Summaries neu berechnen
            val allRounds = getAllRounds()
            allRounds.map { it.date }.distinct().forEach { date ->
                updateDailySummary(date, allRounds)
            }

            // Alte SharedPrefs löschen nach erfolgreicher Migration
            prefs.edit().clear().apply()
            Log.i(TAG, "Migrated ${oldRounds.size} rounds from SharedPreferences to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Migration from SharedPreferences failed", e)
        }
    }

    companion object {
        fun calculateDailySummary(date: LocalDate, roundsForDate: List<RoundStatistics>): DailySummary {
            val drawnNumbersForDate = roundsForDate.flatMap { it.drawnNumbers }
            val numberCounts = drawnNumbersForDate.groupingBy { it }.eachCount()
            val maxCount = numberCounts.maxOfOrNull { it.value }
            val minCount = numberCounts.minOfOrNull { it.value }

            val mostFrequent = if (maxCount != null)
                numberCounts.filter { it.value == maxCount }.keys.toList()
            else emptyList()

            val leastFrequent = if (minCount != null)
                numberCounts.filter { it.value == minCount }.keys.toList()
            else emptyList()

            val missingNumbers = ((1..90).toSet() - drawnNumbersForDate.toSet()).toList().sorted()
            val totalRounds = roundsForDate.size
            val totalDraws = drawnNumbersForDate.size
            val averageDrawsPerRound = if (totalRounds > 0) totalDraws.toDouble() / totalRounds else 0.0

            return DailySummary(
                date = date,
                mostFrequentNumbers = mostFrequent,
                leastFrequentNumbers = leastFrequent,
                missingNumbers = missingNumbers,
                totalRounds = totalRounds,
                totalDraws = totalDraws,
                averageDrawsPerRound = averageDrawsPerRound
            )
        }
    }
}
