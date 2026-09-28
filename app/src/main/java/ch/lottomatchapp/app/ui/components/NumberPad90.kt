package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

/**
 * 1–90 Zahl-Tastatur. Bereits gezogene Zahlen sind ausgegraut und nicht tappbar.
 * Das Grid lässt sich über den Header ein-/ausklappen, damit der Karten-Bereich
 * sichtbar bleibt wenn nicht aktiv markiert wird.
 */
@Composable
fun NumberPad90(
    drawnNumbers: Set<Int>,
    onPickNumber: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val density = LocalDensity.current
    val cellSpacing = 4.dp
    // Wie im Karten-Raster: Schriftgrösse aus der Zellgrösse ableiten, moderat und
    // gedeckelt mit der Systemschrift skalieren, damit zwei Ziffern nie abgeschnitten werden.
    val fontScale = density.fontScale.coerceIn(1f, 1.2f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tap_number_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.remaining_numbers, 90 - drawnNumbers.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(
                        if (expanded) R.string.numberpad_collapse else R.string.numberpad_expand
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                val cellSize = (maxWidth - cellSpacing * 9) / 10
                val numberFontSize = with(density) { (cellSize * 0.45f * fontScale).toSp() }

                Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    for (rowIdx in 0 until 9) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                        ) {
                            for (colIdx in 0 until 10) {
                                val number = rowIdx * 10 + colIdx + 1
                                val drawn = number in drawnNumbers
                                NumberCell(
                                    number = number,
                                    drawn = drawn,
                                    fontSize = numberFontSize,
                                    onClick = { onPickNumber(number) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberCell(
    number: Int,
    drawn: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val numberDescription = stringResource(R.string.numberpad_number_description, number)
    val drawnState = stringResource(R.string.numberpad_state_drawn)

    val containerColor by animateColorAsState(
        targetValue = if (drawn) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                      else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 220),
        label = "cellColor"
    )
    val contentColor = if (drawn) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = numberDescription
                if (drawn) stateDescription = drawnState
            }
            .clickable(enabled = !drawn, role = Role.Button) {
                // Einzige Haptik beim Zahlen-Tipp (nicht zusätzlich im Screen).
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                textDecoration = if (drawn) TextDecoration.LineThrough else null
            )
        }
    }
}
