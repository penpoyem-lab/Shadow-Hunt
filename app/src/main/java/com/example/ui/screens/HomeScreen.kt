package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserProfileData
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    profile: UserProfileData,
    onPlayNow: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onRejoinRoom: () -> Unit,
    hasActiveRoom: Boolean,
    activeRoomCode: String,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp) // space for bottom nav bar
    ) {
        // TOP STATUS BAR (Coins, Diamonds, Profile level, Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User Avatar & Level
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenProfile() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, NeonPurple, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_anime_kishalay),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Lv.${profile.level}  •  ${profile.currentXp}/${profile.maxXp} XP",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            // Currency & Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Coins
                GlassPanel(
                    modifier = Modifier.padding(end = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "12,750",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Diamonds
                GlassPanel(
                    modifier = Modifier.padding(end = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "150",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Settings Gear
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // HERO BANNER (Original Artwork from uploaded mockups)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_shadow_hunt),
                contentDescription = "Shadow Hunt Hero",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            // Bottom Gradient Fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, DarkBackground)
                        )
                    )
            )
        }

        // ACTION BUTTONS SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // PLAY NOW (Primary Glowing Red Button)
            NeonGamingButton(
                text = "PLAY NOW",
                onClick = onPlayNow,
                icon = Icons.Default.PlayArrow,
                gradientColors = listOf(Color(0xFFFF1E43), Color(0xFFD50000)),
                borderColor = Color(0xFFFF5252),
                height = 58.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // CREATE ROOM and JOIN ROOM (Dual Neon Action Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CREATE ROOM (Electric Blue)
                NeonGamingButton(
                    text = "CREATE ROOM",
                    onClick = onCreateRoom,
                    icon = Icons.Default.Add,
                    trailingText = "",
                    gradientColors = listOf(Color(0xFF00B0FF), Color(0xFF0072FF)),
                    borderColor = Color(0xFF40C4FF),
                    height = 50.dp,
                    modifier = Modifier.weight(1f)
                )

                // JOIN ROOM (Neon Purple)
                NeonGamingButton(
                    text = "JOIN ROOM",
                    onClick = onJoinRoom,
                    icon = Icons.Default.Group,
                    trailingText = "",
                    gradientColors = listOf(Color(0xFF9D00FF), Color(0xFF6200EA)),
                    borderColor = Color(0xFFB388FF),
                    height = 50.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            // REJOIN ACTIVE GAME CARD (If room exists)
            if (hasActiveRoom) {
                Spacer(modifier = Modifier.height(14.dp))
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRejoinRoom() },
                    borderColor = NeonRed,
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NeonRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "REJOIN YOUR GAME",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Room Code: $activeRoomCode",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NeonRed
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HOW TO PLAY SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF323456),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "// ",
                                color = NeonRed,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "HOW TO PLAY ?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Learn More >",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ElectricBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HowToPlayStepItem(
                            stepNumber = 1,
                            title = "Join Room",
                            subtitle = "Create or join a game",
                            icon = Icons.Default.Groups,
                            accentColor = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        HowToPlayStepItem(
                            stepNumber = 2,
                            title = "Hide or Seek",
                            subtitle = "Be hunter or runner",
                            icon = Icons.Default.DirectionsRun,
                            accentColor = NeonPurple,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        HowToPlayStepItem(
                            stepNumber = 3,
                            title = "Survive & Win",
                            subtitle = "Outsmart the zone",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GAME STATS CARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF323456),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "// ",
                            color = NeonRed,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "GAME STATS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "• Live Global",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounterBox(
                            value = "2.5M+",
                            label = "Total Games",
                            icon = Icons.Default.SportsEsports,
                            iconTint = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(DarkCardBorder)
                        )
                        StatCounterBox(
                            value = "1.8M+",
                            label = "Players Caught",
                            icon = Icons.Default.GpsFixed,
                            iconTint = NeonRed,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(DarkCardBorder)
                        )
                        StatCounterBox(
                            value = "742K+",
                            label = "Victories",
                            icon = Icons.Default.EmojiEvents,
                            iconTint = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RECENT ACHIEVEMENTS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color(0xFF323456),
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
                                tint = NeonPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent Achievements",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View All >",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NeonPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AchievementBadgeItem("First Hunt", Icons.Default.MilitaryTech, GoldAccent)
                        AchievementBadgeItem("Master Seeker", Icons.Default.Security, NeonPurple)
                        AchievementBadgeItem("Survivor", Icons.Default.DirectionsRun, ElectricBlue)
                        AchievementBadgeItem("Perfect Round", Icons.Default.Adjust, NeonRed)
                        AchievementBadgeItem("???", Icons.Default.Lock, TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadgeItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.5.dp, accentColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
            color = Color.White,
            maxLines = 1
        )
    }
}
