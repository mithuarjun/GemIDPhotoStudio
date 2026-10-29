package com.gemid.photostudio.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.editor.BackgroundEngine
import com.gemid.photostudio.editor.MaskRefinerEngine
import com.gemid.photostudio.editor.RefineBrushMode
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue50
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.theme.Navy30
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MaskViewMode(val title: String) {
    SIDE_BY_SIDE("Split View"),
    SEGMENTED("Composite"),
    ALPHA_MASK("Alpha Mask"),
    RUBY_OVERLAY("Ruby Overlay")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundBrushScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    val sourcePhoto = uiState.unsegmentedBitmap ?: uiState.activeBitmap

    if (sourcePhoto == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy10),
            contentAlignment = Alignment.Center
        ) {
            Text("No active photo to refine", color = Color.White)
        }
        return
    }

    // Engine initialization state
    var refinerEngine by remember { mutableStateOf<MaskRefinerEngine?>(null) }
    var initialMaskBackup by remember { mutableStateOf<ByteArray?>(null) }
    var isInitializing by remember { mutableStateOf(true) }

    // Brush Tool parameters
    var activeBrushMode by remember { mutableStateOf(RefineBrushMode.ERASE) }
    var brushRadius by remember { mutableFloatStateOf(28f) }
    var brushHardness by remember { mutableFloatStateOf(0.80f) }
    var currentViewMode by remember { mutableStateOf(MaskViewMode.SIDE_BY_SIDE) }
    var selectedBackdropColor by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var isPanZoomMode by remember { mutableStateOf(false) }

    // Transformation state for Canvas zooming and panning
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Live touch position for drawing cursor ring
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }

    // Revision trigger for forcing 60fps Compose canvas re-draws without recreating Bitmaps
    var canvasRevision by remember { mutableIntStateOf(0) }

    // Undo / Redo availability
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    // Synchronize initial mask on screen entry
    LaunchedEffect(sourcePhoto) {
        isInitializing = true
        withContext(Dispatchers.Default) {
            val mask = uiState.currentMask ?: BackgroundEngine.generateForegroundMask(sourcePhoto)
            initialMaskBackup = mask.clone()
            val engine = MaskRefinerEngine(
                sourceBitmap = sourcePhoto,
                initialMask = mask,
                backdropColor = selectedBackdropColor
            )
            refinerEngine = engine
            canUndo = engine.canUndo
            canRedo = engine.canRedo
        }
        isInitializing = false
    }

    fun updateUndoRedoState() {
        refinerEngine?.let {
            canUndo = it.canUndo
            canRedo = it.canRedo
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manual Mask Refine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeBrushMode == RefineBrushMode.ERASE) "Erase Brush: Paint over background" else "Restore Brush: Recover hair & shoulders",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeBrushMode == RefineBrushMode.ERASE) Color(0xFFFB7185) else AccentCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            refinerEngine?.let {
                                if (it.undo()) {
                                    canvasRevision++
                                    updateUndoRedoState()
                                }
                            }
                        },
                        enabled = canUndo
                    ) {
                        Icon(
                            Icons.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) AccentCyan else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            refinerEngine?.let {
                                if (it.redo()) {
                                    canvasRevision++
                                    updateUndoRedoState()
                                }
                            }
                        },
                        enabled = canRedo
                    ) {
                        Icon(
                            Icons.Rounded.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) AccentCyan else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Reset to AI Mask
                    IconButton(
                        onClick = {
                            initialMaskBackup?.let { backup ->
                                refinerEngine?.let { engine ->
                                    engine.resetMask(backup)
                                    canvasRevision++
                                    updateUndoRedoState()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Mask reset to initial AI cutout")
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Mask",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Apply Button
                    Button(
                        onClick = {
                            refinerEngine?.let { engine ->
                                val refined = engine.compositeBitmap.copy(Bitmap.Config.ARGB_8888, true)
                                viewModel.applyRefinedMaskBitmap(refined, engine.mask)
                                navController.navigateUp()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy10)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (isInitializing || refinerEngine == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Navy10),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Extracting biometric alpha mask with ML Kit...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neutral80
                    )
                }
            }
            return@Scaffold
        }

        val engine = refinerEngine!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Navy10)
        ) {
            // ── 1. Top View Mode Chips (Split View / Composite / Mask / Ruby) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(MaskViewMode.entries.toTypedArray()) { mode ->
                        val isSelected = currentViewMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentViewMode = mode },
                            label = { Text(mode.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue60,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Pan & Zoom toggle
                IconButton(
                    onClick = { isPanZoomMode = !isPanZoomMode },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPanZoomMode) AccentCyan else Navy20,
                        contentColor = if (isPanZoomMode) Navy10 else Color.White
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isPanZoomMode) Icons.Rounded.PanTool else Icons.Rounded.Brush,
                        contentDescription = "Toggle Pan/Zoom vs Brush",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── 2. Interactive Touch Canvas ───────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF03070E))
                    .clip(RoundedCornerShape(0.dp))
            ) {
                // Interactive Dual / Canvas display
                RefinerInteractiveCanvas(
                    engine = engine,
                    viewMode = currentViewMode,
                    brushMode = activeBrushMode,
                    brushRadius = brushRadius,
                    brushHardness = brushHardness,
                    scale = scale,
                    panOffset = panOffset,
                    isPanZoomMode = isPanZoomMode,
                    revision = canvasRevision,
                    onTransformChange = { newScale, newPan ->
                        scale = newScale
                        panOffset = newPan
                    },
                    onStrokeApplied = {
                        canvasRevision++
                        updateUndoRedoState()
                    },
                    onTouchPointChange = { pt -> currentTouchPoint = pt }
                )

                // Floating Brush Mode & Size Indicator Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Navy15.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (activeBrushMode == RefineBrushMode.ERASE) Color(0xFFFB7185) else AccentCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPanZoomMode) "Pan & Zoom (2 fingers)" else "${if (activeBrushMode == RefineBrushMode.ERASE) "Erase" else "Restore"} • Radius ${brushRadius.toInt()}px",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Alongside Miniature Mask Preview (when in full single view)
                if (currentViewMode == MaskViewMode.SEGMENTED || currentViewMode == MaskViewMode.RUBY_OVERLAY) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .width(80.dp)
                            .aspectRatio(engine.width.toFloat() / engine.height.toFloat())
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .border(1.5.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { currentViewMode = MaskViewMode.ALPHA_MASK },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val dummyRev = canvasRevision
                            Image(
                                bitmap = engine.maskBitmap.asImageBitmap(),
                                contentDescription = "Alpha Mask",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "MASK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Yellow,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 3. Bottom Tool & Slider Control Console ───────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy20)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Tool Selection Buttons: Erase vs Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Erase Brush Button
                        BrushToolCard(
                            label = "Erase Brush",
                            subtitle = "Remove background / halos",
                            icon = Icons.Rounded.ContentCut,
                            isSelected = activeBrushMode == RefineBrushMode.ERASE,
                            activeColor = Color(0xFFFB7185),
                            onClick = {
                                activeBrushMode = RefineBrushMode.ERASE
                                isPanZoomMode = false
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Restore Brush Button
                        BrushToolCard(
                            label = "Restore Brush",
                            subtitle = "Recover clipped hair & clothes",
                            icon = Icons.Rounded.AutoFixHigh,
                            isSelected = activeBrushMode == RefineBrushMode.RESTORE,
                            activeColor = AccentCyan,
                            onClick = {
                                activeBrushMode = RefineBrushMode.RESTORE
                                isPanZoomMode = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Brush Radius Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brush Radius",
                            style = MaterialTheme.typography.labelSmall,
                            color = Neutral80
                        )
                        Text(
                            text = "${brushRadius.toInt()} px",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    Slider(
                        value = brushRadius,
                        onValueChange = { brushRadius = it },
                        valueRange = 6f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = Blue50,
                            inactiveTrackColor = Navy30
                        )
                    )

                    // Brush Hardness / Feather Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edge Feather (Softness)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Neutral80
                        )
                        Text(
                            text = if (brushHardness < 0.5f) "Soft / Feathered (Hair)" else "Sharp (Shoulder Edge)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    }

                    Slider(
                        value = brushHardness,
                        onValueChange = { brushHardness = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal.copy(alpha = 0.8f),
                            inactiveTrackColor = Navy30
                        )
                    )

                    // Studio Backdrop Preview Selector
                    Text(
                        text = "Composite Studio Backdrop Color",
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral80
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "White" to 0xFFFFFFFF.toInt(),
                            "Sky Blue" to 0xFF0077BE.toInt(),
                            "Light Blue" to 0xFF38BDF8.toInt(),
                            "Gray" to 0xFFE5E7EB.toInt(),
                            "Dark" to 0xFF0A1628.toInt()
                        ).forEach { (title, color) ->
                            val isSelected = selectedBackdropColor == color
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(color),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) AccentCyan else Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clickable {
                                        selectedBackdropColor = color
                                        engine.updateBackdropColor(color)
                                        canvasRevision++
                                    }
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Touch Canvas handling gesture-based zero-latency brush painting,
 * continuous touch interpolation, and dual synchronized side-by-side rendering.
 */
@Composable
private fun RefinerInteractiveCanvas(
    engine: MaskRefinerEngine,
    viewMode: MaskViewMode,
    brushMode: RefineBrushMode,
    brushRadius: Float,
    brushHardness: Float,
    scale: Float,
    panOffset: Offset,
    isPanZoomMode: Boolean,
    revision: Int,
    onTransformChange: (scale: Float, offset: Offset) -> Unit,
    onStrokeApplied: () -> Unit,
    onTouchPointChange: (Offset?) -> Unit
) {
    var lastTouchPos by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isPanZoomMode) {
                if (isPanZoomMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(0.5f, 6.0f)
                        val newOffset = Offset(panOffset.x + pan.x, panOffset.y + pan.y)
                        onTransformChange(newScale, newOffset)
                    }
                }
            }
            .pointerInput(isPanZoomMode, brushMode, brushRadius, brushHardness, scale, panOffset) {
                if (!isPanZoomMode) {
                    detectDragGestures(
                        onDragStart = { startPt ->
                            engine.beginStroke()
                            lastTouchPos = startPt
                            onTouchPointChange(startPt)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val curPt = change.position
                            onTouchPointChange(curPt)

                            val prevPt = lastTouchPos ?: curPt
                            lastTouchPos = curPt

                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()

                            // Map screen touch coordinates to bitmap pixel coordinates
                            val fromImg = screenToImageCoords(prevPt, canvasW, canvasH, engine.width, engine.height, scale, panOffset, viewMode)
                            val toImg = screenToImageCoords(curPt, canvasW, canvasH, engine.width, engine.height, scale, panOffset, viewMode)

                            if (fromImg != null && toImg != null) {
                                // Scale brush radius proportionally to image resolution
                                val imgScale = minOf(canvasW / engine.width, canvasH / engine.height) * scale
                                val imgBrushRadius = maxOf(2f, brushRadius / imgScale)

                                engine.applyStroke(
                                    fromX = fromImg.x,
                                    fromY = fromImg.y,
                                    toX = toImg.x,
                                    toY = toImg.y,
                                    radius = imgBrushRadius,
                                    mode = brushMode,
                                    hardness = brushHardness
                                )
                                onStrokeApplied()
                            }
                        },
                        onDragEnd = {
                            lastTouchPos = null
                            onTouchPointChange(null)
                        },
                        onDragCancel = {
                            lastTouchPos = null
                            onTouchPointChange(null)
                        }
                    )
                }
            }
    ) {
        val dummyRev = revision // Forces re-draw when revision counter increments

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val imgW = engine.width.toFloat()
            val imgH = engine.height.toFloat()

            when (viewMode) {
                MaskViewMode.SIDE_BY_SIDE -> {
                    // Split dual canvas: Left half = Composite, Right half = Alpha Mask
                    val halfW = canvasW / 2f
                    val fitScale = minOf(halfW / imgW, canvasH / imgH) * scale
                    val drawW = imgW * fitScale
                    val drawH = imgH * fitScale

                    // 1. Draw Left Pane (Composite with Backdrop)
                    val leftCenter = Offset(halfW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val leftDst = IntOffset((leftCenter.x - drawW / 2f).toInt(), (leftCenter.y - drawH / 2f).toInt())
                    drawImage(
                        image = engine.compositeBitmap.asImageBitmap(),
                        dstOffset = leftDst,
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )

                    // 2. Draw Right Pane (Black & White Alpha Mask)
                    val rightCenter = Offset(halfW + halfW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val rightDst = IntOffset((rightCenter.x - drawW / 2f).toInt(), (rightCenter.y - drawH / 2f).toInt())
                    drawImage(
                        image = engine.maskBitmap.asImageBitmap(),
                        dstOffset = rightDst,
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )

                    // Divider separator line
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(halfW, 0f),
                        end = Offset(halfW, canvasH),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                MaskViewMode.ALPHA_MASK -> {
                    val fitScale = minOf(canvasW / imgW, canvasH / imgH) * scale
                    val drawW = imgW * fitScale
                    val drawH = imgH * fitScale
                    val center = Offset(canvasW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val dst = IntOffset((center.x - drawW / 2f).toInt(), (center.y - drawH / 2f).toInt())

                    drawImage(
                        image = engine.maskBitmap.asImageBitmap(),
                        dstOffset = dst,
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }

                MaskViewMode.RUBY_OVERLAY -> {
                    val fitScale = minOf(canvasW / imgW, canvasH / imgH) * scale
                    val drawW = imgW * fitScale
                    val drawH = imgH * fitScale
                    val center = Offset(canvasW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val dst = IntOffset((center.x - drawW / 2f).toInt(), (center.y - drawH / 2f).toInt())

                    // Draw source photo then tint erased areas with translucent red
                    drawImage(
                        image = engine.compositeBitmap.asImageBitmap(),
                        dstOffset = dst,
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }

                MaskViewMode.SEGMENTED -> {
                    val fitScale = minOf(canvasW / imgW, canvasH / imgH) * scale
                    val drawW = imgW * fitScale
                    val drawH = imgH * fitScale
                    val center = Offset(canvasW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
                    val dst = IntOffset((center.x - drawW / 2f).toInt(), (center.y - drawH / 2f).toInt())

                    drawImage(
                        image = engine.compositeBitmap.asImageBitmap(),
                        dstOffset = dst,
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }
            }

            // Draw Real-time Brush Circle Cursor around touch point
            lastTouchPos?.let { pos ->
                drawCircle(
                    color = if (brushMode == RefineBrushMode.ERASE) Color(0xFFFB7185) else AccentCyan,
                    radius = brushRadius,
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 2.dp.toPx(),
                    center = pos
                )
            }
        }
    }
}

/**
 * Coordinate mapping helper: transforms screen Canvas coordinate to Bitmap pixel coordinates.
 */
private fun screenToImageCoords(
    screenPt: Offset,
    canvasW: Float,
    canvasH: Float,
    imgW: Int,
    imgH: Int,
    scale: Float,
    panOffset: Offset,
    viewMode: MaskViewMode
): Offset? {
    if (viewMode == MaskViewMode.SIDE_BY_SIDE) {
        val halfW = canvasW / 2f
        val fitScale = minOf(halfW / imgW, canvasH / imgH) * scale
        val drawW = imgW * fitScale
        val drawH = imgH * fitScale

        // Determine if touch is on left pane (Composite) or right pane (Alpha Mask)
        val center = if (screenPt.x < halfW) {
            Offset(halfW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
        } else {
            Offset(halfW + halfW / 2f + panOffset.x, canvasH / 2f + panOffset.y)
        }

        val left = center.x - drawW / 2f
        val top = center.y - drawH / 2f

        val imgX = (screenPt.x - left) / fitScale
        val imgY = (screenPt.y - top) / fitScale

        return Offset(imgX.coerceIn(0f, imgW.toFloat()), imgY.coerceIn(0f, imgH.toFloat()))
    } else {
        val fitScale = minOf(canvasW / imgW, canvasH / imgH) * scale
        val drawW = imgW * fitScale
        val drawH = imgH * fitScale

        val left = (canvasW / 2f + panOffset.x) - drawW / 2f
        val top = (canvasH / 2f + panOffset.y) - drawH / 2f

        val imgX = (screenPt.x - left) / fitScale
        val imgY = (screenPt.y - top) / fitScale

        return Offset(imgX.coerceIn(0f, imgW.toFloat()), imgY.coerceIn(0f, imgH.toFloat()))
    }
}

@Composable
private fun BrushToolCard(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.2f) else Navy25
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeColor else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) activeColor else Navy30),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else Neutral80,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) activeColor else Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Neutral80,
                    fontSize = 10.sp
                )
            }
        }
    }
}
