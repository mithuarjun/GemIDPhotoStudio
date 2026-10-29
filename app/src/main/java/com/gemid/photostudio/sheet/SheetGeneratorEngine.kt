package com.gemid.photostudio.sheet

import android.graphics.Bitmap
import android.graphics.Color
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.editor.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SheetRenderOptions(
    val includeCutMarks: Boolean = true,
    val cutMarkColor: Int = Color.argb(180, 80, 80, 80), // Hairline grey
    val drawPhotoBorder: Boolean = true,
    val borderWidth: Float = 3f,
    val includeStudioWatermark: Boolean = true
)

object SheetGeneratorEngine {

    /**
     * Generates the full 300 DPI master print sheet bitmap using ImageProcessor.
     */
    suspend fun renderSheetBitmap(
        primaryPhoto: Bitmap,
        batchPhotos: List<Bitmap> = emptyList(),
        grid: GridMatrixResult,
        options: SheetRenderOptions = SheetRenderOptions()
    ): Bitmap = withContext(Dispatchers.Default) {
        val allPhotos = if (batchPhotos.isNotEmpty()) batchPhotos else listOf(primaryPhoto)
        val paper = PaperSize.entries.find { it.definition == grid.paper } ?: PaperSize.POSTCARD_4X6

        return@withContext ImageProcessor.createMixedPrintSheet(
            photos = allPhotos,
            paperSize = paper,
            template = PassportTemplate.INDIAN_PASSPORT,
            isLandscape = grid.isLandscape,
            showBorders = options.drawPhotoBorder,
            borderColor = options.cutMarkColor,
            borderWidth = options.borderWidth,
            showScissorMarks = options.includeCutMarks,
            customPhotoCount = grid.activeCount,
            includeStudioWatermark = options.includeStudioWatermark
        )
    }

    /**
     * Generates a fast, scaled-down preview bitmap suitable for Compose UI rendering without memory pressure.
     */
    fun createPreviewBitmap(
        masterSheet: Bitmap,
        targetWidth: Int = 1000
    ): Bitmap {
        return ImageProcessor.createPreviewBitmap(masterSheet, targetWidth)
    }
}
