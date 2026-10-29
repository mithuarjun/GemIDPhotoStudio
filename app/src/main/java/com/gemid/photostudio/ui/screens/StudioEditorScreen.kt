package com.gemid.photostudio.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import com.gemid.photostudio.util.ImageUtils

/**
 * Instant Edit Mode Screen:
 * - Direct visual feedback of normalized photo.
 * - Horizontal template selector (Indian Passport, Stamp Size, SSC/UPSC, US Passport) + Custom Pixel Size.
 * - Offline ML Kit background change (Solid White, Sky Blue, Reset).
 * - Clean interactive crop / zoom canvas.
 */
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

    var showCustomInput by remember { mutableStateOf(viewModel.isCustomSizeEnabled) }
    var inputWidth by remember { mutableStateOf(viewModel.customWidthPx) }
    var inputHeight by remember { mutableStateOf(viewModel.customHeightPx) }

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
                        Text("Edit Photo & Size", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (viewModel.isCustomSizeEnabled) "Custom ${viewModel.customWidthPx}×${viewModel.customHeightPx} px"
                            else viewModel.selectedTemplate.title,
                            color = AccentCyan,
                            fontSize = 11.sp
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
                            imageVector = Icons.Rounded.ArrowForward,
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Image Preview & Interactive Cropper Canvas ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Navy20)
                    .border(1.dp, Navy25, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(viewModel.loadingMessage, color = Color.White, fontSize = 12.sp)
                    }
                } else if (editedBitmap != null) {
                    if (isCropMode) {
                        // Interactive Cropper Canvas
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
                        // Clean Preview Mode
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
                            color = Navy10.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clickable { isCropMode = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Crop, contentDescription = "Crop", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crop / Align", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Empty state: Quick Camera/Gallery pick buttons
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
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = "Camera", tint = Navy10, modifier = Modifier.size(18.dp))
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
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Standard Template & Custom Pixel Size Selector ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Size / Template",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "300 DPI Standard",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Template Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val templates = listOf(
                    PassportTemplate.INDIAN_PASSPORT,
                    PassportTemplate.STAMP_SIZE,
                    PassportTemplate.SSC_UPSC_EXAM,
                    PassportTemplate.US_PASSPORT
                )

                templates.forEach { template ->
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
                                    PassportTemplate.INDIAN_PASSPORT -> "Indian Passport (35×45mm)"
                                    PassportTemplate.STAMP_SIZE -> "Stamp Size (20×25mm)"
                                    PassportTemplate.SSC_UPSC_EXAM -> "SSC / UPSC (35×45mm)"
                                    PassportTemplate.US_PASSPORT -> "US Passport (2×2\")"
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
                    onClick = {
                        showCustomInput = true
                    },
                    label = {
                        Text(
                            text = "Custom Px Size",
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

            // Custom Pixel Size Inputs
            AnimatedVisibility(visible = showCustomInput) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Navy15,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputWidth,
                            onValueChange = { inputWidth = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Width (px)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            onValueChange = { inputHeight = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Height (px)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                if (inputWidth.isNotBlank() && inputHeight.isNotBlank()) {
                                    viewModel.setCustomSize(inputWidth, inputHeight)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply", color = Navy10, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. Background Change Toolbar ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Tools (Offline ML Kit)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text("Zero Latency", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Solid White
                Button(
                    onClick = { viewModel.changeBackground(AndroidColor.WHITE) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Solid White", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Sky Blue
                Button(
                    onClick = { viewModel.changeBackground(AndroidColor.parseColor("#B0C4DE")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C4DE)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sky Blue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Reset
                OutlinedButton(
                    onClick = { viewModel.resetImage() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
