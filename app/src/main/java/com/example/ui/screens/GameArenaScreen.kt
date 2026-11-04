package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.ShadowHuntEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.*

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
    val activeRunnersCount = players.count { it.role == PlayerRole.RUNNER && !it.isTagged }
    val totalRemainingPlayers = players.count { !it.isTagged }

    var isTacticalMapExpanded by remember { mutableStateOf(false) }

    // Pulsing animations for Safe Zone ring and warning
    val infiniteTransition = rememberInfiniteTransition(label = "zonePulse")
    val zoneGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoneGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // MAIN 2D VIRTUAL ARENA CANVAS
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // Coordinate mapping: 0..1000 arena space -> Screen space with camera centered on local player
            val playerX = localPlayer?.x ?: 500f
            val playerY = localPlayer?.y ?: 500f

            // Zoom factor
            val zoom = if (isTacticalMapExpanded) 0.55f else 1.15f
            val cameraOffsetX = canvasWidthPx / 2f - playerX * zoom
            val cameraOffsetY = canvasHeightPx / 2f - playerY * zoom

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Grid Background (Cyberpunk Arena Floor)
                val gridSize = 60f * zoom
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
                val arenaSize = 1000f * zoom

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
                    val obsScreenX = cameraOffsetX + obs.x * zoom
                    val obsScreenY = cameraOffsetY + obs.y * zoom
                    val obsWidth = obs.width * zoom
                    val obsHeight = obs.height * zoom

                    // Building footprint
                    drawRoundRect(
                        color = Color(0xFF141728),
                        topLeft = Offset(obsScreenX, obsScreenY),
                        size = Size(obsWidth, obsHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    // Building roof border & neon edge
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

                // 4. Safe Zone Glowing Boundary Circle & Danger Zone Overlay
                val zoneCenterScreenX = cameraOffsetX + safeZone.centerX * zoom
                val zoneCenterScreenY = cameraOffsetY + safeZone.centerY * zoom
                val zoneRadiusScreen = safeZone.currentRadius * zoom

                // Inner Safe Area Tint (Soft Green / Cyan)
                drawCircle(
                    color = Color(0x1A00E676),
                    radius = zoneRadiusScreen,
                    center = Offset(zoneCenterScreenX, zoneCenterScreenY)
                )

                // Safe Zone Outer Ring (Glowing Neon Blue/Green)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = zoneGlowAlpha),
                            Color(0xFF00E676).copy(alpha = zoneGlowAlpha),
                            Color(0xFF00E5FF).copy(alpha = zoneGlowAlpha)
                        ),
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY)
                    ),
                    radius = zoneRadiusScreen,
                    center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                    style = Stroke(width = 4.dp.toPx())
                )

                // Danger Warning Red Ring if shrinking
                if (safeZone.isShrinking || gamePhase == GamePhase.ZONE_WARNING) {
                    drawCircle(
                        color = Color(0xFFFF1E43).copy(alpha = zoneGlowAlpha * 0.7f),
                        radius = zoneRadiusScreen + 8.dp.toPx(),
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 5. Sonar Radar Ping Wave
                if (sonarActive) {
                    val pingRadiusScreen = sonarProgress * 600f * zoom
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = (1f - sonarProgress) * 0.8f),
                        radius = pingRadiusScreen,
                        center = Offset(zoneCenterScreenX, zoneCenterScreenY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 6. Draw Players (Seekers in Red, Runners in Blue)
                players.forEach { p ->
                    val pScreenX = cameraOffsetX + p.x * zoom
                    val pScreenY = cameraOffsetY + p.y * zoom

                    val isPlayerSeeker = p.role == PlayerRole.SEEKER
                    val isTaggedRunner = p.isTagged

                    if (isTaggedRunner) {
                        // Tagged player ghost marker
                        drawCircle(
                            color = Color(0x446B7280),
                            radius = 12f * zoom,
                            center = Offset(pScreenX, pScreenY)
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(pScreenX - 8f * zoom, pScreenY - 8f * zoom),
                            end = Offset(pScreenX + 8f * zoom, pScreenY + 8f * zoom),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(pScreenX + 8f * zoom, pScreenY - 8f * zoom),
                            end = Offset(pScreenX - 8f * zoom, pScreenY + 8f * zoom),
                            strokeWidth = 2f
                        )
                    } else if (isPlayerSeeker) {
                        // SEEKER: Menacing Red Glow + Danger Radius
                        drawCircle(
                            color = Color(0x22FF1E43),
                            radius = 65f * zoom, // Tag range circle
                            center = Offset(pScreenX, pScreenY)
                        )
                        drawCircle(
                            color = Color(0x66FF1E43),
                            radius = 65f * zoom,
                            center = Offset(pScreenX, pScreenY),
                            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                        // Body
                        drawCircle(
                            color = Color(0xFFFF1E43),
                            radius = 16f * zoom,
                            center = Offset(pScreenX, pScreenY)
                        )
                        // Eyes
                        drawCircle(
                            color = Color.White,
                            radius = 4f * zoom,
                            center = Offset(pScreenX, pScreenY - 4f * zoom)
                        )
                    } else {
                        // RUNNER: Electric Blue / Cyan Marker
                        // Check if revealed by Sonar or close to local player
                        val isLocal = p.isLocalPlayer
                        val isRevealed = isLocal || sonarActive || !isSeeker

                        if (isRevealed) {
                            // Pulsing Runner Signal
                            drawCircle(
                                color = Color(0x3300E5FF),
                                radius = 22f * zoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 14f * zoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f * zoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                        } else {
                            // Sonar faint blip indicator
                            drawCircle(
                                color = Color(0x1A00E5FF),
                                radius = 8f * zoom,
                                center = Offset(pScreenX, pScreenY)
                            )
                        }
                    }

                    // Local Player Highlight Ring & Direction Indicator
                    if (p.isLocalPlayer) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = 24f * zoom,
                            center = Offset(pScreenX, pScreenY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // TOP BAR HUD (Timer, Remaining Players, Role Indicator, Safe Zone status)
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
                // Pause / Exit button
                IconButton(
                    onClick = onPauseOrExit,
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

                // Remaining Players & Aerial Map Toggle
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

                    // Tactical Radar / Google Map view toggle
                    IconButton(
                        onClick = { isTacticalMapExpanded = !isTacticalMapExpanded },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isTacticalMapExpanded) ElectricBlue else DarkSurfaceGlass)
                            .border(1.dp, ElectricBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Tactical Aerial Map",
                            tint = if (isTacticalMapExpanded) Color.Black else ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SUB-HEADER STATUS (Safe Zone Countdown & Player Role)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Safe Zone Pill
                GlassPanel(
                    shape = RoundedCornerShape(20.dp),
                    borderColor = if (safeZone.isShrinking) NeonRed else ElectricBlue,
                    backgroundColor = DarkSurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (safeZone.isShrinking) NeonRed else ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (safeZone.isShrinking) {
                                "Safe Zone Shrinking NOW!"
                            } else {
                                "Safe Zone in: %02ds (Stage %d/5)".format(safeZone.countdownSeconds, safeZone.stage)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (safeZone.isShrinking) NeonRed else Color.White
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

        // BOTTOM CONTROLS PANEL
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
                    // Stamina ring
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
    }
}
