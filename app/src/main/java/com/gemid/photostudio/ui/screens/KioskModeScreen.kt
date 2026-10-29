package com.gemid.photostudio.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.export.ExportFormat
import com.gemid.photostudio.export.PdfExportResult
import com.gemid.photostudio.export.WifiPrintHelper
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.sheet.PaperSize
import com.gemid.photostudio.sheet.PrintPaperDefinition
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue50
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.GradientAmber
import com.gemid.photostudio.ui.theme.GradientBlue
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy30
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

/**
 * Express Kiosk Mode Screen:
 * Designed specifically for high-speed walk-in retail counters and photo studios.
 * Condenses the workflow into 3 single-screen steps:
 * 1. Snap / Select Photo
 * 2. Pick Standard Indian / Exam Preset
 * 3. Instant Print / Save with auto 4x6 grid, hairline borders, and scissor marks.
 * Complex editing sliders are hidden to allow any shop assistant to operate with zero mistakes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskModeScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var customerName by remember { mutableStateOf("") }

    // Quick gallery photo picker
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.loadFromGallery(it)
        }
    }

    // Auto-ensure 4x6 paper, photo borders, and scissor marks are configured for Kiosk
    LaunchedEffect(Unit) {
        viewModel.selectPaper(PrintPaperDefinition.SIZE_4X6)
        viewModel.showBorders = true
        viewModel.showScissorMarks = true
        if (uiState.activeBitmap != null) {
            viewModel.generateMasterSheetBitmap()
        }
    }

    // Standard high-demand presets for Indian studios
    val kioskPresets = remember {
        listOf(
            PassportTemplate.INDIAN_PASSPORT,
            PassportTemplate.STAMP_SIZE,
            PassportTemplate.SSC_UPSC_EXAM,
            PassportTemplate.INDIAN_PAN,
            PassportTemplate.US_PASSPORT,
            PassportTemplate.OCI_CARD
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FAST-PASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Express Kiosk Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "3-Step High-Speed Counter Studio",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neutral80
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.Studio.route) }
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Advanced Editor",
                            tint = AccentCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy10)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Navy10
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // ── STEP 1: SNAP / SELECT PHOTO ─────────────────────────
            item {
                StepHeader(
                    stepNumber = "1",
                    title = "Customer Photo",
                    subtitle = "Snap camera portrait or pick from gallery"
                )
                Spacer(modifier = Modifier.height(10.dp))

                val activeBitmap = uiState.activeBitmap
                if (activeBitmap != null) {
                    // Photo loaded card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy20),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                            ) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Customer Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Photo Ready",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${activeBitmap.width} × ${activeBitmap.height} px • Ready for 4×6 Sheet",
                                    fontSize = 11.sp,
                                    color = Neutral80
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Change photo
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Blue60.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, Blue50),
                                        modifier = Modifier.clickable {
                                            galleryPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "Change",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Auto White BG
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.clickable {
                                            viewModel.replaceBackground(StudioBackdrop.WHITE)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Auto White BG",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty state: 2 big touch buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate(Screen.Camera.route) },
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Snap Camera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy20),
                            border = BorderStroke(1.dp, Blue50)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pick Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── STEP 2: CHOOSE PHOTO SPEC PRESET ────────────────────
            item {
                StepHeader(
                    stepNumber = "2",
                    title = "Passport / Exam Preset",
                    subtitle = "Tap standard format (auto-scales 300 DPI)"
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(kioskPresets) { preset ->
                        val isSelected = uiState.selectedTemplate.id == preset.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Blue60.copy(alpha = 0.4f) else Navy20
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) AccentCyan else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    viewModel.selectTemplate(preset)
                                    viewModel.generateMasterSheetBitmap()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${preset.widthMm.toInt()}×${preset.heightMm.toInt()} mm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) AccentCyan else Neutral80
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = preset.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = preset.countryOrType,
                                    fontSize = 10.sp,
                                    color = Neutral80,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ── STEP 3: INSTANT 4X6 PRINT & SAVE ────────────────────
            item {
                StepHeader(
                    stepNumber = "3",
                    title = "Instant 4×6 Print & Order Save",
                    subtitle = "Auto-configured: 8 photos, cutting lines (✂) & borders"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20),
                    border = BorderStroke(1.dp, Blue50.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Customer Name Field for Room DB
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name / Order Note") },
                            placeholder = { Text("e.g. Ramesh Kumar (Walk-in)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = Blue50,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sheet preview thumbnail
                        val sheetBitmap = uiState.generatedSheetBitmap
                        if (sheetBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = sheetBitmap.asImageBitmap(),
                                    contentDescription = "4x6 Master Sheet",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Navy30),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.activeBitmap == null) "Select a customer photo to preview sheet" else "Generating 300 DPI 4×6 sheet...",
                                    color = Neutral80,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Badge specs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "4×6 Card (${uiState.computedGridResult.maxCapacity} Photos)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AccentCyan
                            )
                            Text(
                                text = "Borders: ON • Cut Marks (✂): ON",
                                fontSize = 11.sp,
                                color = Neutral80
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1-Tap Instant Wi-Fi Print Button
                        Button(
                            onClick = {
                                val printTarget = uiState.generatedSheetBitmap ?: uiState.activeBitmap
                                if (printTarget != null) {
                                    val name = customerName.ifBlank { "WalkIn_Customer" }
                                    viewModel.recordOrder(customerName = name, exportFormat = "PRINT")
                                    WifiPrintHelper.printSheet(
                                        context = context,
                                        sheetBitmap = printTarget,
                                        paper = PrintPaperDefinition.SIZE_4X6,
                                        isLandscape = false,
                                        jobTitle = "GemID_Kiosk_${name}"
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Print spooler triggered! Order saved to history.")
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please import a photo first!")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B)
                            ),
                            enabled = uiState.activeBitmap != null
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ INSTANT WI-FI PRINT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2 Secondary buttons: Save 300 DPI JPEG + Export PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val name = customerName.ifBlank { "WalkIn_Customer" }
                                    viewModel.exportSheetImage(
                                        context = context,
                                        format = ExportFormat.JPEG,
                                        onSuccess = {
                                            viewModel.recordOrder(customerName = name, exportFormat = "JPEG")
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Saved 300 DPI Sheet to Gallery & Order History!")
                                            }
                                        },
                                        onError = { err ->
                                            coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                                enabled = uiState.activeBitmap != null
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save JPEG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val name = customerName.ifBlank { "WalkIn_Customer" }
                                    viewModel.exportSheetPdf(
                                        context = context,
                                        onSuccess = { res: PdfExportResult ->
                                            viewModel.recordOrder(customerName = name, exportFormat = "PDF")
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Exported scale-accurate PDF: ${res.fileName}")
                                            }
                                        },
                                        onError = { err ->
                                            coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Blue50),
                                enabled = uiState.activeBitmap != null
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    stepNumber: String,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = AccentCyan,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Navy10
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Neutral80
            )
        }
    }
}
