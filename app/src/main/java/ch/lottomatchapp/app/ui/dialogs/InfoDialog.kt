package ch.lottomatchapp.app.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
internal fun InfoDialog(onDismiss: () -> Unit) {
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
