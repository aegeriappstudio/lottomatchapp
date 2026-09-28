package ch.lottomatchapp.app.persistence

import ch.lottomatchapp.app.data.LottoUndoState
import ch.lottomatchapp.app.data.UndoAction

/**
 * Serialisiert die komplette Undo-Kette (neuester Zustand zuerst) in einen
 * String, damit sie einen Prozess-Tod überlebt. Pro Zustand ein Record:
 *
 *     actionCode FS actionArg FS token FS savedRoundId FS <encodeState-Payload>
 *
 * Records sind mit [RECORD_SEPARATOR] getrennt. Beide Trennzeichen kommen im
 * Payload nie vor ([escapeName] escaped sie in Kartennamen; alle anderen
 * Bestandteile sind Zahlen bzw. feste Zeichen). Reine Kotlin-Logik ohne
 * Android-Abhängigkeiten, damit sie per JUnit testbar ist.
 */
object UndoChainCodec {

    const val RECORD_SEPARATOR = '\u001E'
    const val FIELD_SEPARATOR = '\u001F'

    private const val FIELD_COUNT = 5

    fun encode(top: LottoUndoState): String {
        val sb = StringBuilder()
        var cur: LottoUndoState? = top
        var first = true
        while (cur != null) {
            if (!first) sb.append(RECORD_SEPARATOR)
            first = false
            appendRecord(sb, cur)
            cur = cur.undoState
        }
        return sb.toString()
    }

    /**
     * Baut die Kette wieder auf. Liefert `null`, wenn der String leer ist oder
     * irgendein Record nicht decodiert werden kann (dann lieber ganz ohne Undo
     * starten als mit einer halb kaputten Kette).
     */
    fun decode(data: String): LottoUndoState? {
        if (data.isEmpty()) return null
        val records = data.split(RECORD_SEPARATOR)
        val decoded = ArrayList<DecodedRecord>(records.size)
        for (record in records) {
            decoded += decodeRecord(record) ?: return null
        }
        // Von hinten (ältester Zustand) nach vorne verketten.
        var chain: LottoUndoState? = null
        for (i in decoded.indices.reversed()) {
            val r = decoded[i]
            chain = LottoUndoState(
                action = r.action,
                token = r.token,
                savedRoundId = r.savedRoundId,
                cards = r.state.cards,
                winRows = r.state.winRows,
                history = r.state.history,
                lastNumber = r.state.lastNumber,
                undoState = chain
            )
        }
        return chain
    }

    private class DecodedRecord(
        val action: UndoAction?,
        val token: Long,
        val savedRoundId: Long?,
        val state: PersistedState
    )

    private fun appendRecord(sb: StringBuilder, state: LottoUndoState) {
        val (code, arg) = encodeAction(state.action)
        sb.append(code).append(FIELD_SEPARATOR)
        sb.append(arg?.toString() ?: "").append(FIELD_SEPARATOR)
        sb.append(state.token).append(FIELD_SEPARATOR)
        sb.append(state.savedRoundId?.toString() ?: "").append(FIELD_SEPARATOR)
        sb.append(
            encodeState(
                winRows = state.winRows,
                cards = state.cards,
                history = state.history,
                lastNumber = state.lastNumber
            )
        )
    }

    private fun decodeRecord(record: String): DecodedRecord? {
        val fields = record.split(FIELD_SEPARATOR, limit = FIELD_COUNT)
        if (fields.size != FIELD_COUNT) return null
        val code = fields[0]
        val arg = if (fields[1].isEmpty()) null else fields[1].toIntOrNull() ?: return null
        val token = fields[2].toLongOrNull() ?: return null
        val savedRoundId = if (fields[3].isEmpty()) null else fields[3].toLongOrNull() ?: return null
        val action = decodeAction(code, arg) ?: return null
        val state = decodeState(fields[4]) ?: return null
        return DecodedRecord(action.value, token, savedRoundId, state)
    }

    /** Wrapper, damit "kein Action (Wurzel)" von "Decode-Fehler" unterscheidbar ist. */
    private class ActionHolder(val value: UndoAction?)

    private fun encodeAction(action: UndoAction?): Pair<String, Int?> = when (action) {
        null -> "" to null
        is UndoAction.NumberMarked -> "NM" to action.number
        UndoAction.CardAdded -> "CA" to null
        is UndoAction.CardDeleted -> "CD" to action.index
        is UndoAction.CellChanged -> "CC" to action.index
        UndoAction.CardRenamed -> "CR" to null
        UndoAction.WinRowsChanged -> "WR" to null
        is UndoAction.MarksCleared -> "MC" to action.index
        is UndoAction.ScanApplied -> "SA" to action.index
        UndoAction.NewRound -> "NR" to null
        UndoAction.AllCleared -> "AC" to null
        UndoAction.HistoryCleared -> "HC" to null
        is UndoAction.NumberRemoved -> "NX" to action.number
    }

    private fun decodeAction(code: String, arg: Int?): ActionHolder? {
        val action: UndoAction? = when (code) {
            "" -> null
            "NM" -> UndoAction.NumberMarked(arg ?: return null)
            "CA" -> UndoAction.CardAdded
            "CD" -> UndoAction.CardDeleted(arg ?: return null)
            "CC" -> UndoAction.CellChanged(arg ?: return null)
            "CR" -> UndoAction.CardRenamed
            "WR" -> UndoAction.WinRowsChanged
            "MC" -> UndoAction.MarksCleared(arg ?: return null)
            "SA" -> UndoAction.ScanApplied(arg ?: return null)
            "NR" -> UndoAction.NewRound
            "AC" -> UndoAction.AllCleared
            "HC" -> UndoAction.HistoryCleared
            "NX" -> UndoAction.NumberRemoved(arg ?: return null)
            else -> return null
        }
        return ActionHolder(action)
    }
}
