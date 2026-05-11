package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.lottomatchapp.app.data.LottoCard
import ch.lottomatchapp.app.scanner.columnRanges90

@Composable
fun LottoGrid(
    card: LottoCard,
    onLongPress: (Int) -> Unit,
) {
    val rows = card.rows
    val cols = card.cols
    val spacing = 6.dp
    val ranges = columnRanges90()
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
                    fontSize = 12.sp,
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
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { onLongPress(index) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!hasNumber) "" else cell.number.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
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
