package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    onMove: (Float, Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val maxRadius = size.toPx() * 0.38f

                detectDragGestures(
                    onDragStart = { offset ->
                        val diff = offset - center
                        val dist = diff.getDistance()
                        thumbOffset = if (dist > maxRadius) {
                            diff * (maxRadius / dist)
                        } else {
                            diff
                        }
                        onMove(thumbOffset.x / maxRadius, thumbOffset.y / maxRadius)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val diff = change.position - center
                        val dist = diff.getDistance()
                        thumbOffset = if (dist > maxRadius) {
                            diff * (maxRadius / dist)
                        } else {
                            diff
                        }
                        onMove(thumbOffset.x / maxRadius, thumbOffset.y / maxRadius)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width * 0.45f
            val thumbRadius = this.size.width * 0.18f

            // Outer Base Plate
            drawCircle(
                color = Color(0x66101222),
                radius = outerRadius,
                center = center
            )
            // Outer Ring Border
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF9D00FF),
                        Color(0xFFFF1E43),
                        Color(0xFF00E5FF)
                    ),
                    center = center
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Crosshair guidelines
            val lineLen = outerRadius * 0.7f
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(center.x - lineLen, center.y),
                end = Offset(center.x + lineLen, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(center.x, center.y - lineLen),
                end = Offset(center.x, center.y + lineLen),
                strokeWidth = 1.5.dp.toPx()
            )

            // Inner Thumb Stick
            val thumbCenter = center + thumbOffset
            drawCircle(
                color = Color(0x4400E5FF),
                radius = thumbRadius + 4.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF005B9F)),
                    center = thumbCenter,
                    radius = thumbRadius
                ),
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius * 0.35f,
                center = thumbCenter
            )
        }
    }
}
