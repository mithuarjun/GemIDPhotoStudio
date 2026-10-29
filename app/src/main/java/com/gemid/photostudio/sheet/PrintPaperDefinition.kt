package com.gemid.photostudio.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import java.util.Locale

/**
 * Standard & Custom Print Paper Specifications at 300 DPI (dots per inch).
 * Standardized for photo studio minilabs (Epson, DNP, Canon, Noritsu, Fuji Frontier)
 * and custom studio roll or card paper sizes.
 */
data class PrintPaperDefinition(
    val id: String,
    val title: String,
    val dimensionInches: String,
    val widthInches: Float,
    val heightInches: Float,
    val widthMm: Float,
    val heightMm: Float,
    val defaultGridRows: Int = 1,
    val defaultGridCols: Int = 1,
    val standardDpi: Int = 300,
    val description: String = "",
    val isCustom: Boolean = false
) {
    fun pixelWidth(isLandscape: Boolean = false): Int {
        val w = if (isLandscape) maxOf(widthInches, heightInches) else minOf(widthInches, heightInches)
        return (w * standardDpi).toInt()
    }

    fun pixelHeight(isLandscape: Boolean = false): Int {
        val h = if (isLandscape) minOf(widthInches, heightInches) else maxOf(widthInches, heightInches)
        return (h * standardDpi).toInt()
    }

    fun aspectRatio(isLandscape: Boolean = false): Float {
        val pw = pixelWidth(isLandscape).toFloat()
        val ph = pixelHeight(isLandscape).toFloat()
        return if (ph > 0f) pw / ph else 1.0f
    }

    companion object {
        val SIZE_4X6 = PrintPaperDefinition(
            id = "paper_4x6",
            title = "4 × 6 Inches",
            dimensionInches = "4\" × 6\"",
            widthInches = 4.0f,
            heightInches = 6.0f,
            widthMm = 101.6f,
            heightMm = 152.4f,
            defaultGridRows = 4,
            defaultGridCols = 2,
            description = "Standard postcard size. Fits 8 passport photos (2×4 grid) at 300 DPI."
        )

        val SIZE_5X7 = PrintPaperDefinition(
            id = "paper_5x7",
            title = "5 × 7 Inches",
            dimensionInches = "5\" × 7\"",
            widthInches = 5.0f,
            heightInches = 7.0f,
            widthMm = 127.0f,
            heightMm = 177.8f,
            defaultGridRows = 4,
            defaultGridCols = 3,
            description = "Medium studio size. Fits 12 passport photos (3×4 grid) at 300 DPI."
        )

        val SIZE_A4 = PrintPaperDefinition(
            id = "paper_a4",
            title = "A4 Sheet",
            dimensionInches = "8.27\" × 11.69\"",
            widthInches = 8.267f,
            heightInches = 11.693f,
            widthMm = 210.0f,
            heightMm = 297.0f,
            defaultGridRows = 7,
            defaultGridCols = 5,
            description = "Full A4 document paper. Fits 30–35 passport photos at 300 DPI."
        )

        val SIZE_LETTER = PrintPaperDefinition(
            id = "paper_letter",
            title = "US Letter",
            dimensionInches = "8.5\" × 11\"",
            widthInches = 8.5f,
            heightInches = 11.0f,
            widthMm = 215.9f,
            heightMm = 279.4f,
            defaultGridRows = 6,
            defaultGridCols = 5,
            description = "North American letter standard. Fits 30 passport photos at 300 DPI."
        )

        val entries: List<PrintPaperDefinition> = listOf(
            SIZE_4X6,
            SIZE_5X7,
            SIZE_A4,
            SIZE_LETTER
        )

        fun createCustomFromMm(
            title: String,
            widthMm: Float,
            heightMm: Float,
            dpi: Int = 300
        ): PrintPaperDefinition {
            val widthIn = widthMm / 25.4f
            val heightIn = heightMm / 25.4f
            val dimLabel = "${String.format(Locale.US, "%.1f", widthIn)}\" × ${String.format(Locale.US, "%.1f", heightIn)}\""
            return PrintPaperDefinition(
                id = "custom_paper_${System.currentTimeMillis()}",
                title = title.ifBlank { "${widthMm.toInt()}×${heightMm.toInt()} mm Custom" },
                dimensionInches = dimLabel,
                widthInches = widthIn,
                heightInches = heightIn,
                widthMm = widthMm,
                heightMm = heightMm,
                standardDpi = dpi,
                description = "Custom ${widthMm.toInt()}×${heightMm.toInt()} mm (${dimLabel}) sheet at ${dpi} DPI.",
                isCustom = true
            )
        }

        fun createCustomFromInches(
            title: String,
            widthInches: Float,
            heightInches: Float,
            dpi: Int = 300
        ): PrintPaperDefinition {
            return createCustomFromMm(
                title = title,
                widthMm = widthInches * 25.4f,
                heightMm = heightInches * 25.4f,
                dpi = dpi
            )
        }

        fun createCustomFromPixels(
            title: String,
            widthPx: Int,
            heightPx: Int,
            dpi: Int = 300
        ): PrintPaperDefinition {
            val widthIn = widthPx.toFloat() / dpi
            val heightIn = heightPx.toFloat() / dpi
            return createCustomFromInches(title, widthIn, heightIn, dpi)
        }
    }
}

/**
 * Custom Paper Size Dialog: Allows users to input arbitrary paper width and height
 * in millimeters, inches, or exact pixels at 300 DPI.
 */
@Composable
fun CustomPaperSizeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (PrintPaperDefinition) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedUnitTab by remember { mutableIntStateOf(0) } // 0: mm, 1: inches, 2: px (300 DPI)
    var widthInput by remember { mutableStateOf("100") }
    var heightInput by remember { mutableStateOf("150") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val unitLabels = listOf("Millimeters (mm)", "Inches (in)", "Pixels (300 DPI)")

    // Dynamic conversions for live preview
    val wFloat = widthInput.toFloatOrNull() ?: 0f
    val hFloat = heightInput.toFloatOrNull() ?: 0f

    val calculatedMmWidth = when (selectedUnitTab) {
        0 -> wFloat
        1 -> wFloat * 25.4f
        else -> (wFloat / 300f) * 25.4f
    }
    val calculatedMmHeight = when (selectedUnitTab) {
        0 -> hFloat
        1 -> hFloat * 25.4f
        else -> (hFloat / 300f) * 25.4f
    }

    val calculatedInchesWidth = calculatedMmWidth / 25.4f
    val calculatedInchesHeight = calculatedMmHeight / 25.4f

    val calculatedPxWidth = (calculatedInchesWidth * 300).toInt()
    val calculatedPxHeight = (calculatedInchesHeight * 300).toInt()

    // Estimate capacity for standard 35x45mm passport photos with 3mm margins
    val estCols = maxOf(1, ((calculatedMmWidth - 6) / 38).toInt())
    val estRows = maxOf(1, ((calculatedMmHeight - 6) / 48).toInt())
    val estCapacity = estCols * estRows

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Navy20),
            border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Description,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Custom Paper Size",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Input arbitrary paper or roll dimensions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral80
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Neutral80)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Paper Name / Printer Spec Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Paper Name / Roll Size") },
                    placeholder = { Text("e.g. 6×8 Studio, Epson Roll 12×18") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Neutral90
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Unit Selector Tabs
                TabRow(
                    selectedTabIndex = selectedUnitTab,
                    containerColor = Navy15,
                    contentColor = AccentCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedUnitTab]),
                            color = AccentCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    unitLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedUnitTab == index,
                            onClick = {
                                selectedUnitTab = index
                                when (index) {
                                    0 -> { widthInput = "100"; heightInput = "150" }
                                    1 -> { widthInput = "4.0"; heightInput = "6.0" }
                                    2 -> { widthInput = "1200"; heightInput = "1800" }
                                }
                            },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedUnitTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Width & Height Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = {
                            widthInput = it
                            errorMessage = null
                        },
                        label = { Text("Width (${unitLabelShort(selectedUnitTab)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Neutral90
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = {
                            heightInput = it
                            errorMessage = null
                        },
                        label = { Text("Height (${unitLabelShort(selectedUnitTab)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Neutral90
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Preview Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Navy10,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Miniature sheet aspect preview
                        val sheetAspect = if (calculatedMmHeight > 0f) (calculatedMmWidth / calculatedMmHeight).coerceIn(0.3f, 3.0f) else 1.0f
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .aspectRatio(sheetAspect)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .border(1.dp, AccentCyan, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "300 DPI",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy10
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Calibrated Print Resolution:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neutral80
                            )
                            Text(
                                text = "${calculatedPxWidth} × ${calculatedPxHeight} px @ 300 DPI",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", calculatedMmWidth)} × ${String.format(Locale.US, "%.1f", calculatedMmHeight)} mm (${String.format(Locale.US, "%.2f", calculatedInchesWidth)}\" × ${String.format(Locale.US, "%.2f", calculatedInchesHeight)}\")",
                                fontSize = 11.sp,
                                color = Neutral90
                            )
                            Text(
                                text = "Est. Capacity: ~${estCapacity} passport photos",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentTeal
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFB7185),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (calculatedMmWidth < 30f || calculatedMmHeight < 30f) {
                                errorMessage = "Minimum paper dimension is 30 mm (approx. 1.2 inches)"
                                return@Button
                            }
                            if (calculatedMmWidth > 1500f || calculatedMmHeight > 1500f) {
                                errorMessage = "Maximum paper dimension is 1500 mm (approx. 60 inches)"
                                return@Button
                            }

                            val finalTitle = title.ifBlank {
                                "${calculatedMmWidth.toInt()}×${calculatedMmHeight.toInt()} mm Custom"
                            }

                            val paper = PrintPaperDefinition.createCustomFromMm(
                                title = finalTitle,
                                widthMm = calculatedMmWidth,
                                heightMm = calculatedMmHeight
                            )
                            onConfirm(paper)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue60),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Select", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun unitLabelShort(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "mm"
        1 -> "in"
        else -> "px"
    }
}
