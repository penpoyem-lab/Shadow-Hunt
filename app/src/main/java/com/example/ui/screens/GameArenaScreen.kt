package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ShadowHuntEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.*

enum class MiniMapDisplayMode {
    CORNER,     // Compact floating HUD in top-right (138dp)
    EXPANDED,   // Detailed tactical overlay (270dp)
    COLLAPSED   // Minimized floating pill icon
}

@Composable
fun GameArenaScreen(
    engine: ShadowHuntEngine,
    onPauseOrExit: () -> Unit
) {
    val players by engine.players.collectAsState()
    val safeZone by engine.safeZone.collectAsState()
    val gamePhase by engine.gamePhase.collectAsState()
    val remainingSeconds by engine.remainingSeconds.collectAsState()
    val sonarActive by engine.sonarPingActive.collectAsState()
    val sonarProgress by engine.sonarPingProgress.collectAsState()
    val alertMessage by engine.alertMessage.collectAsState()

    val localPlayer = players.find { it.isLocalPlayer } ?: players.firstOrNull()
    val isSeeker = localPlayer?.role == PlayerRole.SEEKER
    val totalRemainingPlayers = players.count { !it.isTagged }

    // Toggleable Mini-Map Mode
    var miniMapMode by remember { mutableStateOf(MiniMapDisplayMode.CORNER) }
    var showExitLockDialog by remember { mutableStateOf(false) }

    // Enforce: No one can exit the app when the game is running
    BackHandler(enabled = gamePhase != GamePhase.GAME_OVER) {
        showExitLockDialog = true
    }

    // =========================================================================
    // TIMER-SYNCED SAFE ZONE URGENCY & PULSING ANIMATIONS
    // =========================================================================
    val countdown = safeZone.countdownSeconds
    val isShrinking = safeZone.isShrinking

    // Urgency level from 0.0 (calm) to 1.0 (critical danger)
    val urgencyLevel = when {
        isShrinking -> 1.0f
        countdown <= 5 -> 0.88f
        countdown <= 10 -> 0.70f
        countdown <= 20 -> 0.40f
        else -> 0.15f
    }

    // Dynamic pulse duration synced to timer: faster when countdown is low or shrinking
    val pulseDurationMillis = when {
        isShrinking -> 380
        countdown <= 5 -> 480
        countdown <= 10 -> 700
        countdown <= 20 -> 1000
        else -> 1500
    }

    val infiniteTransition = rememberInfiniteTransition(label = "safeZoneUrgencyPulse")

    // Rhythmic breathing alpha
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    // Breathing sine wave for organic boundary movement
    val breathingPhase = pulseProgress * 2f * PI.toFloat()
    val breathingOffsetPx = sin(breathingPhase) * (2f + urgencyLevel * 5f)

    // Expanding shockwave ring from the perimeter
    val shockwaveProgress = pulseProgress
    val shockwaveExpansionDp = (8f + urgencyLevel * 20f).dp
    val shockwaveAlpha = (1f - shockwaveProgress) * (0.35f + urgencyLevel * 0.55f)

    // Continuous 360° rotating radar sweep needle for minimap
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweepAngle"
    )

    // Local player distance to safe zone perimeter
    val pX = localPlayer?.x ?: 500f
    val pY = localPlayer?.y ?: 500f
    val distToZoneCenter = hypot(pX - safeZone.centerX, pY - safeZone.centerY)
    val distToPerimeter = distToZoneCenter - safeZone.currentRadius
    val isInsideSafeZone = distToPerimeter <= 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // =========================================================================
        // 1. PRIMARY 2D VIRTUAL ARENA CANVAS
        // =========================================================================
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // Primary camera centered on local player
            val primaryZoom = 1.15f
            val cameraOffsetX = canvasWidthPx / 2f - pX * primaryZoom
            val cameraOffsetY = canvasHeightPx / 2f - pY * primaryZoom

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Grid Background (Cyberpunk Arena Floor)
                val gridSize = 60f * primaryZoom
                val startX = (cameraOffsetX % gridSize)
                val startY = (cameraOffsetY % gridSize)

                var x = startX
                while (x < canvasWidthPx) {
                    drawLine(
                        color = Color(0x1800E5FF),
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeightPx),
                        strokeWidth = 1f
                    )
                    x += gridSize
                }

                var y = startY
                while (y < canvasHeightPx) {
                    drawLine(
                        color = Color(0x1800E5FF),
                        start = Offset(0f, y),
                        end = Offset(canvasWidthPx, y),
                        strokeWidth = 1f
                    )
                    y += gridSize
                }

                // 2. Arena Boundary (0..1000)
                val arenaLeft = cameraOffsetX
                val arenaTop = cameraOffsetY
                val arenaSize = 1000f * primaryZoom

                drawRect(
                    color = Color(0x2217192C),
                    topLeft = Offset(arenaLeft, arenaTop),
                    size = Size(arenaSize, arenaSize)
                )
                drawRect(
                    color = Color(0x889D00FF),
                    topLeft = Offset(arenaLeft, arenaTop),
                    size = Size(arenaSize, arenaSize),
                    style = Stroke(width = 3.dp.toPx())
                )

                // 3. Obstacles (Buildings, Walls, Crates)
                engine.obstacles.forEach { obs ->
                    val obsScreenX = cameraOffsetX + obs.x * primaryZoom
                    val obsScreenY = cameraOffsetY + obs.y * primaryZoom
                    val obsWidth = obs.width * primaryZoom
                    val obsHeight = obs.height * primaryZoom

                    drawRoundRect(
                        color = Color(0xFF141728),
                        topLeft = Offset(obsScreenX, obsScreenY),
                        size = Size(obsWidth, obsHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF9D00FF)),
                            start = Offset(obsScreenX, obsScreenY),
                            end = Offset(obsScreenX + obsWidth, obsScreenY + obsHeight)
                        ),
                        topLeft = Offset(obsScreenX, obsScreenY),
                        size = Size(obsWidth, obsHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 4. ANIMATED SAFE ZONE BOUNDARY WITH TIMER-SYNCED PULSE
                val zoneCenterScreenX = cameraOffsetX + safeZone.centerX * primaryZoom
                val zoneCenterScreenY = cameraOffsetY + safeZone.centerY * primaryZoom
                val baseRadiusScreen = safeZone.currentRadius * primaryZoom
                val animatedRadiusScreen = baseRadiusScreen + breathingOffsetPx

                // Inner Safe Area Tint (Soft Green when calm, soft amber/red when urgent)
                val safeAreaTint = if (urgencyLevel > 0.7f) Color(0x22FF1E43) else Color(0x1A00E676)
                drawCircle(
                    color = safeAreaTint,
                    radius = animatedRadiusScreen,
                    center = Offset(zoneCenterScreenX, zoneCenterScreenY)
                )

                // Pulsing Expanding Shockwave Ring on Boundary
                val shockwaveRadius = animatedRadiusScreen + (shockwaveExpansionDp.toPx() * shockwaveProgress)
                val shockwaveColor = if (urgencyLevel > 0.5f) Color(0xFFFF1E43) else Color(0xFF00E5FF)
                drawCircle(
                    color = shockwaveColor.copy(alpha = shockwaveAlpha),
                    radius = shockwaveRadius,
                    center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                    style = Stroke(width = (2f + urgencyLevel * 2f).dp.toPx())
                )

                // Main Pulsing Boundary Ring
                val ringColors = if (urgencyLevel > 0.6f) {
                    listOf(
                        Color(0xFFFF1E43),
                        Color(0xFFFFD700),
                        Color(0xFFFF1E43)
                    )
                } else {
                    listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF00E676),
                        Color(0xFF00E5FF)
                    )
                }

                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = ringColors,
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY)
                    ),
                    radius = animatedRadiusScreen,
                    center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                    style = Stroke(width = (3.5f + urgencyLevel * 2.5f).dp.toPx())
                )

                // Additional Warning Dashed Ring if Shrinking or in Zone Warning phase
                if (isShrinking || gamePhase == GamePhase.ZONE_WARNING) {
                    drawCircle(
                        color = Color(0xFFFF1E43).copy(alpha = 0.8f),
                        radius = animatedRadiusScreen + 8.dp.toPx(),
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), pulseProgress * 26f)
                        )
                    )
                }

                // 5. Sonar Radar Ping Wave
                if (sonarActive) {
                    val pingRadiusScreen = sonarProgress * 600f * primaryZoom
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = (1f - sonarProgress) * 0.8f),
                        radius = pingRadiusScreen,
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 6. Draw Players (Seekers in Red, Runners in Blue)
                players.forEach { p ->
                    val pScreenX = cameraOffsetX + p.x * primaryZoom
                    val pScreenY = cameraOffsetY + p.y * primaryZoom

                    val isPlayerSeeker = p.role == PlayerRole.SEEKER
                    val isTaggedRunner = p.isTagged

                    if (isTaggedRunner) {
                        drawCircle(
                            color = Color(0x446B7280),
                            radius = 12f * primaryZoom,
                            center = Offset(pScreenX, pScreenY)
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(pScreenX - 8f * primaryZoom, pScreenY - 8f * primaryZoom),
                            end = Offset(pScreenX + 8f * primaryZoom, pScreenY + 8f * primaryZoom),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(pScreenX + 8f * primaryZoom, pScreenY - 8f * primaryZoom),
                            end = Offset(pScreenX - 8f * primaryZoom, pScreenY + 8f * primaryZoom),
                            strokeWidth = 2f
                        )
                    } else if (isPlayerSeeker) {
                        // Seeker Red Glow & Capture Radius
                        drawCircle(
                            color = Color(0x22FF1E43),
                            radius = 65f * primaryZoom,
                            center = Offset(pScreenX, pScreenY)
                        )
                        drawCircle(
                            color = Color(0x66FF1E43),
                            radius = 65f * primaryZoom,
                            center = Offset(pScreenX, pScreenY),
                            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                        drawCircle(
                            color = Color(0xFFFF1E43),
                            radius = 16f * primaryZoom,
                            center = Offset(pScreenX, pScreenY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f * primaryZoom,
                            center = Offset(pScreenX, pScreenY - 4f * primaryZoom)
                        )
                    } else {
                        // Runner Cyan Marker
                        val isLocal = p.isLocalPlayer
                        val isRevealed = isLocal || sonarActive || !isSeeker

                        if (isRevealed) {
                            drawCircle(
                                color = Color(0x3300E5FF),
                                radius = 22f * primaryZoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 14f * primaryZoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f * primaryZoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                        } else {
                            drawCircle(
                                color = Color(0x1A00E5FF),
                                radius = 8f * primaryZoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                        }
                    }

                    if (p.isLocalPlayer) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = 24f * primaryZoom,
                            center = Offset(pScreenX, pScreenY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. TOP BAR HUD (Clock, Remaining Players, Role Indicator)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pause / Exit button with match lockdown
                IconButton(
                    onClick = { showExitLockDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(1.dp, DarkCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }

                // Match Clock
                GlassPanel(
                    shape = RoundedCornerShape(16.dp),
                    borderColor = Color(0xFF383B5E),
                    backgroundColor = DarkSurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Remaining Players & MiniMap Mode Selector Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPanel(
                        shape = RoundedCornerShape(14.dp),
                        borderColor = Color(0xFF383B5E),
                        backgroundColor = DarkSurfaceGlass,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Players: $totalRemainingPlayers",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Mini-Map Toggle Button
                    IconButton(
                        onClick = {
                            miniMapMode = when (miniMapMode) {
                                MiniMapDisplayMode.CORNER -> MiniMapDisplayMode.EXPANDED
                                MiniMapDisplayMode.EXPANDED -> MiniMapDisplayMode.COLLAPSED
                                MiniMapDisplayMode.COLLAPSED -> MiniMapDisplayMode.CORNER
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (miniMapMode != MiniMapDisplayMode.COLLAPSED) ElectricBlue else DarkSurfaceGlass)
                            .border(1.dp, ElectricBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = when (miniMapMode) {
                                MiniMapDisplayMode.CORNER -> Icons.Default.Map
                                MiniMapDisplayMode.EXPANDED -> Icons.Default.FullscreenExit
                                MiniMapDisplayMode.COLLAPSED -> Icons.Default.Radar
                            },
                            contentDescription = "Toggle Mini-Map",
                            tint = if (miniMapMode != MiniMapDisplayMode.COLLAPSED) Color.Black else ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SUB-HEADER STATUS (Safe Zone Status & Player Role)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Safe Zone Banner Pill with Pulsing Countdown Indicator
                GlassPanel(
                    shape = RoundedCornerShape(20.dp),
                    borderColor = if (isShrinking || urgencyLevel > 0.6f) NeonRed else ElectricBlue,
                    backgroundColor = DarkSurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isShrinking) NeonRed else if (countdown <= 10) GoldAccent else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isShrinking) {
                                "ZONE SHRINKING NOW!"
                            } else {
                                "Safe Zone in: %02ds (Stage %d/5)".format(countdown, safeZone.stage)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isShrinking) NeonRed else Color.White
                        )
                    }
                }

                // Player Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSeeker) NeonRed.copy(alpha = 0.25f) else ElectricBlue.copy(alpha = 0.25f))
                        .border(1.5.dp, if (isSeeker) NeonRed else ElectricBlue, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSeeker) Icons.Default.GpsFixed else Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = if (isSeeker) NeonRed else ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSeeker) "Seeker" else "Runner",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isSeeker) NeonRed else ElectricBlue
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 3. TOGGLEABLE CORNER MINIMAP (SMOOTH ANIMATED RADAR HUD)
        // =========================================================================
        AnimatedVisibility(
            visible = miniMapMode != MiniMapDisplayMode.COLLAPSED,
            enter = fadeIn(tween(250)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(200)) + scaleOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 112.dp, end = 16.dp)
        ) {
            val miniMapSize = if (miniMapMode == MiniMapDisplayMode.EXPANDED) 260.dp else 142.dp

            Surface(
                modifier = Modifier
                    .size(miniMapSize)
                    .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = ElectricBlue, spotColor = ElectricBlue)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.8.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (urgencyLevel > 0.6f) NeonRed else ElectricBlue,
                                NeonPurple,
                                if (urgencyLevel > 0.6f) NeonRed else ElectricBlue
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable {
                        // Tapping the minimap cycles through modes
                        miniMapMode = if (miniMapMode == MiniMapDisplayMode.CORNER) {
                            MiniMapDisplayMode.EXPANDED
                        } else {
                            MiniMapDisplayMode.CORNER
                        }
                    },
                color = Color(0xF2090A17)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Mini Map Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val mapW = size.width
                        val mapH = size.height
                        val scale = mapW / 1000f

                        // 1. Radar Background Grid
                        drawRect(color = Color(0xFF090A17))

                        // Grid crosslines
                        drawLine(
                            color = Color(0x2200E5FF),
                            start = Offset(mapW / 2f, 0f),
                            end = Offset(mapW / 2f, mapH),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x2200E5FF),
                            start = Offset(0f, mapH / 2f),
                            end = Offset(mapW, mapH / 2f),
                            strokeWidth = 1f
                        )

                        // 2. Radar Obstacles (Buildings)
                        engine.obstacles.forEach { obs ->
                            drawRoundRect(
                                color = Color(0xFF1D2038),
                                topLeft = Offset(obs.x * scale, obs.y * scale),
                                size = Size(obs.width * scale, obs.height * scale),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }

                        // 3. Danger Zone Overlay (Area outside safe zone tinted red)
                        val miniCenterX = safeZone.centerX * scale
                        val miniCenterY = safeZone.centerY * scale
                        val miniRadius = safeZone.currentRadius * scale
                        val miniAnimatedRadius = miniRadius + (breathingOffsetPx * scale)

                        // Draw outer danger tint
                        drawRect(color = Color(0x33FF1744))

                        // Clear/Highlight safe zone inside
                        drawCircle(
                            color = Color(0x2A00E676),
                            radius = miniAnimatedRadius,
                            center = Offset(miniCenterX, miniCenterY)
                        )

                        // Pulsing boundary ring on minimap
                        drawCircle(
                            color = if (urgencyLevel > 0.6f) NeonRed else ElectricBlue,
                            radius = miniAnimatedRadius,
                            center = Offset(miniCenterX, miniCenterY),
                            style = Stroke(width = (2f + urgencyLevel * 1.5f).dp.toPx())
                        )

                        // Shockwave ring on minimap
                        val miniShockwaveRadius = miniAnimatedRadius + (shockwaveExpansionDp.toPx() * scale * shockwaveProgress)
                        drawCircle(
                            color = if (urgencyLevel > 0.6f) NeonRed.copy(alpha = shockwaveAlpha) else ElectricBlue.copy(alpha = shockwaveAlpha),
                            radius = miniShockwaveRadius,
                            center = Offset(miniCenterX, miniCenterY),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // 4. Rotating 360° Military Radar Sweep Needle
                        val sweepRad = Math.toRadians(radarSweepAngle.toDouble())
                        val sweepEndX = (mapW / 2f + cos(sweepRad) * (mapW * 0.7f)).toFloat()
                        val sweepEndY = (mapH / 2f + sin(sweepRad) * (mapH * 0.7f)).toFloat()
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x0000E5FF), Color(0xCC00E5FF)),
                                start = Offset(mapW / 2f, mapH / 2f),
                                end = Offset(sweepEndX, sweepEndY)
                            ),
                            start = Offset(mapW / 2f, mapH / 2f),
                            end = Offset(sweepEndX, sweepEndY),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // 5. Player Markers on MiniMap
                        players.forEach { p ->
                            if (!p.isTagged) {
                                val mx = p.x * scale
                                val my = p.y * scale
                                if (p.role == PlayerRole.SEEKER) {
                                    // Seeker: Red dot with danger ring
                                    drawCircle(
                                        color = Color(0x55FF1E43),
                                        radius = 6f,
                                        center = Offset(mx, my)
                                    )
                                    drawCircle(
                                        color = Color(0xFFFF1E43),
                                        radius = 3.5f,
                                        center = Offset(mx, my)
                                    )
                                } else {
                                    // Runner: Cyan dot (if pinged or local)
                                    val isVisibleOnMap = p.isLocalPlayer || sonarActive || !isSeeker
                                    if (isVisibleOnMap) {
                                        drawCircle(
                                            color = Color(0xFF00E5FF),
                                            radius = 3f,
                                            center = Offset(mx, my)
                                        )
                                    }
                                }
                            }
                        }

                        // Local player indicator with bright ring and directional arrow
                        val localMX = pX * scale
                        val localMY = pY * scale
                        drawCircle(
                            color = Color.Yellow,
                            radius = 4.5f,
                            center = Offset(localMX, localMY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2f,
                            center = Offset(localMX, localMY)
                        )
                    }

                    // Top Bar Info inside MiniMap
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge (IN ZONE / DANGER)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isInsideSafeZone) Color(0xCC00E676) else Color(0xCCFF1E43))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isInsideSafeZone) "SAFE" else "DANGER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.Black
                            )
                        }

                        // Mode toggle icon
                        Icon(
                            imageVector = if (miniMapMode == MiniMapDisplayMode.EXPANDED) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = "Resize Minimap",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Bottom info label inside MiniMap
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isInsideSafeZone) "Stage ${safeZone.stage}/5" else "Out of Zone: ${distToPerimeter.toInt()}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = if (isInsideSafeZone) NeonGreen else NeonRed
                        )
                    }
                }
            }
        }

        // FLOATING ALERT TOAST NOTIFICATION
        AnimatedVisibility(
            visible = alertMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            GlassPanel(
                borderColor = NeonRed,
                backgroundColor = Color(0xEE1E050D),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alertMessage ?: "",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // =========================================================================
        // 4. BOTTOM CONTROLS PANEL (Virtual Joystick + Sprint + Tag/Stealth)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // 1. Virtual Joystick (Bottom Left)
            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 10.dp),
                size = 140.dp,
                onMove = { dx, dy ->
                    engine.setInputVector(dx, dy)
                }
            )

            // 2. Action Controls (Bottom Right)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SPRINT BUTTON (Boost speed with stamina ring)
                val stamina = localPlayer?.stamina ?: 100f
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(2.dp, ElectricBlue, CircleShape)
                        .clickable {
                            engine.startSprint(!(localPlayer?.isSprinting ?: false))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = ElectricBlue,
                            startAngle = -90f,
                            sweepAngle = (stamina / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Sprint",
                        tint = if (localPlayer?.isSprinting == true) ElectricBlue else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // If SEEKER: TAG BUTTON (Glows red when ready to capture!)
                if (isSeeker) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(NeonRed)
                            .border(2.5.dp, Color.White, CircleShape)
                            .clickable {
                                engine.tagNearbyRunners()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Tag Runner",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "TAG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // If RUNNER: STEALTH / CLOAK BUTTON
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.5.dp, NeonPurple, CircleShape)
                            .clickable {
                                engine.triggerStealth()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Stealth",
                            tint = NeonPurple,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Central In-Game Directive Toast
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0E101D))
                    .border(1.dp, if (isSeeker) NeonRed else GoldAccent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSeeker) Icons.Default.Adjust else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isSeeker) NeonRed else GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSeeker) "Approach runners to TAG them!" else "Avoid the seeker & stay in zone!",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Active Match Lockdown Exit Dialog
        if (showExitLockDialog) {
            GameLockExitDialog(
                onDismiss = { showExitLockDialog = false },
                onConfirmForfeit = {
                    showExitLockDialog = false
                    onPauseOrExit()
                }
            )
        }
    }
}
