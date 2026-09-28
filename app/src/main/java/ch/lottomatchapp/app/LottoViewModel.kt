package ch.lottomatchapp.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.ConsentRepository
import ch.lottomatchapp.app.data.ConsentStatus
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.data.LottoDataRepository
import ch.lottomatchapp.app.data.LottoUndoState
import ch.lottomatchapp.app.data.PremiumRepository
import ch.lottomatchapp.app.data.ThemeRepository
import ch.lottomatchapp.app.data.UndoAction
import ch.lottomatchapp.app.logic.GameReducer
import ch.lottomatchapp.app.ui.theme.ThemeVariant
import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics
import ch.lottomatchapp.app.util.Analytics
import ch.lottomatchapp.app.util.getStatsDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

enum class AddCardResult { Added, LimitReached }

class LottoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LottoDataRepository.getInstance(application)
    private val premiumRepo = PremiumRepository.getInstance(application)
    private val consentRepo = ConsentRepository.getInstance(application)
    private val themeRepo = ThemeRepository.getInstance(application)

    val isPremium: StateFlow<Boolean> = premiumRepo.isPremium
    val consentStatus: StateFlow<ConsentStatus> = consentRepo.status

    /**
     * Aktive Theme-Variante. Alle Varianten ausser Classic sind Premium-only —
     * bei non-Premium Nutzern fällt das automatisch auf Classic zurück, selbst
     * wenn im Repo ein Premium-Theme gespeichert ist (z.B. nach Refund).
     */
    val themeVariant: StateFlow<ThemeVariant> =
        kotlinx.coroutines.flow.combine(themeRepo.selected, premiumRepo.isPremium) { variant, premium ->
            if (premium) variant else ThemeVariant.Classic
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeVariant.Classic)

    /** Vom User gewählte Variante (ohne Premium-Gate; für den Theme-Picker). */
    val themePreference: StateFlow<ThemeVariant> = themeRepo.selected

    fun setThemeVariant(variant: ThemeVariant) {
        themeRepo.setVariant(variant)
    }

    fun setConsent(granted: Boolean) {
        consentRepo.setStatus(if (granted) ConsentStatus.Granted else ConsentStatus.Denied)
        Analytics.applyConsent(granted)
    }

    val maxCards: StateFlow<Int> = premiumRepo.isPremium
        .map { if (it) PREMIUM_MAX_CARDS else FREE_MAX_CARDS }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FREE_MAX_CARDS)

    private val _cards = MutableStateFlow<List<LottoCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _winRows = MutableStateFlow(1)
    val winRows = _winRows.asStateFlow()

    private val _history = MutableStateFlow<List<Int>>(emptyList())
    val history = _history.asStateFlow()

    private val _lastNumber = MutableStateFlow<Int?>(null)
    val lastNumber = _lastNumber.asStateFlow()

    /**
     * Aktion, die per [undo] rückgängig gemacht würde; `null` wenn nichts
     * rückgängig zu machen ist. Die Lokalisierung passiert in der UI.
     */
    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    val undoAction: StateFlow<UndoAction?> = _undoAction.asStateFlow()
    val canUndo: StateFlow<Boolean> = _undoAction
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Laufende Token-Nummer für Undo-Zustände. Nach einem Neustart wird sie aus
     * der wiederhergestellten Kette fortgesetzt, damit Tokens nie doppelt vergeben werden.
     */
    private var tokenCounter: Long = maxToken(repository.appData.value.undoState)

    val allRounds: StateFlow<List<RoundStatistics>> = repository.roundsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSummaries: StateFlow<Map<LocalDate, DailySummary>> = repository.summariesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch {
            repository.ensureStatsMigrated()
        }
        viewModelScope.launch {
            repository.appData.collect { data ->
                _cards.value = data.cards
                _winRows.value = data.winRows
                _history.value = data.history
                _lastNumber.value = data.lastNumber
                // Wurzelzustand (undoState == null) hat nichts mehr rückgängig zu machen
                _undoAction.value = data.undoState?.takeIf { it.undoState != null }?.action
            }
        }
    }

    /**
     * Oberster Zustand der Undo-Kette bzw. ein Wurzelzustand aus den aktuellen
     * Daten, falls noch keine Kette existiert. Liest direkt aus dem Repository,
     * damit mehrere Aktionen im selben Frame nicht auf veralteten Flows arbeiten.
     */
    private fun currentState(): LottoUndoState {
        val data = repository.appData.value
        return data.undoState ?: LottoUndoState(
            action = null,
            token = 0L,
            savedRoundId = null,
            cards = data.cards,
            winRows = data.winRows,
            history = data.history,
            lastNumber = data.lastNumber,
            undoState = null
        )
    }

    private fun nextToken(): Long = ++tokenCounter

    /** Persistiert den neuen Zustand und liefert dessen Token (für gezieltes Undo). */
    private fun commit(state: LottoUndoState): Long {
        repository.saveUndoState(state)
        return state.token
    }

    fun addCard(): AddCardResult {
        if (_cards.value.size >= maxCards.value) return AddCardResult.LimitReached
        val newIndex = _cards.value.size + 1
        val defaultName = getApplication<Application>().getString(R.string.card_title, newIndex)
        commit(GameReducer.addCard(currentState(), LottoCard(name = defaultName), nextToken()))
        Analytics.cardAdded(newIndex)
        return AddCardResult.Added
    }

    /** @return Token des neuen Zustands, für [undo] mit Token aus der Snackbar. */
    fun deleteCard(index: Int): Long =
        commit(GameReducer.deleteCard(currentState(), index, nextToken()))

    fun updateCardFromEdit(index: Int, updatedCard: LottoCard) {
        commit(GameReducer.replaceCard(currentState(), index, updatedCard, nextToken()))
    }

    fun updateCardName(index: Int, newName: String) {
        commit(GameReducer.renameCard(currentState(), index, newName, nextToken()))
    }

    fun markNumberAcrossAll(number: Int): Boolean {
        val next = GameReducer.markNumber(currentState(), number, nextToken()) ?: return false
        commit(next)
        return true
    }

    fun setWinRows(rows: Int) {
        commit(GameReducer.setWinRows(currentState(), rows, nextToken()))
    }

    /** @return Token des neuen Zustands, für [undo] mit Token aus der Snackbar. */
    fun clearMarksOnCard(index: Int): Long =
        commit(GameReducer.clearMarksOnCard(currentState(), index, nextToken()))

    fun applyScanToCard(index: Int, scannedCard: LottoCard) {
        commit(GameReducer.applyScanToCard(currentState(), index, scannedCard, nextToken()))
        Analytics.cardScanned()
    }

    fun startNewRound() {
        viewModelScope.launch {
            val roundNumber = allRounds.value.size + 1
            val drawnNumbers = _history.value + listOfNotNull(_lastNumber.value)

            var savedRoundId: Long? = null
            if (drawnNumbers.isNotEmpty()) {
                val cardStates = _cards.value.map { it.toCardState() }
                val winnersCount = _cards.value.count { it.completedRows().isNotEmpty() }
                val maxCompletedRows = _cards.value.maxOfOrNull { it.completedRows().size } ?: 0

                val round = RoundStatistics(
                    roundNumber = roundNumber,
                    date = getStatsDate(LocalDateTime.now()),
                    drawnNumbers = drawnNumbers,
                    cardStates = cardStates,
                    winnersCount = winnersCount,
                    maxCompletedRows = maxCompletedRows
                )
                savedRoundId = repository.saveRound(round)
                Analytics.roundCompleted(drawnNumbers.size, winnersCount)
            }

            // Die Room-ID wandert in den Undo-Zustand, damit ein Undo die
            // Statistik-Zeile wieder entfernt (sonst würde sie doppelt gezählt).
            commit(GameReducer.newRound(currentState(), nextToken(), savedRoundId))
        }
    }

    fun wipeAllCardsAndHistory() {
        commit(GameReducer.wipeAll(currentState(), nextToken()))
    }

    fun clearHistory(alsoUnmarkOnCards: Boolean = true) {
        commit(GameReducer.clearHistory(currentState(), alsoUnmarkOnCards, nextToken()))
    }

    fun removeNumberFromHistory(number: Int, alsoUnmarkOnCards: Boolean = true) {
        commit(GameReducer.removeNumberFromHistory(currentState(), number, alsoUnmarkOnCards, nextToken()))
    }

    fun clearStatistics() {
        viewModelScope.launch {
            repository.clearStatistics()
        }
    }

    /** Macht die oberste Aktion rückgängig (falls vorhanden). */
    fun undo() {
        val top = repository.appData.value.undoState ?: return
        val previous = GameReducer.undo(top) ?: return
        performUndo(top, previous)
    }

    /**
     * Macht die oberste Aktion nur rückgängig, wenn sie noch das Token
     * [expectedToken] trägt — also seither nichts anderes passiert ist.
     * @return true wenn ein Undo ausgeführt wurde.
     */
    fun undo(expectedToken: Long): Boolean {
        val top = repository.appData.value.undoState ?: return false
        val previous = GameReducer.undo(top, expectedToken) ?: return false
        performUndo(top, previous)
        return true
    }

    private fun performUndo(top: LottoUndoState, previous: LottoUndoState) {
        if (top.action == UndoAction.NewRound) {
            top.savedRoundId?.let { id ->
                viewModelScope.launch { repository.deleteRound(id) }
            }
        }
        repository.restoreFromUndo(previous)
    }

    companion object {
        const val FREE_MAX_CARDS = 3
        const val PREMIUM_MAX_CARDS = 30
        const val MAX_UNDO_DEPTH = GameReducer.MAX_UNDO_DEPTH

        private fun maxToken(state: LottoUndoState?): Long {
            var max = 0L
            var cur = state
            while (cur != null) {
                if (cur.token > max) max = cur.token
                cur = cur.undoState
            }
            return max
        }
    }
}
