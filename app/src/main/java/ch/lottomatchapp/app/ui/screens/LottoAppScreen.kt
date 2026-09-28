@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.lottomatchapp.app.AddCardResult
import ch.lottomatchapp.app.LottoViewModel
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.ConsentStatus
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.ui.components.DrawnNumbersTape
import ch.lottomatchapp.app.ui.components.LottoCardPanel
import ch.lottomatchapp.app.ui.components.NumberPad90
import ch.lottomatchapp.app.ui.components.WinRuleBar
import ch.lottomatchapp.app.ui.dialogs.AboutDialog
import ch.lottomatchapp.app.ui.dialogs.DeleteAllConfirmationDialog
import ch.lottomatchapp.app.ui.dialogs.HistoryDialog
import ch.lottomatchapp.app.ui.dialogs.InfoDialog
import ch.lottomatchapp.app.ui.dialogs.LegalDialog
import ch.lottomatchapp.app.ui.dialogs.NewRoundConfirmationDialog
import ch.lottomatchapp.app.ui.dialogs.PrivacySettingsDialog
import ch.lottomatchapp.app.ui.dialogs.RatingDialog
import ch.lottomatchapp.app.ui.dialogs.UndoConfirmationDialog
import ch.lottomatchapp.app.util.Analytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun LottoAppScreen(
    viewModel: LottoViewModel = viewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val winRows by viewModel.winRows.collectAsState()
    val history by viewModel.history.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val undoAction by viewModel.undoAction.collectAsState()
    val lastNumber by viewModel.lastNumber.collectAsState()
    val allRounds by viewModel.allRounds.collectAsState()
    val allSummaries by viewModel.allSummaries.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val maxCards by viewModel.maxCards.collectAsState()
    val consentStatus by viewModel.consentStatus.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val prefs = remember {
        context.getSharedPreferences("ch.lottomatchapp.app.prefs", Context.MODE_PRIVATE)
    }

    // Dialog-/Sheet-Flags überleben Rotation und Prozess-Tod (rememberSaveable).
    // Onboarding-Status wird genau einmal aus den Prefs gelesen, nicht bei jeder Recomposition.
    var onboardingDone by rememberSaveable { mutableStateOf(prefs.getBoolean("onboarding_seen", false)) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(!onboardingDone) }
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    var showRatingDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showNewRoundDialog by rememberSaveable { mutableStateOf(false) }
    var showStatistics by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var askUndo by rememberSaveable { mutableStateOf(false) }
    var showMainMenu by rememberSaveable { mutableStateOf(false) }
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var showPrivacySettings by rememberSaveable { mutableStateOf(false) }
    var showThemePicker by rememberSaveable { mutableStateOf(false) }
    // Kurzlebige Hinweis-Banner (3 s, an rememberCoroutineScope gebunden): bewusst nur remember.
    var entryError by remember { mutableStateOf<String?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var winMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val atLimit = cards.size >= maxCards

    
    val sortedCards = remember(cards, winRows, history, lastNumber) {
        cards.mapIndexed { index, card ->
            index to card
        }.sortedWith(
            compareBy<Pair<Int, LottoCard>> { (_, card) ->
                // Primary: cards with the fewest unmarked numbers (numbers still missing) come first
                card.cells.count { it.number != 0 && !it.marked }
            }.thenByDescending { (_, card) ->
                // Among cards with the same missing count, prefer those that already satisfy the win rule
                card.completedRows().size >= winRows
            }.thenByDescending { (_, card) ->
                // Then prefer cards with more marked numbers
                card.cells.count { it.number != 0 && it.marked }
            }
        )
    }

    // Automatically scroll to the best card when there is a win ("Lotto")
    val winningIndex = remember(sortedCards, winRows) {
        sortedCards.indexOfFirst { (_, card) -> card.completedRows().size >= winRows }
    }
    // Track total completed rows across all cards to trigger confetti on each new row completion
    val totalCompletedRows = remember(sortedCards, winRows) {
        sortedCards.sumOf { (_, card) ->
            val completed = card.completedRows().size
            if (completed >= winRows) completed else 0
        }
    }
    var konfettiTrigger by remember { mutableIntStateOf(0) }
    // Mit dem aktuellen Wert initialisieren: Konfetti nur bei neu abgeschlossenen Reihen,
    // nicht beim App-Start / bei Rotation mit bereits gewinnenden Karten.
    var lastTotalCompletedRows by remember { mutableIntStateOf(totalCompletedRows) }

    LaunchedEffect(winningIndex) {
        if (winningIndex >= 0) {
            listState.animateScrollToItem(winningIndex)
        }
    }

    LaunchedEffect(totalCompletedRows) {
        if (totalCompletedRows > lastTotalCompletedRows && totalCompletedRows > 0) {
            konfettiTrigger++
        }
        lastTotalCompletedRows = totalCompletedRows
    }

    LaunchedEffect(Unit) {
        val launches = prefs.getInt("launch_count", 0) + 1
        prefs.edit().putInt("launch_count", launches).apply()
        val alreadyAsked = prefs.getBoolean("rating_asked", false)
        if (!alreadyAsked && launches >= 5) {
            showRatingDialog = true
        }
    }

    /**
     * Snackbar mit Undo, das nur die Aktion mit [token] rückgängig macht. Hat der
     * User inzwischen etwas anderes gemacht (z.B. eine Zahl getippt), passiert nichts.
     */
    fun showUndoSnackbar(message: String, token: Long) {
        val undoLbl = context.getString(R.string.undo)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLbl,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo(expectedToken = token)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (canUndo) {
                        IconButton(onClick = { askUndo = true }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
                        }
                    }
                    // Free users always see the +; tapping at the free limit
                    // routes them to the paywall. Premium users at 30 hide it.
                    if (!atLimit || !isPremium) {
                        IconButton(onClick = {
                            when (viewModel.addCard()) {
                                AddCardResult.Added -> {}
                                AddCardResult.LimitReached -> {
                                    Analytics.paywallShown("card_limit")
                                    showPaywall = true
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_card))
                        }
                    }
                    IconButton(onClick = { showMainMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
        // Tablets / breite Fenster (>= 600dp): Inhalt auf 640dp begrenzen und zentrieren,
        // damit sich die Karten nicht über die ganze Breite ziehen. Phones bleiben unverändert.
        val isWide = maxWidth >= 600.dp
        val contentWidth = if (isWide) Modifier.widthIn(max = 640.dp).fillMaxWidth() else Modifier.fillMaxWidth()
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .then(contentWidth)
                .align(Alignment.TopCenter)
                .imePadding()
                .navigationBarsPadding()
        ) {
            WinRuleBar(value = winRows, onChange = { viewModel.setWinRows(it) })
            DrawnNumbersTape(history = history, lastNumber = lastNumber)
            if (sortedCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_cards_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_cards_body),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        FilledTonalButton(onClick = { viewModel.addCard() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.no_cards_cta))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        count = sortedCards.size,
                        key = { sortedCards[it].first }
                    ) { sortedIndex ->
                        val (originalIndex, card) = sortedCards[sortedIndex]
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        val token = viewModel.clearMarksOnCard(originalIndex)
                                        showUndoSnackbar(context.getString(R.string.marks_cleared_message), token)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        val token = viewModel.deleteCard(originalIndex)
                                        showUndoSnackbar(context.getString(R.string.card_deleted_message), token)
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { CardSwipeBackground(dismissState.dismissDirection) }
                        ) {
                            LottoCardPanel(
                                displayIndex = sortedIndex,
                                card = card,
                                onCardChange = { updated -> viewModel.updateCardFromEdit(originalIndex, updated) },
                                onDelete = { viewModel.deleteCard(originalIndex) },
                                onClearMarks = { viewModel.clearMarksOnCard(originalIndex) },
                                onScanResult = { scanned -> viewModel.applyScanToCard(originalIndex, scanned) },
                                onNameChange = { newName -> viewModel.updateCardName(originalIndex, newName) },
                                onScanMessage = { msg ->
                                    scanMessage = msg
                                    scope.launch {
                                        delay(3000)
                                        if (scanMessage == msg) scanMessage = null
                                    }
                                },
                                onWinMessage = { msg ->
                                    winMessage = msg
                                    scope.launch {
                                        delay(3000)
                                        if (winMessage == msg) winMessage = null
                                    }
                                },
                                winRowsRequired = winRows
                            )
                        }
                    }
                }
            }

            val drawnNumbers = remember(history, lastNumber) {
                (history + listOfNotNull(lastNumber)).toSet()
            }

            NumberPad90(
                drawnNumbers = drawnNumbers,
                onPickNumber = { n ->
                    val hasAnyNumbers = cards.any { it.countNumbers() > 0 }
                    if (!hasAnyNumbers) {
                        val msg = context.getString(R.string.scan_or_enter_numbers_prompt)
                        entryError = msg
                        scope.launch {
                            delay(3000)
                            if (entryError == msg) entryError = null
                        }
                    } else {
                        // Haptik kommt bereits aus NumberPad90 (nah an der Geste), hier nicht doppelt.
                        viewModel.markNumberAcrossAll(n)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            MessageBanner(
                message = entryError,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
            MessageBanner(
                message = scanMessage,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MessageBanner(
                message = winMessage,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (konfettiTrigger > 0) {
            key(konfettiTrigger) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = listOf(
                        Party(
                            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(80),
                            position = Position.Relative(0.5, 0.0)
                        )
                    ),
                    updateListener = null
                )
            }
        }
        }
    }

    val hasDrawnNumbers = history.isNotEmpty() || lastNumber != null

    if (showMainMenu) {
        MainMenuSheet(
            onDismiss = { showMainMenu = false },
            onStartNewRound = {
                showMainMenu = false
                // Ohne gezogene Zahlen gibt es nichts zu verlieren: direkt starten.
                if (hasDrawnNumbers) showNewRoundDialog = true else viewModel.startNewRound()
            },
            onDeleteAll = { showMainMenu = false; showDeleteAllDialog = true },
            onShowHistory = { showMainMenu = false; if (hasDrawnNumbers) showHistory = true },
            onShowStatistics = { showMainMenu = false; showStatistics = true },
            onShowAbout = { showMainMenu = false; showAboutDialog = true },
            onShowPaywall = {
                showMainMenu = false
                Analytics.paywallShown("menu")
                showPaywall = true
            },
            onShowPrivacySettings = {
                showMainMenu = false
                showPrivacySettings = true
            },
            onShowThemePicker = {
                showMainMenu = false
                showThemePicker = true
            },
            isPremium = isPremium,
            historyEnabled = hasDrawnNumbers
        )
    }

    if (showPaywall) {
        PaywallSheet(onDismiss = { showPaywall = false })
    }

    if (showDeleteAllDialog) {
        DeleteAllConfirmationDialog(
            onConfirm = {
                viewModel.wipeAllCardsAndHistory()
                viewModel.setWinRows(1)
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    if (showNewRoundDialog) {
        NewRoundConfirmationDialog(
            onConfirm = {
                showNewRoundDialog = false
                viewModel.startNewRound()
            },
            onDismiss = { showNewRoundDialog = false }
        )
    }

    if (showStatistics) {
        StatisticsSheet(
            onDismiss = { showStatistics = false },
            rounds = allRounds,
            summaries = allSummaries.values.toList(),
            onDeleteAllStatistics = { viewModel.clearStatistics() },
            isPremium = isPremium,
            onUpgradeClick = {
                showStatistics = false
                Analytics.paywallShown("stats_heatmap")
                showPaywall = true
            }
        )
    }

    if (showInfoDialog) {
        if (!onboardingDone) {
            OnboardingSheet(onDismiss = {
                showInfoDialog = false
                onboardingDone = true
                prefs.edit().putBoolean("onboarding_seen", true).apply()
            })
        } else {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
    }

    // Consent-Dialog erst nach dem Onboarding zeigen, und nur wenn noch nicht
    // entschieden wurde. Swipe down / Back-Press wird als "Decline" behandelt.
    if (onboardingDone && consentStatus == ConsentStatus.Undefined && !showInfoDialog) {
        ConsentSheet(
            onAccept = { viewModel.setConsent(true) },
            onDeny = { viewModel.setConsent(false) },
            onShowPrivacyPolicy = { openUrl(context, PRIVACY_POLICY_URL) }
        )
    }

    if (showPrivacySettings) {
        PrivacySettingsDialog(
            consentStatus = consentStatus,
            onConsentChange = { viewModel.setConsent(it) },
            onDismiss = { showPrivacySettings = false }
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentVariant = themePreference,
            onSelect = { viewModel.setThemeVariant(it) },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showLegalDialog) {
        LegalDialog(onDismiss = { showLegalDialog = false })
    }

    if (showRatingDialog) {
        RatingDialog(
            onRateNow = {
                val url = "https://play.google.com/store/apps/details?id=" + context.packageName
                openUrl(context, url)
                prefs.edit().putBoolean("rating_asked", true).apply()
                showRatingDialog = false
            },
            onLater = {
                showRatingDialog = false
            },
            onNever = {
                prefs.edit().putBoolean("rating_asked", true).apply()
                showRatingDialog = false
            }
        )
    }

    val currentUndoAction = undoAction
    if (askUndo && canUndo && currentUndoAction != null) {
        UndoConfirmationDialog(
            actionLabel = currentUndoAction.label(),
            onConfirm = {
                askUndo = false
                viewModel.undo()
            },
            onDismiss = { askUndo = false }
        )
    }

    if (showHistory) {
        HistoryDialog(
            history = history,
            lastNumber = lastNumber,
            onDismiss = { showHistory = false },
            onRemoveNumber = { n -> viewModel.removeNumberFromHistory(n, alsoUnmarkOnCards = true) },
            onClearHistory = { viewModel.clearHistory(alsoUnmarkOnCards = false) }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onShowInfo = { showAboutDialog = false; showInfoDialog = true },
            onShowLegal = { showAboutDialog = false; showLegalDialog = true },
            onSendFeedback = { showAboutDialog = false; sendFeedbackEmail(context) }
        )
    }

}

/** Kurzlebiges Hinweis-Banner unter dem Zahlenfeld (Fehler, Scan-Hinweis, Gewinn). */
@Composable
private fun MessageBanner(
    message: String?,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
