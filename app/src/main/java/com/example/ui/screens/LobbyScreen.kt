package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun LobbyScreen(
    roomConfig: RoomConfig,
    players: List<PlayerState>,
    onLeaveLobby: () -> Unit,
    onStartGame: () -> Unit,
    onToggleReady: () -> Unit,
    onAddBot: () -> Unit
) {
    val context = LocalContext.current
    var isStartingCountdown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableIntStateOf(5) }

    LaunchedEffect(isStartingCountdown) {
        if (isStartingCountdown) {
            while (countdownSec > 0) {
                kotlinx.coroutines.delay(1000)
                countdownSec--
            }
            onStartGame()
        }
    }

    val localPlayer = players.find { it.isLocalPlayer } ?: players.firstOrNull()
    val isHost = localPlayer?.isHost ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onLeaveLobby,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Leave",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LOBBY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = roomConfig.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            Button(
                onClick = onLeaveLobby,
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, NeonRed),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Leave",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonRed
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // GAME CODE BOX
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ElectricBlue,
                borderWidth = 1.8.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "GAME CODE //",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = roomConfig.code,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp
                                ),
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Shadow Hunt Room Code", roomConfig.code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, ElectricBlue, RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = ElectricBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Share and Show QR buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.25f)),
                            border = BorderStroke(1.2.dp, NeonPurple),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share Invite",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Room QR Code: ${roomConfig.code}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.25f)),
                            border = BorderStroke(1.2.dp, ElectricBlue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Show QR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notice Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFB300))
                    .border(1.dp, Color(0x66FFB300), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Virtual arena connection is optional. No GPS or real location is required.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = Color(0xFFFFE082)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATUS BANNER
            val waitingText = if (isStartingCountdown) {
                "HUNT BEGINS IN: 00:0$countdownSec"
            } else if (players.size < 2) {
                "NEED 1 MORE TO START • Waiting for players"
            } else {
                "READY TO HUNT • ${players.size}/${roomConfig.maxPlayers} PLAYERS READY"
            }

            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (players.size >= 2) NeonGreen else NeonRed,
                backgroundColor = if (players.size >= 2) Color(0x3300E676) else Color(0x33FF1E43),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (players.size >= 2) Icons.Default.SportsEsports else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = if (players.size >= 2) NeonGreen else NeonRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = waitingText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // YOUR CREW SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "YOUR CREW //",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
                Text(
                    text = "${players.size} / ${roomConfig.maxPlayers} players",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonPurple
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CREW SLOTS GRID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Actual players
                players.forEach { player ->
                    val avatarOpt = AvatarRegistry.getAvatar(player.avatarId)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(82.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .border(
                                    width = 2.dp,
                                    color = if (player.isHost) GoldAccent else ElectricBlue,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = avatarOpt.drawableRes),
                                contentDescription = player.nickname,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (player.isHost) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Host",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = player.nickname,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (player.isHost) GoldAccent.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (player.isHost) "HOST" else "READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (player.isHost) GoldAccent else NeonGreen
                            )
                        }
                    }
                }

                // Empty Slots with + to invite / add bot
                val emptySlots = (roomConfig.maxPlayers - players.size).coerceAtLeast(0)
                for (i in 0 until emptySlots) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(82.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                .clickable { onAddBot() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Bot Player",
                                tint = TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Empty Slot",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                        Text(
                            text = "+ Tap to Add",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ElectricBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ARENA & DURATION RECAP CARD
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF282B48),
                shape = RoundedCornerShape(16.dp)
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NeonRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "STANDARD ARENA",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${roomConfig.durationMinutes} min • Virtual arena (${roomConfig.seekersCount} Seeker)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    Image(
                        painter = painterResource(id = R.drawable.thumb_neon_city),
                        contentDescription = "Neon City",
                        modifier = Modifier
                            .size(width = 54.dp, height = 34.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // START GAME BUTTON
            if (isHost) {
                NeonGamingButton(
                    text = if (isStartingCountdown) "STARTING IN $countdownSec..." else "START GAME",
                    onClick = {
                        if (players.size < 2) {
                            onAddBot() // ensure at least 2 players
                        }
                        isStartingCountdown = true
                    },
                    icon = Icons.Default.PlayArrow,
                    gradientColors = listOf(Color(0xFFFF1E43), Color(0xFFD50000)),
                    borderColor = Color(0xFFFF5252),
                    height = 58.dp
                )
            } else {
                NeonGamingButton(
                    text = "READY TO PLAY",
                    onClick = onToggleReady,
                    icon = Icons.Default.Check,
                    gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
                    borderColor = Color(0xFF69F0AE),
                    height = 58.dp
                )
            }
        }
    }
}
