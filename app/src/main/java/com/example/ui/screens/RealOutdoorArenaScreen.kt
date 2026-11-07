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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraMugshotCapture
import com.example.game.RealOutdoorGameEngine
import com.example.location.LiveLocationManager
import com.example.model.GamePhase
import com.example.model.PlayerRole
import com.example.model.RealGpsPlayer
import com.example.ui.components.GameLockExitDialog
import com.example.ui.components.OddballAvatar
import com.example.ui.map.DarkTacticalMap
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.sin

/**
 * Real Outdoor Tactical Arena Screen matching reference screenshots:
 *  - IMG_20261106_135118_bf5c3810.jpg
 *  - IMG_20261106_135256_3408d6ac.jpg
 *  - IMG_20261106_135044_137fd0c9.jpg
 *  - IMG_20261106_135240_9e1cc2b5.jpg
 *  - IMG_20261106_135207_501c9881.jpg
 *  - IMG_20261106_135224_3d72aa2e.jpg
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealOutdoorArenaScreen(
    engine: RealOutdoorGameEngine,
    locationManager: LiveLocationManager,
    onPauseOrExit: () -> Unit
) {
    val players by engine.players.collectAsState()
    val safeZone by engine.safeZone.collectAsState()
    val gamePhase by engine.gamePhase.collectAsState()
    val remainingSeconds by engine.remainingSeconds.collectAsState()
    val sonarActive by engine.sonarActive.collectAsState()
    val alertMessage by engine.alertMessage.collectAsState()
    val nearbyTarget by engine.nearbyTarget.collectAsState()
    val nearbyDistance by engine.nearbyDistance.collectAsState()
    val localHealthHp by engine.localHealthHp.collectAsState()
    val headStartSecondsLeft by engine.headStartSecondsLeft.collectAsState()
    val isHeadStartActive by engine.isHeadStartActive.collectAsState()
    val gameFeedMessages by engine.gameFeedMessages.collectAsState()

    val currentLocation by locationManager.currentLocation.collectAsState()
    val userLat = currentLocation?.latitude ?: safeZone.centerLatitude
    val userLon = currentLocation?.longitude ?: safeZone.centerLongitude
    val userBearing = currentLocation?.bearing ?: 0f

    val localPlayer = players.find { it.isLocalPlayer } ?: players.firstOrNull()
    val isSeeker = localPlayer?.role == PlayerRole.SEEKER
    val activeRunners = players.count { it.role == PlayerRole.RUNNER && !it.isTagged }

    var recenterTrigger by remember { mutableStateOf(0) }
    var showExitLockDialog by remember { mutableStateOf(false) }
    var showGameFeedSheet by remember { mutableStateOf(false) }
    var showSquadDialog by remember { mutableStateOf(false) }
    var hasDismissedHealthWarning by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var isCameraActive by remember { mutableStateOf(false) }

    // Enforce match lock
    BackHandler(enabled = gamePhase != GamePhase.GAME_OVER) {
        showExitLockDialog = true
    }

    // Distance calculations
    val distToCenter = if (userLat != 0.0 && safeZone.centerLatitude != 0.0) {
        LiveLocationManager.distanceMeters(userLat, userLon, safeZone.centerLatitude, safeZone.centerLongitude)
    } else 0f
    val isOutsideSafeZone = distToCenter > safeZone.currentRadiusMeters

    // Bearing towards center of safe zone (for the arrow in "LOSING HEALTH" card)
    val angleToCenterDeg = remember(userLat, userLon, safeZone.centerLatitude, safeZone.centerLongitude) {
        val dLat = safeZone.centerLatitude - userLat
        val dLon = safeZone.centerLongitude - userLon
        Math.toDegrees(atan2(dLon, dLat)).toFloat()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "outdoorPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    if (isCameraActive && nearbyTarget != null) {
        CameraMugshotCapture(
            runnerName = nearbyTarget!!.nickname,
            targetDistanceMeters = nearbyDistance,
            onPhotoCaptured = { photoFile ->
                isCameraActive = false
                engine.onMugshotPhotoTaken(photoFile)
            },
            onCancel = { isCameraActive = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14))
    ) {
        // 1. TACTICAL OUTDOOR MAP WITH EXACT SAFE ZONE CIRCLE & RADAR
        DarkTacticalMap(
            centerLatitude = safeZone.centerLatitude,
            centerLongitude = safeZone.centerLongitude,
            userLatitude = userLat,
            userLongitude = userLon,
            userBearing = userBearing,
            safeZone = safeZone,
            players = players,
            isSeeker = isSeeker,
            sonarActive = sonarActive,
            urgencyLevel = if (isOutsideSafeZone) 1.0f else 0.2f,
            breathingOffsetPx = 0f,
            pulseProgress = pulseProgress,
            shockwaveAlpha = 0.5f,
            shockwaveExpansionDp = 16.dp,
            recenterTrigger = recenterTrigger,
            userAccuracyMeters = currentLocation?.accuracy ?: 5f,
            userSpeedMps = currentLocation?.speed ?: 0f,
            modifier = Modifier.fillMaxSize()
        )

        // Red danger vignette tint when outside safe zone
        if (isOutsideSafeZone && !isSeeker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x555A1017))
            )
        }

        // 2. TOP HUD BAR (Matching Screenshots)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Role Pill Badge: 🟢 RUNNER or 🟥 SEEKER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE6141A28))
                        .border(1.dp, Color(0xFF28364E), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSeeker) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFFF3355))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEEKER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MugshotNeonLime)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RUNNER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // Center Match Timer Display & Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timerText = "%02d:%02d".format(minutes, seconds)

                    Text(
                        text = if (isHeadStartActive) "0:%02d".format(headStartSecondsLeft) else timerText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )

                    Text(
                        text = if (isHeadStartActive) "HEAD START" else "$activeRunners LEFT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Pill: "Get back in!" or "Hunt them"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xCC1A1F2C))
                            .border(
                                1.5.dp,
                                if (isOutsideSafeZone) Color(0xFFFF416C) else Color(0xFF4A2530),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!isSeeker && isOutsideSafeZone) {
                                "Get back in!"
                            } else if (isSeeker) {
                                "Hunt them"
                            } else {
                                "Stay inside"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isOutsideSafeZone) Color(0xFFFF8FA3) else Color.White
                        )
                    }
                }

                // Right Lightning / Ability Circle Button & Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape, ambientColor = Color.Black)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2333))
                            .border(1.2.dp, Color(0xFF334460), CircleShape)
                            .clickable {
                                engine.triggerSonarPing()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Radar Ping",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B2333))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (sonarActive) "Active" else "Soon",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFFA0ABBE)
                        )
                    }
                }
            }
        }

        // 3. FLOATING "LOSING HEALTH" CARD (When Runner is outside zone - IMG_20261106_135044_137fd0c9.jpg)
        if (isOutsideSafeZone && !isSeeker) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0xAAFF1744),
                        spotColor = Color(0xFFFF1744)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFA750D17))
                    .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Compass Arrow pointing to safe zone center
                    Canvas(
                        modifier = Modifier
                            .size(46.dp)
                            .rotate(angleToCenterDeg - userBearing)
                    ) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.5f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(size.width * 0.5f, size.height * 0.72f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                        drawPath(path, color = Color.Black, style = Stroke(width = 3.dp.toPx()))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "LOSING HEALTH",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Follow the arrow, get back in",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }
        }

        // 4. FLOATING TOAST ALERT (If any engine message is active)
        AnimatedVisibility(
            visible = alertMessage != null && !isOutsideSafeZone,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 118.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE1E050D))
                    .border(1.dp, Color(0xFFFF3355), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = alertMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // 5. RIGHT FLOATING ACTION BUTTONS (Recenter, Chat/Feed with badge, Squad)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recenter Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(Color(0xE61B2333))
                    .border(1.dp, Color(0xFF334460), CircleShape)
                    .clickable { recenterTrigger++ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Chat / Game Feed Button with unread notification badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(Color(0xE61B2333))
                    .border(1.dp, Color(0xFF334460), CircleShape)
                    .clickable { showGameFeedSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Game Feed",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                // Red unread badge "1"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3355)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // Squad Roster Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color.Black)
                    .clip(CircleShape)
                    .background(Color(0xE61B2333))
                    .border(1.dp, Color(0xFF334460), CircleShape)
                    .clickable { showSquadDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Squad",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 6. BOTTOM HUD: CIRCULAR HEALTH/LEFT DIAL + CATCH BUTTON / WARNING BAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Popup Warning for Runner: "Outside = losing health. At 0 you become a seeker." with "Got it" button
            if (isOutsideSafeZone && !isSeeker && !hasDismissedHealthWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0D121C))
                        .border(1.5.dp, MugshotNeonLime, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Outside = losing health. At 0\nyou become a seeker.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 18.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MugshotNeonLime)
                                .clickable { hasDismissedHealthWarning = true }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Got it",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Bottom Action Row: Circular Dial (HP / Left) + Large CATCH Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Dial:
                // For RUNNER: "92 HP" / "31 HP"
                // For SEEKER: "1 left"
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131A26))
                        .border(1.dp, Color(0xFF28364E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Canvas Ring Progress Arc
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        val strokeW = 4.5.dp.toPx()
                        val hpPercent = if (isSeeker) 1f else (localHealthHp / 100f).coerceIn(0f, 1f)
                        val arcColor = if (isSeeker) {
                            Color(0xFFFF5252)
                        } else if (localHealthHp > 50) {
                            MugshotNeonLime
                        } else {
                            Color(0xFFFF3355)
                        }

                        // Background ring
                        drawCircle(
                            color = Color(0xFF202A3C),
                            style = Stroke(width = strokeW)
                        )

                        // Colored progress arc
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = 360f * hpPercent,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isSeeker) "$activeRunners" else "$localHealthHp",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isSeeker) "left" else "HP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFFA0ABBE)
                        )
                    }
                }

                // If Seeker: Huge Coral-Red "🎯 CATCH" Button (IMG_20261106_135240_9e1cc2b5.jpg)
                if (isSeeker) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = Color(0x77FF4A50),
                                spotColor = Color(0xFFFF5252)
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF6B6B), Color(0xFFFF4848))
                                )
                            )
                            .clickable {
                                if (nearbyTarget != null) {
                                    isCameraActive = true
                                } else {
                                    engine.triggerCatch()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CATCH",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    // Runner Safe Status Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF121826))
                            .border(1.dp, Color(0xFF26344E), RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOutsideSafeZone) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isOutsideSafeZone) Color(0xFFFF3355) else MugshotNeonLime,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isOutsideSafeZone) "OUT OF ZONE" else "RUN & HIDE!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (isOutsideSafeZone) Color(0xFFFF3355) else Color.White
                                )
                                Text(
                                    text = if (isOutsideSafeZone) "Losing health every second" else "Seeker is on the hunt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA0ABBE)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. GAME FEED MODAL SHEET (IMG_20261106_135224_3d72aa2e.jpg)
        if (showGameFeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGameFeedSheet = false },
                containerColor = Color(0xFF0F1522),
                scrimColor = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    // Header Row with Title and Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GAME FEED",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E283C))
                                .clickable { showGameFeedSheet = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Feed Message List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        gameFeedMessages.forEach { msg ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "09:37",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF6E7E95)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MugshotNeonLime)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 20.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Bottom Chat Input Bar: Camera + TextField + Mic + SEND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Camera Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A2436))
                                .border(1.dp, Color(0xFF2C3C56), CircleShape)
                                .clickable { isCameraActive = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = MugshotNeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Message Text Field
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Message...", color = Color(0xFF7A8A9E)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141D2C),
                                unfocusedContainerColor = Color(0xFF141D2C),
                                focusedBorderColor = Color(0xFF334662),
                                unfocusedBorderColor = Color(0xFF243248),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        // Microphone Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A2436))
                                .border(1.dp, Color(0xFF2C3C56), CircleShape)
                                .clickable {
                                    chatInputText = "Enemy spotted near north perimeter!"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // SEND Button (Mugshot Olive/Green pill)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF3D5230))
                                .clickable {
                                    if (chatInputText.isNotBlank()) {
                                        engine.addGameFeedMessage("${localPlayer?.nickname ?: "You"}: $chatInputText")
                                        chatInputText = ""
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SEND",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = MugshotNeonLime
                            )
                        }
                    }
                }
            }
        }

        // 8. SQUAD ROSTER DIALOG
        if (showSquadDialog) {
            AlertDialog(
                onDismissRequest = { showSquadDialog = false },
                containerColor = Color(0xFF0F1524),
                title = {
                    Text(
                        text = "Match Squad (${players.size} Players)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        players.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161E30))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OddballAvatar(avatarId = p.avatarId, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.nickname,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (p.role == PlayerRole.SEEKER) "Seeker (Hunter)" else "Runner (Hider)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (p.role == PlayerRole.SEEKER) Color(0xFFFF5252) else MugshotNeonLime
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSquadDialog = false }) {
                        Text("CLOSE", color = MugshotNeonLime, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 9. EXIT LOCKDOWN DIALOG
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
