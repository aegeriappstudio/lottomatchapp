package ch.lottomatchapp.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.ui.screens.PRIVACY_POLICY_URL
import ch.lottomatchapp.app.ui.screens.TERMS_URL
import ch.lottomatchapp.app.ui.screens.openUrl

@Composable
internal fun LegalDialog(onDismiss: () -> Unit) {
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
