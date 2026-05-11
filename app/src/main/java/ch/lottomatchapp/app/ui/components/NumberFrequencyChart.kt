package ch.lottomatchapp.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberFrequencyChart(
    numberCounts: Map<Int, Int>,
    title: String,
    modifier: Modifier = Modifier,
    topN: Int = 10
) {
    if (numberCounts.isEmpty()) return

    val top = numberCounts.entries
        .sortedByDescending { it.value }
        .take(topN)
    val maxCount = top.maxOf { it.value }

    val barColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontSize = 12.sp, color = textColor)
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((top.size * 28 + 4).dp)
        ) {
            val labelWidth = 40.dp.toPx()
            val countWidth = 30.dp.toPx()
            val barAreaWidth = size.width - labelWidth - countWidth - 8.dp.toPx()
            val barHeight = 20.dp.toPx()
            val rowHeight = 28.dp.toPx()

            top.forEachIndexed { index, (number, count) ->
                val y = index * rowHeight

                // Number label
                val labelResult = textMeasurer.measure("$number", labelStyle)
                drawText(
                    labelResult,
                    topLeft = Offset(labelWidth - labelResult.size.width - 4.dp.toPx(), y + (barHeight - labelResult.size.height) / 2)
                )

                // Bar
                val barWidth = if (maxCount > 0) (count.toFloat() / maxCount) * barAreaWidth else 0f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(labelWidth, y),
                    size = Size(barWidth.coerceAtLeast(4.dp.toPx()), barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                // Count label
                val countResult = textMeasurer.measure("$count", labelStyle)
                drawText(
                    countResult,
                    topLeft = Offset(labelWidth + barWidth + 4.dp.toPx(), y + (barHeight - countResult.size.height) / 2)
                )
            }
        }
    }
}
