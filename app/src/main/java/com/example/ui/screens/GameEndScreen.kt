package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvatarRegistry
import com.example.model.PlayerRole
import com.example.model.PlayerState
import com.example.ui.components.GlassPanel
import com.example.ui.components.NeonGamingButton
import com.example.ui.theme.*

@Composable
fun GameEndScreen(
    players: List<PlayerState>,
    onPlayAgain: () -> Unit,
    onReturnHome: () -> Unit
) {
    val runners = players.filter { it.role == PlayerRole.RUNNER }
    val survivingRunners = runners.filter { !it.isTagged }
    val isSeekerVictory = survivingRunners.isEmpty()

    val mvp = players.maxByOrNull { if (it.role == PlayerRole.SEEKER) 5 else it.tagTimeSeconds } ?: players.first()
    val mvpAvatar = AvatarRegistry.getAvatar(mvp.avatarId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // VICTORY / DEFEAT HEADLINE
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(if (isSeekerVictory) NeonRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f))
                .border(2.dp, if (isSeekerVictory) NeonRed else NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSeekerVictory) Icons.Default.GpsFixed else Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = if (isSeekerVictory) NeonRed else GoldAccent,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isSeekerVictory) "SEEKER VICTORY!" else "RUNNER SURVIVAL!",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = if (isSeekerVictory) NeonRed else NeonGreen
        )

        Text(
            text = if (isSeekerVictory) "All runners were tagged before the safe zone expired." else "Surviving runners outlasted the hunters in the arena!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // MVP SHOWCASE CARD
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GoldAccent,
            borderWidth = 1.5.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MATCH MVP",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = GoldAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldAccent, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = mvpAvatar.drawableRes),
                        contentDescription = mvp.nickname,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mvp.nickname,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )

                Text(
                    text = if (mvp.role == PlayerRole.SEEKER) "Master Seeker • Total Control" else "Last Standing Survivor",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // MATCH RECAP TABLE
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = DarkCardBorder,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MATCH STATISTICS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                RecapRow("Total Players", "${players.size}")
                RecapRow("Runners Captured", "${players.count { it.role == PlayerRole.RUNNER && it.isTagged }}")
                RecapRow("Runners Survived", "${survivingRunners.size}")
                RecapRow("Safe Zone Final Stage", "Stage 5 (Sudden Death)")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ACTION BUTTONS
        NeonGamingButton(
            text = "PLAY AGAIN",
            onClick = onPlayAgain,
            icon = Icons.Default.Replay,
            gradientColors = listOf(Color(0xFFFF1E43), Color(0xFFD50000)),
            borderColor = Color(0xFFFF5252),
            height = 56.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.2.dp, DarkCardBorder)
        ) {
            Text(
                text = "RETURN HOME",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun RecapRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}
