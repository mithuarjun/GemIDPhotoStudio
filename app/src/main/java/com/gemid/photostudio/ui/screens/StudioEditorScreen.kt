package com.gemid.photostudio.ui.screens

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.editor.CropCalculator
import com.gemid.photostudio.editor.CropRatioPreset
import com.gemid.photostudio.editor.InteractiveCropper
import com.gemid.photostudio.editor.ToneAdjustments
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import com.gemid.photostudio.util.ImageUtils
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pro-Grade Studio Editor Screen:
 * 1. Live Canva/Photoshop-style editing workspace with real-time sliders for Brightness,
 *    Contrast, Warmth, Sharpness, and Skin Smoothing / Beautify.
 * 2. Multi-unit custom dimensions (px, mm, cm, in) with instant 300 DPI math conversion.
 * 3. Indian market priority presets (Passport 35x45mm, Stamp 20x25mm, SSC/UPSC, PAN 25x35mm).
 * 4. Bulletproof one-tap reset engine restoring raw pristine photo.
 * 5. Offline ML Kit background tools (Solid White, Sky Blue, Reset).
 */
enum class DimensionUnit(val symbol: String, val title: String) {
    PX("px", "Pixels"),
    MM("mm", "Millimeters"),
    CM("cm", "Centimeters"),
    IN("in", "Inches")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioEditorScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val isLoading = viewModel.isLoading
    val snackbarHostState = remember { SnackbarHostState() }

    var isCropMode by remember { mutableStateOf(false) }
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentPanOffset by remember { mutableStateOf(Offset.Zero) }
    var currentCropRect by remember { mutableStateOf(Rect.Zero) }
    var currentCanvasSize by remember { mutableStateOf(Size.Zero) }

    var activeToolTab by remember { mutableIntStateOf(0) } // 0: Size & Template, 1: Beautify & Tone, 2: Background
    var showCustomInput by remember { mutableStateOf(viewModel.isCustomSizeEnabled) }
    var selectedUnit by remember { mutableStateOf(DimensionUnit.PX) }
    var inputWidth by remember { mutableStateOf(viewModel.customWidthPx) }
    var inputHeight by remember { mutableStateOf(viewModel.customHeightPx) }

    // Pro-Grade Sliders State
    var sliderBrightness by remember { mutableFloatStateOf(viewModel.toneAdjustments.brightness) }
    var sliderContrast by remember { mutableFloatStateOf(viewModel.toneAdjustments.contrast) }
    var sliderWarmth by remember { mutableFloatStateOf(viewModel.toneAdjustments.warmth) }
    var sliderSharpness by remember { mutableFloatStateOf(viewModel.toneAdjustments.sharpness) }
    var sliderSkinSmooth by remember { mutableFloatStateOf(viewModel.toneAdjustments.skinSmooth) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.loadPhoto(context, tempCameraUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadPhoto(context, uri)
        }
    }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.statusMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Photo Studio Editor",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        val spec = viewModel.getActivePhotoSpec()
                        Text(
                            text = "${spec.title} • ${spec.dimensionMmLabel} (300 DPI)",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy10),
                actions = {
                    Button(
                        onClick = {
                            if (editedBitmap != null) {
                                viewModel.generateMasterSheet()
                                navController.navigate(Screen.SheetLayout.route)
                            }
                        },
                        enabled = editedBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next: Sheet", color = Navy10, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next",
                            tint = Navy10,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Navy10
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Image Preview & Interactive Cropper Canvas ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Navy20)
                    .border(1.dp, Navy25, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(viewModel.loadingMessage, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (editedBitmap != null) {
                    if (isCropMode) {
                        InteractiveCropper(
                            bitmap = editedBitmap!!,
                            cropPreset = CropRatioPreset.fromPhotoSpec(viewModel.getActivePhotoSpec()),
                            modifier = Modifier.fillMaxSize(),
                            onTransformChange = { scale, offset, rect, size ->
                                currentScale = scale
                                currentPanOffset = offset
                                currentCropRect = rect
                                currentCanvasSize = size
                            }
                        )

                        // Floating Crop Confirmation Bar
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentCropRect.width > 0 && currentCropRect.height > 0) {
                                        val cropped = CropCalculator.performCrop(
                                            source = editedBitmap!!,
                                            cropRect = currentCropRect,
                                            canvasSize = currentCanvasSize,
                                            scale = currentScale,
                                            panOffset = currentPanOffset
                                        )
                                        viewModel.applyCroppedBitmap(cropped)
                                    }
                                    isCropMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = "Apply", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply Crop", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { isCropMode = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Studio Preview Mode
                        Image(
                            bitmap = editedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )

                        // Toggle Crop Button
                        Surface(
                            shape = CircleShape,
                            color = Navy10.copy(alpha = 0.88f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clickable { isCropMode = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Crop, contentDescription = "Crop", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crop / Align", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Empty state fallback
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No photo loaded", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val file = ImageUtils.createCaptureFile(context)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = "Camera", tint = Navy10, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", color = Navy10, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy25),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Studio Tools Tab Bar (Canva/Photoshop Style) ───────────────
            TabRow(
                selectedTabIndex = activeToolTab,
                containerColor = Navy15,
                contentColor = AccentCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeToolTab]),
                        color = AccentCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Navy25, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeToolTab == 0,
                    onClick = { activeToolTab = 0 },
                    text = { Text("Sizes & Presets", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = activeToolTab == 1,
                    onClick = { activeToolTab = 1 },
                    text = { Text("Beautify & Tone", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = activeToolTab == 2,
                    onClick = { activeToolTab = 2 },
                    text = { Text("Background", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── TAB 0: Indian Priority Templates & Multi-Unit Custom Dimensions
            if (activeToolTab == 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Templates (Indian Standard First)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text("300 DPI", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val indianPriorityTemplates = listOf(
                            PassportTemplate.INDIAN_PASSPORT,
                            PassportTemplate.STAMP_SIZE,
                            PassportTemplate.SSC_UPSC_EXAM,
                            PassportTemplate.INDIAN_PAN,
                            PassportTemplate.US_PASSPORT
                        )

                        indianPriorityTemplates.forEach { template ->
                            val isSelected = viewModel.selectedTemplate == template && !viewModel.isCustomSizeEnabled
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    showCustomInput = false
                                    viewModel.setTemplate(template)
                                },
                                label = {
                                    Text(
                                        text = when (template) {
                                            PassportTemplate.INDIAN_PASSPORT -> "🇮🇳 Passport (35×45mm)"
                                            PassportTemplate.STAMP_SIZE -> "🇮🇳 Stamp (20×25mm)"
                                            PassportTemplate.SSC_UPSC_EXAM -> "🇮🇳 SSC/UPSC (35×45mm)"
                                            PassportTemplate.INDIAN_PAN -> "🇮🇳 PAN (25×35mm)"
                                            PassportTemplate.US_PASSPORT -> "🇺🇸 US (2×2\")"
                                            else -> template.title
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Navy15,
                                    labelColor = Color.LightGray,
                                    selectedContainerColor = AccentCyan,
                                    selectedLabelColor = Navy10
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Navy25,
                                    selectedBorderColor = AccentCyan
                                )
                            )
                        }

                        // Custom Size Chip
                        FilterChip(
                            selected = viewModel.isCustomSizeEnabled || showCustomInput,
                            onClick = { showCustomInput = true },
                            label = {
                                Text(
                                    text = "Custom (px/mm/cm/in)",
                                    fontWeight = if (viewModel.isCustomSizeEnabled) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Navy15,
                                labelColor = Color.LightGray,
                                selectedContainerColor = AccentCyan,
                                selectedLabelColor = Navy10
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = viewModel.isCustomSizeEnabled || showCustomInput,
                                borderColor = Navy25,
                                selectedBorderColor = AccentCyan
                            )
                        )
                    }

                    // Multi-Unit Custom Dimensions Input Panel
                    AnimatedVisibility(visible = showCustomInput) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Navy15,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DimensionUnit.entries.forEach { unit ->
                                        val isUnitSelected = selectedUnit == unit
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (selectedUnit != unit) {
                                                        val currValW = inputWidth.toFloatOrNull() ?: 413f
                                                        val currValH = inputHeight.toFloatOrNull() ?: 531f
                                                        val currPxW = when (selectedUnit) {
                                                            DimensionUnit.PX -> currValW
                                                            DimensionUnit.MM -> (currValW / 25.4f) * 300f
                                                            DimensionUnit.CM -> (currValW / 2.54f) * 300f
                                                            DimensionUnit.IN -> currValW * 300f
                                                        }
                                                        val currPxH = when (selectedUnit) {
                                                            DimensionUnit.PX -> currValH
                                                            DimensionUnit.MM -> (currValH / 25.4f) * 300f
                                                            DimensionUnit.CM -> (currValH / 2.54f) * 300f
                                                            DimensionUnit.IN -> currValH * 300f
                                                        }

                                                        inputWidth = when (unit) {
                                                            DimensionUnit.PX -> currPxW.roundToInt().toString()
                                                            DimensionUnit.MM -> String.format(Locale.US, "%.1f", (currPxW / 300f) * 25.4f)
                                                            DimensionUnit.CM -> String.format(Locale.US, "%.2f", (currPxW / 300f) * 2.54f)
                                                            DimensionUnit.IN -> String.format(Locale.US, "%.2f", currPxW / 300f)
                                                        }
                                                        inputHeight = when (unit) {
                                                            DimensionUnit.PX -> currPxH.roundToInt().toString()
                                                            DimensionUnit.MM -> String.format(Locale.US, "%.1f", (currPxH / 300f) * 25.4f)
                                                            DimensionUnit.CM -> String.format(Locale.US, "%.2f", (currPxH / 300f) * 2.54f)
                                                            DimensionUnit.IN -> String.format(Locale.US, "%.2f", currPxH / 300f)
                                                        }
                                                        selectedUnit = unit
                                                    }
                                                }
                                                .border(
                                                    width = if (isUnitSelected) 1.5.dp else 1.dp,
                                                    color = if (isUnitSelected) AccentCyan else Navy25,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            color = if (isUnitSelected) AccentCyan.copy(alpha = 0.2f) else Navy20
                                        ) {
                                            Text(
                                                text = unit.symbol.uppercase(),
                                                color = if (isUnitSelected) AccentCyan else Color.White,
                                                fontWeight = if (isUnitSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputWidth,
                                        onValueChange = { inputWidth = it },
                                        label = { Text("Width (${selectedUnit.symbol})", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = Navy25
                                        )
                                    )

                                    OutlinedTextField(
                                        value = inputHeight,
                                        onValueChange = { inputHeight = it },
                                        label = { Text("Height (${selectedUnit.symbol})", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = Navy25
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val valW = inputWidth.toFloatOrNull() ?: 413f
                                            val valH = inputHeight.toFloatOrNull() ?: 531f
                                            val targetPxW = when (selectedUnit) {
                                                DimensionUnit.PX -> valW.roundToInt()
                                                DimensionUnit.MM -> ((valW / 25.4f) * 300f).roundToInt()
                                                DimensionUnit.CM -> ((valW / 2.54f) * 300f).roundToInt()
                                                DimensionUnit.IN -> (valW * 300f).roundToInt()
                                            }.coerceAtLeast(60)

                                            val targetPxH = when (selectedUnit) {
                                                DimensionUnit.PX -> valH.roundToInt()
                                                DimensionUnit.MM -> ((valH / 25.4f) * 300f).roundToInt()
                                                DimensionUnit.CM -> ((valH / 2.54f) * 300f).roundToInt()
                                                DimensionUnit.IN -> (valH * 300f).roundToInt()
                                            }.coerceAtLeast(60)

                                            viewModel.setCustomSize(targetPxW.toString(), targetPxH.toString())
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("Apply", color = Navy10, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── TAB 1: Pro-Grade Tone Sliders & Skin Beautify ─────────────
            if (activeToolTab == 1) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Navy15,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Studio Skin Beautify & Tone Balancing",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Skin Smoothing / Beautify Slider
                        ProSlider(
                            title = "Skin Smooth / Beautify",
                            value = sliderSkinSmooth,
                            range = 0f..1f,
                            displayValue = "${(sliderSkinSmooth * 100).toInt()}%",
                            onValueChange = {
                                sliderSkinSmooth = it
                                viewModel.updateToneAdjustments(
                                    viewModel.toneAdjustments.copy(skinSmooth = it)
                                )
                            }
                        )

                        // Brightness & Exposure Slider
                        ProSlider(
                            title = "Brightness & Exposure",
                            value = sliderBrightness,
                            range = -40f..40f,
                            displayValue = if (sliderBrightness > 0) "+${sliderBrightness.toInt()}" else "${sliderBrightness.toInt()}",
                            onValueChange = {
                                sliderBrightness = it
                                viewModel.updateToneAdjustments(
                                    viewModel.toneAdjustments.copy(brightness = it)
                                )
                            }
                        )

                        // Contrast Slider
                        ProSlider(
                            title = "Contrast & Depth",
                            value = sliderContrast,
                            range = 0.6f..1.5f,
                            displayValue = "${String.format(Locale.US, "%.2f", sliderContrast)}x",
                            onValueChange = {
                                sliderContrast = it
                                viewModel.updateToneAdjustments(
                                    viewModel.toneAdjustments.copy(contrast = it)
                                )
                            }
                        )

                        // Color Warmth Slider (Skin Tone Balancing)
                        ProSlider(
                            title = "Color Warmth (Skin Balancing)",
                            value = sliderWarmth,
                            range = -35f..35f,
                            displayValue = if (sliderWarmth > 0) "Warm +${sliderWarmth.toInt()}" else if (sliderWarmth < 0) "Cool ${sliderWarmth.toInt()}" else "Neutral",
                            onValueChange = {
                                sliderWarmth = it
                                viewModel.updateToneAdjustments(
                                    viewModel.toneAdjustments.copy(warmth = it)
                                )
                            }
                        )

                        // Sharpness Enhancement Slider
                        ProSlider(
                            title = "Sharpness & Clarity",
                            value = sliderSharpness,
                            range = 0f..1f,
                            displayValue = "${(sliderSharpness * 100).toInt()}%",
                            onValueChange = {
                                sliderSharpness = it
                                viewModel.updateToneAdjustments(
                                    viewModel.toneAdjustments.copy(sharpness = it)
                                )
                            }
                        )
                    }
                }
            }

            // ── TAB 2: Studio Background Tools ────────────────────────────
            if (activeToolTab == 2) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Navy15,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Offline ML Kit Background Replacement",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.changeBackground(AndroidColor.WHITE) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Solid White", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.changeBackground(AndroidColor.parseColor("#B0C4DE")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C4DE)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sky Blue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.changeBackground(AndroidColor.parseColor("#38BDF8")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Passport Blue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Bulletproof One-Tap Reset Button ──────────────────────────
            OutlinedButton(
                onClick = {
                    viewModel.resetImage()
                    isCropMode = false
                    currentScale = 1f
                    currentPanOffset = Offset.Zero
                    currentCropRect = Rect.Zero
                    selectedUnit = DimensionUnit.PX
                    inputWidth = "413"
                    inputHeight = "531"
                    showCustomInput = false
                    sliderBrightness = 0f
                    sliderContrast = 1f
                    sliderWarmth = 0f
                    sliderSharpness = 0f
                    sliderSkinSmooth = 0f
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset All", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset All Edits, Crops & Filters", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(displayValue, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = Navy25
            )
        )
    }
}
