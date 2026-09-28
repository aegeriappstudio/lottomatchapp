@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.stats.DailySummary
import ch.lottomatchapp.app.stats.RoundStatistics
import ch.lottomatchapp.app.ui.components.NumberFrequencyChart
import ch.lottomatchapp.app.ui.components.NumberFrequencyHeatmap
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun StatisticsSheet(
    onDismiss: () -> Unit,
    rounds: List<RoundStatistics>,
    summaries: List<DailySummary>,
    onDeleteAllStatistics: () -> Unit,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            // Header with title and delete button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.statistics_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(stringResource(R.string.statistics_delete_all_button))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Global overview
            if (rounds.isNotEmpty()) {
                item {
                    val globalCounts = rounds
                        .flatMap { it.drawnNumbers }
                        .groupingBy { it }
                        .eachCount()
                    val top5 = globalCounts.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .joinToString { "${it.key} (${it.value})" }

                    Text(
                        text = stringResource(R.string.statistics_global_top5, top5),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.statistics_heatmap_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isPremium) {
                        NumberFrequencyHeatmap(
                            numberCounts = globalCounts,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        )
                    } else {
                        LockedHeatmapTeaser(
                            onClick = onUpgradeClick,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                    NumberFrequencyChart(
                        numberCounts = globalCounts,
                        title = stringResource(R.string.statistics_chart_title),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }
            }

            // Daily summaries
            item {
                Text(
                    text = stringResource(R.string.statistics_daily_summaries_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (summaries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.statistics_no_daily_summaries),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(summaries.sortedByDescending { it.date }) { summary ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dateText = summary.date.format(dateFormatter)
                        Text(
                            text = stringResource(R.string.statistics_date_label, dateText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.statistics_most_frequent_numbers,
                                summary.mostFrequentNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_least_frequent_numbers,
                                summary.leastFrequentNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_missing_numbers,
                                summary.missingNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_rounds_and_draws,
                                summary.totalRounds,
                                summary.totalDraws
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // Completed rounds
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.statistics_completed_rounds_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            if (rounds.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.statistics_no_completed_rounds),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(rounds.sortedByDescending { it.roundNumber }) { round ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val dateText = round.date.format(dateFormatter)
                        Text(
                            text = stringResource(
                                R.string.statistics_round_header,
                                round.roundNumber,
                                dateText
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_drawn_numbers_label,
                                round.drawnNumbers.joinToString()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.statistics_round_winners_line,
                                round.winnersCount,
                                round.maxCompletedRows
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        round.cardStates.forEach { cardState ->
                            Text(
                                text = stringResource(R.string.history_card_summary_line, cardState.cardName, cardState.numbersToWin),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteAllStatistics()
                }) {
                    Text(stringResource(R.string.statistics_delete_all_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.statistics_delete_all_confirmation_title)) },
            text = { Text(stringResource(R.string.statistics_delete_all_confirmation_message)) }
        )
    }
}
