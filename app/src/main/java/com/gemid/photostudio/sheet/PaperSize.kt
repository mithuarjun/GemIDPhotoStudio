package com.gemid.photostudio.sheet

/**
 * Standard & Custom Paper Sizes calibrated for photo studio minilabs and document printing at 300 DPI.
 */
data class PaperSize(
    val title: String,
    val dimensionInches: String,
    val definition: PrintPaperDefinition,
    val isCustom: Boolean = false
) {
    val id: String get() = definition.id
    val widthMm: Float get() = definition.widthMm
    val heightMm: Float get() = definition.heightMm
    val widthInches: Float get() = definition.widthInches
    val heightInches: Float get() = definition.heightInches
    val standardDpi: Int get() = definition.standardDpi

    fun pixelWidth(isLandscape: Boolean = false): Int = definition.pixelWidth(isLandscape)
    fun pixelHeight(isLandscape: Boolean = false): Int = definition.pixelHeight(isLandscape)
    fun aspectRatio(isLandscape: Boolean = false): Float = definition.aspectRatio(isLandscape)

    companion object {
        val POSTCARD_4X6 = PaperSize(
            title = "4 × 6 Inch",
            dimensionInches = "4\" × 6\"",
            definition = PrintPaperDefinition.SIZE_4X6
        )
        val SIZE_5X7 = PaperSize(
            title = "5 × 7 Inch",
            dimensionInches = "5\" × 7\"",
            definition = PrintPaperDefinition.SIZE_5X7
        )
        val A4 = PaperSize(
            title = "A4 Sheet",
            dimensionInches = "8.27\" × 11.69\"",
            definition = PrintPaperDefinition.SIZE_A4
        )
        val LETTER = PaperSize(
            title = "US Letter",
            dimensionInches = "8.5\" × 11\"",
            definition = PrintPaperDefinition.SIZE_LETTER
        )

        val standardEntries: List<PaperSize> = listOf(POSTCARD_4X6, SIZE_5X7, A4, LETTER)
        val entries: List<PaperSize> get() = standardEntries

        fun fromDefinition(definition: PrintPaperDefinition): PaperSize {
            return PaperSize(
                title = definition.title,
                dimensionInches = definition.dimensionInches,
                definition = definition,
                isCustom = definition.isCustom
            )
        }
    }
}
