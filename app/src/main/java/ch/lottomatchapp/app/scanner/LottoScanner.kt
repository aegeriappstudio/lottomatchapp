@file:OptIn(ExperimentalMaterial3Api::class)

package ch.lottomatchapp.app.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.lottomatchapp.app.R
import ch.lottomatchapp.app.data.LottoCard
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

private const val TAG = "LottoScanner"

private const val OVERLAY_ASPECT: Float = 2.35f

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanLottoCardSheet(
    onCancel: () -> Unit,
    onResult: (LottoCard) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState, dragHandle = null) {
        ScanLottoCardContent(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(16.dp),
            onCancel = onCancel,
            onResult = onResult
        )
    }
}

@ExperimentalGetImage
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun ScanLottoCardContent(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onResult: (LottoCard) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bestCount by remember { mutableStateOf(0) }
    var fired by remember { mutableStateOf(false) }
    var pendingCard by remember { mutableStateOf<LottoCard?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    val frameAccumulator = remember { FrameAccumulator(bufferSize = 5, requiredHits = 3) }

    val progress = (bestCount / 15f).coerceIn(0f, 1f)

    val bestBadgeColor by animateColorAsState(
        targetValue = lerp(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            progress
        ),
        animationSpec = tween(durationMillis = 300),
        label = "bestBadgeColor"
    )

    Column(modifier) {
        Text(stringResource(R.string.scan_lotto_card_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    // Verhindert parallele OCR-Auswertungen und reduziert Last:
                    // Es wird immer nur ein Frame gleichzeitig von ML Kit verarbeitet.
                    val isProcessingFrame = AtomicBoolean(false)

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val recognizer =
                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setResolutionSelector(
                                androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        androidx.camera.core.resolutionselector.ResolutionStrategy(
                                            android.util.Size(1920, 1080),
                                            androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                        )
                                    )
                                    .build()
                            )
                            .build()
                            .also { ia ->
                                ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imgProxy ->
                                    val mediaImage = imgProxy.image
                                    if (mediaImage != null && !fired && isProcessingFrame.compareAndSet(false, true)) {
                                        // Bild-Preprocessing: Kontrast + Entsättigung
                                        val inputImage = try {
                                            val bitmap = imgProxy.toBitmap()
                                            val enhanced = ImagePreprocessor.enhance(bitmap)
                                            InputImage.fromBitmap(enhanced, imgProxy.imageInfo.rotationDegrees)
                                        } catch (e: Exception) {
                                            // Fallback auf Rohbild bei Preprocessing-Fehlern
                                            Log.w(TAG, "Preprocessing failed, using raw image", e)
                                            InputImage.fromMediaImage(
                                                mediaImage,
                                                imgProxy.imageInfo.rotationDegrees
                                            )
                                        }
                                        recognizer.process(inputImage)
                                            .addOnSuccessListener { result ->
                                                val list = extractNumbers(result)
                                                // Multi-Frame-Konsistenz: nur stabile Zahlen verwenden
                                                val card = frameAccumulator.getStableCard(list)
                                                val cnt = card.countNumbers()
                                                bestCount = max(bestCount, cnt)

                                                if (!fired && card.isCompleteCard()) {
                                                    fired = true
                                                    pendingCard = card
                                                    showConfirm = true
                                                }
                                            }
                                            .addOnCompleteListener {
                                                isProcessingFrame.set(false)
                                                imgProxy.close()
                                            }
                                    } else {
                                        imgProxy.close()
                                    }
                                }
                            }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to bind camera use cases", e)
                        }


                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(8.dp)
            ) {
                // leichter Overlay-Schleier
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.28f)
                        )
                )

                LottoScanOverlay(
                    progress = progress,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = bestBadgeColor,
                    shadowElevation = 4.dp,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = stringResource(R.string.numbers_found_count, bestCount),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (progress < 0.7f) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (showConfirm && pendingCard != null) {
            Spacer(Modifier.height(12.dp))
            ScanResultConfirmation(
                card = pendingCard!!,
                onAccept = {
                    pendingCard?.let(onResult)
                    showConfirm = false
                },
                onRetry = {
                    pendingCard = null
                    showConfirm = false
                    fired = false
                    bestCount = 0
                    frameAccumulator.reset()
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_preview_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun ScanResultConfirmation(
    card: LottoCard,
    onAccept: () -> Unit,
    onRetry: () -> Unit
) {
    val totalNumbers = card.countNumbers()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.scan_confirm_title),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.scan_confirm_message,
                    totalNumbers,
                    15
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            ScannedCardPreview(card = card)
            Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                ) {
                    Text(stringResource(R.string.scan_confirm_retry))
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                ) {
                    Text(stringResource(R.string.scan_confirm_accept))
                }
            }
        }
    }

@Composable
private fun ScannedCardPreview(card: LottoCard) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (r in 0 until card.rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (c in 0 until card.cols) {
                    val cell = card.cells[r * card.cols + c]
                    Surface(
                        modifier = Modifier
                            .width(24.dp)
                            .height(22.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        tonalElevation = if (cell.number != 0) 1.dp else 0.dp,
                        color = if (cell.number != 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (cell.number != 0) {
                                Text(
                                    text = cell.number.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_preview_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun LottoScanOverlay(
    rows: Int = 3,
    cols: Int = 9,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val ranges = columnRanges90()
    val clamped = progress.coerceIn(0f, 1f)

    val frameColor by animateColorAsState(
        targetValue = lerp(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.98f),
            clamped
        ),
        animationSpec = tween(durationMillis = 400),
        label = "scanFrameColor"
    )

    val frameStrokeWidthDp by animateDpAsState(
        targetValue = if (clamped > 0.8f) 3.dp else 2.dp,
        animationSpec = tween(durationMillis = 400),
        label = "scanFrameStrokeWidth"
    )

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth(0.95f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (c in 0 until cols) {
                val r = ranges[c]
                Text(
                    text = "${r.first}-${r.last}",
                    modifier = Modifier,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(OVERLAY_ASPECT)
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = frameStrokeWidthDp.toPx()
                val thin = 1.dp.toPx()

                drawRoundRect(
                    color = frameColor,
                    style = Stroke(stroke),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )

                val cellW = size.width / cols
                val cellH = size.height / rows

                for (i in 1 until cols) {
                    drawLine(
                        color = gridColor,
                        start = Offset(cellW * i, 0f),
                        end = Offset(cellW * i, size.height),
                        strokeWidth = thin
                    )
                }
                for (j in 1 until rows) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, cellH * j),
                        end = Offset(size.width, cellH * j),
                        strokeWidth = thin
                    )
                }
            }
        }
    }
}