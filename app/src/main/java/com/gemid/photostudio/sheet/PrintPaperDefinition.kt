package com.gemid.photostudio.sheet

/**
 * Standard Print Paper Specifications at 300 DPI (dots per inch).
 * Standardized for photo studio minilabs (Epson, DNP, Canon, Noritsu, Fuji Frontier).
 */
enum class PrintPaperDefinition(
    val id: String,
    val title: String,
    val dimensionInches: String,
    val widthInches: Float,
    val heightInches: Float,
    val widthMm: Float,
    val heightMm: Float,
    val defaultGridRows: Int,
    val defaultGridCols: Int,
    val standardDpi: Int = 300,
    val description: String
) {
    SIZE_4X6(
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
    ),
    SIZE_5X7(
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
    ),
    SIZE_A4(
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
    ),
    SIZE_LETTER(
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
    );

    fun pixelWidth(isLandscape: Boolean = false): Int {
        val w = if (isLandscape) maxOf(widthInches, heightInches) else minOf(widthInches, heightInches)
        return (w * standardDpi).toInt()
    }

    fun pixelHeight(isLandscape: Boolean = false): Int {
        val h = if (isLandscape) minOf(widthInches, heightInches) else maxOf(widthInches, heightInches)
        return (h * standardDpi).toInt()
    }

    fun aspectRatio(isLandscape: Boolean = false): Float {
        return pixelWidth(isLandscape).toFloat() / pixelHeight(isLandscape).toFloat()
    }
}
