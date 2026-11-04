package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.min

// Iconic Google Play 4-Color Palette
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

/**
 * Google Play Services Style Pull-to-Refresh Component.
 * Features:
 * - Floating circular card with elevation
 * - Iconic Google 4-color cycling spinner with sweep rotation
 * - Smooth spring physics with pull resistance
 * - Works natively on all Compose scrollable containers (LazyColumn, Column with verticalScroll)
 */
@Composable
fun GooglePlayPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTriggerDistanceDp: Float = 80f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { refreshTriggerDistanceDp.dp.toPx() }
    val maxDragPx = triggerPx * 1.6f

    var pullOffsetPx by remember { mutableFloatStateOf(0f) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (isRefreshing) triggerPx else pullOffsetPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pullOffset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If pulling up while already dragged down
                if (source == NestedScrollSource.UserInput && available.y < 0 && pullOffsetPx > 0) {
                    val consumed = available.y
                    pullOffsetPx = (pullOffsetPx + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // User pulling down from top
                if (source == NestedScrollSource.UserInput && available.y > 0 && !isRefreshing) {
                    val resistance = 0.5f * (1f - (pullOffsetPx / maxDragPx).coerceIn(0f, 0.8f))
                    val newOffset = (pullOffsetPx + available.y * resistance).coerceIn(0f, maxDragPx)
                    val delta = newOffset - pullOffsetPx
                    pullOffsetPx = newOffset
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffsetPx >= triggerPx && !isRefreshing) {
                    onRefresh()
                }
                pullOffsetPx = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = with(density) { (animatedOffsetPx * 0.4f).toDp() })
        ) {
            content()
        }

        // Floating Google Play Spinner Badge
        if (animatedOffsetPx > 6f || isRefreshing) {
            val progress = (animatedOffsetPx / triggerPx).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { (animatedOffsetPx - 10f).coerceAtLeast(14f).toDp() })
                    .size(46.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF141928))
                    .border(1.dp, Color(0xFF28324D), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                GooglePlaySpinner(
                    isRefreshing = isRefreshing,
                    pullProgress = progress
                )
            }
        }
    }
}

/**
 * Google Play style circular arc spinner that cycles Blue -> Red -> Yellow -> Green
 */
@Composable
fun GooglePlaySpinner(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "googlePlaySpin")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Dynamic color cycling: 0 = Blue, 1 = Red, 2 = Yellow, 3 = Green
    val colorIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() % 4 }),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    )

    val googleColors = listOf(GoogleBlue, GoogleRed, GoogleYellow, GoogleGreen)
    val activeColor = if (isRefreshing) googleColors[colorIndex % googleColors.size] else ElectricBlue

    val sweepAngle = if (isRefreshing) {
        // Breathing arc length between 45° and 270°
        160f
    } else {
        (pullProgress * 270f).coerceAtLeast(10f)
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(24.dp)
            .rotate(if (isRefreshing) rotation else pullProgress * 360f)
    ) {
        val strokeWidth = 3.2.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        drawArc(
            color = activeColor,
            startAngle = if (isRefreshing) 0f else -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
