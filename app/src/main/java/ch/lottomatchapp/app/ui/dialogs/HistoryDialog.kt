@file:OptIn(ExperimentalLayoutApi::class)

package ch.lottomatchapp.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

/**
 * Zahlenverlauf. Jede Zahl (inkl. der zuletzt gezogenen, optisch hervorgehoben)
 * lässt sich per Tap auf den Chip wieder entfernen.
 */
@Composable
internal fun HistoryDialog(
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        history.forEach { n ->
                            AssistChip(onClick = { onRemoveNumber(n) }, label = { Text(n.toString()) })
                        }
                        lastNumber?.let { n ->
                            AssistChip(
                                onClick = { onRemoveNumber(n) },
                                label = { Text(n.toString(), fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
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
