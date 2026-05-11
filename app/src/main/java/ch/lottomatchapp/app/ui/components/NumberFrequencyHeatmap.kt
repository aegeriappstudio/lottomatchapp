package ch.lottomatchapp.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Heatmap-Visualisierung aller 90 Zahlen. Die Hintergrund-Farbintensität pro Zelle
 * skaliert mit der Häufigkeit der jeweiligen Zahl. Hilft auf einen Blick zu sehen,
 * welche Zahlen häufig bzw. selten gezogen wurden.
 */
@Composable
fun NumberFrequencyHeatmap(
    numberCounts: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val maxCount = numberCounts.values.maxOrNull() ?: 0
    val highlightColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until 9) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 10) {
                    val number = row * 10 + col + 1
                    val count = numberCounts[number] ?: 0
                    val intensity = if (maxCount == 0) 0f
                                    else (count.toFloat() / maxCount).coerceIn(0f, 1f)

                    val cellColor = lerp(baseColor.copy(alpha = 0.35f), highlightColor, intensity)
                    val textColor = if (intensity > 0.55f) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = cellColor,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = number.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
