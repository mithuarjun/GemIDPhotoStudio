package com.gemid.photostudio.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

/**
 * Direct Export & Wi-Fi Print Screen:
 * Large, clear, high-contrast buttons for:
 * 1. Direct Wi-Fi Printer connection (WifiPrintHelper / PrintManager)
 * 2. High-Res 300 DPI JPEG export (with EXIF resolution injection)
 * 3. PDF export (1:1 physical scale)
 * 4. Lossless PNG export
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val masterSheetBitmap by viewModel.masterSheetBitmap.collectAsState()
    val selectedPaper = viewModel.selectedPaperSize
    val selectedCount = viewModel.selectedPhotoCount

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
                        Text("Export & Print", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${selectedPaper.title} • $selectedCount Photos (300 DPI)", color = AccentCyan, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Rounded.Home, contentDescription = "Home", tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy10)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Navy10
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Sheet Preview Thumbnail ───────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy20),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy25)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (masterSheetBitmap != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                Image(
                                    bitmap = masterSheetBitmap!!.asImageBitmap(),
                                    contentDescription = "Print Sheet Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Text("No sheet generated", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select Export or Print Action",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── 1. Direct Wi-Fi Printer Connection ────────────────────
                ExportActionCard(
                    title = "Direct Wi-Fi Print",
                    subtitle = "Send to Canon SELPHY, Epson, HP or Mopria printer",
                    badgeText = "RECOMMENDED FOR SHOPS",
                    icon = Icons.Rounded.Print,
                    accentColor = AccentCyan,
                    onClick = {
                        val printed = viewModel.printSheetDirect(context)
                        if (!printed) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Print service unavailable or no sheet ready")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 2. High-Res 300 DPI JPEG Export ───────────────────────
                ExportActionCard(
                    title = "Save 300 DPI JPEG",
                    subtitle = "High-resolution photo print format with injected EXIF",
                    badgeText = "LAB QUALITY",
                    icon = Icons.Rounded.Image,
                    accentColor = Color(0xFF48CAE4),
                    onClick = {
                        viewModel.exportJpeg(context) { fileName ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved JPEG: $fileName")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 3. 1:1 Scale PDF Document Export ──────────────────────
                ExportActionCard(
                    title = "Export 1:1 Scale PDF",
                    subtitle = "Exact physical millimeter dimensions for document minilabs",
                    badgeText = "CALIBRATED SCALE",
                    icon = Icons.Rounded.PictureAsPdf,
                    accentColor = Color(0xFFFF70A6),
                    onClick = {
                        viewModel.exportPdf(context) { fileName ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved PDF: $fileName")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 4. Lossless PNG Export ────────────────────────────────
                ExportActionCard(
                    title = "Save Lossless PNG",
                    subtitle = "Uncompressed digital archival graphics format",
                    badgeText = "LOSSLESS",
                    icon = Icons.Rounded.SaveAlt,
                    accentColor = Color(0xFF70D6FF),
                    onClick = {
                        viewModel.exportPng(context) { fileName ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved PNG: $fileName")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Loading overlay
            AnimatedVisibility(
                visible = viewModel.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.loadingMessage,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportActionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Navy15,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
