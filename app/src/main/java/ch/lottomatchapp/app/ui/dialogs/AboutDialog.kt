package ch.lottomatchapp.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

@Composable
internal fun AboutDialog(
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
