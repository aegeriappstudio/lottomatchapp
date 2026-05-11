package ch.lottomatchapp.app.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Bildvorverarbeitung zur Verbesserung der OCR-Erkennung auf Lottokarten.
 * Optimiert für fette, schwarze Zahlen auf hellem (oft grünlichem) Hintergrund.
 */
object ImagePreprocessor {

    /**
     * Wendet Kontrastverstärkung und Entsättigung an.
     * Ziel: schwarze Ziffern maximieren, hellen Hintergrund weisseln,
     * damit ML Kit einzelne Ziffern besser trennen kann.
     */
    fun enhance(source: Bitmap): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint()

        // 1) Komplett entsättigen → Graustufen
        //    Lottokarten haben grünen Hintergrund, der ML Kit verwirren kann
        val grayscale = ColorMatrix()
        grayscale.setSaturation(0f)

        // 2) Starker Kontrast: Dunkle Pixel (Zahlen) dunkler, helle (Hintergrund) heller
        //    Das hilft ML Kit die einzelnen Ziffern sauberer zu segmentieren
        val contrast = 1.8f
        val brightness = -30f
        val translate = (1f - contrast) * 128f + brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Kombinieren: erst Graustufen, dann Kontrast
        grayscale.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(grayscale)
        canvas.drawBitmap(mutable, 0f, 0f, paint)

        return mutable
    }
}
