@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ch.lottomatchapp.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.annotation.StringRes
import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.lottomatchapp.app.AddCardResult
import ch.lottomatchapp.app.LottoViewModel
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.ConsentStatus
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.ui.theme.ThemeVariant
import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics
import ch.lottomatchapp.app.ui.components.DrawnNumbersTape
import ch.lottomatchapp.app.ui.components.LottoCardPanel
import ch.lottomatchapp.app.ui.components.NumberFrequencyChart
import ch.lottomatchapp.app.ui.components.NumberFrequencyHeatmap
import ch.lottomatchapp.app.ui.components.NumberPad90
import ch.lottomatchapp.app.ui.components.WinRuleBar
import ch.lottomatchapp.app.util.Analytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LottoAppScreen(
    viewModel: LottoViewModel = viewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val winRows by viewModel.winRows.collectAsState()
    val history by viewModel.history.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val undoLabel by viewModel.undoLabel.collectAsState()
    val lastNumber by viewModel.lastNumber.collectAsState()
    val allRounds by viewModel.allRounds.collectAsState()
    val allSummaries by viewModel.allSummaries.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val maxCards by viewModel.maxCards.collectAsState()
    val consentStatus by viewModel.consentStatus.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val prefs = remember {
        context.getSharedPreferences("ch.lottomatchapp.app.prefs", Context.MODE_PRIVATE)
    }

    var showHistory by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(!prefs.getBoolean("onboarding_seen", false)) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var askUndo by remember { mutableStateOf(false) }
    var showMainMenu by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_seen", false)) }
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                                        viewModel.clearMarksOnCard(originalIndex)
                                        val msg = context.getString(R.string.marks_cleared_message)
                                        val undoLbl = context.getString(R.string.undo)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = msg,
                                                actionLabel = undoLbl,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undo()
                                            }
                                        }
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.deleteCard(originalIndex)
                                        val msg = context.getString(R.string.card_deleted_message)
                                        val undoLbl = context.getString(R.string.undo)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = msg,
                                                actionLabel = undoLbl,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undo()
                                            }
                                        }
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
                    } else if (viewModel.markNumberAcrossAll(n)) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AnimatedVisibility(
                visible = entryError != null,
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
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = entryError ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = scanMessage != null,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scanMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = winMessage != null,
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = winMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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

    if (showMainMenu) {
        MainMenuSheet(
            onDismiss = { showMainMenu = false },
            onStartNewRound = { showMainMenu = false; viewModel.startNewRound() },
            onDeleteAll = { showMainMenu = false; showDeleteAllDialog = true },
            onShowHistory = { showMainMenu = false; if (history.isNotEmpty()) showHistory = true },
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
            historyEnabled = history.isNotEmpty()
        )
    }

    if (showPaywall) {
        PaywallSheet(onDismiss = { showPaywall = false })
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.reset_all_confirmation_title)) },
            text = { Text(stringResource(R.string.reset_all_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeAllCardsAndHistory()
                    viewModel.setWinRows(1)
                    showDeleteAllDialog = false
                }) {
                    Text(stringResource(R.string.reset_all_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
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
        if (!prefs.getBoolean("onboarding_seen", false)) {
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

    if (askUndo && canUndo) {
        AlertDialog(
            onDismissRequest = { askUndo = false },
            confirmButton = {
                TextButton(onClick = {
                    askUndo = false
                    viewModel.undo()
                }) { Text(stringResource(R.string.undo)) }
            },
            dismissButton = { TextButton(onClick = { askUndo = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.undo_confirmation_title)) },
            text = { Text(undoLabel) }
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

@Composable
private fun LockedHeatmapTeaser(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.premium_locked_heatmap_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.premium_locked_heatmap_body),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.premium_unlock_cta))
            }
        }
    }
}

@Composable
private fun CardSwipeBackground(direction: SwipeToDismissBoxValue) {
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Icon(
                Icons.Default.CleaningServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            SwipeToDismissBoxValue.EndToStart -> Icon(
                Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            else -> {}
        }
    }
}

@Composable
private fun MainMenuSheet(
    onDismiss: () -> Unit,
    onStartNewRound: () -> Unit,
    onDeleteAll: () -> Unit,
    onShowHistory: () -> Unit,
    onShowStatistics: () -> Unit,
    onShowAbout: () -> Unit,
    onShowPaywall: () -> Unit,
    onShowPrivacySettings: () -> Unit,
    onShowThemePicker: () -> Unit,
    isPremium: Boolean,
    historyEnabled: Boolean
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.start_new_round)) },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onStartNewRound)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_all_cards)) },
                leadingContent = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onDeleteAll)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.history)) },
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                modifier = Modifier
                    .clickable(enabled = historyEnabled, onClick = onShowHistory)
                    .then(if (!historyEnabled) Modifier.alpha(0.38f) else Modifier)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.statistics)) },
                leadingContent = { Icon(Icons.Default.Analytics, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShowStatistics)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (!isPremium) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.premium_menu_label)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable(onClick = onShowPaywall)
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_app)) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShowAbout)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_settings_label)) },
                leadingContent = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShowPrivacySettings)
            )
            if (isPremium) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme_menu_label)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable(onClick = onShowThemePicker)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryDialog(
    history: List<Int>,
    lastNumber: Int?,
    onDismiss: () -> Unit,
    onRemoveNumber: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.history_dialog_title)) },
        text = {
            Column {
                if (history.isEmpty() && lastNumber == null) {
                    Text(stringResource(R.string.no_numbers_entered_yet))
                } else {
                    if (history.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            history.forEach { n ->
                                AssistChip(onClick = { onRemoveNumber(n) }, label = { Text(n.toString()) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    lastNumber?.let { n ->
                        Text(stringResource(R.string.last_number_label, n))
                        Spacer(Modifier.height(12.dp))
                    }
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.clear_history_button))
                    }
                }
            }
        }
    )
}


private const val PRIVACY_POLICY_URL = "https://github.com/aegeriappstudio/lottomatchapp/wiki/Datenschutz-Lottomatch-App"
private const val TERMS_URL = "https://github.com/aegeriappstudio/lottomatchapp/wiki/Nutzungsbedingungen-Lottomatch-App"
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun sendFeedbackEmail(context: Context) {
    val feedbackEmail = context.getString(R.string.feedback_email_address)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_email_subject))
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.feedback_email_chooser))
    context.startActivity(chooser)
}

@Composable
private fun PrivacySettingsDialog(
    consentStatus: ConsentStatus,
    onConsentChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val granted = consentStatus == ConsentStatus.Granted
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_settings_label)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.privacy_settings_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConsentChange(!granted) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.privacy_settings_toggle),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(checked = granted, onCheckedChange = onConsentChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun LegalDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        },
        title = { Text(stringResource(R.string.legal_info_title)) },
        text = {
            Column {
                Text(stringResource(R.string.legal_info_body))
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { openUrl(context, PRIVACY_POLICY_URL); onDismiss() }) {
                    Text(stringResource(R.string.privacy_policy))
                }
                TextButton(onClick = { openUrl(context, TERMS_URL); onDismiss() }) {
                    Text(stringResource(R.string.terms_of_service))
                }
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onShowInfo: () -> Unit,
    onShowLegal: () -> Unit,
    onSendFeedback: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        },
        title = { Text(stringResource(R.string.about_app)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.legal_info_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onShowInfo) {
                    Text(stringResource(R.string.info))
                }
                TextButton(onClick = onShowLegal) {
                    Text(stringResource(R.string.legal))
                }
                TextButton(onClick = onSendFeedback) {
                    Text(stringResource(R.string.feedback))
                }
            }
        }
    )
}

@Composable
private fun RatingDialog(
    onRateNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.rating_dialog_title)) },
        text = { Text(stringResource(R.string.rating_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onRateNow) {
                Text(stringResource(R.string.rating_now))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.rating_later))
                }
                TextButton(onClick = onNever) {
                    Text(stringResource(R.string.rating_never))
                }
            }
        }
    )
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.ok))
            }
        },
        title = { Text(stringResource(R.string.info_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoBullet(R.string.info_bullet_1)
                InfoBullet(R.string.info_bullet_2)
                InfoBullet(R.string.info_bullet_3)
                InfoBullet(R.string.info_bullet_4)
                InfoBullet(R.string.info_bullet_5)
                InfoBullet(R.string.info_bullet_6)
                InfoBullet(R.string.info_bullet_7)
                InfoBullet(R.string.info_bullet_8)
            }
        }
    )
}

@Composable
private fun InfoBullet(@StringRes textRes: Int) {
    Row {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsSheet(
    onDismiss: () -> Unit,
    rounds: List<RoundStatistics>,
    summaries: List<DailySummary>,
    onDeleteAllStatistics: () -> Unit,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            // Header with title and delete button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.statistics_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(stringResource(R.string.statistics_delete_all_button))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Global overview
            if (rounds.isNotEmpty()) {
                item {
                    val globalCounts = rounds
                        .flatMap { it.drawnNumbers }
                        .groupingBy { it }
                        .eachCount()
                    val top5 = globalCounts.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .joinToString { "${it.key} (${it.value})" }

                    Text(
                        text = stringResource(R.string.statistics_global_top5, top5),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.statistics_heatmap_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isPremium) {
                        NumberFrequencyHeatmap(
                            numberCounts = globalCounts,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        )
                    } else {
                        LockedHeatmapTeaser(
                            onClick = onUpgradeClick,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                    NumberFrequencyChart(
                        numberCounts = globalCounts,
                        title = stringResource(R.string.statistics_chart_title),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }
            }

            // Daily summaries
            item {
                Text(
                    text = stringResource(R.string.statistics_daily_summaries_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (summaries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.statistics_no_daily_summaries),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(summaries.sortedByDescending { it.date }) { summary ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dateText = summary.date.format(dateFormatter)
                        Text(
                            text = stringResource(R.string.statistics_date_label, dateText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.statistics_most_frequent_numbers,
                                summary.mostFrequentNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_least_frequent_numbers,
                                summary.leastFrequentNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_missing_numbers,
                                summary.missingNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_rounds_and_draws,
                                summary.totalRounds,
                                summary.totalDraws
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // Completed rounds
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.statistics_completed_rounds_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (rounds.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.statistics_no_completed_rounds),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(rounds.sortedByDescending { it.roundNumber }) { round ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dateText = round.date.format(dateFormatter)
                        Text(
                            text = stringResource(
                                R.string.statistics_round_header,
                                round.roundNumber,
                                dateText
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_drawn_numbers_label,
                                round.drawnNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_round_winners_line,
                                round.winnersCount,
                                round.maxCompletedRows
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        round.cardStates.forEach { cardState ->
                            Text(
                                text = stringResource(R.string.history_card_summary_line, cardState.cardName, cardState.numbersToWin),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAllStatistics()
                }) {
                    Text(stringResource(R.string.statistics_delete_all_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.statistics_delete_all_confirmation_title)) },
            text = { Text(stringResource(R.string.statistics_delete_all_confirmation_message)) }
        )
    }
}
