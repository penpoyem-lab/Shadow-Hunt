package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MugshotNeonLime

/**
 * Renders the signature "Oddball" creature avatars from the Last Seen Mugshot interface.
 */
@Composable
fun OddballAvatar(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF1E283A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.85f)) {
            val w = this.size.width
            val h = this.size.height

            when (avatarId.lowercase()) {
                "turnip", "john", "purple" -> {
                    // John's sleepy purple droplet / turnip Oddball
                    // Sprout on top
                    val sproutPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.18f)
                        quadraticBezierTo(w * 0.35f, h * 0.04f, w * 0.38f, 0f)
                        quadraticBezierTo(w * 0.52f, h * 0.08f, w * 0.5f, h * 0.18f)
                        quadraticBezierTo(w * 0.65f, h * 0.04f, w * 0.62f, 0f)
                        quadraticBezierTo(w * 0.48f, h * 0.08f, w * 0.5f, h * 0.18f)
                    }
                    drawPath(sproutPath, color = MugshotNeonLime)

                    // Teardrop body
                    val bodyPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.95f)
                        cubicTo(w * 0.15f, h * 0.82f, w * 0.18f, h * 0.35f, w * 0.5f, h * 0.2f)
                        cubicTo(w * 0.82f, h * 0.35f, w * 0.85f, h * 0.82f, w * 0.5f, h * 0.95f)
                        close()
                    }
                    drawPath(
                        path = bodyPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF9B51E0), Color(0xFF6B2FB8), Color(0xFF4A188A))
                        )
                    )
                    drawPath(bodyPath, color = Color(0xFF2C1058), style = Stroke(width = w * 0.04f))

                    // Sleepy eyes with drooping eyelids
                    val eyeW = w * 0.22f
                    val eyeH = h * 0.14f
                    // Left eye
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.25f, h * 0.44f),
                        size = Size(eyeW, eyeH)
                    )
                    drawCircle(
                        color = Color(0xFF2C1058),
                        radius = eyeW * 0.32f,
                        center = Offset(w * 0.36f, h * 0.52f)
                    )
                    // Sleepy eyelid
                    drawArc(
                        color = Color(0xFF6B2FB8),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(w * 0.25f, h * 0.42f),
                        size = Size(eyeW, eyeH * 0.9f)
                    )

                    // Right eye
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.53f, h * 0.44f),
                        size = Size(eyeW, eyeH)
                    )
                    drawCircle(
                        color = Color(0xFF2C1058),
                        radius = eyeW * 0.32f,
                        center = Offset(w * 0.64f, h * 0.52f)
                    )
                    // Sleepy eyelid
                    drawArc(
                        color = Color(0xFF6B2FB8),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(w * 0.53f, h * 0.42f),
                        size = Size(eyeW, eyeH * 0.9f)
                    )
                }

                "star", "spiky", "hunter" -> {
                    // Star spiky cyan creature
                    val center = Offset(w * 0.5f, h * 0.52f)
                    val outerR = w * 0.45f
                    val innerR = w * 0.30f
                    val starPath = Path()
                    val points = 12
                    for (i in 0 until points * 2) {
                        val angle = (i * Math.PI / points).toFloat() - (Math.PI / 2).toFloat()
                        val r = if (i % 2 == 0) outerR else innerR
                        val x = center.x + r * kotlin.math.cos(angle)
                        val y = center.y + r * kotlin.math.sin(angle)
                        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                    }
                    starPath.close()

                    drawPath(
                        path = starPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00F2FE), Color(0xFF00C9A7), Color(0xFF0D5C75))
                        )
                    )
                    drawPath(starPath, color = Color(0xFF042838), style = Stroke(width = w * 0.04f))

                    // Two wide surprised circular eyes
                    val rEye = w * 0.12f
                    drawCircle(Color.White, rEye, Offset(w * 0.36f, h * 0.5f))
                    drawCircle(Color(0xFF042838), rEye * 0.5f, Offset(w * 0.36f, h * 0.5f))
                    drawCircle(Color.White, rEye * 0.18f, Offset(w * 0.33f, h * 0.47f))

                    drawCircle(Color.White, rEye, Offset(w * 0.64f, h * 0.5f))
                    drawCircle(Color(0xFF042838), rEye * 0.5f, Offset(w * 0.64f, h * 0.5f))
                    drawCircle(Color.White, rEye * 0.18f, Offset(w * 0.61f, h * 0.47f))

                    // Cute little mouth
                    drawCircle(Color(0xFF042838), w * 0.04f, Offset(w * 0.5f, h * 0.66f))
                }

                else -> {
                    // Krish's Pin Cyclops Oddball (Default)
                    // Antenna on top
                    drawLine(
                        color = MugshotNeonLime,
                        start = Offset(w * 0.5f, h * 0.22f),
                        end = Offset(w * 0.5f, h * 0.06f),
                        strokeWidth = w * 0.05f
                    )
                    drawCircle(MugshotNeonLime, w * 0.07f, Offset(w * 0.5f, h * 0.06f))

                    // Map pin shaped rounded head & pointed chin
                    val pinPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.94f)
                        cubicTo(w * 0.16f, h * 0.75f, w * 0.12f, h * 0.38f, w * 0.5f, h * 0.22f)
                        cubicTo(w * 0.88f, h * 0.38f, w * 0.84f, h * 0.75f, w * 0.5f, h * 0.94f)
                        close()
                    }
                    drawPath(
                        path = pinPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF0052CC))
                        )
                    )
                    drawPath(pinPath, color = Color(0xFF002244), style = Stroke(width = w * 0.04f))

                    // Big center cyclops eye with purple iris
                    val eyeCenter = Offset(w * 0.5f, h * 0.54f)
                    val eyeRadius = w * 0.22f
                    drawCircle(Color.White, eyeRadius, eyeCenter)
                    drawCircle(Color(0xFF7928CA), eyeRadius * 0.68f, eyeCenter)
                    drawCircle(Color(0xFF1E0A3C), eyeRadius * 0.42f, eyeCenter)
                    // Highlight glare
                    drawCircle(Color.White, eyeRadius * 0.22f, Offset(eyeCenter.x - eyeRadius * 0.25f, eyeCenter.y - eyeRadius * 0.25f))
                }
            }
        }
    }
}
