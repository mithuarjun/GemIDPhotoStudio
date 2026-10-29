package com.gemid.photostudio.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.gemid.photostudio.ui.theme.Navy25
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
                            text = "${viewModel.selectedPaperSize.title} • ${grid.sheetWidthPx}×${grid.sheetHeightPx}px @ 300 DPI",
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

            // ── 2. Specifications Summary Card ────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpecItem(label = "Paper Size", value = viewModel.selectedPaperSize.title)
                        SpecItem(label = "Photo Count", value = "${grid.activeCount} photos")
                        SpecItem(label = "DPI Resolution", value = "300 DPI")
                        SpecItem(label = "Cut Guidelines", value = "✂ & Dashed")
                    }
                }
            }

            // ── 3. Clean, Large Action Buttons ─────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Print & Export Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral90
                )
            }

            // 1. Direct Wi-Fi Minilab Printing
            item {
                LargeActionButton(
                    title = "Direct Wi-Fi Minilab Print",
                    subtitle = "Epson, Canon SELPHY, HiTi, Mopria & System Spooler",
                    icon = Icons.Rounded.Print,
                    badgeText = "DIRECT PRINT",
                    containerColor = Navy25,
                    borderColor = AccentTeal,
                    iconTint = AccentTeal,
                    onClick = {
                        val printed = viewModel.printSheetDirect(context)
                        if (!printed) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Print service unavailable or no sheet generated.")
                            }
                        }
                    }
                )
            }

            // 2. High-Res 300 DPI JPEG Export
            item {
                LargeActionButton(
                    title = "Save 300 DPI JPEG",
                    subtitle = "Embedded 300 DPI EXIF headers • Standard minilab lab ready",
                    icon = Icons.Rounded.SaveAlt,
                    badgeText = "MAX QUALITY",
                    containerColor = Blue60,
                    borderColor = AccentCyan,
                    iconTint = Color.White,
                    onClick = {
                        viewModel.exportSheetImage(
                            context = context,
                            format = ExportFormat.JPEG,
                            onSuccess = { result ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved 300 DPI JPEG (${result.fileName}) to Pictures/GemIDPhotoStudio!")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    }
                )
            }

            // 3. Lossless PNG Export
            item {
                LargeActionButton(
                    title = "Save Lossless PNG",
                    subtitle = "Uncompressed 300 DPI pixel-perfect raster copy",
                    icon = Icons.Rounded.Image,
                    badgeText = "LOSSLESS",
                    containerColor = Navy20,
                    borderColor = Color.White.copy(alpha = 0.2f),
                    iconTint = AccentCyan,
                    onClick = {
                        viewModel.exportSheetImage(
                            context = context,
                            format = ExportFormat.PNG,
                            onSuccess = { result ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved Lossless PNG (${result.fileName}) to Pictures/GemIDPhotoStudio!")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    }
                )
            }

            // 4. Print-Ready PDF (100% exact physical scale)
            item {
                LargeActionButton(
                    title = "Save Printable PDF",
                    subtitle = "Exact 1:1 physical millimeter scale document for minilabs",
                    icon = Icons.Rounded.PictureAsPdf,
                    badgeText = "1:1 SCALE",
                    containerColor = Navy20,
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.6f),
                    iconTint = Color(0xFFF59E0B),
                    onClick = {
                        viewModel.exportSheetPdf(
                            context = context,
                            onSuccess = { result ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved 100% Scale PDF (${result.fileName}) to Documents!")
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    }
                )
            }

            // 5. Share Print Sheet
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.exportSheetImage(
                            context = context,
                            format = ExportFormat.JPEG,
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share Print Sheet with Client", color = AccentCyan, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Neutral80)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun LargeActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeText: String,
    containerColor: Color,
    borderColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconTint.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, iconTint.copy(alpha = 0.6f)),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconTint.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, iconTint)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Neutral80,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
