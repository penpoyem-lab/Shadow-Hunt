package com.example.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CreateRoomScreen(
    initialNickname: String,
    initialAvatarId: String,
    onBack: () -> Unit,
    onCreateGame: (RoomConfig, String, String) -> Unit
) {
    var nickname by remember { mutableStateOf(initialNickname) }
    var selectedAvatarId by remember { mutableStateOf(initialAvatarId) }
    var isPrivate by remember { mutableStateOf(true) }
    var maxPlayers by remember { mutableIntStateOf(6) }
    var seekersCount by remember { mutableIntStateOf(1) }
    var durationMinutes by remember { mutableIntStateOf(10) }
    var selectedArenaId by remember { mutableStateOf("neon_city") }
    var shrinkSpeed by remember { mutableStateOf("Normal") }
    var hintFrequency by remember { mutableIntStateOf(30) }
    var safeZoneRadius by remember { mutableFloatStateOf(350f) }
    var safeZoneStages by remember { mutableIntStateOf(5) }
    var showAdjustSafeZoneDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CREATE A GAME",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "ROOM SETTINGS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // NICKNAME SECTION
            Text(
                text = "Your Nickname",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 16) nickname = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ElectricBlue
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurfaceGlass,
                    unfocusedContainerColor = DarkSurfaceGlass,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // CHOOSE AVATAR SECTION
            Text(
                text = "Choose Your Avatar",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarRegistry.options.forEach { avatar ->
                    val isSelected = selectedAvatarId == avatar.id
                    val glowColor = Color(avatar.glowColorHex)

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) glowColor else DarkCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAvatarId = avatar.id }
                    ) {
                        Image(
                            painter = painterResource(id = avatar.drawableRes),
                            contentDescription = avatar.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(glowColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ROOM SETTINGS PANEL
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF282B48),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Room Visibility Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Room Visibility",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isPrivate) "Private Room (Code required)" else "Public Room (Open matchmaking)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardBorder
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Max Players Stepper
                    SettingStepperRow(
                        label = "MAX PLAYERS",
                        description = "2 - 10 players in arena",
                        valueText = "$maxPlayers",
                        icon = Icons.Default.Groups,
                        iconTint = ElectricBlue,
                        onDecrement = { if (maxPlayers > 2) maxPlayers-- },
                        onIncrement = { if (maxPlayers < 10) maxPlayers++ }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Seekers Stepper
                    SettingStepperRow(
                        label = "SEEKERS",
                        description = "1 - 3 hunters",
                        valueText = "$seekersCount",
                        icon = Icons.Default.GpsFixed,
                        iconTint = NeonRed,
                        onDecrement = { if (seekersCount > 1) seekersCount-- },
                        onIncrement = { if (seekersCount < 3 && seekersCount < maxPlayers) seekersCount++ }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Duration Stepper
                    SettingStepperRow(
                        label = "GAME DURATION",
                        description = "Match round length",
                        valueText = "$durationMinutes min",
                        icon = Icons.Default.Schedule,
                        iconTint = ElectricBlue,
                        onDecrement = { if (durationMinutes > 5) durationMinutes -= 5 },
                        onIncrement = { if (durationMinutes < 15) durationMinutes += 5 }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Virtual Arena
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "VIRTUAL ARENA",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Choose your arena",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }

                        // Arena Thumbnail badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, NeonPurple, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.thumb_neon_city),
                                contentDescription = "Neon City",
                                modifier = Modifier
                                    .size(width = 38.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Neon City",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Safe Zone Speed
                    SettingStepperRow(
                        label = "SAFE ZONE SPEED",
                        description = "How fast the zone shrinks",
                        valueText = shrinkSpeed,
                        icon = Icons.Default.Shield,
                        iconTint = GoldAccent,
                        onDecrement = {
                            shrinkSpeed = when (shrinkSpeed) {
                                "Blitz" -> "Fast"
                                "Fast" -> "Normal"
                                else -> "Normal"
                            }
                        },
                        onIncrement = {
                            shrinkSpeed = when (shrinkSpeed) {
                                "Normal" -> "Fast"
                                "Fast" -> "Blitz"
                                else -> "Blitz"
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ADJUST SAFE ZONE BUTTON
                    Surface(
                        onClick = { showAdjustSafeZoneDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1E00E5FF),
                        border = BorderStroke(1.2.dp, ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ADJUST SAFE ZONE",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Radius: ${safeZoneRadius.toInt()}m • $safeZoneStages Phases",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = ElectricBlue
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder
                    )

                    // Hint Frequency
                    SettingStepperRow(
                        label = "HINT FREQUENCY",
                        description = "Sonar position reveal interval",
                        valueText = "$hintFrequency sec",
                        icon = Icons.Default.Radar,
                        iconTint = ElectricBlue,
                        onDecrement = { if (hintFrequency > 15) hintFrequency -= 15 },
                        onIncrement = { if (hintFrequency < 60) hintFrequency += 15 }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Virtual Arena Disclaimer Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFB300))
                    .border(1.dp, Color(0x66FFB300), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Virtual arena only — no GPS or real-world location sharing.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = Color(0xFFFFE082)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            NeonGamingButton(
                text = "CREATE GAME",
                onClick = {
                    val code = "SH" + (1000..9999).random().toString()
                    val config = RoomConfig(
                        code = code,
                        name = "${nickname}'s Room",
                        maxPlayers = maxPlayers,
                        seekersCount = seekersCount,
                        durationMinutes = durationMinutes,
                        arenaId = selectedArenaId,
                        shrinkSpeed = shrinkSpeed,
                        hintFrequencySeconds = hintFrequency,
                        isPrivate = isPrivate,
                        safeZoneRadiusMeters = safeZoneRadius,
                        safeZoneStages = safeZoneStages
                    )
                    onCreateGame(config, nickname, selectedAvatarId)
                },
                icon = Icons.Default.SportsEsports,
                gradientColors = listOf(Color(0xFFFF1E43), Color(0xFFD50000)),
                borderColor = Color(0xFFFF5252),
                height = 56.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
        }

        // Interactive Adjust Safe Zone Dialog
        if (showAdjustSafeZoneDialog) {
            AdjustSafeZoneDialog(
                initialRadiusMeters = safeZoneRadius,
                initialStages = safeZoneStages,
                initialSpeed = shrinkSpeed,
                onDismiss = { showAdjustSafeZoneDialog = false },
                onApply = { newRadius, newStages, newSpeed ->
                    safeZoneRadius = newRadius
                    safeZoneStages = newStages
                    shrinkSpeed = newSpeed
                    showAdjustSafeZoneDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingStepperRow(
    label: String,
    description: String,
    valueText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Decrease",
                    tint = Color.White
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Increase",
                    tint = Color.White
                )
            }
        }
    }
}
