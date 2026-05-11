package ch.lottomatchapp.app.scanner

import ch.lottomatchapp.app.data.LottoCard

/**
 * Sammelt OCR-Erkennungen über mehrere Frames und gibt nur Zahlen weiter,
 * die in mindestens [requiredHits] von [bufferSize] Frames konsistent erkannt wurden.
 * Das eliminiert Flackern und Fehlerkennungen.
 *
 * Speichert zusätzlich die grösste Bounding-Box-Fläche pro Zahl über alle Frames,
 * damit echte Kartenzahlen (gross) gegenüber Rausch-Text (klein) bevorzugt werden.
 */
class FrameAccumulator(
    private val bufferSize: Int = 5,
    private val requiredHits: Int = 3
) {
    // Ringpuffer: jeder Eintrag = Set der in einem Frame erkannten Zahlen
    private val frameBuffer = ArrayDeque<Set<Int>>(bufferSize)

    // Grösste bekannte Bounding-Box-Fläche pro Zahl (über alle Frames)
    private val maxAreaByNumber = mutableMapOf<Int, Float>()

    /**
     * Fügt die Erkennungsergebnisse eines Frames hinzu und gibt die
     * konsolidierten (stabile) Zahlen zurück.
     */
    fun addFrame(recognized: List<RecognizedNumber>): List<RecognizedNumber> {
        val numbers = recognized.map { it.value }.toSet()
        frameBuffer.addLast(numbers)
        if (frameBuffer.size > bufferSize) {
            frameBuffer.removeFirst()
        }

        // Grösste Fläche pro Zahl tracken
        for (rn in recognized) {
            val area = rn.width * rn.height
            val existing = maxAreaByNumber[rn.value] ?: 0f
            if (area > existing) {
                maxAreaByNumber[rn.value] = area
            }
        }

        // Zähle wie oft jede Zahl über alle Frames vorkommt
        val hitCounts = mutableMapOf<Int, Int>()
        for (frame in frameBuffer) {
            for (n in frame) {
                hitCounts[n] = (hitCounts[n] ?: 0) + 1
            }
        }

        // Nur Zahlen behalten, die in genügend Frames vorkommen
        val stableNumbers = hitCounts.filterValues { it >= requiredHits }.keys

        // Aus den aktuellen Erkennungen die stabilen Zahlen mit ihren
        // Positionsdaten zurückgeben (nutzt die neusten Koordinaten)
        return recognized.filter { it.value in stableNumbers }
    }

    /**
     * Gibt die beste konsolidierte Karte zurück.
     */
    fun getStableCard(currentRecognized: List<RecognizedNumber>): LottoCard {
        val stable = addFrame(currentRecognized)
        return mapRecognizedToLottoCard(stable)
    }

    /** Zählt wie viele Zahlen aktuell stabil erkannt sind. */
    fun currentStableCount(): Int {
        if (frameBuffer.isEmpty()) return 0
        val hitCounts = mutableMapOf<Int, Int>()
        for (frame in frameBuffer) {
            for (n in frame) {
                hitCounts[n] = (hitCounts[n] ?: 0) + 1
            }
        }
        return hitCounts.count { it.value >= requiredHits }
    }

    fun reset() {
        frameBuffer.clear()
        maxAreaByNumber.clear()
    }
}
