@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

@Composable
internal fun MainMenuSheet(
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
