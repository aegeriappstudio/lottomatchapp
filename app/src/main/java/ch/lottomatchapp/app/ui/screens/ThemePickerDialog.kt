package ch.lottomatchapp.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.ui.theme.ThemeVariant
import ch.lottomatchapp.app.ui.theme.colorScheme

/** Label- und Beschreibungs-Ressource je Variante; exhaustiv, damit neue Varianten nicht vergessen gehen. */
private fun ThemeVariant.labelRes(): Int = when (this) {
    ThemeVariant.Classic  -> R.string.theme_classic_label
    ThemeVariant.Tombola  -> R.string.theme_tombola_label
    ThemeVariant.Casino   -> R.string.theme_casino_label
    ThemeVariant.Sunset   -> R.string.theme_sunset_label
    ThemeVariant.Midnight -> R.string.theme_midnight_label
    ThemeVariant.Forest   -> R.string.theme_forest_label
}

private fun ThemeVariant.descriptionRes(): Int = when (this) {
    ThemeVariant.Classic  -> R.string.theme_classic_description
    ThemeVariant.Tombola  -> R.string.theme_tombola_description
    ThemeVariant.Casino   -> R.string.theme_casino_description
    ThemeVariant.Sunset   -> R.string.theme_sunset_description
    ThemeVariant.Midnight -> R.string.theme_midnight_description
    ThemeVariant.Forest   -> R.string.theme_forest_description
}

@Composable
fun ThemePickerDialog(
    currentVariant: ThemeVariant,
    onSelect: (ThemeVariant) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_picker_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val dark = isSystemInDarkTheme()
                ThemeVariant.entries.forEach { variant ->
                    val scheme = variant.colorScheme(dark)
                    ThemeOption(
                        label = stringResource(variant.labelRes()),
                        description = stringResource(variant.descriptionRes()),
                        swatchColor = scheme.primary,
                        swatchRingColor = scheme.secondary,
                        selected = currentVariant == variant,
                        onClick = { onSelect(variant) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    swatchColor: Color,
    swatchRingColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Surface(
            shape = CircleShape,
            color = swatchColor,
            border = BorderStroke(3.dp, swatchRingColor),
            modifier = Modifier.size(28.dp)
        ) {}
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
