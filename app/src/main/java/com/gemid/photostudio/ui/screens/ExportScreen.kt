package com.gemid.photostudio.ui.screens

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.export.ExportFormat
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedFormat by remember { mutableStateOf("JPEG") }
    var selectedQuality by remember { mutableStateOf("Ultra HD (300 DPI)") }
    var sheetCost by remember { mutableStateOf(5f) }
    var customerPrice by remember { mutableStateOf(50f) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val grid = uiState.computedGridResult
    val sheetBitmap = uiState.generatedSheetBitmap

    // Auto-generate sheet if navigating directly without prior trigger
    LaunchedEffect(sheetBitmap) {
        if (sheetBitmap == null && (uiState.activeBitmap != null || viewModel.photoList.isNotEmpty())) {
            viewModel.generateMasterSheetBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Export & Print Hub", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${grid.paper.dimensionInches} • ${grid.sheetWidthPx}×${grid.sheetHeightPx}px @ 300 DPI",
                            fontSize = 11.sp,
                            color = AccentCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
            // ── 1. Master Sheet Preview ────────────────────────────
            item {
                Text(
                    text = "300 DPI Print Sheet Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral90
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy15),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sheetBitmap != null && !sheetBitmap.isRecycled) {
                            val aspect = grid.paper.aspectRatio(grid.isLandscape)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .aspectRatio(aspect)
                                    .shadow(16.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                Image(
                                    bitmap = sheetBitmap.asImageBitmap(),
                                    contentDescription = "Master Sheet",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            // Fallback mockup
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.4f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Navy20),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.Print,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${grid.paper.dimensionInches} Sheet (${grid.activeCount} photos)",
                                        fontSize = 12.sp,
                                        color = Neutral80
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Format Picker ───────────────────────────────────
            item {
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral90
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportFormatChip(
                        label = "High-Res JPEG",
                        icon = Icons.Rounded.Image,
                        isSelected = selectedFormat == "JPEG",
                        onClick = { selectedFormat = "JPEG" },
                        modifier = Modifier.weight(1f)
                    )
                    ExportFormatChip(
                        label = "Lossless PNG",
                        icon = Icons.Rounded.Image,
                        isSelected = selectedFormat == "PNG",
                        onClick = { selectedFormat = "PNG" },
                        modifier = Modifier.weight(1f)
                    )
                    ExportFormatChip(
                        label = "Printable PDF",
                        icon = Icons.Rounded.PictureAsPdf,
                        isSelected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 3. Resolution & Print Quality ──────────────────────
            item {
                Text(
                    text = "Print Quality Specification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral90
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        QualityOption(
                            title = "Ultra HD (300 DPI)",
                            subtitle = "${grid.sheetWidthPx} × ${grid.sheetHeightPx} px • Lab Quality minilab standard",
                            isSelected = selectedQuality == "Ultra HD (300 DPI)",
                            onClick = { selectedQuality = "Ultra HD (300 DPI)" }
                        )

                        QualityOption(
                            title = "Standard (200 DPI)",
                            subtitle = "${(grid.sheetWidthPx * 2 / 3)} × ${(grid.sheetHeightPx * 2 / 3)} px • Fast office inkjet",
                            isSelected = selectedQuality == "Standard (200 DPI)",
                            onClick = { selectedQuality = "Standard (200 DPI)" }
                        )
                    }
                }
            }

            // ── Commercial Shop Pricing & Paper Waste Calculator ───
            item {
                ShopPricingCalculator(
                    paperTitle = grid.paper.title,
                    totalPhotos = grid.maxCapacity,
                    photoWidthMm = uiState.selectedTemplate.widthMm,
                    photoHeightMm = uiState.selectedTemplate.heightMm,
                    paperWidthMm = grid.paper.widthMm,
                    paperHeightMm = grid.paper.heightMm,
                    sheetCost = sheetCost,
                    customerPrice = customerPrice,
                    onCostChange = { sheetCost = it },
                    onPriceChange = { customerPrice = it }
                )
            }

            // ── 4. Export & Action Buttons ──────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Save to Device Gallery / Documents
                Button(
                    onClick = {
                        when (selectedFormat) {
                            "PDF" -> {
                                viewModel.exportSheetPdf(
                                    context = context,
                                    onSuccess = { result ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Saved PDF (${result.fileName}) at 100% physical scale!")
                                        }
                                    },
                                    onError = { error ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                    }
                                )
                            }
                            "PNG" -> {
                                viewModel.exportSheetImage(
                                    context = context,
                                    format = ExportFormat.PNG,
                                    onSuccess = { result ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Saved Lossless PNG (300 DPI) to Pictures/GemIDPhotoStudio!")
                                        }
                                    },
                                    onError = { error ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                    }
                                )
                            }
                            else -> {
                                viewModel.exportSheetImage(
                                    context = context,
                                    format = ExportFormat.JPEG,
                                    onSuccess = { result ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Saved Max-Quality JPEG with 300 DPI EXIF headers!")
                                        }
                                    },
                                    onError = { error ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue60)
                ) {
                    Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save High-Res $selectedFormat to Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Share
                OutlinedButton(
                    onClick = {
                        if (selectedFormat == "PDF") {
                            viewModel.exportSheetPdf(
                                context = context,
                                onSuccess = { result ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, result.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Print PDF"))
                                },
                                onError = { error ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                }
                            )
                        } else {
                            val format = if (selectedFormat == "PNG") ExportFormat.PNG else ExportFormat.JPEG
                            viewModel.exportSheetImage(
                                context = context,
                                format = format,
                                onSuccess = { result ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = result.mimeType
                                        putExtra(Intent.EXTRA_STREAM, result.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share High-Res Print Sheet"))
                                },
                                onError = { error ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share $selectedFormat with Client", color = AccentCyan, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Wi-Fi Printing (Epson, Canon SELPHY, HiTi, Mopria)
                OutlinedButton(
                    onClick = {
                        val printed = viewModel.printSheetDirect(context)
                        if (!printed) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Print service unavailable or no sheet generated.")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AccentTeal.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Print via Wi-Fi / Spooler (Epson, Canon, HiTi)", color = AccentTeal, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

// ── Private Helpers ────────────────────────────────────────────────

@Composable
private fun ExportFormatChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue60 else Navy20
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) AccentCyan else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Neutral80,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Neutral90,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QualityOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentCyan,
                unselectedColor = Neutral80
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) AccentCyan else Neutral90
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Neutral80
            )
        }
    }
}

@Composable
private fun ShopPricingCalculator(
    paperTitle: String,
    totalPhotos: Int,
    photoWidthMm: Float,
    photoHeightMm: Float,
    paperWidthMm: Float,
    paperHeightMm: Float,
    sheetCost: Float,
    customerPrice: Float,
    onCostChange: (Float) -> Unit,
    onPriceChange: (Float) -> Unit
) {
    val totalSheetAreaMm2 = paperWidthMm * paperHeightMm
    val totalPhotosAreaMm2 = totalPhotos * photoWidthMm * photoHeightMm
    val utilizationPercent = if (totalSheetAreaMm2 > 0) {
        ((totalPhotosAreaMm2 / totalSheetAreaMm2) * 100).toInt().coerceIn(0, 100)
    } else 0
    val marginWastePercent = (100 - utilizationPercent).coerceIn(0, 100)

    val profitPerSheet = customerPrice - sheetCost
    val profitMarginPercent = if (customerPrice > 0) ((profitPerSheet / customerPrice) * 100).toInt() else 0
    val costPerPhoto = if (totalPhotos > 0) sheetCost / totalPhotos else 0f
    val pricePerPhoto = if (totalPhotos > 0) customerPrice / totalPhotos else 0f

    Text(
        text = "Commercial Shop Pricing & Paper Waste",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Neutral90
    )
    Spacer(modifier = Modifier.height(8.dp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy20),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Commercial metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Calculate,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sheet Profit & Economics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "+$profitMarginPercent% MARGIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cost per sheet
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Paper & Ink Cost",
                        fontSize = 11.sp,
                        color = Neutral80
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(3f, 5f, 8f).forEach { cost ->
                            val isSel = sheetCost == cost
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Blue60 else Navy10,
                                border = BorderStroke(1.dp, if (isSel) AccentCyan else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCostChange(cost) }
                            ) {
                                Text(
                                    text = "₹${cost.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Neutral80,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Retail price per sheet
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "Customer Price / Sheet",
                        fontSize = 11.sp,
                        color = Neutral80
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(40f, 50f, 60f, 80f).forEach { price ->
                            val isSel = customerPrice == price
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF10B981).copy(alpha = 0.3f) else Navy10,
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPriceChange(price) }
                            ) {
                                Text(
                                    text = "₹${price.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF10B981) else Neutral80,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profit & Loss Breakdown Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy10, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn(title = "Sheet Retail", value = "₹${customerPrice.toInt()}", valueColor = Color.White)
                MetricColumn(title = "Sheet Cost", value = "₹${sheetCost.toInt()}", valueColor = Color(0xFFFB7185))
                MetricColumn(title = "Net Profit", value = "+₹${profitPerSheet.toInt()}", valueColor = Color(0xFF10B981))
                MetricColumn(title = "Per Photo", value = "₹${"%.1f".format(pricePerPhoto)}", valueColor = AccentCyan)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Paper Surface Utilization vs Cutting Margin Waste
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paper Surface Utilization",
                    fontSize = 11.sp,
                    color = Neutral80
                )
                Text(
                    text = "$utilizationPercent% Print • $marginWastePercent% Margin/Cut",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dual progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(utilizationPercent / 100f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentCyan, Color(0xFF10B981))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily Revenue Target for Walk-Ins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyTargetBadge(modifier = Modifier.weight(1f), customers = 10, profit = profitPerSheet * 10)
                DailyTargetBadge(modifier = Modifier.weight(1f), customers = 25, profit = profitPerSheet * 25)
                DailyTargetBadge(modifier = Modifier.weight(1f), customers = 50, profit = profitPerSheet * 50)
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = Neutral80)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun DailyTargetBadge(
    modifier: Modifier = Modifier,
    customers: Int,
    profit: Float
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Navy10,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$customers Orders/Day", fontSize = 10.sp, color = Neutral80)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "₹${profit.toInt()} profit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
        }
    }
}

