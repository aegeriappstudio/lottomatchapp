package ch.lottomatchapp.app.scanner

import androidx.compose.ui.geometry.Offset
import ch.lottomatchapp.app.data.Cell
import ch.lottomatchapp.app.data.LottoCard
import com.google.mlkit.vision.text.Text as MlText
import kotlin.math.abs

fun columnRanges90(): List<IntRange> = listOf(
    1..9, 10..19, 20..29, 30..39, 40..49, 50..59, 60..69, 70..79, 80..90
)

data class RecognizedNumber(
    val value: Int,
    val center: Offset,
    val width: Float,
    val height: Float,
    val confidence: Float = 1f
)

/** Minimale Confidence für ML Kit-Erkennungen (0-1). */
private const val MIN_CONFIDENCE = 0.55f

fun extractNumbers(result: MlText): List<RecognizedNumber> {
    val raw = mutableListOf<RecognizedNumber>()
    for (block in result.textBlocks) {
        for (line in block.lines) {
            for (el in line.elements) {
                val conf = el.confidence
                if (conf < MIN_CONFIDENCE) continue

                val t = el.text.trim().filter { it.isDigit() }
                if (t.isEmpty()) continue
                val box = el.boundingBox ?: continue

                val n = t.toIntOrNull()
                if (n != null && n in 1..90) {
                    raw += RecognizedNumber(
                        value = n,
                        center = Offset(box.exactCenterX(), box.exactCenterY()),
                        width = box.width().toFloat(),
                        height = box.height().toFloat(),
                        confidence = conf
                    )
                } else if (t.length > 2) {
                    // Verkettete Zahlen aufteilen (z.B. "5465" → [54, 65])
                    val numbers = splitConcatenatedNumbers(t)
                    if (numbers.isNotEmpty()) {
                        val numCount = numbers.size
                        val segmentWidth = box.width().toFloat() / numCount
                        for ((i, num) in numbers.withIndex()) {
                            val segmentCenterX = box.left + (segmentWidth * i) + (segmentWidth / 2f)
                            raw += RecognizedNumber(
                                value = num,
                                center = Offset(segmentCenterX, box.exactCenterY()),
                                width = segmentWidth,
                                height = box.height().toFloat(),
                                confidence = conf
                            )
                        }
                    }
                }
            }
        }
    }
    if (raw.isEmpty()) return emptyList()

    // ── Schritt 1: Minimaler Grössenfilter ──
    // Offensichtlich zu kleine Erkennungen entfernen (< 45% der Medianhöhe).
    // Bewusst grosszügig, damit einstellige Zahlen (z.B. "8") nicht verloren gehen.
    val heights = raw.map { it.height }.sorted()
    val medianH = heights[heights.size / 2]
    val minHeight = medianH * 0.45f
    val sizeFiltered = raw.filter { it.height >= minHeight }
    if (sizeFiltered.isEmpty()) return emptyList()

    // ── Schritt 2: Breitenausreisser filtern ──
    // Sehr breite Bounding Boxes (z.B. Kartennummer "103930") rausfiltern
    val widths = sizeFiltered.map { it.width }.sorted()
    val medianW = widths[widths.size / 2]
    val maxAllowedWidth = medianW * 3.0f
    val widthFiltered = sizeFiltered.filter { it.width <= maxAllowedWidth }
    if (widthFiltered.isEmpty()) return sizeFiltered

    // ── Schritt 3: Top-Kandidaten nach Grösse ──
    // Kartenzahlen sind die grössten Textelemente im Bild.
    // Statt strikte Schwellwerte zu verwenden (die auch echte Zahlen treffen),
    // nehmen wir einfach die grössten Erkennungen und lassen die
    // Grid-Zuordnung + X-Validierung den Rest erledigen.
    // Max 25 Kandidaten: genug für 15 echte + Reserve bei Duplikaten/Splits.
    val topBySize = widthFiltered
        .sortedByDescending { it.height }
        .take(25)

    // ── Schritt 4: X-Position-Konsistenzprüfung ──
    // Auf einer Lottokarte sind die Spalten physisch von links nach rechts
    // angeordnet. Wir prüfen ob die X-Position einer Zahl grob zu ihrer
    // erwarteten Spalte passt. Stark inkonsistente Zuordnungen verwerfen.
    val xValidated = validateXPositions(topBySize)

    // ── Schritt 5: Deduplizierung ──
    // Bei gleicher Zahl die Erkennung mit der grössten Bounding Box behalten
    return xValidated.groupBy { it.value }.values.map { grp ->
        grp.maxByOrNull { it.height * it.width }!!
    }
}

/**
 * Validiert die X-Positionen der erkannten Zahlen gegen ihre erwarteten Spalten.
 *
 * Auf einer Lottokarte (Tombola 90) sind die Spalten physisch geordnet:
 * Links = kleine Zahlen (1-9), Rechts = grosse Zahlen (80-90).
 * Wenn eine Zahl z.B. den Wert 89 hat aber ganz links sitzt, ist sie
 * wahrscheinlich falsch erkannt.
 */
private fun validateXPositions(numbers: List<RecognizedNumber>): List<RecognizedNumber> {
    if (numbers.size < 3) return numbers

    // X-Positionen aller Zahlen sammeln und in 9 Spalten-Bins aufteilen
    val xMin = numbers.minOf { it.center.x }
    val xMax = numbers.maxOf { it.center.x }
    val xRange = xMax - xMin
    if (xRange < 1f) return numbers // alle auf gleicher X-Position

    return numbers.filter { rn ->
        val col = columnRanges90().indexOfFirst { rn.value in it }
        if (col < 0) return@filter false

        // Normalisierte X-Position (0 = ganz links, 1 = ganz rechts)
        val normalizedX = (rn.center.x - xMin) / xRange

        // Erwarteter normalisierter X-Bereich für diese Spalte
        // Spalte 0 (1-9) → X ≈ 0.0, Spalte 8 (80-90) → X ≈ 1.0
        // Grosszügige Toleranz für perspektivische Verzerrung
        val expectedX = col / 8f
        val tolerance = 0.28f

        abs(normalizedX - expectedX) <= tolerance
    }
}

/**
 * Spaltet verkettete Ziffernstrings in gültige Lotto-Zahlen (1-90) auf.
 * Probiert alle sinnvollen Aufteilungen und wählt die beste.
 * Beispiel: "5465" → [54, 65], "6970" → [69, 70], "3347" → [33, 47]
 */
internal fun splitConcatenatedNumbers(digits: String): List<Int> {
    if (digits.length <= 2) return emptyList()

    // Alle möglichen Aufteilungen in 1- und 2-stellige Zahlen finden
    val allSplits = mutableListOf<List<Int>>()
    findSplits(digits, 0, mutableListOf(), allSplits)

    if (allSplits.isEmpty()) return emptyList()

    // Beste Aufteilung wählen: bevorzuge 2-stellige Zahlen (typisch für Lottokarten)
    return allSplits
        .filter { it.size >= 2 } // mindestens 2 Zahlen
        .maxByOrNull { split ->
            // Score: mehr 2-stellige Zahlen = besser
            split.count { it >= 10 } * 10 + split.size
        } ?: emptyList()
}

/**
 * Rekursive Aufspaltung: findet alle möglichen Zerlegungen eines Ziffernstrings
 * in gültige Lotto-Zahlen (1-90).
 */
private fun findSplits(
    digits: String,
    pos: Int,
    current: MutableList<Int>,
    results: MutableList<List<Int>>
) {
    if (pos == digits.length) {
        results.add(current.toList())
        return
    }

    // 1-stellig versuchen
    if (pos < digits.length) {
        val one = digits.substring(pos, pos + 1).toIntOrNull()
        if (one != null && one in 1..9) {
            current.add(one)
            findSplits(digits, pos + 1, current, results)
            current.removeAt(current.lastIndex)
        }
    }

    // 2-stellig versuchen
    if (pos + 1 < digits.length) {
        val two = digits.substring(pos, pos + 2).toIntOrNull()
        if (two != null && two in 1..90) {
            current.add(two)
            findSplits(digits, pos + 2, current, results)
            current.removeAt(current.lastIndex)
        }
    }
}

/** 3x9 Zuordnung: Spalte aus Wertebereich, Zeile via k-Means(3) auf Y */
fun mapRecognizedToLottoCard(recognizedRaw: List<RecognizedNumber>): LottoCard {
    if (recognizedRaw.isEmpty()) return LottoCard.empty()

    val withCol = recognizedRaw.mapNotNull { rn ->
        val col = columnRanges90().indexOfFirst { rn.value in it }
        if (col >= 0) rn to col else null
    }
    if (withCol.isEmpty()) return LottoCard.empty()

    // Perspektivkorrektur: Neigung erkennen und Y-Koordinaten normalisieren
    val corrected = correctSkew(withCol)

    val ys = corrected.map { it.first.center.y }
    val centers = kMeans1D(ys, k = 3).sorted()
    fun nearestCenter(y: Float): Float = centers.minByOrNull { abs(it - y) } ?: centers.first()
    fun rowIndex(y: Float): Int = centers.indexOf(nearestCenter(y)).coerceIn(0, 2)

    val rows = 3
    val cols = 9
    val grid = Array(rows) { IntArray(cols) { 0 } }
    val perRowCount = IntArray(rows) { 0 }

    // Sortierung: Grösste Bounding Box zuerst (echte Kartenzahlen sind grösser
    // als Rausch-Text), dann Confidence, dann Nähe zum Zeilenzentrum
    val sorted = corrected.sortedWith(
        compareByDescending<Pair<RecognizedNumber, Int>> { it.first.height * it.first.width }
            .thenByDescending { it.first.confidence }
            .thenBy { abs(it.first.center.y - nearestCenter(it.first.center.y)) }
    )

    for ((rn, col) in sorted) {
        val r = rowIndex(rn.center.y)
        if (r !in 0..2) continue
        if (perRowCount[r] >= 5) continue
        if (grid[r][col] == 0) {
            grid[r][col] = rn.value
            perRowCount[r]++
        } else {
            val alt = listOf(col - 1, col + 1).firstOrNull { it in 0 until cols && grid[r][it] == 0 }
            if (alt != null) {
                grid[r][alt] = rn.value
                perRowCount[r]++
            }
        }
        if (perRowCount.sum() >= 15) break
    }

    val cells = buildList {
        for (r in 0 until rows) for (c in 0 until cols)
            add(Cell(number = grid[r][c], marked = false))
    }
    return LottoCard(rows = rows, cols = cols, cells = cells)
}

/**
 * Erkennt Kartenneigung und korrigiert Y-Koordinaten per linearer Regression.
 */
private fun correctSkew(
    items: List<Pair<RecognizedNumber, Int>>
): List<Pair<RecognizedNumber, Int>> {
    if (items.size < 4) return items

    val xs = items.map { it.first.center.x }
    val ys = items.map { it.first.center.y }
    val n = xs.size.toFloat()
    val meanX = xs.sum() / n
    val meanY = ys.sum() / n

    var numerator = 0f
    var denominator = 0f
    for (i in xs.indices) {
        val dx = xs[i] - meanX
        numerator += dx * (ys[i] - meanY)
        denominator += dx * dx
    }

    if (denominator < 1f) return items

    val slope = numerator / denominator

    // Nur korrigieren wenn Neigung signifikant (> ~2°)
    if (abs(slope) < 0.035f) return items

    return items.map { (rn, col) ->
        val correctedY = rn.center.y - slope * (rn.center.x - meanX)
        val correctedRn = rn.copy(center = Offset(rn.center.x, correctedY))
        correctedRn to col
    }
}

/* Einfaches 1D k-Means */
private fun kMeans1D(values: List<Float>, k: Int, iters: Int = 12): List<Float> {
    if (values.isEmpty()) return List(k) { 0f }
    val sorted = values.sorted()
    val step = (sorted.size.toFloat() / k).coerceAtLeast(1f)
    var centers = (0 until k).map { idx ->
        sorted[(idx * step).toInt().coerceIn(0, sorted.lastIndex)]
    }
    repeat(iters) {
        val buckets = Array(k) { mutableListOf<Float>() }
        for (v in values) {
            val ci = centers.indices.minByOrNull { i -> abs(v - centers[i]) } ?: 0
            buckets[ci].add(v)
        }
        centers = centers.indices.map { i ->
            val b = buckets[i]
            if (b.isEmpty()) centers[i] else (b.sum() / b.size)
        }
    }
    return centers
}
