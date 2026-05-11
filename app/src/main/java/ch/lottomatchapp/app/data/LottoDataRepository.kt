package ch.lottomatchapp.app.data

import android.content.Context
import ch.lottomatchapp.app.persistence.decodeState
import ch.lottomatchapp.app.persistence.encodeState
import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics
import ch.lottomatchapp.app.stats.StatisticsRepository
import ch.lottomatchapp.app.widget.LottomatchWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Zentrales Repository für Spielzustand (Karten, History, Undo) und Statistiken.
 */
data class AppData(
    val cards: List<LottoCard>,
    val winRows: Int,
    val history: List<Int>,
    val lastNumber: Int?,
    val undoState: LottoUndoState?
)

class LottoDataRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val statsRepo = StatisticsRepository(appContext)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val widget = LottomatchWidget()

    private val _appData = MutableStateFlow(loadInitialState())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    val roundsFlow: Flow<List<RoundStatistics>> = statsRepo.roundsFlow
    val summariesFlow: Flow<Map<LocalDate, DailySummary>> = statsRepo.summariesFlow

    private fun loadInitialState(): AppData {
        val encoded = prefs.getString(KEY_STATE, null)
        val persisted = encoded?.let { decodeState(it) }

        val cards = persisted?.cards ?: listOf(LottoCard.empty(name = "Card 1"))
        val winRows = persisted?.winRows ?: 1
        val history = persisted?.history ?: emptyList()
        val lastNumber = persisted?.lastNumber

        // Undo-Kette nach App-Neustart weglassen ist ok
        return AppData(
            cards = cards,
            winRows = winRows,
            history = history,
            lastNumber = lastNumber,
            undoState = null
        )
    }

    /**
     * Wird aus dem ViewModel bei jedem State-Update aufgerufen.
     * Persistiert den Spielzustand und aktualisiert den StateFlow.
     */
    fun saveUndoState(state: LottoUndoState) {
        val encoded = encodeState(
            winRows = state.winRows,
            cards = state.cards,
            history = state.history,
            lastNumber = state.lastNumber
        )
        prefs.edit()
            .putString(KEY_STATE, encoded)
            .apply()

        _appData.value = AppData(
            cards = state.cards,
            winRows = state.winRows,
            history = state.history,
            lastNumber = state.lastNumber,
            undoState = state
        )

        ioScope.launch {
            // Widget refreshen (no-op falls keins installiert)
            runCatching { widget.updateAll(appContext) }
        }
    }

    fun restoreFromUndo(current: LottoUndoState) {
        val previous = current.undoState ?: return
        saveUndoState(previous)
    }

    // --- Statistik-Fassade ---

    suspend fun ensureStatsMigrated() = statsRepo.ensureMigrated()

    suspend fun saveRound(round: RoundStatistics) = statsRepo.saveRound(round)

    suspend fun clearStatistics() = statsRepo.clearAll()

    companion object {
        private const val PREFS_NAME = "ch.lottomatchapp.app"
        private const val KEY_STATE = "game_state"

        @Volatile
        private var INSTANCE: LottoDataRepository? = null

        fun getInstance(context: Context): LottoDataRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LottoDataRepository(context).also { INSTANCE = it }
            }
    }
}
