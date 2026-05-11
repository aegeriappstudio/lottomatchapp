package ch.lottomatchapp.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R

/**
 * Kompakter Fortschritts-Indikator pro Karte: Ring mit Zahl in der Mitte.
 * Bei hasWin wird ein Check-Icon angezeigt.
 */
@Composable
fun WinProgressIndicator(
    missingForWin: Int,
    totalRequired: Int,
    hasWin: Boolean,
    modifier: Modifier = Modifier
) {
    val targetProgress = when {
        hasWin -> 1f
        totalRequired <= 0 -> 0f
        else -> (1f - missingForWin.toFloat() / totalRequired).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "winProgress"
    )

    val ringColor by animateColorAsState(
        targetValue = when {
            hasWin -> MaterialTheme.colorScheme.primary
            missingForWin <= 1 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "winProgressColor"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 3.dp,
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (hasWin) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = missingForWin.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            }
        }
        Text(
            text = pluralStringResource(R.plurals.numbers_to_win_label, missingForWin, missingForWin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
