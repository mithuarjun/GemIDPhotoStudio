package com.gemid.photostudio.data.model

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Straighten
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Blue60
import com.gemid.photostudio.ui.theme.Navy10
import com.gemid.photostudio.ui.theme.Navy15
import com.gemid.photostudio.ui.theme.Navy20
import com.gemid.photostudio.ui.theme.Navy25
import com.gemid.photostudio.ui.theme.Navy30
import com.gemid.photostudio.ui.theme.Neutral80
import com.gemid.photostudio.ui.theme.Neutral90
import java.util.Locale

/**
 * Universal Photo Specification contract.
 * Represents both official standardized government templates and dynamic user-created custom photo sizes.
 */
interface PhotoSpec {
    val id: String
    val title: String
    val countryOrType: String
    val widthMm: Float
    val heightMm: Float
    val standardDpi: Int
    val minHeadPercent: Float
    val maxHeadPercent: Float
    val recommendedBgColor: Color
    val description: String
    val isCustom: Boolean

    val aspectRatio: Float
        get() = if (heightMm > 0f) widthMm / heightMm else 1.0f

    val pixelWidth: Int
        get() = ((widthMm / 25.4f) * standardDpi).toInt()

    val pixelHeight: Int
        get() = ((heightMm / 25.4f) * standardDpi).toInt()

    val dimensionMmLabel: String
        get() = "${String.format(Locale.US, "%.1f", widthMm).removeSuffix(".0")}×${String.format(Locale.US, "%.1f", heightMm).removeSuffix(".0")} mm"

    val dimensionInchesLabel: String
        get() = "${String.format(Locale.US, "%.2f", widthMm / 25.4f)}\" × ${String.format(Locale.US, "%.2f", heightMm / 25.4f)}\""
}

/**
 * Standard ID and Passport Photo specifications according to official
 * government and international (ICAO Doc 9303 / ISO/IEC 19794-5) standards.
 */
enum class PassportTemplate(
    override val id: String,
    override val title: String,
    override val countryOrType: String,
    override val widthMm: Float,
    override val heightMm: Float,
    override val standardDpi: Int = 300,
    override val minHeadPercent: Float = 0.70f,
    override val maxHeadPercent: Float = 0.80f,
    override val recommendedBgColor: Color = Color.White,
    override val description: String
) : PhotoSpec {
    INDIAN_PASSPORT(
        id = "in_passport",
        title = "Indian Passport",
        countryOrType = "India",
        widthMm = 35f,
        heightMm = 45f,
        description = "Standard 35×45 mm with white background and 70-80% face coverage."
    ),
    US_PASSPORT(
        id = "us_passport",
        title = "US Passport / Visa",
        countryOrType = "United States",
        widthMm = 50.8f, // 2 inches
        heightMm = 50.8f, // 2 inches
        description = "Standard 2×2 inch (51×51 mm) square format on pure white background."
    ),
    INDIAN_PAN(
        id = "in_pan",
        title = "Indian PAN Card",
        countryOrType = "India",
        widthMm = 25f,
        heightMm = 35f,
        description = "Compact 25×35 mm format required for NSDL & UTIITSL applications."
    ),
    STAMP_SIZE(
        id = "stamp_size",
        title = "Stamp Size Photo",
        countryOrType = "Universal / India",
        widthMm = 20f,
        heightMm = 25f,
        description = "Universal 20×25 mm mini stamp format for job forms & registers."
    ),
    SCHENGEN_VISA(
        id = "schengen_visa",
        title = "Schengen Visa",
        countryOrType = "Europe",
        widthMm = 35f,
        heightMm = 45f,
        description = "European standard 35×45 mm with neutral light grey/white background."
    ),
    OCI_CARD(
        id = "oci_card",
        title = "OCI / PIO Card",
        countryOrType = "Overseas India",
        widthMm = 50.8f,
        heightMm = 50.8f,
        description = "2×2 inch photo required for Overseas Citizen of India registration."
    ),
    SSC_UPSC_EXAM(
        id = "in_exam_ssc",
        title = "SSC / UPSC Exams",
        countryOrType = "Govt. Exams",
        widthMm = 35f,
        heightMm = 45f,
        description = "35×45 mm with date of photo and candidate name banner."
    );

    override val isCustom: Boolean get() = false
}

/**
 * User-defined dynamic photo specification.
 * Supports arbitrary millimeter, inch, or pixel dimensions.
 */
data class CustomPhotoSpec(
    override val id: String = "custom_spec_${System.currentTimeMillis()}",
    override val title: String,
    override val countryOrType: String = "Custom Size",
    override val widthMm: Float,
    override val heightMm: Float,
    override val standardDpi: Int = 300,
    override val minHeadPercent: Float = 0.70f,
    override val maxHeadPercent: Float = 0.80f,
    override val recommendedBgColor: Color = Color.White,
    override val description: String = "${widthMm.toInt()}×${heightMm.toInt()} mm custom specification.",
    override val isCustom: Boolean = true
) : PhotoSpec {

    companion object {
        fun fromMm(
            title: String,
            widthMm: Float,
            heightMm: Float,
            countryOrType: String = "Custom Size"
        ): CustomPhotoSpec {
            return CustomPhotoSpec(
                title = title.ifBlank { "${widthMm.toInt()}×${heightMm.toInt()} mm Custom" },
                countryOrType = countryOrType,
                widthMm = widthMm,
                heightMm = heightMm,
                description = "${String.format(Locale.US, "%.1f", widthMm)}×${String.format(Locale.US, "%.1f", heightMm)} mm custom photo format."
            )
        }

        fun fromInches(
            title: String,
            widthInches: Float,
            heightInches: Float,
            countryOrType: String = "Custom Size"
        ): CustomPhotoSpec {
            val wMm = widthInches * 25.4f
            val hMm = heightInches * 25.4f
            return CustomPhotoSpec(
                title = title.ifBlank { "${String.format(Locale.US, "%.1f", widthInches)}\"×${String.format(Locale.US, "%.1f", heightInches)}\" Custom" },
                countryOrType = countryOrType,
                widthMm = wMm,
                heightMm = hMm,
                description = "${String.format(Locale.US, "%.2f", widthInches)}\" × ${String.format(Locale.US, "%.2f", heightInches)}\" custom photo format."
            )
        }

        fun fromPixels(
            title: String,
            widthPx: Int,
            heightPx: Int,
            dpi: Int = 300,
            countryOrType: String = "Custom Size"
        ): CustomPhotoSpec {
            val wMm = (widthPx.toFloat() / dpi) * 25.4f
            val hMm = (heightPx.toFloat() / dpi) * 25.4f
            return CustomPhotoSpec(
                title = title.ifBlank { "${widthPx}×${heightPx} px @ ${dpi}DPI" },
                countryOrType = countryOrType,
                widthMm = wMm,
                heightMm = hMm,
                standardDpi = dpi,
                description = "${widthPx}×${heightPx} px at ${dpi} DPI (${wMm.toInt()}×${hMm.toInt()} mm)."
            )
        }
    }
}

/**
 * Dialog allowing users to define arbitrary photo specifications in mm, inches, or pixels at 300 DPI.
 */
@Composable
fun CustomPhotoSpecDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (CustomPhotoSpec) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedUnitTab by remember { mutableIntStateOf(0) } // 0: mm, 1: inches, 2: px (300 DPI)
    var widthInput by remember { mutableStateOf("35") }
    var heightInput by remember { mutableStateOf("45") }
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

    val calculatedPxWidth = ((calculatedMmWidth / 25.4f) * 300).toInt()
    val calculatedPxHeight = ((calculatedMmHeight / 25.4f) * 300).toInt()

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
                                Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Custom Photo Size",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Define arbitrary passport or ID format",
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

                // Title / Job Name Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Name / Job ID") },
                    placeholder = { Text("e.g. Dubai Visa 40×60, Bank Exam") },
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
                                // Set reasonable default values on switch
                                when (index) {
                                    0 -> { widthInput = "35"; heightInput = "45" }
                                    1 -> { widthInput = "2.0"; heightInput = "2.0" }
                                    2 -> { widthInput = "413"; heightInput = "531" }
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
                        label = { Text("Width (${unitSuffix(selectedUnitTab)})") },
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
                        label = { Text("Height (${unitSuffix(selectedUnitTab)})") },
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
                        // Miniature aspect preview box
                        val aspect = if (calculatedMmHeight > 0f) (calculatedMmWidth / calculatedMmHeight).coerceIn(0.4f, 2.5f) else 1.0f
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .aspectRatio(aspect)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Blue60.copy(alpha = 0.4f))
                                .border(1.dp, AccentCyan, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${calculatedMmWidth.toInt()}×${calculatedMmHeight.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Calibrated Resolution at 300 DPI:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neutral80
                            )
                            Text(
                                text = "${calculatedPxWidth} × ${calculatedPxHeight} pixels",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", calculatedMmWidth)} × ${String.format(Locale.US, "%.1f", calculatedMmHeight)} mm (${String.format(Locale.US, "%.2f", calculatedMmWidth / 25.4f)}\" × ${String.format(Locale.US, "%.2f", calculatedMmHeight / 25.4f)}\")",
                                fontSize = 11.sp,
                                color = Neutral90
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
                            if (calculatedMmWidth < 10f || calculatedMmHeight < 10f) {
                                errorMessage = "Minimum photo dimension is 10 mm"
                                return@Button
                            }
                            if (calculatedMmWidth > 300f || calculatedMmHeight > 300f) {
                                errorMessage = "Maximum single photo dimension is 300 mm"
                                return@Button
                            }

                            val finalTitle = title.ifBlank {
                                "${calculatedMmWidth.toInt()}×${calculatedMmHeight.toInt()} mm Custom"
                            }

                            val spec = CustomPhotoSpec.fromMm(
                                title = finalTitle,
                                widthMm = calculatedMmWidth,
                                heightMm = calculatedMmHeight
                            )
                            onConfirm(spec)
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

private fun unitSuffix(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "mm"
        1 -> "in"
        else -> "px"
    }
}
