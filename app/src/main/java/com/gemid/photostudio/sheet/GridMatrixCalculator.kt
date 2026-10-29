package com.gemid.photostudio.sheet

import android.graphics.RectF
import kotlin.math.abs

data class CellPosition(
    val row: Int,
    val col: Int,
    val index: Int,
    val bounds: RectF
)

data class CutLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

data class Crosshair(
    val x: Float,
    val y: Float,
    val armLength: Float = 14f
)

data class GridMatrixResult(
    val paper: PrintPaperDefinition,
    val isLandscape: Boolean,
    val sheetWidthPx: Int,
    val sheetHeightPx: Int,
    val photoWidthPx: Int,
    val photoHeightPx: Int,
    val rows: Int,
    val cols: Int,
    val maxCapacity: Int,
    val activeCount: Int,
    val marginXPx: Float,
    val marginYPx: Float,
    val spacingXPx: Float,
    val spacingYPx: Float,
    val cells: List<CellPosition>,
    val cutLines: List<CutLine>,
    val crosshairs: List<Crosshair>
)

/**
 * Zero-Wastage Sheet Packing & Edge-to-Edge Grid Engine:
 * - 0% Paper Wastage: Photos extend edge-to-edge covering 100% of the paper surface.
 * - No Space Margin on Paper Edge: Margin is locked strictly to 0 mm on all 4 paper borders.
 * - Aspect-Ratio Optimized Orientation: Dynamically chooses portrait/landscape orientation
 *   minimizing cropping distortion for Indian passport and stamp sizes.
 * - Internal Hairline Cutting Grid: Generates precise interior cut lines and intersection crosshairs.
 */
object GridMatrixCalculator {

    const val MM_TO_INCH = 1f / 25.4f
    const val MIN_SAFE_MARGIN_MM = 0.0f // Zero margin on paper edge
    const val TIGHT_GUTTER_MM = 0.0f     // Zero gutter between photos (seamless edge-to-edge)

    fun computeGrid(
        paper: PrintPaperDefinition,
        photoWidthMm: Float = 35f,
        photoHeightMm: Float = 45f,
        isLandscape: Boolean = false,
        marginMm: Float = 0.0f,
        spacingMm: Float = 0.0f,
        customCount: Int? = null,
        dpi: Int = 300
    ): GridMatrixResult {
        val targetAspect = if (photoHeightMm > 0f) photoWidthMm / photoHeightMm else 0.778f

        // Evaluate both orientations with 0 margin
        val candidateLandscape = evaluatePacking(
            paper = paper,
            photoWidthMm = photoWidthMm,
            photoHeightMm = photoHeightMm,
            isLandscape = true,
            marginMm = marginMm,
            spacingMm = spacingMm,
            customCount = customCount,
            dpi = dpi
        )

        val candidatePortrait = evaluatePacking(
            paper = paper,
            photoWidthMm = photoWidthMm,
            photoHeightMm = photoHeightMm,
            isLandscape = false,
            marginMm = marginMm,
            spacingMm = spacingMm,
            customCount = customCount,
            dpi = dpi
        )

        return if (customCount != null) {
            // Pick orientation whose cell aspect ratio is closest to the desired photo aspect ratio
            val cellAspectLand = if (candidateLandscape.photoHeightPx > 0) {
                candidateLandscape.photoWidthPx.toFloat() / candidateLandscape.photoHeightPx.toFloat()
            } else 1.0f
            val cellAspectPort = if (candidatePortrait.photoHeightPx > 0) {
                candidatePortrait.photoWidthPx.toFloat() / candidatePortrait.photoHeightPx.toFloat()
            } else 1.0f

            val diffLand = abs(cellAspectLand - targetAspect)
            val diffPort = abs(cellAspectPort - targetAspect)

            if (diffLand < diffPort) candidateLandscape else candidatePortrait
        } else {
            if (candidateLandscape.maxCapacity > candidatePortrait.maxCapacity) candidateLandscape
            else if (candidatePortrait.maxCapacity > candidateLandscape.maxCapacity) candidatePortrait
            else if (isLandscape) candidateLandscape else candidatePortrait
        }
    }

    private fun evaluatePacking(
        paper: PrintPaperDefinition,
        photoWidthMm: Float,
        photoHeightMm: Float,
        isLandscape: Boolean,
        marginMm: Float,
        spacingMm: Float,
        customCount: Int?,
        dpi: Int
    ): GridMatrixResult {
        val sheetWidthPx = paper.pixelWidth(isLandscape)
        val sheetHeightPx = paper.pixelHeight(isLandscape)

        val safeMarginPx = marginMm * MM_TO_INCH * dpi
        val spacingPx = spacingMm * MM_TO_INCH * dpi

        val printableWidth = sheetWidthPx - (2 * safeMarginPx)
        val printableHeight = sheetHeightPx - (2 * safeMarginPx)

        // 1. Determine optimal columns and rows for zero paper wastage
        val (cols, rows) = if (customCount != null) {
            when (customCount) {
                6 -> if (isLandscape) 3 to 2 else 2 to 3
                12 -> if (isLandscape) 4 to 3 else 3 to 4
                18 -> if (isLandscape) 6 to 3 else 3 to 6
                24 -> if (isLandscape) 6 to 4 else 4 to 6
                else -> {
                    val c = kotlin.math.ceil(kotlin.math.sqrt(customCount.toDouble() * (sheetWidthPx.toDouble() / sheetHeightPx))).toInt()
                    val r = kotlin.math.ceil(customCount.toDouble() / c).toInt()
                    maxOf(1, c) to maxOf(1, r)
                }
            }
        } else {
            val photoWPx = (photoWidthMm * MM_TO_INCH * dpi).toInt().coerceAtLeast(100)
            val photoHPx = (photoHeightMm * MM_TO_INCH * dpi).toInt().coerceAtLeast(100)
            val c = maxOf(1, (printableWidth / photoWPx).toInt())
            val r = maxOf(1, (printableHeight / photoHPx).toInt())
            c to r
        }

        val maxCapacity = cols * rows
        val activeCount = customCount ?: maxCapacity

        // 2. Distribute dimensions edge-to-edge across the sheet (0 mm outer margin)
        val cellWidthF = (sheetWidthPx - ((cols - 1) * spacingPx)) / cols.toFloat()
        val cellHeightF = (sheetHeightPx - ((rows - 1) * spacingPx)) / rows.toFloat()

        val photoWPx = cellWidthF.toInt()
        val photoHPx = cellHeightF.toInt()

        val marginXPx = safeMarginPx
        val marginYPx = safeMarginPx

        // 3. Generate Cell Rectangles covering 100% of the sheet
        val cells = ArrayList<CellPosition>()
        var itemIndex = 0

        for (r in 0 until rows) {
            val top = marginYPx + (r * (cellHeightF + spacingPx))
            val bottom = if (r == rows - 1) sheetHeightPx - marginYPx else top + cellHeightF

            for (c in 0 until cols) {
                if (itemIndex < activeCount) {
                    val left = marginXPx + (c * (cellWidthF + spacingPx))
                    val right = if (c == cols - 1) sheetWidthPx - marginXPx else left + cellWidthF

                    cells.add(
                        CellPosition(
                            row = r,
                            col = c,
                            index = itemIndex,
                            bounds = RectF(left, top, right, bottom)
                        )
                    )
                    itemIndex++
                }
            }
        }

        // 4. Generate Internal Dividing Cut Lines (Dividers between photos)
        val cutLines = ArrayList<CutLine>()
        val crosshairs = ArrayList<Crosshair>()

        // Interior vertical cut lines
        for (c in 1 until cols) {
            val lineX = marginXPx + (c * (cellWidthF + spacingPx)) - (spacingPx / 2f)
            cutLines.add(CutLine(lineX, 0f, lineX, sheetHeightPx.toFloat()))
        }

        // Interior horizontal cut lines
        for (r in 1 until rows) {
            val lineY = marginYPx + (r * (cellHeightF + spacingPx)) - (spacingPx / 2f)
            cutLines.add(CutLine(0f, lineY, sheetWidthPx.toFloat(), lineY))
        }

        // Crosshairs at interior intersection points
        for (c in 1 until cols) {
            val lineX = marginXPx + (c * (cellWidthF + spacingPx)) - (spacingPx / 2f)
            for (r in 1 until rows) {
                val lineY = marginYPx + (r * (cellHeightF + spacingPx)) - (spacingPx / 2f)
                crosshairs.add(Crosshair(lineX, lineY))
            }
        }

        return GridMatrixResult(
            paper = paper,
            isLandscape = isLandscape,
            sheetWidthPx = sheetWidthPx,
            sheetHeightPx = sheetHeightPx,
            photoWidthPx = photoWPx,
            photoHeightPx = photoHPx,
            rows = rows,
            cols = cols,
            maxCapacity = maxCapacity,
            activeCount = activeCount,
            marginXPx = marginXPx,
            marginYPx = marginYPx,
            spacingXPx = spacingPx,
            spacingYPx = spacingPx,
            cells = cells,
            cutLines = cutLines,
            crosshairs = crosshairs
        )
    }
}
