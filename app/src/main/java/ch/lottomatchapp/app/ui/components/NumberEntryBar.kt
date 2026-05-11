package ch.lottomatchapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

@Composable
fun NumberEntryBar(
    history: List<Int>,
    lastNumber: Int?,
    remainingNumbers: Int,
    onShowHistory: () -> Unit,
    onSubmitNumber: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var input by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.enter_number_label),
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastNumber != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lastNumber.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.last_drawn),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Text(
                    stringResource(R.string.remaining_numbers, remainingNumbers),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { new ->
                    if (new.all { c -> c.isDigit() || c == ' ' || c == ',' }) input = new
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.number_entry_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = {
                    val numbers = input
                        .replace(',', ' ')
                        .split(' ')
                        .mapNotNull { it.trim().toIntOrNull() }
                    numbers.forEach { n -> onSubmitNumber(n) }
                    input = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .height(56.dp)
                    .width(120.dp)
            ) {
                Text(stringResource(R.string.mark_button))
            }
        }
    }
}