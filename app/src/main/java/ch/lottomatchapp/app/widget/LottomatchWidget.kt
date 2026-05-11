package ch.lottomatchapp.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ch.lottomatchapp.app.MainActivity
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.persistence.decodeState

internal data class WidgetGameState(
    val bestMissing: Int,   // -1 = no cards at all
    val lastNumber: Int?
)

class LottomatchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetGameState) {
    val context = LocalContext.current
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .padding(14.dp)
            .clickable(actionStartActivity(launchIntent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )
        Spacer(GlanceModifier.height(6.dp))

        when {
            state.bestMissing < 0 -> {
                Text(
                    text = context.getString(R.string.widget_no_cards),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 13.sp
                    )
                )
            }
            state.bestMissing == 0 -> {
                Text(
                    text = context.getString(R.string.widget_lotto_message),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                )
            }
            else -> {
                Text(
                    text = state.bestMissing.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp
                    )
                )
                Text(
                    text = context.getString(R.string.widget_to_win_label),
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                )
            }
        }

        if (state.lastNumber != null) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = context.getString(R.string.widget_last_drawn, state.lastNumber),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 11.sp
                )
            )
        }
    }
}

internal fun loadWidgetState(context: Context): WidgetGameState {
    val prefs = context.getSharedPreferences("ch.lottomatchapp.app", Context.MODE_PRIVATE)
    val encoded = prefs.getString("game_state", null) ?: return WidgetGameState(-1, null)
    val state = decodeState(encoded) ?: return WidgetGameState(-1, null)

    val cards = state.cards
    if (cards.isEmpty()) return WidgetGameState(-1, state.lastNumber)

    val winRows = state.winRows
    val bestMissing = cards.minOfOrNull { card ->
        val rowStats = (0 until card.rows).map { r ->
            val rowCells = card.cells.subList(r * card.cols, (r + 1) * card.cols)
            val required = rowCells.count { it.number != 0 }
            val hits = rowCells.count { it.number != 0 && it.marked }
            required to (required - hits).coerceAtLeast(0)
        }.filter { it.first > 0 }

        if (rowStats.isEmpty()) {
            Int.MAX_VALUE
        } else {
            val rowsUsed = winRows.coerceAtMost(rowStats.size)
            rowStats.sortedBy { it.second }.take(rowsUsed).sumOf { it.second }
        }
    }

    return WidgetGameState(
        bestMissing = if (bestMissing == null || bestMissing == Int.MAX_VALUE) -1 else bestMissing,
        lastNumber = state.lastNumber
    )
}

class LottomatchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LottomatchWidget()
}
