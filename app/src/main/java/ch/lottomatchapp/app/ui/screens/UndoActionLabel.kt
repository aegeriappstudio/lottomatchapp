package ch.lottomatchapp.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.UndoAction

/**
 * Lokalisierter Text für eine Undo-Aktion. Karten-Indizes sind intern
 * 0-basiert, der User sieht sie 1-basiert.
 */
@Composable
fun UndoAction.label(): String = when (this) {
    is UndoAction.NumberMarked -> stringResource(R.string.undo_number_marked, number)
    UndoAction.CardAdded -> stringResource(R.string.undo_add_card)
    is UndoAction.CardDeleted -> stringResource(R.string.undo_card_deleted, index + 1)
    is UndoAction.CellChanged -> stringResource(R.string.undo_cell_changed, index + 1)
    UndoAction.CardRenamed -> stringResource(R.string.undo_card_name_changed)
    UndoAction.WinRowsChanged -> stringResource(R.string.undo_change_win_rows)
    is UndoAction.MarksCleared -> stringResource(R.string.undo_clear_marks_for_card, index + 1)
    is UndoAction.ScanApplied -> stringResource(R.string.undo_scan_applied, index + 1)
    UndoAction.NewRound -> stringResource(R.string.undo_start_new_round)
    UndoAction.AllCleared -> stringResource(R.string.undo_wipe_all)
    UndoAction.HistoryCleared -> stringResource(R.string.undo_history_cleared)
    is UndoAction.NumberRemoved -> stringResource(R.string.undo_number_removed_from_history, number)
}
