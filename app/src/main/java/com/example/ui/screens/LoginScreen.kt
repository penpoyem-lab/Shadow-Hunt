package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.OddballAvatar
import com.example.ui.theme.*

/**
 * Clean Hunter Entry screen without password obstacles or demo accounts.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (username: String) -> Unit,
    onGuestLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("krish") }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    fun attemptLogin() {
        val chosenName = username.trim().ifEmpty { "Hunter" }
        onLoginSuccess(chosenName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MugshotDarkBg)
    ) {
        // Ambient Liquid Background Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MugshotNeonLime.copy(alpha = 0.08f),
                            WaterShadowCyan.copy(alpha = 0.05f),
                            MugshotDarkBg
                        ),
                        radius = 1100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .padding(top = 28.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Oddball Hero Avatar with Water Shadow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = WaterShadowLime,
                        spotColor = MugshotNeonLime
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, MugshotNeonLime, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                OddballAvatar(avatarId = selectedAvatarId, size = 100.dp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Game Brand Title
            Text(
                text = "LAST SEEN MUGSHOT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White
            )

            Text(
                text = "REAL OUTDOOR GPS SQUAD HUNT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MugshotNeonLime
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Choose Your Oddball Creature Row
            Text(
                text = "CHOOSE YOUR CREATURE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFFA0ABBE)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "krish" to "Cyclops",
                    "john" to "Turnip",
                    "star" to "Spiky"
                ).forEach { (id, label) ->
                    val isSelected = selectedAvatarId == id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF1E2838) else Color(0xFF131826))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MugshotNeonLime else Color(0xFF263248),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAvatarId = id }
                            .padding(10.dp)
                    ) {
                        OddballAvatar(avatarId = id, size = 52.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) MugshotNeonLime else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Liquid Glass Capsule Card for Player Name Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(MugshotCardBg)
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x88FFFFFF), MugshotCardBorder, Color(0x10000000))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "HUNTER CODENAME",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFA0ABBE)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MugshotNeonLime
                            )
                        },
                        placeholder = {
                            Text("Enter your codename (e.g. Hunter)", color = Color(0xFF6E7E95))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            attemptLogin()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F1522),
                            unfocusedContainerColor = Color(0xFF0F1522),
                            focusedBorderColor = MugshotNeonLime,
                            unfocusedBorderColor = Color(0xFF2B374E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MugshotNeonLime
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "🔒 No passwords required. Your codename and real GPS coordinates will sync with your friends in the live match.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color(0xFF7A8A9E)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Button: START PLAYING
                    LiquidGlassButton(
                        text = "START PLAYING",
                        onClick = { attemptLogin() },
                        icon = Icons.Default.ArrowForward,
                        trailingText = ">>",
                        gradientColors = listOf(MugshotNeonLime, MugshotNeonLimeDark),
                        textColor = Color.Black,
                        waterShadowColor = WaterShadowLime,
                        height = 58.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Button: QUICK GUEST ACCESS
                    LiquidGlassButton(
                        text = "QUICK GUEST ACCESS",
                        onClick = onGuestLogin,
                        icon = Icons.Default.FlashOn,
                        trailingText = null,
                        gradientColors = listOf(Color(0xFF1E2838), Color(0xFF131A26)),
                        textColor = Color.White,
                        waterShadowColor = Color.Transparent,
                        height = 52.dp
                    )
                }
            }
        }
    }
}
