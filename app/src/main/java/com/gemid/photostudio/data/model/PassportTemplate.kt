package com.gemid.photostudio.data.model

import androidx.compose.ui.graphics.Color

/**
 * Standard ID and Passport Photo specifications according to official
 * government and international (ICAO Doc 9303 / ISO/IEC 19794-5) standards.
 */
enum class PassportTemplate(
    val id: String,
    val title: String,
    val countryOrType: String,
    val widthMm: Float,
    val heightMm: Float,
    val standardDpi: Int = 300,
    val minHeadPercent: Float = 0.70f,
    val maxHeadPercent: Float = 0.80f,
    val recommendedBgColor: Color = Color.White,
    val description: String
) {
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

    val aspectRatio: Float
        get() = widthMm / heightMm

    val pixelWidth: Int
        get() = ((widthMm / 25.4f) * standardDpi).toInt()

    val pixelHeight: Int
        get() = ((heightMm / 25.4f) * standardDpi).toInt()
}
