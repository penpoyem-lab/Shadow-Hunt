package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraMugshotCapture
import com.example.game.RealOutdoorGameEngine
import com.example.location.LiveLocationManager
import com.example.model.GamePhase
import com.example.model.PlayerRole
import com.example.ui.components.GlassPanel
import com.example.ui.components.NeonGamingButton
import com.example.ui.map.DarkTacticalMap
import com.example.ui.map.LeafletMapView
import com.example.ui.map.MapLibreMapView
import com.example.ui.theme.*
import java.io.File
import kotlin.math.sin

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

    val currentLocation by locationManager.currentLocation.collectAsState()
    val userLat = currentLocation?.latitude ?: safeZone.centerLatitude
    val userLon = currentLocation?.longitude ?: safeZone.centerLongitude
    val userBearing = currentLocation?.bearing ?: 0f

    val localPlayer = players.find { it.isLocalPlayer } ?: players.firstOrNull()
    val isSeeker = localPlayer?.role == PlayerRole.SEEKER
    val activeRunners = players.count { it.role == PlayerRole.RUNNER && !it.isTagged }

    var miniMapMode by remember { mutableStateOf(MiniMapDisplayMode.CORNER) }
    var isCameraActive by remember { mutableStateOf(false) }
    var useStreetTilesMap by remember { mutableStateOf(true) }
    var recenterTrigger by remember { mutableStateOf(0) }

    // Pulsing Urgency Animation synchronized with timer
    val countdown = safeZone.countdownSeconds
    val isShrinking = safeZone.isShrinking

    val urgencyLevel = when {
        isShrinking -> 1.0f
        countdown <= 5 -> 0.88f
        countdown <= 10 -> 0.70f
        countdown <= 20 -> 0.40f
        else -> 0.15f
    }

    val pulseDurationMillis = when {
        isShrinking -> 380
        countdown <= 5 -> 480
        countdown <= 10 -> 700
        countdown <= 20 -> 1000
        else -> 1500
    }

    val infiniteTransition = rememberInfiniteTransition(label = "outdoorPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    val breathingPhase = pulseProgress * 2f * Math.PI.toFloat()
    val breathingOffsetPx = sin(breathingPhase) * (2f + urgencyLevel * 6f)
    val shockwaveAlpha = (1f - pulseProgress) * (0.35f + urgencyLevel * 0.55f)
    val shockwaveExpansionDp = (8f + urgencyLevel * 20f).dp

    // Safe zone proximity math
    val distToCenter = LiveLocationManager.distanceMeters(
        userLat, userLon,
        safeZone.centerLatitude, safeZone.centerLongitude
    )
    val distToEdge = distToCenter - safeZone.currentRadiusMeters
    val isInsideSafeZone = distToEdge <= 0f

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
            .background(DarkBackground)
    ) {
        // 1. DUAL MAP ENGINE: OPENSTREETMAP TACTICAL TILES OR NATIVE CANVAS RADAR
        if (useStreetTilesMap) {
            LeafletMapView(
                modifier = Modifier.fillMaxSize(),
                userLatitude = userLat,
                userLongitude = userLon,
                userHeading = userBearing,
                userRole = if (isSeeker) PlayerRole.SEEKER else PlayerRole.RUNNER,
                safeZone = safeZone,
                runners = players.filter { it.role == PlayerRole.RUNNER },
                isRadarPulseActive = sonarActive,
                recenterTrigger = recenterTrigger,
                onRunnerSelected = { /* runner clicked */ }
            )
        } else {
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
                urgencyLevel = urgencyLevel,
                breathingOffsetPx = breathingOffsetPx,
                pulseProgress = pulseProgress,
                shockwaveAlpha = shockwaveAlpha,
                shockwaveExpansionDp = shockwaveExpansionDp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. TOP STATUS BAR HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exit Button
                IconButton(
                    onClick = onPauseOrExit,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(1.dp, DarkCardBorder, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Exit", tint = Color.White)
                }

                // Map Engine Switch (OpenStreetMap Tactical Tiles vs Tactical Radar)
                Surface(
                    onClick = { useStreetTilesMap = !useStreetTilesMap },
                    shape = RoundedCornerShape(14.dp),
                    color = if (useStreetTilesMap) ElectricBlue.copy(alpha = 0.25f) else DarkSurfaceGlass,
                    border = BorderStroke(1.dp, if (useStreetTilesMap) ElectricBlue else DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (useStreetTilesMap) Icons.Default.Map else Icons.Default.Radar,
                            contentDescription = "Map Mode",
                            tint = if (useStreetTilesMap) ElectricBlue else NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (useStreetTilesMap) "STREET MAP" else "RADAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Live Game Timer
                GlassPanel(
                    shape = RoundedCornerShape(16.dp),
                    borderColor = Color(0xFF383B5E),
                    backgroundColor = DarkSurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                            color = Color.White
                        )
                    }
                }

                // Players & Minimap Mode Toggle
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
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Runners: $activeRunners", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }

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
                            contentDescription = "Toggle Minimap",
                            tint = if (miniMapMode != MiniMapDisplayMode.COLLAPSED) Color.Black else ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-status Row: Safe Zone Countdown & Role Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Safe Zone Pill
                GlassPanel(
                    shape = RoundedCornerShape(20.dp),
                    borderColor = if (isShrinking) NeonRed else ElectricBlue,
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
                            text = if (isShrinking) "ZONE SHRINKING NOW!" else "Safe Zone in: %02ds (Stage %d/5)".format(countdown, safeZone.stage),
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

            // Real-Time GPS Precision Telemetry Pill
            GlassPanel(
                shape = RoundedCornerShape(12.dp),
                borderColor = if (currentLocation != null) Color(0xFF1B5E20) else Color(0xFF5D4037),
                backgroundColor = Color(0xCC090C16),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (currentLocation != null) NeonGreen else GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLocation != null) {
                                "GPS: %.5f, %.5f (±%.0fm)".format(
                                    currentLocation!!.latitude,
                                    currentLocation!!.longitude,
                                    currentLocation!!.accuracy
                                )
                            } else {
                                "SEARCHING GPS SATELLITES..."
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (currentLocation != null) NeonGreen else GoldAccent
                        )
                    }

                    Text(
                        text = if (currentLocation != null) "LIVE LOCKED" else "ACQUIRING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (currentLocation != null) NeonGreen else Color(0xFF9E9E9E)
                    )
                }
            }
        }

        // 3. FLOATING ALERT TOAST
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
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = alertMessage ?: "", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }

        // 4. FLOATING TACTICAL ACTION BUTTONS (Right Edge)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recenter Map on Player's Exact GPS Location
            FloatingActionButton(
                onClick = {
                    recenterTrigger++
                    if (currentLocation != null) {
                        engine.anchorGameToLocation(currentLocation!!.latitude, currentLocation!!.longitude)
                    }
                },
                containerColor = Color(0xFF0F1322),
                contentColor = ElectricBlue,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.5.dp, ElectricBlue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter on My Location",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Radar Sonar Ping Scan Button
            FloatingActionButton(
                onClick = { engine.triggerSonarPing() },
                containerColor = Color(0xFF0F1322),
                contentColor = if (isSeeker) NeonRed else ElectricBlue,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.5.dp, if (isSeeker) NeonRed else ElectricBlue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Radar Ping",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. BOTTOM ACTION & CAMERA CAPTURE PANEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TARGET PROXIMITY CARD (If seeker is within 20m of runner!)
            if (isSeeker && nearbyTarget != null) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    borderColor = NeonRed,
                    backgroundColor = Color(0xEE1A050B),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NeonRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "RUNNER IN RANGE!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = NeonRed
                                )
                                Text(
                                    text = "${nearbyTarget!!.nickname} is %.1fm away".format(nearbyDistance),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { isCameraActive = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "SNAP", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Real Geofence Safe Zone Distance Bar
            GlassPanel(
                shape = RoundedCornerShape(20.dp),
                borderColor = if (isInsideSafeZone) NeonGreen else NeonRed,
                backgroundColor = Color(0xDD0D0F1B),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isInsideSafeZone) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isInsideSafeZone) NeonGreen else NeonRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isInsideSafeZone) {
                            "INSIDE SAFE ZONE (%.0fm to edge)".format(-distToEdge)
                        } else {
                            "DANGER: %.0fm OUT OF SAFE ZONE!".format(distToEdge)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isInsideSafeZone) Color.White else NeonRed
                    )
                }
            }
        }
    }
}
