package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.scanner.columnRanges90

/**
 * 3x9-Raster einer Karte. Ein Tipp (oder langes Drücken) auf ein Feld öffnet über
 * [onCellClick] den Zahlen-Editor. Jedes Feld ist für TalkBack als Button mit
 * Position (1-basiert), Zahl und Markierungszustand beschrieben.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LottoGrid(
    card: LottoCard,
    onCellClick: (Int) -> Unit,
) {
    val rows = card.rows
    val cols = card.cols
    val spacing = 6.dp
    val ranges = columnRanges90()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val markedState = stringResource(R.string.cell_state_marked)

    // Die 9 Spalten füllen die Kartenbreite, die Zellen können also nicht wachsen.
    // Schriftgrössen werden deshalb aus der Zellgrösse abgeleitet (dp -> sp, also ohne
    // System-Schriftskalierung), damit Zahlen nie abgeschnitten werden. Die Zahl wächst
    // mit der Systemschrift nur moderat (gedeckelt) mit, damit zwei Ziffern immer passen.
    val fontScale = density.fontScale.coerceIn(1f, 1.2f)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - spacing * (cols - 1)) / cols
        val numberFontSize = with(density) { (cellSize * 0.55f * fontScale).toSp() }
        val headerFontSize = with(density) { (cellSize * 0.36f).toSp() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (c in 0 until cols) {
                    val r = ranges[c]
                    Text(
                        text = "${r.first}-${r.last}",
                        modifier = Modifier.weight(1f),
                        fontSize = headerFontSize,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (c in 0 until cols) {
                        val index = r * cols + c
                        val cell = card.cells[index]
                        val isMarked = cell.marked
                        val hasNumber = cell.number != 0

                        val targetScale = if (isMarked) 1.06f else 1f
                        val targetAlpha = if (isMarked) 1f else 0.92f

                        val scale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = tween(durationMillis = 140),
                            label = "cellScale"
                        )

                        val alpha by animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = tween(durationMillis = 140),
                            label = "cellAlpha"
                        )

                        val cellDescription = if (hasNumber) {
                            stringResource(R.string.cell_description_number, r + 1, c + 1, cell.number)
                        } else {
                            stringResource(R.string.cell_description_empty, r + 1, c + 1)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    alpha = alpha
                                )
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    when {
                                        isMarked -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isMarked)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .semantics {
                                    contentDescription = cellDescription
                                    role = Role.Button
                                    if (isMarked) stateDescription = markedState
                                }
                                .combinedClickable(
                                    onClick = { onCellClick(index) },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCellClick(index)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!hasNumber) "" else cell.number.toString(),
                                fontSize = numberFontSize,
                                // Markierung zusätzlich zur Farbe: fett + unterstrichen,
                                // damit der Zustand in jedem Theme auch ohne Farbe erkennbar ist.
                                fontWeight = if (isMarked) FontWeight.Bold else FontWeight.SemiBold,
                                textDecoration = if (isMarked) TextDecoration.Underline else null,
                                maxLines = 1,
                                softWrap = false,
                                color = when {
                                    isMarked -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.height(spacing))
            }
        }
    }
}
