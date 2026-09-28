package ch.lottomatchapp.app.data

/**
 * Die Aktion, die zu einem [LottoUndoState] geführt hat. Wird in der UI
 * lokalisiert (siehe `UndoAction.label()`), damit im ViewModel keine
 * hartkodierten Texte liegen. Karten-Indizes sind 0-basiert; die UI zeigt
 * sie 1-basiert an.
 */
sealed class UndoAction {
    data class NumberMarked(val number: Int) : UndoAction()
    data object CardAdded : UndoAction()
    data class CardDeleted(val index: Int) : UndoAction()
    data class CellChanged(val index: Int) : UndoAction()
    data object CardRenamed : UndoAction()
    data object WinRowsChanged : UndoAction()
    data class MarksCleared(val index: Int) : UndoAction()
    data class ScanApplied(val index: Int) : UndoAction()
    data object NewRound : UndoAction()
    data object AllCleared : UndoAction()
    data object HistoryCleared : UndoAction()
    data class NumberRemoved(val number: Int) : UndoAction()
}
