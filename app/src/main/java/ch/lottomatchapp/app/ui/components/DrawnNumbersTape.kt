package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Horizontaler Stream der zuletzt gezogenen Zahlen.
 * Die jüngste Zahl wird hervorgehoben.
 */
@Composable
fun DrawnNumbersTape(
    history: List<Int>,
    lastNumber: Int?,
    modifier: Modifier = Modifier,
    maxItems: Int = 20
) {
    val numbers = remember(history, lastNumber) {
        (history + listOfNotNull(lastNumber)).takeLast(maxItems)
    }
    if (numbers.isEmpty()) return

    val listState = rememberLazyListState()
    LaunchedEffect(numbers.size, numbers.lastOrNull()) {
        if (numbers.isNotEmpty()) listState.animateScrollToItem(numbers.size - 1)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        items(numbers.size, key = { idx -> "${idx}-${numbers[idx]}" }) { idx ->
            val n = numbers[idx]
            val isLast = idx == numbers.size - 1
            DrawnNumberChip(number = n, highlighted = isLast)
        }
    }
}

@Composable
private fun DrawnNumberChip(number: Int, highlighted: Boolean) {
    val container by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "chipContainer"
    )
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        shadowElevation = if (highlighted) 4.dp else 0.dp,
        modifier = Modifier.size(width = 48.dp, height = 40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }
}
