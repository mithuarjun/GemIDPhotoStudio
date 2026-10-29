package com.gemid.photostudio.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gemid.photostudio.data.local.CustomPresetRepository
import com.gemid.photostudio.data.model.CustomPhotoSpec
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.editor.BackgroundEngine
import com.gemid.photostudio.editor.CropRatioPreset
import com.gemid.photostudio.editor.ExamStampConfig
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.editor.ToneAdjustments
import com.gemid.photostudio.sheet.GridMatrixCalculator
import com.gemid.photostudio.sheet.GridMatrixResult
import com.gemid.photostudio.sheet.PrintPaperDefinition
import com.gemid.photostudio.sheet.SheetGeneratorEngine
import com.gemid.photostudio.sheet.SheetRenderOptions
import android.content.Context
import com.gemid.photostudio.export.ExportFormat
import com.gemid.photostudio.export.ExportResult
import com.gemid.photostudio.export.HighDpiExporter
import com.gemid.photostudio.export.PdfDocumentGenerator
import com.gemid.photostudio.export.PdfExportResult
import com.gemid.photostudio.export.WifiPrintHelper
import com.gemid.photostudio.util.ImageUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.gemid.photostudio.editor.ImageProcessor
import com.gemid.photostudio.sheet.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Stack

data class StudioUiState(
    val photoUri: Uri? = null,
    val originalSourceUri: Uri? = null,
    val activeBitmap: Bitmap? = null,
    val photoWidth: Int = 0,
    val photoHeight: Int = 0,
    val fileSizeBytes: Long = 0,
    val rotationAngle: Int = 0,
    val selectedTemplate: PhotoSpec = PassportTemplate.INDIAN_PASSPORT,
    val cropPreset: CropRatioPreset = CropRatioPreset.INDIAN_PASSPORT,
    val toneAdjustments: ToneAdjustments = ToneAdjustments(),
    val currentBackdrop: StudioBackdrop = StudioBackdrop.ORIGINAL,
    val examStampConfig: ExamStampConfig = ExamStampConfig(),
    val isCroppingActive: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val recentImports: List<Uri> = emptyList(),
    val unsegmentedBitmap: Bitmap? = null,
    val currentMask: ByteArray? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // ── Sheet Layout Engine State ─────────────────────────────────
    val selectedPaper: PrintPaperDefinition = PrintPaperDefinition.SIZE_4X6,
    val isSheetLandscape: Boolean = false,
    val customSheetPhotoCount: Int? = null,
    val includeCutMarks: Boolean = true,
    val cutMarkColorType: Int = 0, // 0: Hairline Grey, 1: Hairline Black
    val computedGridResult: GridMatrixResult = GridMatrixCalculator.computeGrid(
        paper = PrintPaperDefinition.SIZE_4X6,
        photoWidthMm = PassportTemplate.INDIAN_PASSPORT.widthMm,
        photoHeightMm = PassportTemplate.INDIAN_PASSPORT.heightMm,
        isLandscape = false
    ),
    val generatedSheetBitmap: Bitmap? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    // ── Mixed Photo Queue & Advanced Sheet Customization ────────────
    val photoList = mutableStateListOf<Bitmap>()
    var showBorders by mutableStateOf(true)
    var borderColor by mutableStateOf(Color.LTGRAY)
    var borderWidth by mutableStateOf(3f)
    var showScissorMarks by mutableStateOf(true)
    var selectedPaperSize by mutableStateOf(PaperSize.POSTCARD_4X6)

    // ── Persistent Custom Preset & Custom Paper Storage ────────────
    private val customPresetRepo = CustomPresetRepository(application)
    val customPhotoSpecs = MutableStateFlow<List<CustomPhotoSpec>>(emptyList())
    val customPaperSizes = MutableStateFlow<List<PaperSize>>(emptyList())

    init {
        loadPersistedCustomPresets()
    }

    private fun loadPersistedCustomPresets() {
        val specs = customPresetRepo.getCustomPhotoSpecs()
        customPhotoSpecs.value = specs
        val papers = customPresetRepo.getCustomPapers()
        customPaperSizes.value = papers.map { PaperSize.fromDefinition(it) }
    }

    fun getAllPhotoSpecs(): List<PhotoSpec> {
        return PassportTemplate.entries.toList() + customPhotoSpecs.value
    }

    fun getAllPaperSizes(): List<PaperSize> {
        return PaperSize.entries + customPaperSizes.value
    }

    fun addCustomPhotoSpec(spec: CustomPhotoSpec) {
        customPresetRepo.saveCustomPhotoSpec(spec)
        customPhotoSpecs.value = customPresetRepo.getCustomPhotoSpecs()
        selectTemplate(spec)
    }

    fun deleteCustomPhotoSpec(spec: PhotoSpec) {
        customPresetRepo.deleteCustomPhotoSpec(spec.id)
        customPhotoSpecs.value = customPresetRepo.getCustomPhotoSpecs()
        if (_uiState.value.selectedTemplate.id == spec.id) {
            selectTemplate(PassportTemplate.INDIAN_PASSPORT)
        }
    }

    fun addCustomPaperDefinition(paperDef: PrintPaperDefinition) {
        customPresetRepo.saveCustomPaper(paperDef)
        val updated = customPresetRepo.getCustomPapers()
        customPaperSizes.value = updated.map { PaperSize.fromDefinition(it) }
        val newPaperSize = PaperSize.fromDefinition(paperDef)
        setPaperSizeTarget(newPaperSize)
    }

    fun deleteCustomPaper(paper: PaperSize) {
        customPresetRepo.deleteCustomPaper(paper.id)
        val updated = customPresetRepo.getCustomPapers()
        customPaperSizes.value = updated.map { PaperSize.fromDefinition(it) }
        if (selectedPaperSize.id == paper.id) {
            setPaperSizeTarget(PaperSize.POSTCARD_4X6)
        }
    }



    fun addPhotoToQueue(bitmap: Bitmap) {
        photoList.add(bitmap)
        generateMasterSheetBitmap()
    }

    fun removePhotoFromQueue(index: Int) {
        if (index in photoList.indices) {
            photoList.removeAt(index)
            generateMasterSheetBitmap()
        }
    }

    fun clearPhotoQueue() {
        photoList.clear()
        generateMasterSheetBitmap()
    }

    fun duplicatePhotoInQueue(index: Int) {
        if (index in photoList.indices) {
            val bmp = photoList[index]
            photoList.add(index + 1, bmp)
            generateMasterSheetBitmap()
        }
    }

    fun addActivePhotoToQueue() {
        val active = _uiState.value.activeBitmap ?: return
        photoList.add(active)
        generateMasterSheetBitmap()
    }

    fun addPhotosFromGallery(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            for (uri in uris) {
                try {
                    val norm = ImageUtils.processAndNormalizeImage(context, uri)
                    photoList.add(norm.bitmap)
                } catch (_: Exception) {}
            }
            generateMasterSheetBitmap()
        }
    }

    fun setPaperSizeTarget(paperSize: PaperSize) {
        selectedPaperSize = paperSize
        selectPaper(paperSize.definition)
    }

    fun updateBorders(show: Boolean, color: Int = borderColor, width: Float = borderWidth) {
        showBorders = show
        borderColor = color
        borderWidth = width
        generateMasterSheetBitmap()
    }

    fun updateScissorMarks(show: Boolean) {
        showScissorMarks = show
        _uiState.update { it.copy(includeCutMarks = show) }
        generateMasterSheetBitmap()
    }

    // Bounded Undo / Redo history stacks (max 4 snapshots to avoid memory bloat)
    private val undoStack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()
    private val MAX_HISTORY = 4

    private fun pushUndoSnapshot(bitmap: Bitmap) {
        if (undoStack.size >= MAX_HISTORY) {
            val oldest = undoStack.removeAt(0)
            if (!oldest.isRecycled && oldest != _uiState.value.activeBitmap) {
                oldest.recycle()
            }
        }
        undoStack.push(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false))
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    private fun updateUndoRedoAvailability() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.activeBitmap ?: return
        redoStack.push(current)
        val previous = undoStack.pop()
        _uiState.update {
            it.copy(
                activeBitmap = previous,
                photoWidth = previous.width,
                photoHeight = previous.height
            )
        }
        updateUndoRedoAvailability()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.activeBitmap ?: return
        undoStack.push(current)
        val next = redoStack.pop()
        _uiState.update {
            it.copy(
                activeBitmap = next,
                photoWidth = next.width,
                photoHeight = next.height
            )
        }
        updateUndoRedoAvailability()
    }

    /**
     * Loads and normalizes an image imported from the Gallery Picker.
     */
    fun loadFromGallery(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Importing & correcting EXIF...", errorMessage = null) }
            try {
                val context = getApplication<Application>().applicationContext
                val result = ImageUtils.processAndNormalizeImage(context, uri)

                clearHistory()

                _uiState.update {
                    it.copy(
                        photoUri = result.fileUri,
                        originalSourceUri = uri,
                        activeBitmap = result.bitmap,
                        unsegmentedBitmap = result.bitmap,
                        currentMask = null,
                        photoWidth = result.processedWidth,
                        photoHeight = result.processedHeight,
                        fileSizeBytes = result.fileSizeBytes,
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = null,
                        rotationAngle = 0,
                        toneAdjustments = ToneAdjustments(),
                        currentBackdrop = StudioBackdrop.ORIGINAL,
                        examStampConfig = ExamStampConfig()
                    )
                }
                if (photoList.isEmpty()) {
                    photoList.add(result.bitmap)
                }
                recalculateGrid()
                generateMasterSheetBitmap()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "Failed to load photo: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    /**
     * Loads multiple photos picked from the Gallery Picker.
     */
    fun loadMultipleFromGallery(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Importing ${uris.size} photos...",
                    errorMessage = null
                )
            }
            try {
                val context = getApplication<Application>().applicationContext
                val loadedBitmaps = mutableListOf<Bitmap>()
                for (uri in uris) {
                    try {
                        val norm = ImageUtils.processAndNormalizeImage(context, uri)
                        loadedBitmaps.add(norm.bitmap)
                    } catch (_: Exception) {}
                }

                if (loadedBitmaps.isNotEmpty()) {
                    photoList.addAll(loadedBitmaps)
                    val first = loadedBitmaps.first()
                    clearHistory()

                    _uiState.update {
                        it.copy(
                            photoUri = uris.first(),
                            originalSourceUri = uris.first(),
                            activeBitmap = first,
                            unsegmentedBitmap = first,
                            currentMask = null,
                            photoWidth = first.width,
                            photoHeight = first.height,
                            recentImports = uris,
                            isLoading = false,
                            loadingMessage = "",
                            errorMessage = null,
                            rotationAngle = 0
                        )
                    }
                    recalculateGrid()
                    generateMasterSheetBitmap()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = "",
                            errorMessage = "No valid photos could be loaded"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "Failed to import photos: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Handles image captured via CameraX.
     */
    fun onPhotoCaptured(capturedUri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Processing high-res capture...",
                    errorMessage = null
                )
            }
            try {
                val context = getApplication<Application>().applicationContext
                val result = ImageUtils.processAndNormalizeImage(context, capturedUri)

                clearHistory()

                _uiState.update {
                    it.copy(
                        photoUri = result.fileUri,
                        originalSourceUri = capturedUri,
                        activeBitmap = result.bitmap,
                        unsegmentedBitmap = result.bitmap,
                        currentMask = null,
                        photoWidth = result.processedWidth,
                        photoHeight = result.processedHeight,
                        fileSizeBytes = result.fileSizeBytes,
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = null,
                        rotationAngle = 0
                    )
                }
                if (photoList.isEmpty()) {
                    photoList.add(result.bitmap)
                }
                recalculateGrid()
                generateMasterSheetBitmap()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "Failed to process photo: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ── 1. Cropping Engine ──────────────────────────────────────────

    fun setCropPreset(preset: CropRatioPreset) {
        _uiState.update { it.copy(cropPreset = preset) }
    }

    fun setCroppingActive(isActive: Boolean) {
        _uiState.update { it.copy(isCroppingActive = isActive) }
    }

    fun applyCroppedBitmap(cropped: Bitmap) {
        val current = _uiState.value.activeBitmap
        if (current != null) {
            pushUndoSnapshot(current)
        }
        _uiState.update {
            it.copy(
                activeBitmap = cropped,
                unsegmentedBitmap = cropped,
                currentMask = null,
                photoWidth = cropped.width,
                photoHeight = cropped.height,
                isCroppingActive = false
            )
        }
        recalculateGrid()
    }

    // ── 2. Background Replacement & Mask Refinement Engine ──────────

    fun replaceBackground(backdrop: StudioBackdrop) {
        val current = _uiState.value.activeBitmap ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Segmenting portrait & applying ${backdrop.title} backdrop...",
                    errorMessage = null
                )
            }
            try {
                val baseSource = _uiState.value.unsegmentedBitmap ?: current
                val mask = BackgroundEngine.generateForegroundMask(baseSource)
                val result = BackgroundEngine.applyMaskToBitmap(baseSource, mask, backdrop.colorArgb)
                pushUndoSnapshot(current)
                _uiState.update {
                    it.copy(
                        activeBitmap = result,
                        unsegmentedBitmap = baseSource,
                        currentMask = mask,
                        photoWidth = result.width,
                        photoHeight = result.height,
                        currentBackdrop = backdrop,
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "Background replacement failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Commits a refined mask and composite photo into the studio pipeline.
     */
    fun applyRefinedMaskBitmap(refinedBitmap: Bitmap, mask: ByteArray) {
        val current = _uiState.value.activeBitmap
        if (current != null) {
            pushUndoSnapshot(current)
        }
        _uiState.update {
            it.copy(
                activeBitmap = refinedBitmap,
                currentMask = mask,
                photoWidth = refinedBitmap.width,
                photoHeight = refinedBitmap.height
            )
        }
        recalculateGrid()
        generateMasterSheetBitmap()
    }

    // ── 3. Tone & Color Adjustments ────────────────────────────────

    fun updateToneAdjustments(adjustments: ToneAdjustments) {
        _uiState.update { it.copy(toneAdjustments = adjustments) }
    }

    fun resetToneAdjustments() {
        _uiState.update { it.copy(toneAdjustments = ToneAdjustments()) }
    }

    fun bakeToneAdjustments() {
        val current = _uiState.value.activeBitmap ?: return
        val adjustments = _uiState.value.toneAdjustments
        if (adjustments.isDefault) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Baking tone adjustments...") }
            val baked = withContext(Dispatchers.Default) {
                adjustments.applyToBitmap(current)
            }
            pushUndoSnapshot(current)
            _uiState.update {
                it.copy(
                    activeBitmap = baked,
                    toneAdjustments = ToneAdjustments(),
                    isLoading = false,
                    loadingMessage = ""
                )
            }
        }
    }

    // ── 4. Indian Exam Name & Date Banner Overlay ──────────────────

    fun updateExamStampConfig(config: ExamStampConfig) {
        _uiState.update { it.copy(examStampConfig = config) }
    }

    fun applyExamStamp() {
        val current = _uiState.value.activeBitmap ?: return
        val stampConfig = _uiState.value.examStampConfig
        if (!stampConfig.isEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Applying exam name & date banner...") }
            val stamped = withContext(Dispatchers.Default) {
                stampConfig.applyToBitmap(current)
            }
            pushUndoSnapshot(current)
            _uiState.update {
                it.copy(
                    activeBitmap = stamped,
                    examStampConfig = it.examStampConfig.copy(isEnabled = false),
                    isLoading = false,
                    loadingMessage = ""
                )
            }
        }
    }

    // ── 5. Sheet Layout & Grid Generation Engine ───────────────────

    fun selectPaper(paper: PrintPaperDefinition) {
        _uiState.update { it.copy(selectedPaper = paper, customSheetPhotoCount = null) }
        recalculateGrid()
    }

    fun toggleSheetOrientation() {
        _uiState.update { it.copy(isSheetLandscape = !it.isSheetLandscape) }
        recalculateGrid()
    }

    fun setCustomSheetPhotoCount(count: Int?) {
        _uiState.update { it.copy(customSheetPhotoCount = count) }
        recalculateGrid()
    }

    fun setIncludeCutMarks(include: Boolean) {
        _uiState.update { it.copy(includeCutMarks = include) }
    }

    fun setCutMarkColorType(type: Int) {
        _uiState.update { it.copy(cutMarkColorType = type) }
    }

    private fun recalculateGrid() {
        val s = _uiState.value
        val grid = GridMatrixCalculator.computeGrid(
            paper = s.selectedPaper,
            photoWidthMm = s.selectedTemplate.widthMm,
            photoHeightMm = s.selectedTemplate.heightMm,
            isLandscape = s.isSheetLandscape,
            customCount = s.customSheetPhotoCount
        )
        _uiState.update { it.copy(computedGridResult = grid) }
    }

    fun generateMasterSheetBitmap(onComplete: () -> Unit = {}) {
        val photos = if (photoList.isNotEmpty()) {
            photoList.toList()
        } else {
            listOfNotNull(_uiState.value.activeBitmap)
        }
        if (photos.isEmpty()) return

        val s = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Generating ${selectedPaperSize.title} sheet at 300 DPI...",
                    errorMessage = null
                )
            }
            try {
                val sheetBmp = ImageProcessor.createMixedPrintSheet(
                    photos = photos,
                    paperSize = selectedPaperSize,
                    template = s.selectedTemplate,
                    isLandscape = s.isSheetLandscape,
                    showBorders = showBorders,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    showScissorMarks = showScissorMarks,
                    customPhotoCount = s.customSheetPhotoCount
                )

                _uiState.update {
                    it.copy(
                        generatedSheetBitmap = sheetBmp,
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = "",
                        errorMessage = "Failed to render sheet: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ── 6. High-Resolution Export & Printing Pipeline ─────────────

    fun exportSheetImage(
        context: Context,
        format: ExportFormat,
        onSuccess: (ExportResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val sheetBitmap = _uiState.value.generatedSheetBitmap ?: _uiState.value.activeBitmap
        if (sheetBitmap == null) {
            onError("No print sheet available to export")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Exporting 300 DPI ${format.name}...") }
            try {
                val result = HighDpiExporter.exportImage(
                    context = context,
                    bitmap = sheetBitmap,
                    format = format,
                    qualityPercent = 100,
                    dpi = 300
                )
                _uiState.update { it.copy(isLoading = false, loadingMessage = "") }
                onSuccess(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadingMessage = "", errorMessage = e.localizedMessage) }
                onError(e.localizedMessage ?: "Failed to export image")
            }
        }
    }

    fun exportSheetPdf(
        context: Context,
        onSuccess: (PdfExportResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val sheetBitmap = _uiState.value.generatedSheetBitmap ?: _uiState.value.activeBitmap
        if (sheetBitmap == null) {
            onError("No print sheet available to generate PDF")
            return
        }

        val paper = _uiState.value.selectedPaper
        val isLandscape = _uiState.value.isSheetLandscape

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Generating exact physical scale PDF...") }
            try {
                val result = PdfDocumentGenerator.generatePdf(
                    context = context,
                    sheetBitmap = sheetBitmap,
                    paper = paper,
                    isLandscape = isLandscape
                )
                _uiState.update { it.copy(isLoading = false, loadingMessage = "") }
                onSuccess(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadingMessage = "", errorMessage = e.localizedMessage) }
                onError(e.localizedMessage ?: "Failed to generate PDF")
            }
        }
    }

    fun printSheetDirect(context: Context): Boolean {
        val sheetBitmap = _uiState.value.generatedSheetBitmap ?: _uiState.value.activeBitmap ?: return false
        val paper = _uiState.value.selectedPaper
        val isLandscape = _uiState.value.isSheetLandscape
        return WifiPrintHelper.printSheet(
            context = context,
            sheetBitmap = sheetBitmap,
            paper = paper,
            isLandscape = isLandscape
        )
    }

    // ── Rotation & Utilities ───────────────────────────────────────

    fun rotatePhotoClockwise() {
        val currentBitmap = _uiState.value.activeBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Rotating...") }
            val rotated = withContext(Dispatchers.Default) {
                ImageUtils.rotateBitmapClockwise(currentBitmap)
            }
            pushUndoSnapshot(currentBitmap)
            _uiState.update {
                it.copy(
                    activeBitmap = rotated,
                    photoWidth = rotated.width,
                    photoHeight = rotated.height,
                    rotationAngle = (it.rotationAngle + 90) % 360,
                    isLoading = false,
                    loadingMessage = ""
                )
            }
            recalculateGrid()
        }
    }

    fun selectTemplate(template: PhotoSpec) {
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                cropPreset = CropRatioPreset.fromPhotoSpec(template)
            )
        }
        recalculateGrid()
    }

    fun clearPhoto() {
        clearHistory()
        _uiState.update {
            it.copy(
                photoUri = null,
                originalSourceUri = null,
                activeBitmap = null,
                photoWidth = 0,
                photoHeight = 0,
                fileSizeBytes = 0,
                rotationAngle = 0,
                toneAdjustments = ToneAdjustments(),
                currentBackdrop = StudioBackdrop.ORIGINAL,
                examStampConfig = ExamStampConfig(),
                unsegmentedBitmap = null,
                currentMask = null,
                generatedSheetBitmap = null
            )
        }
    }

    private fun clearHistory() {
        while (undoStack.isNotEmpty()) {
            val bmp = undoStack.pop()
            if (!bmp.isRecycled && bmp != _uiState.value.activeBitmap) bmp.recycle()
        }
        while (redoStack.isNotEmpty()) {
            val bmp = redoStack.pop()
            if (!bmp.isRecycled && bmp != _uiState.value.activeBitmap) bmp.recycle()
        }
        updateUndoRedoAvailability()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        clearHistory()
        val bitmap = _uiState.value.activeBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        val sheet = _uiState.value.generatedSheetBitmap
        if (sheet != null && !sheet.isRecycled) {
            sheet.recycle()
        }
    }
}
