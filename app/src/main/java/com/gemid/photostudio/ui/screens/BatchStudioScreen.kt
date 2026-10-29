package com.gemid.photostudio.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.gemid.photostudio.batch.BatchItem
import com.gemid.photostudio.batch.BatchItemStatus
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.editor.StudioBackdrop
import com.gemid.photostudio.export.ExportFormat
import com.gemid.photostudio.export.HighDpiExporter
import com.gemid.photostudio.export.PdfDocumentGenerator
import com.gemid.photostudio.export.WifiPrintHelper
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.sheet.PaperSize
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue50
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.GradientBlue
import com.gemid.photostudio.ui.theme.GradientPurple
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.theme.Navy30
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchStudioScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    val batchQueue = viewModel.batchQueue
    val config = viewModel.batchConfig
    val isProcessing = viewModel.isBatchProcessing
    val progress = viewModel.batchProgress
    val progressText = viewModel.batchProgressText
    val generatedSheet = uiState.generatedSheetBitmap

    // Multi-Image Gallery Picker for batch ingestion
    val batchPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addBatchUris(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1-Tap Batch Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Auto-Crop • Background Cleanup • Studio Tone • 300 DPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Batch Ingestion Action Cards ───────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Select Raw Photos (2+ Photos)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Neutral90
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Blue60.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Blue60.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${batchQueue.size} Selected",
                                    color = AccentTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pick from Gallery
                            Button(
                                onClick = {
                                    batchPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue60)
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery Multi-Pick", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Capture via Camera
                            OutlinedButton(
                                onClick = { navController.navigate(Screen.Camera.route) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera Snap", fontSize = 12.sp, color = AccentCyan, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Display Batch Queue Horizontal Tray
                        if (batchQueue.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(batchQueue) { index, item ->
                                    BatchItemCard(
                                        item = item,
                                        index = index + 1,
                                        onRemove = { viewModel.removeBatchItem(index) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Clear All",
                                    fontSize = 11.sp,
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { viewModel.clearBatchQueue() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. One-Tap Studio Target Settings ─────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "2. Studio Optimization Profile",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Neutral90
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Passport Template
                        Text(text = "Passport Specification", fontSize = 12.sp, color = Neutral80)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(viewModel.getAllPhotoSpecs()) { template ->
                                val isSelected = config.template.id == template.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateBatchConfig(config.copy(template = template)) },
                                    label = { Text(if (template.isCustom) "${template.title} ★" else template.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Blue60,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Studio Backdrop Color (White #FFFFFF vs Sky Blue #0077BE)
                        Text(text = "Auto-Background Replacement", fontSize = 12.sp, color = Neutral80)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioBackdropChip(
                                title = "Pure White (#FFFFFF)",
                                colorArgb = StudioBackdrop.WHITE.colorArgb,
                                isSelected = config.backdrop == StudioBackdrop.WHITE,
                                onClick = { viewModel.updateBatchConfig(config.copy(backdrop = StudioBackdrop.WHITE)) }
                            )
                            StudioBackdropChip(
                                title = "Sky Blue (#0077BE)",
                                colorArgb = StudioBackdrop.SKY_BLUE.colorArgb,
                                isSelected = config.backdrop == StudioBackdrop.SKY_BLUE,
                                onClick = { viewModel.updateBatchConfig(config.copy(backdrop = StudioBackdrop.SKY_BLUE)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Paper Size (4x6, 5x7, A4, Custom)
                        Text(text = "Print Paper Sheet Format", fontSize = 12.sp, color = Neutral80)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(viewModel.getAllPaperSizes()) { paper ->
                                val isSelected = config.paperSize.id == paper.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateBatchConfig(config.copy(paperSize = paper)) },
                                    label = { Text(if (paper.isCustom) "${paper.title} ★" else paper.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Blue60,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Feature Switches
                        FeatureSwitchRow(
                            label = "Auto-Face Detection & Biometric Cropping",
                            checked = config.autoCrop,
                            onCheckedChange = { viewModel.updateBatchConfig(config.copy(autoCrop = it)) }
                        )
                        FeatureSwitchRow(
                            label = "Studio Auto-Enhancement (Exposure & Skin Tone)",
                            checked = config.autoEnhanceTone,
                            onCheckedChange = { viewModel.updateBatchConfig(config.copy(autoEnhanceTone = it)) }
                        )
                        FeatureSwitchRow(
                            label = "Photo Borders & Scissor Cutting Marks (✂)",
                            checked = config.showBorders && config.showScissorMarks,
                            onCheckedChange = {
                                viewModel.updateBatchConfig(
                                    config.copy(showBorders = it, showScissorMarks = it)
                                )
                            }
                        )
                    }
                }
            }

            // ── 3. Big 1-Tap Processing Action Button ─────────────────
            item {
                val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (batchQueue.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please select at least 2 raw photos first")
                                }
                            } else {
                                viewModel.runBatchOptimizationPipeline {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Batch studio sheet ready at 300 DPI!")
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Processing Batch...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (batchQueue.isEmpty()) "Select Photos to Begin" else "⚡ Run 1-Tap Batch Studio (${batchQueue.size} Photos)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentCyan,
                            trackColor = Navy30
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = progressText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentTeal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── 4. Instant Composite Result Sheet & Export Actions ─────
            if (generatedSheet != null) {
                item {
                    Text(
                        text = "3. Print-Ready 300 DPI Composite Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Neutral90
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy15),
                        border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = generatedSheet.asImageBitmap(),
                                contentDescription = "Generated Composite Print Sheet",
                                modifier = Modifier
                                    .fillMaxHeight(0.96f)
                                    .aspectRatio(config.paperSize.aspectRatio(false))
                                    .shadow(12.dp, RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White),
                                contentScale = ContentScale.Fit
                            )

                            // Specs tag overlay
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✂ ", fontSize = 11.sp, color = AccentCyan)
                                    Text(
                                        text = "${config.paperSize.title} • 300 DPI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 5. One-Tap Export & Print Buttons ─────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Direct Wi-Fi Print
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    WifiPrintHelper.printBitmapDirectly(
                                        activity = activity,
                                        bitmap = generatedSheet,
                                        jobName = "GemID_Batch_${config.paperSize.id}",
                                        paper = config.paperSize.definition
                                    )
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to access print service")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue60)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Direct Wi-Fi Print (Zero Watermarks)", fontWeight = FontWeight.Bold)
                        }

                        // Save 300 DPI Image & PDF Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save JPEG/PNG
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportSheetImage(
                                        context = context,
                                        format = ExportFormat.JPEG,
                                        onSuccess = { res ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Saved 300 DPI Sheet to Gallery: ${res.fileName}")
                                            }
                                        },
                                        onError = { err ->
                                            coroutineScope.launch { snackbarHostState.showSnackbar(err) }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Blue50)
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Image", fontSize = 12.sp, color = Color.White)
                            }

                            // Export PDF
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportPdfDocument(
                                        context = context,
                                        onSuccess = { res ->
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
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Blue50)
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // Navigate to Sheet Layout Hub for fine customization
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.SheetLayout.route) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fine-Tune Layout & Paper Trimmer Guides", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Batch Queue Item Thumbnail Card ──────────────────────────────────

@Composable
private fun BatchItemCard(
    item: BatchItem,
    index: Int,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 86.dp, height = 120.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy25),
        border = BorderStroke(
            1.dp,
            when (item.status) {
                BatchItemStatus.COMPLETED -> AccentTeal
                BatchItemStatus.PROCESSING -> AccentCyan
                BatchItemStatus.FAILED -> Color.Red
                BatchItemStatus.QUEUED -> Color.White.copy(alpha = 0.15f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val displayBitmap = item.optimizedBitmap ?: item.rawBitmap
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Photo #$index",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Index label
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = "#$index",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Remove button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clickable(onClick = onRemove),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(3.dp)
                )
            }

            // Status Badge Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                when (item.status) {
                    BatchItemStatus.COMPLETED -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Ready",
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp).padding(2.dp)
                    )
                    BatchItemStatus.PROCESSING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).padding(3.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                    BatchItemStatus.FAILED -> Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = "Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp).padding(2.dp)
                    )
                    BatchItemStatus.QUEUED -> {}
                }
            }
        }
    }
}

// ── Studio Backdrop Selector Chip ────────────────────────────────────

@Composable
private fun StudioBackdropChip(
    title: String,
    colorArgb: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Blue60 else Navy25,
        border = BorderStroke(
            1.dp,
            if (isSelected) AccentCyan else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb))
                    .border(0.8.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ── Feature Switch Helper Row ────────────────────────────────────────

@Composable
private fun FeatureSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Neutral80,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Blue60
            )
        )
    }
}
