package com.gemid.photostudio.batch

import android.graphics.Bitmap
import android.net.Uri
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.sheet.PaperSize
import java.util.UUID

enum class BatchItemStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class BatchItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri? = null,
    val rawBitmap: Bitmap,
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val currentStep: String = "Queued",
    val optimizedBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

data class BatchStudioConfig(
    val template: PhotoSpec = PassportTemplate.INDIAN_PASSPORT,
    val backdrop: StudioBackdrop = StudioBackdrop.WHITE,
    val paperSize: PaperSize = PaperSize.POSTCARD_4X6,
    val autoCrop: Boolean = true,
    val autoCleanBackground: Boolean = true,
    val autoEnhanceTone: Boolean = true,
    val showBorders: Boolean = true,
    val showScissorMarks: Boolean = true
)
