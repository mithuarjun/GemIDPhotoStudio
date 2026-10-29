package com.gemid.photostudio.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemid.photostudio.navigation.Screen
import com.gemid.photostudio.sheet.PaperSize
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.viewmodel.StudioViewModel

/**
 * Minilab-Optimized Fixed-Count Sheet Layout Screen:
 * - Strictly locked to multiples of 6 (6, 12, 18, 24 photos).
 * - Multi-image distribution queue: evenly divides slots (e.g. 12 = 6 for A + 6 for B; 18 = 6 each for A, B, C).
 * - Dense zero-wastage packing on 4x6, 5x7, and A4 minilab sheets.
 * - 1:1 physical aspect ratio preview at 300 DPI scaling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetLayoutScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val masterSheetBitmap by viewModel.masterSheetBitmap.collectAsState()
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val selectedPaper = viewModel.selectedPaperSize
    val selectedCount = viewModel.selectedPhotoCount
    val activeSpec = viewModel.getActivePhotoSpec()
    val photoQueue = viewModel.photoQueue

    val addQueueLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addPhotoToQueue(context, uri)
        }
    }

    LaunchedEffect(editedBitmap, selectedPaper, selectedCount) {
        if (editedBitmap != null && masterSheetBitmap == null) {
            viewModel.generateMasterSheet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Print Sheet Layout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${selectedPaper.title} • $selectedCount Photos Pack (300 DPI)",
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
                            navController.navigate(Screen.Export.route)
                        },
                        enabled = masterSheetBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next: Export", color = Navy10, fontWeight = FontWeight.Bold)
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
            // ── 1. Live 300 DPI Physical Aspect Ratio Preview Card ────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Navy20),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy25)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (masterSheetBitmap != null) {
                        val sheetW = masterSheetBitmap!!.width.toFloat()
                        val sheetH = masterSheetBitmap!!.height.toFloat()
                        val paperAspect = if (sheetH > 0) sheetW / sheetH else 0.667f

                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(paperAspect)
                                .clip(RoundedCornerShape(8.dp)),
                            color = Color.White,
                            shadowElevation = 10.dp
                        ) {
                            Image(
                                bitmap = masterSheetBitmap!!.asImageBitmap(),
                                contentDescription = "300 DPI Composite Print Sheet",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Badge showing physical scale & scissor icon
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Navy10.copy(alpha = 0.88f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "✂ 1:1 Physical Lab Scale • 300 DPI",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else if (editedBitmap == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No photo loaded yet", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { navController.navigate(Screen.Home.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Choose Photo", color = Navy10, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Compositing 300 DPI Print Sheet...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 2. Smart Multi-Image Queue (Even Division in Multiples of 6)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Navy15,
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mixed-Customer Photo Queue (${photoQueue.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        // Add photo to queue button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentCyan.copy(alpha = 0.15f),
                            modifier = Modifier.clickable {
                                addQueueLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add Photo", tint = AccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Photo", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo queue thumbnail tray
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        photoQueue.forEachIndexed { index, bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Photo number badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(2.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                if (photoQueue.size > 1) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Red,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                            .clickable { viewModel.removePhotoFromQueue(index) }
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Distribution Info Text
                    val numPhotos = maxOf(1, photoQueue.size)
                    val slotsPerPhoto = maxOf(1, selectedCount / numPhotos)
                    val distDescription = if (numPhotos == 1) {
                        "All $selectedCount slots filled with Photo 1"
                    } else {
                        "Smart Queue: $slotsPerPhoto slots assigned per photo evenly"
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ $distDescription",
                        color = AccentTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 3. Exact Multiples-of-6 Layout (6, 12, 18, 24) ────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exact Photo Count (Multiples of 6)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Studio Standard",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(6, 12, 18, 24).forEach { count ->
                    val isSelected = selectedCount == count
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.setPhotoCount(count) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Navy25,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        color = if (isSelected) AccentCyan.copy(alpha = 0.22f) else Navy20
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$count",
                                color = if (isSelected) AccentCyan else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Photos",
                                color = if (isSelected) AccentCyan else Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 4. Minilab Zero-Wastage Paper Size Selector ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Minilab Paper Size",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Zero Margin • Full Bleed",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val papers = listOf(
                    PaperSize.POSTCARD_4X6 to "4 × 6 Inch",
                    PaperSize.SIZE_5X7 to "5 × 7 Inch",
                    PaperSize.A4 to "A4 Sheet"
                )

                papers.forEach { (paper, label) ->
                    val isSelected = selectedPaper.id == paper.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.setPaper(paper) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Navy25,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        color = if (isSelected) Navy15 else Navy20
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) AccentCyan else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = paper.dimensionInches,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 5. Borders & Scissor Cutting Mark Toggles ──────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Navy15,
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy25),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Borders Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Individual Photo Borders", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Renders sharp boundary border on each photo", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = viewModel.showBorders,
                            onCheckedChange = { viewModel.toggleBorders() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scissor Marks Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Scissor Marks & Dashed Cut Lines", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Renders physical ✂ marks and dashed guidelines", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = viewModel.showScissorMarks,
                            onCheckedChange = { viewModel.toggleScissors() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
