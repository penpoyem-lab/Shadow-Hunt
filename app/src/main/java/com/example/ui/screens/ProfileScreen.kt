package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import com.example.model.UserProfileData
import com.example.ui.components.GlassPanel
import com.example.ui.components.NeonGamingButton
import com.example.ui.components.StatCounterBox
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    profile: UserProfileData,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onCustomizeAvatar: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        // TOP APP BAR
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

            Text(
                text = "MY PROFILE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White
            )

            IconButton(
                onClick = onOpenSettings,
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

        // PROFILE HERO CARD
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF282B48),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with Edit badge
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, NeonPurple, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_anime_kishalay),
                                contentDescription = profile.nickname,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(NeonPurple)
                                    .clickable { onCustomizeAvatar() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.nickname,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lv.${profile.level}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // XP Progress Bar
                            LinearProgressIndicator(
                                progress = { profile.currentXp.toFloat() / profile.maxXp.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonPurple,
                                trackColor = DarkCardBorder
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${profile.currentXp} / ${profile.maxXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CUSTOMISE AVATAR BUTTON
                    NeonGamingButton(
                        text = "CUSTOMISE AVATAR",
                        onClick = onCustomizeAvatar,
                        icon = Icons.Default.Palette,
                        gradientColors = listOf(Color(0xFF9D00FF), Color(0xFF00B0FF)),
                        borderColor = Color(0xFFB388FF),
                        height = 46.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PLAYER ID CARD
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ElectricBlue,
                borderWidth = 1.5.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YOUR PLAYER ID",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Text(
                                text = profile.playerId,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Shadow Hunt Player ID", profile.playerId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Player ID Copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COPY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SAVE PROGRESS / GOOGLE SIGN IN CARD
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF2E3150),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SAVE YOUR PROGRESS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Sign in to keep your stats across devices",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Cloud sync active as Guest (${profile.nickname})", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ⓘ Guest progress stays on this device",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YOUR STATS CARD
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF282B48),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "YOUR STATS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Keep Playing >>",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounterBox(
                            value = "${profile.gamesPlayed}",
                            label = "GAMES PLAYED",
                            icon = Icons.Default.SportsEsports,
                            iconTint = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCounterBox(
                            value = "${profile.wins}",
                            label = "WINS",
                            icon = Icons.Default.EmojiEvents,
                            iconTint = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCounterBox(
                            value = "${profile.tags}",
                            label = "TAGS",
                            icon = Icons.Default.GpsFixed,
                            iconTint = NeonRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatCounterBox(
                            value = profile.survivalFormatted,
                            label = "SURVIVAL",
                            icon = Icons.Default.Schedule,
                            iconTint = NeonGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ACHIEVEMENTS
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF282B48),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACHIEVEMENTS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View All >>",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileAchievementBadge("FIRST HUNT", Icons.Default.MilitaryTech, GoldAccent)
                        ProfileAchievementBadge("MASTER SEEKER", Icons.Default.Security, NeonPurple)
                        ProfileAchievementBadge("SURVIVOR", Icons.Default.DirectionsRun, ElectricBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAchievementBadge(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(2.dp, accentColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1
        )
    }
}
