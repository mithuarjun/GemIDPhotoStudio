package com.gemid.photostudio.sheet

import android.graphics.RectF

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

object GridMatrixCalculator {

    private const val MM_TO_INCH = 1f / 25.4f

    /**
     * Calculates the dynamic grid matrix, maximizing photo count while preserving
     * exact 300 DPI passport proportions with symmetrical centering and cutting marks.
     */
    fun computeGrid(
        paper: PrintPaperDefinition,
        photoWidthMm: Float = 35f,
        photoHeightMm: Float = 45f,
        isLandscape: Boolean = false,
        marginMm: Float = 4.5f,
        spacingMm: Float = 1.8f,
        customCount: Int? = null,
        dpi: Int = 300
    ): GridMatrixResult {
        val sheetWidthPx = paper.pixelWidth(isLandscape)
        val sheetHeightPx = paper.pixelHeight(isLandscape)

        val photoWidthPx = (photoWidthMm * MM_TO_INCH * dpi).toInt()
        val photoHeightPx = (photoHeightMm * MM_TO_INCH * dpi).toInt()

        val minMarginPx = marginMm * MM_TO_INCH * dpi
        val spacingPx = spacingMm * MM_TO_INCH * dpi

        val printableWidth = sheetWidthPx - (2 * minMarginPx)
        val printableHeight = sheetHeightPx - (2 * minMarginPx)

        // 1. Calculate max fit
        val cols = maxOf(1, ((printableWidth + spacingPx) / (photoWidthPx + spacingPx)).toInt())
        val rows = maxOf(1, ((printableHeight + spacingPx) / (photoHeightPx + spacingPx)).toInt())
        val maxCapacity = cols * rows

        val activeCount = customCount?.coerceIn(1, maxCapacity) ?: maxCapacity

        // 2. Symmetric Centering: Calculate remaining space and divide equally into margins
        val gridTotalWidth = (cols * photoWidthPx) + ((cols - 1) * spacingPx)
        val gridTotalHeight = (rows * photoHeightPx) + ((rows - 1) * spacingPx)

        val marginXPx = maxOf(0f, (sheetWidthPx - gridTotalWidth) / 2f)
        val marginYPx = maxOf(0f, (sheetHeightPx - gridTotalHeight) / 2f)

        // 3. Generate Cell Rectangles
        val cells = ArrayList<CellPosition>()
        var itemIndex = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (itemIndex < activeCount) {
                    val left = marginXPx + (c * (photoWidthPx + spacingPx))
                    val top = marginYPx + (r * (photoHeightPx + spacingPx))
                    val right = left + photoWidthPx
                    val bottom = top + photoHeightPx

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

        // 4. Generate Cutting Marks (Crop lines & Crosshairs)
        val cutLines = ArrayList<CutLine>()
        val crosshairs = ArrayList<Crosshair>()
        val tickLength = 24f // length of outer edge cutting ticks in pixels

        // Outer margin guide ticks (extending to edge of paper for trimmer alignment)
        for (c in 0 until cols) {
            val cellLeft = marginXPx + (c * (photoWidthPx + spacingPx))
            val cellRight = cellLeft + photoWidthPx

            // Top ticks
            cutLines.add(CutLine(cellLeft, 0f, cellLeft, tickLength))
            cutLines.add(CutLine(cellRight, 0f, cellRight, tickLength))
            // Bottom ticks
            cutLines.add(CutLine(cellLeft, sheetHeightPx - tickLength, cellLeft, sheetHeightPx.toFloat()))
            cutLines.add(CutLine(cellRight, sheetHeightPx - tickLength, cellRight, sheetHeightPx.toFloat()))
        }

        for (r in 0 until rows) {
            val cellTop = marginYPx + (r * (photoHeightPx + spacingPx))
            val cellBottom = cellTop + photoHeightPx

            // Left ticks
            cutLines.add(CutLine(0f, cellTop, tickLength, cellTop))
            cutLines.add(CutLine(0f, cellBottom, tickLength, cellBottom))
            // Right ticks
            cutLines.add(CutLine(sheetWidthPx - tickLength, cellTop, sheetWidthPx.toFloat(), cellTop))
            cutLines.add(CutLine(sheetWidthPx - tickLength, cellBottom, sheetWidthPx.toFloat(), cellBottom))
        }

        // Crosshairs at cell intersections
        for (cell in cells) {
            val b = cell.bounds
            // Add subtle corner crosshairs
            crosshairs.add(Crosshair(b.left, b.top))
            crosshairs.add(Crosshair(b.right, b.top))
            crosshairs.add(Crosshair(b.left, b.bottom))
            crosshairs.add(Crosshair(b.right, b.bottom))
        }

        return GridMatrixResult(
            paper = paper,
            isLandscape = isLandscape,
            sheetWidthPx = sheetWidthPx,
            sheetHeightPx = sheetHeightPx,
            photoWidthPx = photoWidthPx,
            photoHeightPx = photoHeightPx,
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
