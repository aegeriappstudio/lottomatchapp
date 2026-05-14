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
     * Aktive Theme-Variante. Tombola ist Premium-only — bei non-Premium
     * Nutzern fällt das automatisch auf Classic zurück, selbst wenn der
     * Repo-Wert Tombola ist (z.B. nach Refund).
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

    private val _undoState = MutableStateFlow<LottoUndoState?>(null)
    val canUndo = MutableStateFlow(false)
    val undoLabel = MutableStateFlow("")

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
                _undoState.value = data.undoState
                canUndo.value = data.undoState != null
                undoLabel.value = data.undoState?.description ?: ""
            }
        }
    }

    private fun updateState(description: String, transform: (LottoUndoState) -> LottoUndoState) {
        val baseState = LottoUndoState(
            description = undoLabel.value,
            cards = _cards.value,
            winRows = _winRows.value,
            history = _history.value,
            lastNumber = _lastNumber.value,
            undoState = _undoState.value
        )
        // Transform anwenden und neue Undo-Kette aufbauen:
        // neuer Zustand zeigt auf den vorherigen Zustand.
        val transformed = transform(baseState)
        val trimmedBase = trimUndoChain(baseState, MAX_UNDO_DEPTH - 1)
        val newState = transformed.copy(
            description = description,
            undoState = trimmedBase
        )
        undoLabel.value = description
        canUndo.value = true
        repository.saveUndoState(newState)
    }

    fun addCard(): AddCardResult {
        if (_cards.value.size >= maxCards.value) return AddCardResult.LimitReached
        val newIndex = _cards.value.size + 1
        val defaultName = getApplication<Application>().getString(R.string.card_title, newIndex)
        updateState("New card added") {
            it.copy(cards = _cards.value + LottoCard(name = defaultName))
        }
        Analytics.cardAdded(newIndex)
        return AddCardResult.Added
    }

    fun deleteCard(index: Int) {
        updateState("Card ${index + 1} deleted") {
            it.copy(cards = _cards.value.filterIndexed { i, _ -> i != index })
        }
    }

    fun updateCardFromEdit(index: Int, updatedCard: LottoCard) {
        updateState("Cell on card ${index + 1} changed") {
            it.copy(
                cards = _cards.value.mapIndexed { i, card ->
                    if (i == index) updatedCard else card
                }
            )
        }
    }

    fun updateCardName(index: Int, newName: String) {
        updateState("Card name changed") {
            it.copy(
                cards = _cards.value.mapIndexed { i, card ->
                    if (i == index) card.copy(name = newName) else card
                }
            )
        }
    }

    fun markNumberAcrossAll(number: Int): Boolean {
        if (number !in 1..90 || _history.value.contains(number) || _lastNumber.value == number) {
            return false
        }
        updateState("Number $number marked") {
            it.copy(
                cards = _cards.value.map { card -> card.markNumber(number) },
                history = _lastNumber.value?.let { n -> _history.value + n } ?: _history.value,
                lastNumber = number
            )
        }
        return true
    }

    fun setWinRows(rows: Int) {
        updateState("Win rows changed") { it.copy(winRows = rows) }
    }

    fun clearMarksOnCard(index: Int) {
        updateState("Marks on card ${index + 1} cleared") {
            it.copy(
                cards = _cards.value.mapIndexed { i, card ->
                    if (i == index) card.cleared() else card
                }
            )
        }
    }

    fun applyScanToCard(index: Int, scannedCard: LottoCard) {
        updateState("Scan applied to card ${index + 1}") {
            it.copy(
                cards = _cards.value.mapIndexed { i, c ->
                    if (i == index) c.copy(cells = scannedCard.cells) else c
                }
            )
        }
        Analytics.cardScanned()
    }

    fun startNewRound() {
        viewModelScope.launch {
            val roundNumber = allRounds.value.size + 1
            val drawnNumbers = _history.value + listOfNotNull(_lastNumber.value)

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
                repository.saveRound(round)
                Analytics.roundCompleted(drawnNumbers.size, winnersCount)
            }

            updateState("New round started") {
                it.copy(
                    cards = _cards.value.map { card -> card.cleared() },
                    history = emptyList(),
                    lastNumber = null,
                    winRows = 1
                )
            }
        }
    }


    fun wipeAllCardsAndHistory() {
        updateState("All cards cleared") {
            it.copy(
                cards = emptyList(),
                history = emptyList(),
                lastNumber = null
            )
        }
    }

    fun clearHistory(alsoUnmarkOnCards: Boolean = true) {
        updateState("History cleared") {
            val updatedCards = if (alsoUnmarkOnCards) {
                val numbersToUnmark = _history.value + listOfNotNull(_lastNumber.value)
                _cards.value.map { card -> card.unmarkNumbers(numbersToUnmark) }
            } else _cards.value
            it.copy(cards = updatedCards, history = emptyList(), lastNumber = null)
        }
    }

    fun removeNumberFromHistory(number: Int, alsoUnmarkOnCards: Boolean = true) {
        updateState("Number $number removed from history") {
            val updatedCards = if (alsoUnmarkOnCards) {
                _cards.value.map { card -> card.unmarkNumber(number) }
            } else _cards.value

            it.copy(
                cards = updatedCards,
                history = _history.value.filter { historyNumber -> historyNumber != number }
            )
        }
    }

    fun clearStatistics() {
        viewModelScope.launch {
            repository.clearStatistics()
        }
    }

    fun undo() {
        _undoState.value?.let {
            repository.restoreFromUndo(it)
        }
    }

    companion object {
        const val FREE_MAX_CARDS = 3
        const val PREMIUM_MAX_CARDS = 30
        const val MAX_UNDO_DEPTH = 30

        fun trimUndoChain(state: LottoUndoState?, depth: Int): LottoUndoState? {
            if (state == null || depth <= 0) return null
            return state.copy(undoState = trimUndoChain(state.undoState, depth - 1))
        }
    }
}
