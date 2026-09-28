package ch.lottomatchapp.app.data

import android.content.Context
import android.util.Log
import ch.lottomatchapp.app.persistence.UndoChainCodec
import ch.lottomatchapp.app.persistence.decodeState
import ch.lottomatchapp.app.persistence.encodeState
import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics
import ch.lottomatchapp.app.stats.StatisticsRepository
import ch.lottomatchapp.app.widget.LottomatchWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private const val TAG = "LottoDataRepository"

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

    /**
     * Eigener Single-Thread-Dispatcher für die Undo-Datei, damit schnell
     * aufeinanderfolgende Saves garantiert in Reihenfolge geschrieben werden.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val undoFileScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val undoFile = File(appContext.filesDir, UNDO_CHAIN_FILE)
    private val widget = LottomatchWidget()

    private val _appData = MutableStateFlow(loadInitialState())
    val appData: StateFlow<AppData> = _appData.asStateFlow()

    val roundsFlow: Flow<List<RoundStatistics>> = statsRepo.roundsFlow
    val summariesFlow: Flow<Map<LocalDate, DailySummary>> = statsRepo.summariesFlow

    private fun loadInitialState(): AppData {
        val encoded = prefs.getString(KEY_STATE, null)
        val persisted = encoded?.let { decodeState(it) }
        if (encoded != null && persisted == null) {
            Log.w(TAG, "Persisted game state could not be decoded, falling back to defaults")
        }

        val cards = persisted?.cards ?: listOf(LottoCard.empty(name = "Card 1"))
        val winRows = persisted?.winRows ?: 1
        val history = persisted?.history ?: emptyList()
        val lastNumber = persisted?.lastNumber

        // Undo-Kette nur übernehmen, wenn ihr oberster Zustand exakt dem
        // gespeicherten Spielzustand entspricht (Prefs und Datei werden getrennt
        // geschrieben; bei einem Prozess-Tod dazwischen lieber ohne Undo starten).
        val chain = loadUndoChain()
        val undoState = chain?.takeIf {
            persisted != null &&
                it.cards == cards && it.winRows == winRows &&
                it.history == history && it.lastNumber == lastNumber
        }
        if (chain != null && undoState == null) {
            Log.w(TAG, "Undo chain does not match persisted game state, discarding it")
        }

        return AppData(
            cards = cards,
            winRows = winRows,
            history = history,
            lastNumber = lastNumber,
            undoState = undoState
        )
    }

    private fun loadUndoChain(): LottoUndoState? {
        if (!undoFile.exists()) return null
        val decoded = runCatching { UndoChainCodec.decode(undoFile.readText()) }
            .onFailure { e -> Log.w(TAG, "Undo chain could not be read", e) }
            .getOrNull()
        if (decoded == null) {
            Log.w(TAG, "Undo chain could not be decoded, starting without undo")
        }
        return decoded
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

        undoFileScope.launch {
            // Kette ist immutable, Encoding darf deshalb hier im Hintergrund laufen.
            runCatching { undoFile.writeText(UndoChainCodec.encode(state)) }
                .onFailure { e -> Log.w(TAG, "Undo chain could not be written", e) }
        }

        ioScope.launch {
            // Widget refreshen (no-op falls keins installiert)
            runCatching { widget.updateAll(appContext) }
                .onFailure { e -> Log.w(TAG, "Widget update failed", e) }
        }
    }

    fun restoreFromUndo(previous: LottoUndoState) {
        saveUndoState(previous)
    }

    // --- Statistik-Fassade ---

    suspend fun ensureStatsMigrated() = statsRepo.ensureMigrated()

    suspend fun saveRound(round: RoundStatistics): Long = statsRepo.saveRound(round)

    suspend fun deleteRound(id: Long) = statsRepo.deleteRound(id)

    suspend fun clearStatistics() = statsRepo.clearAll()

    companion object {
        private const val PREFS_NAME = "ch.lottomatchapp.app"
        private const val KEY_STATE = "game_state"
        private const val UNDO_CHAIN_FILE = "undo_chain.txt"

        @Volatile
        private var INSTANCE: LottoDataRepository? = null

        fun getInstance(context: Context): LottoDataRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LottoDataRepository(context).also { INSTANCE = it }
            }
    }
}
