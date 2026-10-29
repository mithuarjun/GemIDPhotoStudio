package com.gemid.photostudio.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemid.photostudio.data.model.PassportTemplate
import com.gemid.photostudio.ui.theme.AccentCyan
import com.gemid.photostudio.ui.theme.AccentTeal
import com.gemid.photostudio.ui.theme.Navy10

@Composable
fun CameraPassportOverlay(
    template: PassportTemplate,
    showGrid: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Subtle breathing animation for overlay guidelines
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate bounding box for passport photo aspect ratio
            val maxFrameWidth = canvasWidth * 0.82f
            val maxFrameHeight = canvasHeight * 0.72f

            val templateAspect = template.aspectRatio // width / height

            var frameWidth: Float
            var frameHeight: Float

            if (maxFrameWidth / templateAspect <= maxFrameHeight) {
                frameWidth = maxFrameWidth
                frameHeight = frameWidth / templateAspect
            } else {
                frameHeight = maxFrameHeight
                frameWidth = frameHeight * templateAspect
            }

            val frameLeft = (canvasWidth - frameWidth) / 2f
            val frameTop = (canvasHeight - frameHeight) / 2f - 30f // Slightly elevated for camera thumb room
            val frameRight = frameLeft + frameWidth
            val frameBottom = frameTop + frameHeight

            val cornerRadius = 24.dp.toPx()

            // 1. Draw Dark Scrim outside passport cutout
            val scrimPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(frameLeft, frameTop, frameRight, frameBottom),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
            }

            clipPath(scrimPath, clipOp = ClipOp.Difference) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.65f),
                    size = size
                )
            }

            // 2. Outer Passport Frame Border
            drawRoundRect(
                color = AccentCyan.copy(alpha = 0.9f * glowAlpha),
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 3.dp.toPx())
            )

            // 3. Optional Rule-of-Thirds Grid
            if (showGrid) {
                val gridStroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                val gridColor = Color.White.copy(alpha = 0.15f)

                // Vertical lines
                drawLine(
                    color = gridColor,
                    start = Offset(frameLeft + frameWidth / 3f, frameTop),
                    end = Offset(frameLeft + frameWidth / 3f, frameBottom),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(frameLeft + (frameWidth * 2f / 3f), frameTop),
                    end = Offset(frameLeft + (frameWidth * 2f / 3f), frameBottom),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )

                // Horizontal lines
                drawLine(
                    color = gridColor,
                    start = Offset(frameLeft, frameTop + frameHeight / 3f),
                    end = Offset(frameRight, frameTop + frameHeight / 3f),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(frameLeft, frameTop + (frameHeight * 2f / 3f)),
                    end = Offset(frameRight, frameTop + (frameHeight * 2f / 3f)),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )
            }

            // 4. Center Symmetry Axis
            val centerX = frameLeft + frameWidth / 2f
            drawLine(
                color = AccentTeal.copy(alpha = 0.4f),
                start = Offset(centerX, frameTop + 16.dp.toPx()),
                end = Offset(centerX, frameBottom - 16.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )

            // 5. Biometric Oval Face Outline (ICAO 70-80% height standard)
            val headOvalWidth = frameWidth * 0.52f
            val headOvalHeight = frameHeight * 0.62f
            val headOvalCenterX = centerX
            val headOvalCenterY = frameTop + (frameHeight * 0.42f)

            val headOvalLeft = headOvalCenterX - headOvalWidth / 2f
            val headOvalTop = headOvalCenterY - headOvalHeight / 2f

            drawOval(
                color = AccentCyan.copy(alpha = 0.7f * glowAlpha),
                topLeft = Offset(headOvalLeft, headOvalTop),
                size = Size(headOvalWidth, headOvalHeight),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                )
            )

            // 6. Eye-Level Guideline (approx 56% up from bottom of frame)
            val eyeLevelY = frameTop + (frameHeight * 0.38f)
            val eyeGuideHalfWidth = headOvalWidth * 0.44f

            drawLine(
                color = AccentTeal.copy(alpha = 0.85f * glowAlpha),
                start = Offset(headOvalCenterX - eyeGuideHalfWidth, eyeLevelY),
                end = Offset(headOvalCenterX + eyeGuideHalfWidth, eyeLevelY),
                strokeWidth = 2.dp.toPx()
            )

            // Eye marker crosshairs
            val eyeSpacing = headOvalWidth * 0.22f
            val leftEyeX = headOvalCenterX - eyeSpacing
            val rightEyeX = headOvalCenterX + eyeSpacing

            // Left Eye Circle & Cross
            drawCircle(
                color = AccentTeal.copy(alpha = 0.8f),
                radius = 6.dp.toPx(),
                center = Offset(leftEyeX, eyeLevelY),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = AccentTeal.copy(alpha = 0.9f),
                radius = 2.dp.toPx(),
                center = Offset(leftEyeX, eyeLevelY)
            )

            // Right Eye Circle & Cross
            drawCircle(
                color = AccentTeal.copy(alpha = 0.8f),
                radius = 6.dp.toPx(),
                center = Offset(rightEyeX, eyeLevelY),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = AccentTeal.copy(alpha = 0.9f),
                radius = 2.dp.toPx(),
                center = Offset(rightEyeX, eyeLevelY)
            )

            // 7. Chin Notch Guide
            val chinY = headOvalCenterY + headOvalHeight / 2f
            drawLine(
                color = AccentCyan.copy(alpha = 0.85f),
                start = Offset(headOvalCenterX - 16.dp.toPx(), chinY),
                end = Offset(headOvalCenterX + 16.dp.toPx(), chinY),
                strokeWidth = 3.dp.toPx()
            )

            // 8. Subtle Shoulder Outline Arcs
            val shoulderPath = Path().apply {
                // Left shoulder
                moveTo(headOvalCenterX - headOvalWidth * 0.28f, headOvalCenterY + headOvalHeight * 0.40f)
                cubicTo(
                    headOvalCenterX - headOvalWidth * 0.5f, headOvalCenterY + headOvalHeight * 0.55f,
                    headOvalCenterX - frameWidth * 0.45f, frameBottom - 10.dp.toPx(),
                    frameLeft + 10.dp.toPx(), frameBottom
                )
                // Right shoulder
                moveTo(headOvalCenterX + headOvalWidth * 0.28f, headOvalCenterY + headOvalHeight * 0.40f)
                cubicTo(
                    headOvalCenterX + headOvalWidth * 0.5f, headOvalCenterY + headOvalHeight * 0.55f,
                    headOvalCenterX + frameWidth * 0.45f, frameBottom - 10.dp.toPx(),
                    frameRight - 10.dp.toPx(), frameBottom
                )
            }
            drawPath(
                path = shoulderPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )
        }

        // Top HUD Information Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = Navy10.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AccentCyan.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Face,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Align eyes on line • Head inside oval",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.size(6.dp))

            // Standard format badge
            Text(
                text = "${template.title} (${template.widthMm.toInt()}×${template.heightMm.toInt()} mm)",
                color = AccentTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
