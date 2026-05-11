package ch.lottomatchapp.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ch.lottomatchapp.app.R

@Composable
fun NumberInputDialog(
    initial: Int,
    allowedRange: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(if (initial == 0) "" else initial.toString()) }
    val inRange = text.toIntOrNull()?.let { it in allowedRange } ?: text.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = inRange,
                onClick = {
                    val value = text.toIntOrNull() ?: 0
                    onConfirm(value)
                }
            ) { Text(stringResource(id = R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } },
        title = { Text(stringResource(R.string.number_input_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() } || newText.isEmpty()) text = newText
                    },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.number_entry_placeholder, allowedRange.first)) },
                    supportingText = {
                        Text(stringResource(R.string.number_input_dialog_supporting_text, allowedRange.first, allowedRange.last))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val value = text.toIntOrNull() ?: 0
                            if (value in allowedRange || text.isEmpty()) {
                                onConfirm(value)
                            }
                        }
                    )
                )
            }
        }
    )
}
