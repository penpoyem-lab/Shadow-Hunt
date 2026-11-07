package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

/**
 * Last Seen Mugshot Lobby Screen matching IMG_20261106_143302_2db62686.jpg
 * and IMG_20261106_143604_39b6ab50.jpg.
 *
 * Enforces:
 *  - Strict 3-human player minimum to unlock the "START GAME ->" button
 *  - Signature Neon Lime + dark slate aesthetic with liquid glass water shadows
 *  - "Scan to Join" QR modal with lime corner brackets
 *  - "Game Feel" settings bottom sheet
 *  - Oddball avatars with host star badge
 */
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
    var liveConfig by remember { mutableStateOf(roomConfig) }

    var showScanToJoinSheet by remember { mutableStateOf(false) }
    var showGameFeelSheet by remember { mutableStateOf(false) }
    var showOddballDialog by remember { mutableStateOf(false) }
    var showAllPlayersDialog by remember { mutableStateOf(false) }
    var showLastWallDialog by remember { mutableStateOf(false) }

    val localPlayer = players.find { it.isLocalPlayer } ?: players.firstOrNull()
    val isHost = localPlayer?.isHost ?: true
    var localChosenAvatarId by remember { mutableStateOf(localPlayer?.avatarId ?: "krish") }

    // Strict 3-real-human requirement as confirmed by the user
    val requiredPlayers = 3
    val playersCount = players.size
    val neededPlayers = (requiredPlayers - playersCount).coerceAtLeast(0)
    val isReadyToStart = playersCount >= requiredPlayers

    // 1. SCAN TO JOIN BOTTOM SHEET
    if (showScanToJoinSheet) {
        ScanToJoinBottomSheet(
            roomCode = liveConfig.code,
            onDismiss = { showScanToJoinSheet = false }
        )
    }

    // 2. GAME FEEL BOTTOM SHEET
    if (showGameFeelSheet) {
        GameFeelBottomSheet(
            currentConfig = liveConfig,
            onSaveConfig = { updated ->
                liveConfig = updated
            },
            onDismiss = { showGameFeelSheet = false }
        )
    }

    // 3. ODDBALL PICKER DIALOG
    if (showOddballDialog) {
        AlertDialog(
            onDismissRequest = { showOddballDialog = false },
            containerColor = Color(0xFF0F1524),
            title = {
                Text(
                    text = "Pick Your Oddball",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Choose your custom creature avatar for this match:",
                        color = Color(0xFFA0ABBE)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("krish" to "Cyclops Pin", "john" to "Turnip Drop", "star" to "Spiky Star").forEach { (id, name) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        localChosenAvatarId = id
                                        showOddballDialog = false
                                        Toast.makeText(context, "Oddball changed to $name", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            ) {
                                OddballAvatar(avatarId = id, size = 60.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOddballDialog = false }) {
                    Text("DONE", color = MugshotNeonLime, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. SEE ALL PLAYERS DIALOG
    if (showAllPlayersDialog) {
        AlertDialog(
            onDismissRequest = { showAllPlayersDialog = false },
            containerColor = Color(0xFF0F1524),
            title = {
                Text(
                    text = "Squad Roster (${players.size} Players)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    players.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161E30))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OddballAvatar(avatarId = p.avatarId, size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.nickname,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (p.isHost) "Room Host · Real Player" else "Connected · Real Player",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (p.isHost) MugshotNeonLime else Color(0xFF7A8A9E)
                                )
                            }
                            if (p.isHost) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MugshotNeonLime),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Host",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllPlayersDialog = false }) {
                    Text("CLOSE", color = MugshotNeonLime, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 5. LAST WALL DIALOG
    if (showLastWallDialog) {
        AlertDialog(
            onDismissRequest = { showLastWallDialog = false },
            containerColor = Color(0xFF0F1524),
            title = {
                Text(
                    text = "The Last Wall",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "When only the final circle remains (50m across), the safe boundary becomes stationary. Runners must dodge the seeker in tight quarters until time expires!\n\nLeaving the Last Wall boundary triggers an immediate instant-elimination warning.",
                    color = Color(0xFFA0ABBE),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showLastWallDialog = false }) {
                    Text("UNDERSTOOD", color = MugshotNeonLime, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MugshotDarkBg)
    ) {
        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 110.dp) // Leave space for sticky bottom bar
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // TOP BAR: LAST SEEN MUGSHOT + LOBBY & Leave Lobby Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAST SEEN MUGSHOT",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "LOBBY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF8A9AAF)
                    )
                }

                // Leave Lobby Button (Wine / Burgundy rounded pill)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MugshotWineButton)
                        .border(1.dp, MugshotWineBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onLeaveLobby)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Leave Lobby",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFF8FA3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. GAME CODE CARD (Rounded charcoal card with copy, share & show QR)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(MugshotCardBg)
                    .border(1.dp, MugshotCardBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    // Header row: GAME CODE label & Copy button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GAME CODE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF7A8A9E)
                        )

                        // Copy Icon button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A2132))
                                .border(1.dp, Color(0xFF2B374E), RoundedCornerShape(12.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Game Code", liveConfig.code))
                                    Toast.makeText(context, "Code copied: ${liveConfig.code}", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = MugshotNeonLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Huge Room Code Display
                    Text(
                        text = liveConfig.code,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row: SHARE INVITE & SHOW QR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SHARE INVITE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF172030))
                                .border(1.dp, Color(0xFF28364E), RoundedCornerShape(14.dp))
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join my Last Seen Mugshot game! Room Code: ${liveConfig.code}\nLaunch the app and enter the code to join!"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Game Code"))
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MugshotNeonLime,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHARE INVITE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // SHOW QR
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF172030))
                                .border(1.dp, Color(0xFF28364E), RoundedCornerShape(14.dp))
                                .clickable { showScanToJoinSheet = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MugshotNeonLime,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHOW QR",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PLAYER REQUIREMENT BANNER (Red pill when < 3, Green pill when >= 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isReadyToStart) MugshotOliveBg else MugshotRedBg)
                    .border(
                        1.dp,
                        if (isReadyToStart) MugshotOliveBorder else MugshotRedBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isReadyToStart) MugshotNeonLime else Color(0xFFFF3355))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isReadyToStart) {
                                "Squad Ready to Start"
                            } else {
                                "Need $neededPlayers more to start"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isReadyToStart) {
                                "$playersCount players in lobby · Minimum $requiredPlayers reached"
                            } else {
                                "Waiting for players · minimum $requiredPlayers."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isReadyToStart) Color(0xFFA6E07A) else Color(0xFFC0707C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. YOUR CREW SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your crew",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✳",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MugshotNeonLime
                    )
                }

                Text(
                    text = "${playersCount.toString().padStart(2, '0')} / ${roomConfig.maxPlayers.coerceAtLeast(30)} players",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFA0ABBE)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Crew Members Horizontal Scroll List
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(players) { player ->
                    val isThisHost = player.isHost

                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .shadow(
                                elevation = if (isThisHost) 8.dp else 2.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = if (isThisHost) WaterShadowLime else Color.Transparent,
                                spotColor = if (isThisHost) MugshotNeonLime else Color.Transparent
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .background(MugshotCardBg)
                            .border(
                                width = if (isThisHost) 2.dp else 1.dp,
                                color = if (isThisHost) MugshotNeonLime else MugshotCardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar Box with optional Host Star Badge
                            Box(contentAlignment = Alignment.TopEnd) {
                                val displayAvatar = if (player.isLocalPlayer) localChosenAvatarId else player.avatarId
                                OddballAvatar(avatarId = displayAvatar, size = 66.dp)

                                if (isThisHost) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(MugshotNeonLime),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Host",
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nickname
                            Text(
                                text = player.nickname,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.White,
                                maxLines = 1
                            )

                            // Status subtitle
                            Text(
                                text = if (player.isLocalPlayer) "You · Host" else "In lobby",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (player.isLocalPlayer) MugshotNeonLime else Color(0xFF6E7E95)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: "Change Oddball" & "SEE ALL PLAYERS"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Change Oddball Link
                Text(
                    text = "Change Oddball",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MugshotNeonLime,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showOddballDialog = true }
                        .padding(vertical = 4.dp)
                )

                // SEE ALL PLAYERS Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161E30))
                        .border(1.dp, Color(0xFF28364E), RoundedCornerShape(14.dp))
                        .clickable { showAllPlayersDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SEE ALL PLAYERS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. GAME MODE SELECTOR CARD ("STANDARD" / "CUSTOM" with "EDIT ->" button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MugshotCardBg)
                    .border(1.dp, MugshotCardBorder, RoundedCornerShape(18.dp))
                    .clickable { showGameFeelSheet = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Target or Tune Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2616)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            tint = MugshotNeonLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val isStandard = liveConfig.durationMinutes == 20 && liveConfig.safeZoneRadiusMeters.toInt() == 160
                        Text(
                            text = if (isStandard) "STANDARD" else "CUSTOM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isStandard) {
                                "20 min · The full game"
                            } else {
                                "${liveConfig.durationMinutes} min · ${liveConfig.safeZoneRadiusMeters.toInt()} m circle"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0ABBE)
                        )
                    }
                }

                // EDIT -> Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MugshotOliveButton)
                        .border(1.dp, MugshotOliveBorder, RoundedCornerShape(14.dp))
                        .clickable { showGameFeelSheet = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EDIT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = MugshotNeonLime
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Edit",
                            tint = MugshotNeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. "Last Wall" link
            Text(
                text = "Last Wall",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                color = Color(0xFFA0ABBE),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showLastWallDialog = true }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        // STICKY BOTTOM ACTION BAR (Matching Screenshot 1 & 2)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (isReadyToStart) {
                // ACTIVE: Vibrant Neon Lime pill button with black text and right arrow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(30.dp),
                            ambientColor = WaterShadowLime,
                            spotColor = MugshotNeonLime
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(MugshotNeonLime, Color(0xFFD4FF33))
                            )
                        )
                        .clickable(onClick = onStartGame),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "START GAME",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Start",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // INACTIVE / DISABLED: Dark muted olive pill "NEED X MORE"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFF1E2818))
                        .border(1.dp, Color(0xFF2C3B24), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEED $neededPlayers MORE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF5B6E52)
                    )
                }
            }
        }
    }
}
