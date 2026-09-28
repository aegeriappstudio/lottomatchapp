package ch.lottomatchapp.app.stats

import android.content.Context
import android.util.Log
import ch.lottomatchapp.app.data.db.AppDatabase
import ch.lottomatchapp.app.data.db.RoundEntity
import ch.lottomatchapp.app.data.db.SummaryEntity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

private const val TAG = "StatisticsRepository"

class StatisticsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val roundDao = db.roundDao()
    private val summaryDao = db.summaryDao()

    @Volatile private var migrated = false
    private val migrationMutex = Mutex()

    val roundsFlow: Flow<List<RoundStatistics>> = roundDao.observeAll().map { entities ->
        mapEntitiesToRounds(entities)
    }

    val summariesFlow: Flow<Map<LocalDate, DailySummary>> = summaryDao.observeAll().map { entities ->
        entities.associate { entity ->
            entity.date to DailySummary(
                date = entity.date,
                mostFrequentNumbers = entity.mostFrequentNumbers,
                leastFrequentNumbers = entity.leastFrequentNumbers,
                missingNumbers = entity.missingNumbers,
                totalRounds = entity.totalRounds,
                totalDraws = entity.totalDraws,
                averageDrawsPerRound = entity.averageDrawsPerRound
            )
        }
    }

    suspend fun ensureMigrated() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            try {
                migrateFromSharedPreferences()
            } catch (e: Exception) {
                Log.e(TAG, "Migration from SharedPreferences failed", e)
            }
            migrated = true
        }
    }

    /** Speichert die Runde und liefert die Room-ID (für [deleteRound] beim Undo). */
    suspend fun saveRound(round: RoundStatistics): Long {
        val entity = RoundEntity(
            roundNumber = round.roundNumber,
            date = round.date,
            drawnNumbers = round.drawnNumbers,
            winnersCount = round.winnersCount,
            maxCompletedRows = round.maxCompletedRows,
            cardStatesJson = GsonBuilder().create().toJson(round.cardStates)
        )
        val id = roundDao.insert(entity)
        val allRounds = mapEntitiesToRounds(roundDao.getAllOnce())
        updateDailySummary(round.date, allRounds)
        return id
    }

    /**
     * Entfernt eine per [saveRound] gespeicherte Runde wieder (Undo von "Neue Runde")
     * und berechnet die Tageszusammenfassung des betroffenen Datums neu.
     */
    suspend fun deleteRound(id: Long) {
        val entity = roundDao.getById(id) ?: return
        roundDao.deleteById(id)
        val allRounds = mapEntitiesToRounds(roundDao.getAllOnce())
        if (allRounds.none { it.date == entity.date }) {
            summaryDao.deleteByDate(entity.date)
        } else {
            updateDailySummary(entity.date, allRounds)
        }
    }

    suspend fun clearAll() {
        roundDao.deleteAll()
        summaryDao.deleteAll()
    }

    private suspend fun updateDailySummary(date: LocalDate, allRounds: List<RoundStatistics>) {
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

    private fun mapEntitiesToRounds(entities: List<RoundEntity>): List<RoundStatistics> {
        val gson = GsonBuilder().create()
        return entities.map { entity ->
            val cardStates: List<CardState> = try {
                val type = object : TypeToken<List<CardState>>() {}.type
                gson.fromJson<List<CardState>>(entity.cardStatesJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse cardStatesJson for round ${entity.roundNumber}, using empty list", e)
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
    }

    private suspend fun migrateFromSharedPreferences() {
        val prefs = appContext.getSharedPreferences("ch.lottomatchapp.app.stats", Context.MODE_PRIVATE)
        val roundsJson = prefs.getString("rounds", null) ?: return

        if (roundDao.getAllOnce().isNotEmpty()) return

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

        val allRounds = mapEntitiesToRounds(roundDao.getAllOnce())
        allRounds.map { it.date }.distinct().forEach { date ->
            updateDailySummary(date, allRounds)
        }

        prefs.edit().clear().apply()
        Log.i(TAG, "Migrated ${oldRounds.size} rounds from SharedPreferences to Room")
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
