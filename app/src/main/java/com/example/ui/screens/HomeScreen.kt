package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.example.R
import com.example.model.UserProfileData
import com.example.ui.components.BannerSlide
import com.example.ui.components.GlassPanel
import com.example.ui.components.GooglePlayPullToRefresh
import com.example.ui.components.HeroBannerCarousel
import com.example.ui.components.LiquidGlassButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shadow Hunt Home Screen matching IMG_20261104_102912.png
 * Features:
 *  - Top status bar with sound & settings controls
 *  - High-intensity Hero Radar banner with hunter art and circular scanner
 *  - "REJOIN YOUR GAME" room card (SH7KQ2)
 *  - Glowing neon "CREATE A GAME >>" (red) and "JOIN WITH CODE >>" (purple) pill buttons
 *  - "GLOBAL CHAT" "● LIVE" feed card with RexHunter and ShadowKing
 *  - "Already have an account? Restore it"
 *  - Bottom stats: 2.5M+ Total Players | 1.8M+ Players Caught | 742K+ Victories
 */
@Composable
fun HomeScreen(
    profile: UserProfileData,
    onPlayNow: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onRejoinRoom: () -> Unit,
    hasActiveRoom: Boolean = true,
    activeRoomCode: String = "SH7KQ2",
    onRefreshRoomCode: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    // Pulse animation for LIVE indicator & border glows
    val infiniteTransition = rememberInfiniteTransition(label = "homePulse")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    GooglePlayPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefreshRoomCode()
            coroutineScope.launch {
                delay(1200)
                isRefreshing = false
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp) // Leave room for bottom navigation
        ) {
        // 1. TOP HEADER BAR: Sound Toggle & Settings Gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sound Mute Toggle Button with Water Shadow & Liquid Glass Capsule
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = WaterShadowAqua.copy(alpha = 0.4f),
                        spotColor = WaterShadowCyan.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(Color(0xCC0D1222))
                    .border(
                        1.4.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x88FFFFFF), Color(0x3300F2FE), Color(0x10000000))
                        ),
                        CircleShape
                    )
                    .clickable { isMuted = !isMuted },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Sound Toggle",
                    tint = if (isMuted) TextMuted else LiquidAuroraCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Settings Gear Button with Water Shadow & Liquid Glass Capsule
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = WaterShadowAqua.copy(alpha = 0.4f),
                        spotColor = WaterShadowCyan.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(Color(0xCC0D1222))
                    .border(
                        1.4.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x88FFFFFF), Color(0x3300F2FE), Color(0x10000000))
                        ),
                        CircleShape
                    )
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. HERO PROMOTIONAL CAROUSEL: 5 Interactive Swipeable Banners with 3D Depth & Glowing Dots
        HeroBannerCarousel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            onBannerClick = { slide ->
                onPlayNow()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. ACTION CONTROLS SECTION (Matching IMG_20261104_102912.png)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD 1: REJOIN YOUR GAME (Room Code: SH7KQ2) - Liquid Glass Capsule Card with Water Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = WaterShadowCrimson.copy(alpha = 0.4f),
                        spotColor = WaterShadowAqua.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xE60F1426),
                                Color(0xF2090C16)
                            )
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAAFFFFFF),
                                Color(0x66FF1744),
                                Color(0x4400E5FF),
                                Color(0x10000000)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .clickable(onClick = onRejoinRoom)
            ) {
                // Top Specular Glass Sheen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x88FFFFFF),
                                    Color(0xCC00F2FE),
                                    Color(0x88FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular Red Replay Icon with Liquid Glow
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = WaterShadowCrimson.copy(alpha = 0.5f),
                                spotColor = WaterShadowCrimson
                            )
                            .clip(CircleShape)
                            .background(Color(0xFF2A101C))
                            .border(1.6.dp, LiquidCrimsonCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Rejoin",
                            tint = LiquidCrimsonCoral,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Text Info: "REJOIN YOUR GAME" + "Room Code: SH7KQ2"
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REJOIN YOUR GAME",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Room Code: ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextSecondary
                            )
                            Text(
                                text = activeRoomCode,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                color = LiquidCrimsonCoral
                            )
                        }
                    }

                    // Forward Chevron
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Enter",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(Color(0xFF2E3552))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // "2-10 Players" badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = LiquidAuroraCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "2-10",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Text(
                            text = "Players",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            // BUTTON 2: CREATE A GAME >> (Liquid Glass Capsule Button with Water Shadow)
            LiquidGlassButton(
                text = "CREATE A GAME",
                onClick = onCreateRoom,
                icon = Icons.Default.SportsEsports,
                trailingText = ">>",
                gradientColors = listOf(LiquidCrimsonCoral, LiquidCrimsonOrange),
                waterShadowColor = WaterShadowCrimson,
                height = 64.dp
            )

            // BUTTON 3: JOIN WITH CODE >> (Liquid Glass Capsule Button with Water Shadow)
            LiquidGlassButton(
                text = "JOIN WITH CODE",
                onClick = onJoinRoom,
                icon = Icons.Default.VpnKey,
                trailingText = ">>",
                gradientColors = listOf(RoyalCyberIris, NeonOrchidPink),
                waterShadowColor = WaterShadowPurple,
                height = 64.dp
            )

            // CARD 4: GLOBAL CHAT LIVE - Liquid Glass Capsule Card with Water Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = WaterShadowAqua.copy(alpha = 0.35f),
                        spotColor = WaterShadowCyan.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xE60D1224), Color(0xF2080B16))
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x88FFFFFF),
                                Color(0x5500F2FE),
                                Color(0x10000000)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { showChatDialog = true }
            ) {
                // Top Specular Sheen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x66FFFFFF),
                                    Color(0x9900F2FE),
                                    Color(0x66FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Chat Header Row: GLOBAL CHAT + LIVE indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GLOBAL CHAT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }

                        // Live green pulsing badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676).copy(alpha = liveAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E676)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat messages preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFF7C4DFF), CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_cyber),
                                contentDescription = "RexHunter Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Message Lines
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RexHunter : ",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "The zone shrinks soon!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB0B7C3)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ShadowKing : ",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Let's win this! 🔥",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timestamp: Last message 2 min ago
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Last message 2 min ago",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // ACTION: Already have an account? Restore it - Liquid Capsule Button with Water Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = WaterShadowAqua.copy(alpha = 0.25f),
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xCC0D1222))
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x66FFFFFF), Color(0x3300F2FE), Color(0x10000000))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { showRestoreDialog = true }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = LiquidAuroraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Already have an account? Restore it",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // LIVE REAL-WORLD TELEMETRY STATUS - Liquid Glass Capsule Card with Water Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = WaterShadowEmerald.copy(alpha = 0.35f),
                        spotColor = WaterShadowAqua.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xE60D1426), Color(0xF2080B16))
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x88FFFFFF),
                                Color(0x5500F5A0),
                                Color(0x10000000)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Stat 1: GPS Satellites Locked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "14 SATS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "GPS Precision",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NeonGreen
                            )
                        }
                    }

                    // Divider 1
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF2E334D))
                    )

                    // Live Stat 2: Full Stealth Hunt Protocol
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "STEALTH",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "Undetected",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF8C98B2)
                            )
                        }
                    }

                    // Divider 2
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF2E334D))
                    )

                    // Live Stat 3: Minimum Safe Circle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MIN 1.0 KM",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "Satellite View",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF8C98B2)
                            )
                        }
                    }
                }
            }
        }
    }

    // Account Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text("Restore Hunter Account", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your Hunter ID (${profile.playerId}) and achievements are currently active on this device. Would you like to re-verify your Google Cloud sync?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("OK", color = ElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Global Chat Interactive Dialog
    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ChatBubble, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Global Hunter Chat", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔴 RexHunter: The zone shrinks soon!", color = Color.White)
                    Text("🟣 ShadowKing: Let's win this! 🔥", color = Color.White)
                    Text("🟢 GhostRunner: Watch the north bridge, seekers incoming!", color = ElectricBlue)
                    Text("🟡 HunterX: Locked target 18m away, deploying camera!", color = GoldAccent)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text("Close", color = ElectricBlue)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}
}
