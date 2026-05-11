@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.scanner.ScanLottoCardSheet
import ch.lottomatchapp.app.scanner.columnRanges90
import ch.lottomatchapp.app.ui.dialogs.CameraPermissionGate
import ch.lottomatchapp.app.ui.dialogs.NumberInputDialog

@Composable
fun LottoCardPanel(
    displayIndex: Int,
    card: LottoCard,
    onCardChange: (LottoCard) -> Unit,
    onDelete: () -> Unit,
    onClearMarks: () -> Unit,
    onScanResult: (LottoCard) -> Unit,
    onNameChange: (String) -> Unit,
    onScanMessage: (String) -> Unit,
    onWinMessage: (String) -> Unit,
    winRowsRequired: Int
) {
    val haptics = LocalHapticFeedback.current
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    val completed = card.completedRows()
    val hasWin = completed.size >= winRowsRequired

    val winBorderColor by animateColorAsState(
        targetValue = if (hasWin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 450),
        label = "winBorderColor"
    )

    val winShadowElevation by animateDpAsState(
        targetValue = if (hasWin) 14.dp else 4.dp,
        animationSpec = tween(durationMillis = 450),
        label = "winShadowElevation"
    )

    val winTonalElevation by animateDpAsState(
        targetValue = if (hasWin) 6.dp else 2.dp,
        animationSpec = tween(durationMillis = 450),
        label = "winTonalElevation"
    )

    val borderWidth: Dp by animateDpAsState(
        targetValue = if (hasWin) 3.dp else 1.dp,
        animationSpec = tween(durationMillis = 300),
        label = "winBorderWidth"
    )

    Surface(
        tonalElevation = winTonalElevation,
        shadowElevation = winShadowElevation,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = borderWidth,
            color = winBorderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {

            val context = LocalContext.current
            ActionsRow(
                title = card.name,
                onTitleClick = { showNameDialog = true },
                onScan = { showScanner = true },
                onClear = { confirmClear = true },
                onDelete = { confirmDelete = true },
                onShare = { shareCardAsText(context, card) }
            )

            Spacer(Modifier.height(8.dp))

            data class RowProgress(val required: Int, val missing: Int)

            val rowStats = (0 until card.rows).map { r ->
                val rowCells = card.cells.subList(r * card.cols, (r + 1) * card.cols)
                val required = rowCells.count { it.number != 0 }
                val hits = rowCells.count { it.number != 0 && it.marked }
                val missing = (required - hits).coerceAtLeast(0)
                RowProgress(required, missing)
            }.filter { it.required > 0 }

            val rowsAvailable = rowStats.size
            val rowsUsed = winRowsRequired.coerceAtMost(rowsAvailable)

            val missingForWin = if (rowsUsed == 0) 0 else {
                rowStats.sortedBy { it.missing }.take(rowsUsed).sumOf { it.missing }
            }

            Text(
                text = pluralStringResource(R.plurals.numbers_to_win_label, missingForWin, missingForWin),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (hasWin) {
                AssistChip(
                    onClick = {},
                    label = { Text(pluralStringResource(R.plurals.lotto_win_message, completed.size, completed.size)) },
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            LottoGrid(
                card = card,
                onLongPress = { idx ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingIndex = idx
                }
            )

            val winMessage = pluralStringResource(R.plurals.lotto_win_message, completed.size, completed.size)
            LaunchedEffect(card.cells, winRowsRequired) {
                if (hasWin) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onWinMessage(winMessage)
                }
            }

            if (showNameDialog) {
                EditNameDialog(
                    initialName = card.name,
                    onDismiss = { showNameDialog = false },
                    onConfirm = { newName ->
                        onNameChange(newName)
                        showNameDialog = false
                    }
                )
            }

            editingIndex?.let { idx ->
                val col = idx % card.cols
                val allowed = columnRanges90()[col]
                NumberInputDialog(
                    initial = card.cells[idx].number,
                    allowedRange = allowed,
                    onDismiss = { editingIndex = null },
                    onConfirm = { value ->
                        onCardChange(card.setNumber(idx, value))
                        editingIndex = null
                    }
                )
            }

            if (showScanner) {
                val scanSuccessfulMessage = stringResource(R.string.scan_successful_message, displayIndex + 1)
                CameraPermissionGate(
                    onDenied = { showScanner = false },
                    onGranted = {
                        ScanLottoCardSheet(
                            onCancel = { showScanner = false },
                            onResult = { recognizedCard ->
                                onScanResult(recognizedCard)
                                showScanner = false
                                onScanMessage(scanSuccessfulMessage)
                            }
                        )
                    }
                )
            }

            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    confirmButton = {
                        TextButton(onClick = { onClearMarks(); confirmClear = false }) {
                            Text(stringResource(R.string.clear_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    title = { Text(stringResource(R.string.clear_card_confirmation_title)) },
                    text = { Text(stringResource(R.string.clear_card_confirmation_message)) }
                )
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    confirmButton = {
                        TextButton(onClick = { onDelete(); confirmDelete = false }) {
                            Text(stringResource(R.string.delete_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    title = { Text(stringResource(R.string.delete_card_confirmation_title)) },
                    text = { Text(stringResource(R.string.delete_card_confirmation_message)) }
                )
            }
        }
    }
}

private fun shareCardAsText(context: android.content.Context, card: LottoCard) {
    val sb = StringBuilder()
    sb.appendLine(card.name)
    for (r in 0 until card.rows) {
        val row = card.cells.subList(r * card.cols, (r + 1) * card.cols)
        val nums = row.filter { it.number != 0 }
            .joinToString(" ") { cell ->
                if (cell.marked) "[${cell.number}]" else "${cell.number}"
            }
        if (nums.isNotEmpty()) sb.appendLine(nums)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
private fun ActionsRow(
    title: String,
    onTitleClick: () -> Unit,
    onScan: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clickable(onClick = onTitleClick)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(onClick = onScan, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.scan), modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(onClick = onTitleClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_card), modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CleaningServices, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_card_name_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newText -> text = newText },
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
