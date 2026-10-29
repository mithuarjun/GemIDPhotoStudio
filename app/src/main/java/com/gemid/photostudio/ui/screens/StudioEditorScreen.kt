package com.gemid.photostudio.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.gemid.photostudio.editor.CropCalculator
import com.gemid.photostudio.editor.CropRatioPreset
import com.gemid.photostudio.editor.ExamStampConfig
import com.gemid.photostudio.editor.InteractiveCropper
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.editor.ToneAdjustments
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.ui.components.ToolbarButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioEditorScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedToolIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Cropper State references
    var currentCropScale by remember { mutableFloatStateOf(1f) }
    var currentCropOffset by remember { mutableStateOf(Offset.Zero) }
    var currentCropRect by remember { mutableStateOf(Rect.Zero) }
    var currentCanvasSize by remember { mutableStateOf(Size.Zero) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromGallery(uri)
        }
    }

    val tools = listOf(
        "Crop" to Icons.Rounded.Crop,
        "Backdrop" to Icons.Rounded.Wallpaper,
        "Refine" to Icons.Rounded.Brush,
        "Tone" to Icons.Rounded.Tune,
        "Exam Stamp" to Icons.Rounded.Badge,
        "Rotate" to Icons.Rounded.RotateRight
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Studio Editor",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${uiState.selectedTemplate.title} (${uiState.selectedTemplate.widthMm.toInt()}×${uiState.selectedTemplate.heightMm.toInt()}mm)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            Icons.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) AccentCyan else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            Icons.Rounded.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) AccentCyan else Color.White.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Next / Sheet Hub
                    FilledTonalButton(
                        onClick = {
                            if (!uiState.toneAdjustments.isDefault) {
                                viewModel.bakeToneAdjustments()
                            }
                            if (uiState.examStampConfig.isEnabled) {
                                viewModel.applyExamStamp()
                            }
                            val currentBmp = uiState.activeBitmap
                            if (currentBmp != null && viewModel.photoList.isEmpty()) {
                                viewModel.addPhotoToQueue(currentBmp)
                            }
                            viewModel.generateMasterSheetBitmap()
                            navController.navigate(Screen.SheetLayout.route)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Next", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeBitmap = uiState.activeBitmap

            // ── Canvas / Interactive Workspace ──────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy15)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (activeBitmap != null && !activeBitmap.isRecycled) {
                    if (selectedToolIndex == 0) {
                        // 1. Interactive Cropper View (Pinch-to-zoom & pan)
                        InteractiveCropper(
                            bitmap = activeBitmap,
                            cropPreset = uiState.cropPreset,
                            modifier = Modifier.fillMaxSize(),
                            onTransformChange = { scale, offset, rect, size ->
                                currentCropScale = scale
                                currentCropOffset = offset
                                currentCropRect = rect
                                currentCanvasSize = size
                            }
                        )
                    } else {
                        // 2. Standard Studio Canvas with Live Tone Filter & Stamp Preview
                        val toneMatrix = remember(uiState.toneAdjustments) {
                            uiState.toneAdjustments.toComposeColorMatrix()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .aspectRatio(uiState.selectedTemplate.aspectRatio)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(2.dp, AccentCyan, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Studio Photo Canvas",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (uiState.toneAdjustments.isDefault) null else ColorFilter.colorMatrix(toneMatrix)
                            )

                            // Live Exam Stamp Overlay preview
                            if (uiState.examStampConfig.isEnabled) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            if (uiState.examStampConfig.isDarkBanner) Color.Black.copy(alpha = 0.82f)
                                            else Color.White.copy(alpha = 0.94f)
                                        )
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val textColor = if (uiState.examStampConfig.isDarkBanner) Color.White else Color.Black
                                        if (uiState.examStampConfig.candidateName.isNotBlank()) {
                                            Text(
                                                text = "NAME: ${uiState.examStampConfig.candidateName.uppercase()}",
                                                color = textColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (uiState.examStampConfig.photoDate.isNotBlank()) {
                                            Text(
                                                text = "DOP: ${uiState.examStampConfig.photoDate.uppercase()}",
                                                color = textColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Top-left resolution badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Navy10.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.photoWidth}×${uiState.photoHeight}px",
                                    fontSize = 10.sp,
                                    color = Neutral90,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Floating quick replace / retake buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Navy10.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { navController.navigate(Screen.Camera.route) }
                            ) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    contentDescription = "Retake",
                                    tint = AccentCyan,
                                    modifier = Modifier.padding(10.dp).size(20.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Navy10.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = AccentCyan,
                                    modifier = Modifier.padding(10.dp).size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Navy25)
                                .border(1.5.dp, AccentCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = AccentCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Photo Loaded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral90
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Snap a photo with camera guide\nor import a portrait from your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral80.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { navController.navigate(Screen.Camera.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Photo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("From Gallery")
                            }
                        }
                    }
                }
            }

            // ── Tool Adjustment Panels ──────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy20)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (selectedToolIndex) {
                        0 -> {
                            // ── 1. Interactive Cropper Controls ──────
                            CropperControlsPanel(
                                activeBitmap = activeBitmap,
                                selectedPreset = uiState.cropPreset,
                                onSelectPreset = { viewModel.setCropPreset(it) },
                                onApplyCrop = {
                                    if (activeBitmap != null && currentCanvasSize.width > 0f) {
                                        val cropped = CropCalculator.performCrop(
                                            source = activeBitmap,
                                            cropRect = currentCropRect,
                                            canvasSize = currentCanvasSize,
                                            scale = currentCropScale,
                                            panOffset = currentCropOffset
                                        )
                                        viewModel.applyCroppedBitmap(cropped)
                                    }
                                }
                            )
                        }

                        1 -> {
                            // ── 2. Background Replacement Engine ─────
                            BackdropControlsPanel(
                                currentBackdrop = uiState.currentBackdrop,
                                onSelectBackdrop = { viewModel.replaceBackground(it) },
                                onOpenRefineBrush = { navController.navigate(Screen.BackgroundBrush.route) }
                            )
                        }

                        2 -> {
                            // ── 3. Manual Mask Refinement Tool ───────
                            RefineBrushLauncherPanel(
                                onLaunch = { navController.navigate(Screen.BackgroundBrush.route) }
                            )
                        }

                        3 -> {
                            // ── 4. Tone & Color Adjustments ──────────
                            ToneControlsPanel(
                                adjustments = uiState.toneAdjustments,
                                onUpdate = { viewModel.updateToneAdjustments(it) },
                                onReset = { viewModel.resetToneAdjustments() },
                                onBake = { viewModel.bakeToneAdjustments() }
                            )
                        }

                        4 -> {
                            // ── 5. Indian Exam Name & Date Banner ────
                            ExamStampControlsPanel(
                                config = uiState.examStampConfig,
                                onUpdate = { viewModel.updateExamStampConfig(it) },
                                onApply = { viewModel.applyExamStamp() }
                            )
                        }

                        5 -> {
                            // ── 6. Rotation ──────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Orientation & Rotation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                    Text(
                                        text = "Current angle: ${uiState.rotationAngle}°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Neutral80
                                    )
                                }
                                Button(
                                    onClick = { viewModel.rotatePhotoClockwise() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = activeBitmap != null
                                ) {
                                    Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+90°", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Bottom Navigation Toolbar ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tools.forEachIndexed { index, (label, icon) ->
                    ToolbarButton(
                        label = label,
                        icon = icon,
                        isSelected = selectedToolIndex == index,
                        onClick = { selectedToolIndex = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Loading indicator dialog
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Blue60)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.loadingMessage.ifEmpty { "Processing..." },
                            color = Neutral90,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-Panels for Editing Suite ─────────────────────────────────────

@Composable
private fun CropperControlsPanel(
    activeBitmap: Bitmap?,
    selectedPreset: CropRatioPreset,
    onSelectPreset: (CropRatioPreset) -> Unit,
    onApplyCrop: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Crop Format",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )

            Button(
                onClick = onApplyCrop,
                colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                enabled = activeBitmap != null
            ) {
                Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply Crop", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CropRatioPreset.entries.toTypedArray()) { preset ->
                val isSelected = selectedPreset == preset
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Blue60 else Navy25,
                    modifier = Modifier.clickable { onSelectPreset(preset) }
                ) {
                    Text(
                        text = "${preset.title} (${preset.dimensionsLabel})",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Neutral80,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackdropControlsPanel(
    currentBackdrop: StudioBackdrop,
    onSelectBackdrop: (StudioBackdrop) -> Unit,
    onOpenRefineBrush: () -> Unit
) {
    Column {
        Text(
            text = "Instant Background Replacement",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AccentCyan
        )
        Text(
            text = "AI portrait segmentation with clean solid studio backdrops",
            style = MaterialTheme.typography.bodySmall,
            color = Neutral80
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StudioBackdrop.entries.toTypedArray()) { backdrop ->
                val isSelected = currentBackdrop == backdrop
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Blue60 else Navy25,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentCyan else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.clickable { onSelectBackdrop(backdrop) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (backdrop != StudioBackdrop.ORIGINAL && backdrop != StudioBackdrop.TRANSPARENT) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(backdrop.colorArgb))
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = backdrop.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Neutral80
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Refine Brush Launcher Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Navy15,
            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenRefineBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Brush,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Manual Refine Brush (Erase & Restore)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Touch-paint to fix clipped hair, ears, or stray background",
                            fontSize = 10.sp,
                            color = Neutral80
                        )
                    }
                }
                Icon(
                    Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RefineBrushLauncherPanel(
    onLaunch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Manual Background Refinement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
            Text(
                text = "Touch-paint to erase background or restore clipped hair strands, ears & shoulders.",
                style = MaterialTheme.typography.bodySmall,
                color = Neutral80
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onLaunch,
            colors = ButtonDefaults.buttonColors(containerColor = Blue60),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Brush", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToneControlsPanel(
    adjustments: ToneAdjustments,
    onUpdate: (ToneAdjustments) -> Unit,
    onReset: () -> Unit,
    onBake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color & Tone Adjustments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = Neutral80)
                }
                FilledTonalButton(
                    onClick = onBake,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Blue50),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Bake", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Exposure Slider
        ToneSliderRow(
            label = "Exposure",
            value = adjustments.exposure,
            range = -1.0f..1.0f,
            valueFormat = "%.2f",
            onValueChange = { onUpdate(adjustments.copy(exposure = it)) }
        )

        // Brightness Slider
        ToneSliderRow(
            label = "Brightness",
            value = adjustments.brightness,
            range = -60.0f..60.0f,
            valueFormat = "%.0f",
            onValueChange = { onUpdate(adjustments.copy(brightness = it)) }
        )

        // Contrast Slider
        ToneSliderRow(
            label = "Contrast",
            value = adjustments.contrast,
            range = 0.6f..1.8f,
            valueFormat = "%.2f",
            onValueChange = { onUpdate(adjustments.copy(contrast = it)) }
        )

        // Saturation Slider
        ToneSliderRow(
            label = "Saturation",
            value = adjustments.saturation,
            range = 0.0f..2.0f,
            valueFormat = "%.2f",
            onValueChange = { onUpdate(adjustments.copy(saturation = it)) }
        )

        // Warmth / Temperature Slider
        ToneSliderRow(
            label = "Warmth",
            value = adjustments.warmth,
            range = -40.0f..40.0f,
            valueFormat = "%.0f",
            onValueChange = { onUpdate(adjustments.copy(warmth = it)) }
        )
    }
}

@Composable
private fun ToneSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormat: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Neutral80,
            modifier = Modifier.width(72.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = Blue50,
                inactiveTrackColor = Navy30
            )
        )
        Text(
            text = String.format(valueFormat, value),
            fontSize = 11.sp,
            color = Neutral90,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ExamStampControlsPanel(
    config: ExamStampConfig,
    onUpdate: (ExamStampConfig) -> Unit,
    onApply: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Indian Exam Name & Date Banner",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                Text(
                    text = "SSC / UPSC official bottom strip",
                    style = MaterialTheme.typography.bodySmall,
                    color = Neutral80
                )
            }

            Switch(
                checked = config.isEnabled,
                onCheckedChange = { onUpdate(config.copy(isEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Blue60
                )
            )
        }

        if (config.isEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = config.candidateName,
                    onValueChange = { onUpdate(config.copy(candidateName = it)) },
                    label = { Text("Candidate Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. RAHUL SHARMA", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Neutral80.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = config.photoDate,
                    onValueChange = { onUpdate(config.copy(photoDate = it)) },
                    label = { Text("Date of Photo", fontSize = 11.sp) },
                    placeholder = { Text("DD/MM/YYYY", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Neutral80.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onUpdate(config.copy(isDarkBanner = !config.isDarkBanner))
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (config.isDarkBanner) Color.Black else Color.White,
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.size(18.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (config.isDarkBanner) "Black Strip (White text)" else "White Strip (Black text)",
                        fontSize = 11.sp,
                        color = Neutral90
                    )
                }

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Bake Banner", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
