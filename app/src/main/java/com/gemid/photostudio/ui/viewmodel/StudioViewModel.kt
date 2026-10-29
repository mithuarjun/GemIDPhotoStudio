package com.gemid.photostudio.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.editor.BackgroundEngine
import com.gemid.photostudio.editor.ImageProcessor
import com.gemid.photostudio.editor.ToneAdjustments
import com.gemid.photostudio.export.ExportFormat
import com.gemid.photostudio.export.HighDpiExporter
import com.gemid.photostudio.export.PdfDocumentGenerator
import com.gemid.photostudio.export.WifiPrintHelper
import com.gemid.photostudio.sheet.PaperSize
import com.gemid.photostudio.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * StudioViewModel: High-performance state machine powering the GemIDPhotoStudio MVP.
 * Coordinates photo import normalization, robust offline ML Kit background changes,
 * pro-grade tone adjustments and skin beautification, smart multi-image queue (multiples of 6),
 * one-tap reset state machine, fixed grid sheet compositing, and MediaStore exports.
 */
class StudioViewModel : ViewModel() {

    // ── Photo State ────────────────────────────────────────────────────────
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    private val _masterSheetBitmap = MutableStateFlow<Bitmap?>(null)
    val masterSheetBitmap: StateFlow<Bitmap?> = _masterSheetBitmap.asStateFlow()

    // ── Multi-Image Distribution Queue (Multiples of 6) ───────────────────
    val photoQueue = mutableStateListOf<Bitmap>()

    // ── Pro-Grade Tone Adjustments & Beautify ──────────────────────────────
    var toneAdjustments by mutableStateOf(ToneAdjustments())
        private set

    // ── Template & Sizing State (Indian Standard First) ───────────────────
    var selectedTemplate by mutableStateOf(PassportTemplate.INDIAN_PASSPORT)
        private set

    var customWidthPx by mutableStateOf("413")
    var customHeightPx by mutableStateOf("531")
    var isCustomSizeEnabled by mutableStateOf(false)

    // ── Sheet Layout State (Strictly Multiples of 6: 6, 12, 18, 24) ────────
    var selectedPaperSize by mutableStateOf(PaperSize.POSTCARD_4X6)
        private set

    var selectedPhotoCount by mutableIntStateOf(6)
        private set

    var showBorders by mutableStateOf(true)
    var showScissorMarks by mutableStateOf(true)

    // ── UI / Loading Indicators ───────────────────────────────────────────
    var isLoading by mutableStateOf(false)
        private set

    var loadingMessage by mutableStateOf("")
        private set

    var statusMessage by mutableStateOf<String?>(null)

    // ── Photo Loading & Instant Pre-load Flow ─────────────────────────────
    fun loadPhoto(context: Context, uri: Uri, onLoaded: (() -> Unit)? = null) {
        isLoading = true
        loadingMessage = "Normalizing photo & opening Studio..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = ImageUtils.processAndNormalizeImage(context, uri)
                val bitmap = result.bitmap
                withContext(Dispatchers.Main) {
                    _originalBitmap.value = bitmap
                    val freshCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    _editedBitmap.value = freshCopy
                    photoQueue.clear()
                    photoQueue.add(freshCopy)
                    toneAdjustments = ToneAdjustments()
                    isLoading = false
                    generateMasterSheet()
                    onLoaded?.invoke()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage = "Failed to load image: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    fun addPhotoToQueue(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            loadingMessage = "Adding photo to mixed queue..."
            try {
                val result = ImageUtils.processAndNormalizeImage(context, uri)
                withContext(Dispatchers.Main) {
                    photoQueue.add(result.bitmap)
                    isLoading = false
                    statusMessage = "Photo added. Queue has ${photoQueue.size} photos."
                    generateMasterSheet()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    statusMessage = "Failed to add photo: ${e.localizedMessage}"
                }
            }
        }
    }

    fun removePhotoFromQueue(index: Int) {
        if (index in 0 until photoQueue.size) {
            photoQueue.removeAt(index)
            if (photoQueue.isEmpty() && _editedBitmap.value != null) {
                photoQueue.add(_editedBitmap.value!!)
            }
            generateMasterSheet()
        }
    }

    fun setDirectBitmap(bitmap: Bitmap) {
        _originalBitmap.value = bitmap
        val freshCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        _editedBitmap.value = freshCopy
        photoQueue.clear()
        photoQueue.add(freshCopy)
        generateMasterSheet()
    }

    // ── Template & Size Selection ─────────────────────────────────────────
    fun setTemplate(template: PassportTemplate) {
        selectedTemplate = template
        isCustomSizeEnabled = false
        customWidthPx = template.widthPx300Dpi.toString()
        customHeightPx = template.heightPx300Dpi.toString()
        generateMasterSheet()
    }

    fun setCustomSize(width: String, height: String) {
        customWidthPx = width
        customHeightPx = height
        isCustomSizeEnabled = true
        generateMasterSheet()
    }

    fun getActivePhotoSpec(): PhotoSpec {
        if (isCustomSizeEnabled) {
            val w = customWidthPx.toIntOrNull()?.coerceAtLeast(60) ?: 413
            val h = customHeightPx.toIntOrNull()?.coerceAtLeast(60) ?: 531
            val wMm = (w / 300f) * 25.4f
            val hMm = (h / 300f) * 25.4f
            return object : PhotoSpec {
                override val id: String = "custom_${w}x${h}"
                override val title: String = "Custom ${w}×${h} px"
                override val countryOrType: String = "Custom"
                override val widthMm: Float = wMm
                override val heightMm: Float = hMm
                override val standardDpi: Int = 300
                override val minHeadPercent: Float = 0.70f
                override val maxHeadPercent: Float = 0.80f
                override val recommendedBgColor: Color = Color.White
                override val description: String = "Custom ${w}×${h} px at 300 DPI"
                override val isCustom: Boolean = true
            }
        }
        return selectedTemplate
    }

    // ── Pro-Grade Tone Adjustments & Skin Beautify ─────────────────────────
    fun updateToneAdjustments(adjustments: ToneAdjustments) {
        toneAdjustments = adjustments
        val base = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val tuned = adjustments.applyToBitmap(base)
            withContext(Dispatchers.Main) {
                _editedBitmap.value = tuned
                if (photoQueue.isNotEmpty()) {
                    photoQueue[0] = tuned
                }
                generateMasterSheet()
            }
        }
    }

    // ── Offline Background Tools (ML Kit Selfie Segmentation) ─────────────
    fun changeBackground(color: Int) {
        val current = _editedBitmap.value ?: _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            loadingMessage = "Applying offline ML Kit segmentation..."
            try {
                val processed = BackgroundEngine.removeAndReplaceBackground(current, color)
                withContext(Dispatchers.Main) {
                    _editedBitmap.value = processed
                    if (photoQueue.isNotEmpty()) {
                        photoQueue[0] = processed
                    }
                    isLoading = false
                    generateMasterSheet()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage = "Segmentation error: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    // ── Bulletproof One-Tap Reset Engine ──────────────────────────────────
    fun resetImage() {
        val orig = _originalBitmap.value ?: return
        val pristine = orig.copy(Bitmap.Config.ARGB_8888, true)
        _editedBitmap.value = pristine
        photoQueue.clear()
        photoQueue.add(pristine)
        toneAdjustments = ToneAdjustments()
        selectedTemplate = PassportTemplate.INDIAN_PASSPORT
        customWidthPx = PassportTemplate.INDIAN_PASSPORT.widthPx300Dpi.toString()
        customHeightPx = PassportTemplate.INDIAN_PASSPORT.heightPx300Dpi.toString()
        isCustomSizeEnabled = false
        statusMessage = "All edits, filters, and crops cleared. Reverted to raw photo."
        generateMasterSheet()
    }

    fun applyCroppedBitmap(bitmap: Bitmap) {
        _editedBitmap.value = bitmap
        if (photoQueue.isNotEmpty()) {
            photoQueue[0] = bitmap
        }
        generateMasterSheet()
    }

    // ── Sheet Layout Controls (Multiples of 6) ────────────────────────────
    fun setPaper(paper: PaperSize) {
        selectedPaperSize = paper
        generateMasterSheet()
    }

    fun setPhotoCount(count: Int) {
        // Enforce strictly multiples of 6
        selectedPhotoCount = when (count) {
            6, 12, 18, 24 -> count
            else -> 6
        }
        generateMasterSheet()
    }

    fun toggleBorders() {
        showBorders = !showBorders
        generateMasterSheet()
    }

    fun toggleScissors() {
        showScissorMarks = !showScissorMarks
        generateMasterSheet()
    }

    // ── 300 DPI Sheet Compositing Engine with Smart Mixed Queue ───────────
    fun generateMasterSheet() {
        val activePhotos = if (photoQueue.isNotEmpty()) {
            photoQueue.toList()
        } else {
            listOfNotNull(_editedBitmap.value)
        }
        if (activePhotos.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val sheet = ImageProcessor.createMixedPrintSheet(
                    photos = activePhotos,
                    paperSize = selectedPaperSize,
                    template = getActivePhotoSpec(),
                    isLandscape = false,
                    showBorders = showBorders,
                    borderColor = AndroidColor.LTGRAY,
                    borderWidth = 3f,
                    showScissorMarks = showScissorMarks,
                    customPhotoCount = selectedPhotoCount,
                    includeStudioWatermark = true
                )
                withContext(Dispatchers.Main) {
                    _masterSheetBitmap.value = sheet
                }
            } catch (_: Exception) {
                // Ignore transient sheet render errors
            }
        }
    }

    // ── Wi-Fi Direct Print & MediaStore Exports ───────────────────────────
    fun printSheetDirect(context: Context): Boolean {
        val sheet = _masterSheetBitmap.value ?: return false
        return WifiPrintHelper.printSheet(
            context = context,
            sheetBitmap = sheet,
            paper = selectedPaperSize.definition,
            isLandscape = false,
            jobTitle = "GemID Studio Print ${selectedPaperSize.dimensionInches}"
        )
    }

    fun exportJpeg(context: Context, onResult: (String) -> Unit) {
        val sheet = _masterSheetBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            loadingMessage = "Injecting 300 DPI EXIF & saving JPEG..."
            try {
                val res = HighDpiExporter.exportImage(
                    context = context,
                    bitmap = sheet,
                    format = ExportFormat.JPEG,
                    qualityPercent = 100,
                    dpi = 300
                )
                withContext(Dispatchers.Main) {
                    isLoading = false
                    val msg = "Saved to Pictures/GemIDPhotoStudio/${res.fileName}"
                    statusMessage = msg
                    onResult(msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    statusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun exportPng(context: Context, onResult: (String) -> Unit) {
        val sheet = _masterSheetBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            loadingMessage = "Saving lossless PNG..."
            try {
                val res = HighDpiExporter.exportImage(
                    context = context,
                    bitmap = sheet,
                    format = ExportFormat.PNG,
                    qualityPercent = 100,
                    dpi = 300
                )
                withContext(Dispatchers.Main) {
                    isLoading = false
                    val msg = "Saved to Pictures/GemIDPhotoStudio/${res.fileName}"
                    statusMessage = msg
                    onResult(msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    statusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun exportPdf(context: Context, onResult: (String) -> Unit) {
        val sheet = _masterSheetBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            loadingMessage = "Generating 1:1 physical scale PDF..."
            try {
                val res = PdfDocumentGenerator.generatePdf(
                    context = context,
                    sheetBitmap = sheet,
                    paper = selectedPaperSize.definition,
                    isLandscape = false
                )
                withContext(Dispatchers.Main) {
                    isLoading = false
                    val msg = "Saved to Documents/GemIDPhotoStudio/${res.fileName}"
                    statusMessage = msg
                    onResult(msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    statusMessage = "PDF failed: ${e.localizedMessage}"
                }
            }
        }
    }
}
