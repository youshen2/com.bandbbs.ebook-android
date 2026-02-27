package com.bandbbs.ebook.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import java.io.File

@Composable
fun PdfPageViewer(
    pdfPath: String,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var renderer by remember(pdfPath) { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember(pdfPath) { mutableStateOf<ParcelFileDescriptor?>(null) }
    var bitmap by remember(pdfPath, pageIndex) { mutableStateOf<Bitmap?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(pdfPath) {
        val file = File(pdfPath)
        val openedPfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val openedRenderer = PdfRenderer(openedPfd)
        pfd = openedPfd
        renderer = openedRenderer
        onDispose {
            runCatching { openedRenderer.close() }
            runCatching { openedPfd.close() }
            renderer = null
            pfd = null
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(pdfPath, pageIndex, maxWidthPx, maxHeightPx) {
            val r = renderer ?: return@LaunchedEffect
            bitmap = null
            scale = 1f
            offset = Offset.Zero
            bitmap = withContext(Dispatchers.IO) {
                val safeIndex = pageIndex.coerceIn(0, (r.pageCount - 1).coerceAtLeast(0))
                val page = r.openPage(safeIndex)
                try {
                    val targetWidth = (maxWidthPx * 2f).toInt().coerceIn(1, 2048)
                    val widthScale = targetWidth.toFloat() / page.width.toFloat()
                    val heightCapScale = 4096f / page.height.toFloat()
                    val renderScale = minOf(widthScale, heightCapScale).coerceAtLeast(1f)

                    val bmpW = (page.width * renderScale).toInt().coerceAtLeast(1)
                    val bmpH = (page.height * renderScale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    val matrix = Matrix().apply { postScale(renderScale, renderScale) }
                    page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                } finally {
                    page.close()
                }
            }
        }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            val panMultiplier = (1.2f * newScale).coerceIn(1.2f, 6f)
            offset += Offset(panChange.x * panMultiplier, panChange.y * panMultiplier)
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bmp = bitmap
            if (bmp == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(transformState)
                )
            }
        }
    }
}

