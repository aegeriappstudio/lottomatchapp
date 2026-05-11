package ch.lottomatchapp.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.lottomatchapp.app.R

@Composable
fun WinRulePicker(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            "🏆",
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 2.dp)
        )
        listOf(1, 2, 3).forEach { need ->
            FilterChip(
                selected = value == need,
                onClick = { onChange(need) },
                label = { Text("${need}R", fontSize = 11.sp) },
                modifier = Modifier.padding(horizontal = 1.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
