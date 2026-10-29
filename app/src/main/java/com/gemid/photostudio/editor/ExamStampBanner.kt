package com.gemid.photostudio.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Configuration and processor for Indian Exam Name & Date banner stamp overlays
 * (SSC, UPSC, State PSC, Railway, Banking, Police exams).
 */
data class ExamStampConfig(
    val isEnabled: Boolean = false,
    val candidateName: String = "",
    val photoDate: String = defaultTodayDate(),
    val isDarkBanner: Boolean = true // True: Black translucent banner with white text; False: White banner with black text
) {
    companion object {
        fun defaultTodayDate(): String {
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }
    }

    /**
     * Draws the exam name & date banner directly onto the bitmap canvas.
     */
    fun applyToBitmap(source: Bitmap): Bitmap {
        if (!isEnabled || (candidateName.isBlank() && photoDate.isBlank())) {
            return source
        }

        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = output.width.toFloat()
        val height = output.height.toFloat()

        // Banner height is approximately 15% of total photo height
        val bannerHeight = height * 0.16f
        val bannerTop = height - bannerHeight

        // 1. Draw Translucent Banner Strip
        val bgPaint = Paint().apply {
            color = if (isDarkBanner) {
                Color.argb(220, 10, 14, 23) // Deep black translucent
            } else {
                Color.argb(240, 255, 255, 255) // Pure white banner
            }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, bannerTop, width, height, bgPaint)

        // Subtle top separator line
        val borderPaint = Paint().apply {
            color = if (isDarkBanner) Color.argb(80, 255, 255, 255) else Color.argb(60, 0, 0, 0)
            strokeWidth = maxOf(1f, height * 0.003f)
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, bannerTop, width, bannerTop, borderPaint)

        // 2. Setup Text Paints
        val textColorVal = if (isDarkBanner) Color.WHITE else Color.BLACK
        val textSizeCalculated = bannerHeight * 0.32f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorVal
            textSize = textSizeCalculated
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorVal
            textSize = textSizeCalculated * 0.88f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val textBounds = Rect()

        // 3. Render Name Line
        val nameText = if (candidateName.startsWith("NAME:", ignoreCase = true)) {
            candidateName.uppercase(Locale.US)
        } else if (candidateName.isNotBlank()) {
            "NAME: ${candidateName.uppercase(Locale.US)}"
        } else {
            ""
        }

        // 4. Render Date Line
        val dateText = if (photoDate.startsWith("DOP:", ignoreCase = true) || photoDate.startsWith("DATE:", ignoreCase = true)) {
            photoDate.uppercase(Locale.US)
        } else if (photoDate.isNotBlank()) {
            "DOP: $photoDate"
        } else {
            ""
        }

        if (nameText.isNotBlank() && dateText.isNotBlank()) {
            val line1Y = bannerTop + (bannerHeight * 0.44f)
            val line2Y = bannerTop + (bannerHeight * 0.84f)
            canvas.drawText(nameText, centerX, line1Y, textPaint)
            canvas.drawText(dateText, centerX, line2Y, datePaint)
        } else {
            // Single line centered vertically
            val singleLine = nameText.ifBlank { dateText }
            val singleY = bannerTop + (bannerHeight * 0.62f)
            canvas.drawText(singleLine, centerX, singleY, textPaint)
        }

        return output
    }
}
