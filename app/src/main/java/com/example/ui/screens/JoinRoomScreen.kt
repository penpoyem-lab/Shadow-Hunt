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
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun JoinRoomScreen(
    initialNickname: String,
    initialAvatarId: String,
    onBack: () -> Unit,
    onJoinGame: (roomCode: String, nickname: String, avatarId: String) -> Unit
) {
    var nickname by remember { mutableStateOf(initialNickname) }
    var selectedAvatarId by remember { mutableStateOf(initialAvatarId) }
    var roomCode by remember { mutableStateOf("SH7KQ2") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top Header
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
                    text = "JOIN A GAME",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "ENTER PRIVATE ROOM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonPurple
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

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // NICKNAME
            Text(
                text = "YOUR NICKNAME",
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
                        tint = NeonPurple
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurfaceGlass,
                    unfocusedContainerColor = DarkSurfaceGlass,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CHOOSE AVATAR
            Text(
                text = "CHOOSE YOUR AVATAR",
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

            Spacer(modifier = Modifier.height(24.dp))

            // ROOM CODE INPUT
            Text(
                text = "ROOM CODE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = {
                        if (it.length <= 6) roomCode = it.uppercase()
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("ENTER CODE", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ElectricBlue
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
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = { roomCode = "SH7KQ2" }, // Quick demo fill
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = ElectricBlue
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = NeonRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // JOIN GAME BUTTON
            NeonGamingButton(
                text = "JOIN GAME",
                onClick = {
                    if (roomCode.length < 4) {
                        errorMessage = "Please enter a valid 6-character room code"
                    } else {
                        onJoinGame(roomCode, nickname, selectedAvatarId)
                    }
                },
                icon = Icons.Default.DirectionsRun,
                gradientColors = listOf(Color(0xFFFF1E43), Color(0xFF9D00FF)),
                borderColor = Color(0xFFFF5252),
                height = 56.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SCAN INVITE QR BUTTON
            OutlinedButton(
                onClick = {
                    roomCode = "SH" + (1000..9999).random().toString()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCAN INVITE QR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Helper Information
            Text(
                text = "ⓘ Ask your host for the 6-character room code.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Virtual game only — no GPS or real-world location needed.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextMuted
                )
            }
        }
    }
}
