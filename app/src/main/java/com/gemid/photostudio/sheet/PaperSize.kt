package com.gemid.photostudio.sheet

/**
 * Standard Paper Sizes calibrated for photo studio minilabs and document printing at 300 DPI.
 */
enum class PaperSize(
    val title: String,
    val dimensionInches: String,
    val definition: PrintPaperDefinition
) {
    POSTCARD_4X6(
        title = "4 × 6 Inch",
        dimensionInches = "4\" × 6\"",
        definition = PrintPaperDefinition.SIZE_4X6
    ),
    SIZE_5X7(
        title = "5 × 7 Inch",
        dimensionInches = "5\" × 7\"",
        definition = PrintPaperDefinition.SIZE_5X7
    ),
    A4(
        title = "A4 Sheet",
        dimensionInches = "8.27\" × 11.69\"",
        definition = PrintPaperDefinition.SIZE_A4
    ),
    LETTER(
        title = "US Letter",
        dimensionInches = "8.5\" × 11\"",
        definition = PrintPaperDefinition.SIZE_LETTER
    );

    val id: String get() = definition.id
    val widthMm: Float get() = definition.widthMm
    val heightMm: Float get() = definition.heightMm
    val widthInches: Float get() = definition.widthInches
    val heightInches: Float get() = definition.heightInches
    val standardDpi: Int get() = definition.standardDpi

    fun pixelWidth(isLandscape: Boolean = false): Int = definition.pixelWidth(isLandscape)
    fun pixelHeight(isLandscape: Boolean = false): Int = definition.pixelHeight(isLandscape)
    fun aspectRatio(isLandscape: Boolean = false): Float = definition.aspectRatio(isLandscape)
}
