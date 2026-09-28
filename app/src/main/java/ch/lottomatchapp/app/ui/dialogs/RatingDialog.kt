package ch.lottomatchapp.app.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.lottomatchapp.app.R

@Composable
internal fun RatingDialog(
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
