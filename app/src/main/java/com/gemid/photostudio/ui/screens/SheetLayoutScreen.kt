package com.gemid.photostudio.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * Ultra-simplified Fixed-Count Sheet Layout Screen:
 * - Lets user choose paper size (4x6, 5x7, A4).
 * - Exact photo count layout: 6, 12, 18, or 24 photos per sheet.
 * - Automatically computes the grid matrix and renders photo borders, dashed cut lines,
 *   and physical scissor symbols (✂) in the margins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetLayoutScreen(
    navController: NavController,
    viewModel: StudioViewModel
) {
    val masterSheetBitmap by viewModel.masterSheetBitmap.collectAsState()
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val selectedPaper = viewModel.selectedPaperSize
    val selectedCount = viewModel.selectedPhotoCount

    // Automatically trigger sheet compositing on entry if needed
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
                        Text("Fixed Sheet Layout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${selectedPaper.title} • $selectedCount Photos",
                            color = AccentCyan,
                            fontSize = 11.sp
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
                            imageVector = Icons.Rounded.ArrowForward,
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Live 300 DPI Composite Sheet Preview ───────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                shape = RoundedCornerShape(16.dp),
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
                        // Render physical paper look with white background and subtle frame
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Image(
                                bitmap = masterSheetBitmap!!.asImageBitmap(),
                                contentDescription = "300 DPI Composite Print Sheet",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Badge showing physical scale & scissor icon
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Navy10.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✂ 300 DPI • 1:1 Scale",
                                    color = AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                            CircularProgressIndicator(color = AccentCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Compositing 300 DPI Print Sheet...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Paper Size Selector (4x6, 5x7, A4) ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Paper Size",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Studio Standard",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setPaper(paper) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Navy25,
                                shape = RoundedCornerShape(12.dp)
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
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. Exact Photo Count Layout (6, 12, 18, 24) ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exact Photo Count per Sheet",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Fixed Grid Matrix",
                    color = AccentCyan,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(6, 12, 18, 24).forEach { count ->
                    val isSelected = selectedCount == count
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setPhotoCount(count) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Navy25,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else Navy20
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
                                color = if (isSelected) AccentCyan else Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 4. Borders & Scissor Cutting Mark Toggles ──────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
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
                            Text("Draws clean thin border around each photo", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = viewModel.showBorders,
                            onCheckedChange = { viewModel.toggleBorders() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scissor Marks Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Scissor Marks & Dashed Cut Lines", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Displays physical ✂ marks and cutting guides", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = viewModel.showScissorMarks,
                            onCheckedChange = { viewModel.toggleScissors() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
