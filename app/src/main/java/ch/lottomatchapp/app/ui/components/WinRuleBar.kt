@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

/**
 * Sub-Toolbar direkt unter der TopAppBar: zeigt mit klarem Label,
 * wie viele Reihen für einen Gewinn nötig sind. Ersetzt den alten
 * kryptischen "1R/2R/3R" Picker in den AppBar-Actions.
 */
@Composable
fun WinRuleBar(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(R.string.win_rule_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        SingleChoiceSegmentedButtonRow {
            listOf(1, 2, 3).forEachIndexed { idx, need ->
                SegmentedButton(
                    selected = value == need,
                    onClick = { onChange(need) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                    label = { Text("$need") }
                )
            }
        }
    }
}
