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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ScreenRotation
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.sheet.CustomPaperSizeDialog
import com.gemid.photostudio.sheet.GridMatrixResult
import com.gemid.photostudio.sheet.PaperSize
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
import com.gemid.photostudio.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetLayoutScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val grid = uiState.computedGridResult
    val sheetBitmap = uiState.generatedSheetBitmap

    // Multi-photo picker launcher to import additional photos directly into the queue
    val addPhotosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotosFromGallery(uris)
        }
    }

    var showCustomPaperDialog by remember { mutableStateOf(false) }

    // Ensure initial master sheet is generated
    LaunchedEffect(Unit) {
        if (sheetBitmap == null && (viewModel.photoList.isNotEmpty() || uiState.activeBitmap != null)) {
            viewModel.generateMasterSheetBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Print Sheet Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${viewModel.selectedPaperSize.title} • ${grid.cols}×${grid.rows} Grid @ 300 DPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan
                        )
                    }
                },
                actions = {
                    // Orientation Toggle Button
                    IconButton(onClick = { viewModel.toggleSheetOrientation() }) {
                        Icon(
                            Icons.Rounded.ScreenRotation,
                            contentDescription = "Toggle Orientation",
                            tint = if (uiState.isSheetLandscape) AccentCyan else Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 1. Live Composite Print Sheet Preview ─────────────
                item {
                    LiveSheetPreviewCard(
                        sheetBitmap = sheetBitmap,
                        aspectRatio = viewModel.selectedPaperSize.aspectRatio(uiState.isSheetLandscape),
                        paperTitle = viewModel.selectedPaperSize.title,
                        photoCount = grid.activeCount,
                        isLandscape = uiState.isSheetLandscape,
                        showBorders = viewModel.showBorders,
                        showScissorMarks = viewModel.showScissorMarks
                    )
                }

                // ── 2. Mixed Photo Queue Selection Tray ───────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Layers,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Photo Queue (Mixed Sheet)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Blue60.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Blue60.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${viewModel.photoList.size} Photos",
                                        color = AccentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (viewModel.photoList.size > 1) {
                                    "Multiple photos will automatically loop across the ${grid.activeCount} slots on the sheet."
                                } else {
                                    "Single photo repeated across all slots. Add more photos for mixed batch printing."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral80
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Horizontal Photo Queue List
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(viewModel.photoList) { index, photo ->
                                    PhotoQueueItemCard(
                                        bitmap = photo,
                                        index = index + 1,
                                        onRemove = { viewModel.removePhotoFromQueue(index) },
                                        onDuplicate = { viewModel.duplicatePhotoInQueue(index) }
                                    )
                                }

                                // "Add Photos" Action Button
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Navy25,
                                        border = BorderStroke(1.5.dp, Blue60.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .size(width = 84.dp, height = 110.dp)
                                            .clickable {
                                                addPhotosPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.AddPhotoAlternate,
                                                contentDescription = "Add Photos",
                                                tint = AccentCyan,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Add Photo",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Neutral90
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Add active photo or clear queue
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.activeBitmap != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.addActivePhotoToQueue() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Blue60)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Active Photo", fontSize = 11.sp)
                                    }
                                }

                                if (viewModel.photoList.size > 1) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearPhotoQueue() },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear", fontSize = 11.sp, color = Color.Red.copy(alpha = 0.9f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. Paper Size Targets Selector (4x6, 5x7, A4) ─────
                item {
                    Text(
                        text = "Target Paper Size (300 DPI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Neutral90
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exact millimeter calibration for photo printers & minilabs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral80
                    )
                }

                item {
                    val allPapers = viewModel.getAllPaperSizes()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(allPapers) { paper ->
                            val isSelected = viewModel.selectedPaperSize.id == paper.id
                            PaperTargetCard(
                                paperSize = paper,
                                isLandscape = uiState.isSheetLandscape,
                                isSelected = isSelected,
                                onClick = { viewModel.setPaperSizeTarget(paper) },
                                onDelete = if (paper.isCustom) {
                                    { viewModel.deleteCustomPaper(paper) }
                                } else null
                            )
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Navy20,
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .width(138.dp)
                                    .height(108.dp)
                                    .clickable { showCustomPaperDialog = true }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(AccentCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add Custom Paper",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "+ Custom Paper",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AccentCyan
                                    )
                                    Text(
                                        text = "Any Size / Roll",
                                        fontSize = 9.sp,
                                        color = Neutral80
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 4. Photo Borders Configuration ────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CropFree,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Show Photo Borders",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = "Delicate border around each passport portrait",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Neutral80
                                        )
                                    }
                                }

                                Switch(
                                    checked = viewModel.showBorders,
                                    onCheckedChange = { viewModel.updateBorders(show = it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Blue60
                                    )
                                )
                            }

                            if (viewModel.showBorders) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Border Color",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Neutral80
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BorderColorChip(
                                        label = "Light Grey",
                                        color = android.graphics.Color.LTGRAY,
                                        isSelected = viewModel.borderColor == android.graphics.Color.LTGRAY,
                                        onClick = { viewModel.updateBorders(show = true, color = android.graphics.Color.LTGRAY) }
                                    )
                                    BorderColorChip(
                                        label = "Dark Grey",
                                        color = android.graphics.Color.DKGRAY,
                                        isSelected = viewModel.borderColor == android.graphics.Color.DKGRAY,
                                        onClick = { viewModel.updateBorders(show = true, color = android.graphics.Color.DKGRAY) }
                                    )
                                    BorderColorChip(
                                        label = "Black",
                                        color = android.graphics.Color.BLACK,
                                        isSelected = viewModel.borderColor == android.graphics.Color.BLACK,
                                        onClick = { viewModel.updateBorders(show = true, color = android.graphics.Color.BLACK) }
                                    )
                                    BorderColorChip(
                                        label = "Cyan Studio",
                                        color = android.graphics.Color.argb(255, 0, 229, 255),
                                        isSelected = viewModel.borderColor == android.graphics.Color.argb(255, 0, 229, 255),
                                        onClick = { viewModel.updateBorders(show = true, color = android.graphics.Color.argb(255, 0, 229, 255)) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Border Width",
                                        fontSize = 12.sp,
                                        color = Neutral80
                                    )
                                    Text(
                                        text = "${viewModel.borderWidth.toInt()} px",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Neutral90
                                    )
                                }

                                Slider(
                                    value = viewModel.borderWidth,
                                    onValueChange = { viewModel.updateBorders(show = true, width = it) },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentCyan,
                                        activeTrackColor = Blue50,
                                        inactiveTrackColor = Navy30
                                    )
                                )
                            }
                        }
                    }
                }

                // ── 5. Scissor Cutting Marks (✂) Controls ─────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.ContentCut,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Scissor Cutting Marks (✂)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = "Dashed trimmer lines & scissor icons in margins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Neutral80
                                        )
                                    }
                                }

                                Switch(
                                    checked = viewModel.showScissorMarks,
                                    onCheckedChange = { viewModel.updateScissorMarks(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Blue60
                                    )
                                )
                            }

                            if (viewModel.showScissorMarks) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Navy25,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✂",
                                            fontSize = 20.sp,
                                            color = AccentCyan
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Prints ✂ symbols and dashed guides along sheet gutters for quick slicing with rotary or guillotine paper trimmers.",
                                            fontSize = 11.sp,
                                            color = Neutral80,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 6. Grid Photo Count Stepper ───────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Grid4x4,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Photo Slots to Populate",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Blue60.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Blue60.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${grid.activeCount} / ${grid.maxCapacity}",
                                        color = AccentTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Slider(
                                value = grid.activeCount.toFloat(),
                                onValueChange = {
                                    viewModel.setCustomSheetPhotoCount(it.toInt())
                                    viewModel.generateMasterSheetBitmap()
                                },
                                valueRange = 1f..grid.maxCapacity.toFloat(),
                                steps = maxOf(0, grid.maxCapacity - 2),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentCyan,
                                    activeTrackColor = Blue50,
                                    inactiveTrackColor = Navy30
                                )
                            )

                            // Quick Fill Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (grid.maxCapacity >= 4) {
                                    FilterChip(
                                        selected = grid.activeCount == 4,
                                        onClick = {
                                            viewModel.setCustomSheetPhotoCount(4)
                                            viewModel.generateMasterSheetBitmap()
                                        },
                                        label = { Text("4 Photos", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue60)
                                    )
                                }
                                if (grid.maxCapacity >= 8) {
                                    FilterChip(
                                        selected = grid.activeCount == 8,
                                        onClick = {
                                            viewModel.setCustomSheetPhotoCount(8)
                                            viewModel.generateMasterSheetBitmap()
                                        },
                                        label = { Text("8 Photos (2×4)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue60)
                                    )
                                }
                                FilterChip(
                                    selected = grid.activeCount == grid.maxCapacity,
                                    onClick = {
                                        viewModel.setCustomSheetPhotoCount(null)
                                        viewModel.generateMasterSheetBitmap()
                                    },
                                    label = { Text("Fill All (${grid.maxCapacity})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue60)
                                )
                            }
                        }
                    }
                }

                // ── 7. Proceed to Export & Print Action ───────────────
                item {
                    Button(
                        onClick = {
                            viewModel.generateMasterSheetBitmap {
                                navController.navigate(Screen.Export.route)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Rounded.Print,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proceed to Export & Print",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Loading overlay
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
                                text = uiState.loadingMessage.ifEmpty { "Generating print sheet..." },
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

    if (showCustomPaperDialog) {
        CustomPaperSizeDialog(
            onDismissRequest = { showCustomPaperDialog = false },
            onConfirm = { customPaperDef ->
                viewModel.addCustomPaperDefinition(customPaperDef)
                showCustomPaperDialog = false
            }
        )
    }
}

// ── Live Composite Sheet Preview Card ────────────────────────────────

@Composable
private fun LiveSheetPreviewCard(
    sheetBitmap: Bitmap?,
    aspectRatio: Float,
    paperTitle: String,
    photoCount: Int,
    isLandscape: Boolean,
    showBorders: Boolean,
    showScissorMarks: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Navy15),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (sheetBitmap != null) {
                Image(
                    bitmap = sheetBitmap.asImageBitmap(),
                    contentDescription = "Live 300 DPI Print Sheet Preview",
                    modifier = Modifier
                        .fillMaxHeight(0.96f)
                        .aspectRatio(aspectRatio)
                        .shadow(14.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Blue60)
            }

            // Sheet specs badge overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showScissorMarks) {
                        Text("✂ ", fontSize = 11.sp, color = AccentCyan)
                    }
                    Text(
                        text = "$paperTitle • $photoCount Photos",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ── Sub-Component: Photo Queue Thumbnail Card ────────────────────────

@Composable
private fun PhotoQueueItemCard(
    bitmap: Bitmap,
    index: Int,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 84.dp, height = 110.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy25),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Queue Photo #$index",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Index tag
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = "#$index",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
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

            // Duplicate button
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clickable(onClick = onDuplicate),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = "Duplicate",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// ── Sub-Component: Paper Size Target Card ────────────────────────────

@Composable
private fun PaperTargetCard(
    paperSize: PaperSize,
    isLandscape: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .width(138.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Navy25 else Navy20
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = paperSize.dimensionInches,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) AccentCyan else Neutral90,
                    modifier = Modifier.weight(1f)
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Delete custom paper",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${paperSize.pixelWidth(isLandscape)}×${paperSize.pixelHeight(isLandscape)}px",
                fontSize = 10.sp,
                color = Neutral80
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Blue60 else Navy30
            ) {
                Text(
                    text = when (paperSize.id) {
                        "paper_4x6" -> "8 Photos (2×4)"
                        "paper_5x7" -> "12 Photos (3×4)"
                        "paper_a4" -> "35 Photos (5×7)"
                        "paper_letter" -> "30 Photos (5×6)"
                        else -> "${paperSize.widthMm.toInt()}×${paperSize.heightMm.toInt()} mm"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ── Sub-Component: Border Color Chip ─────────────────────────────────

@Composable
private fun BorderColorChip(
    label: String,
    color: Int,
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
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(0.8.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
