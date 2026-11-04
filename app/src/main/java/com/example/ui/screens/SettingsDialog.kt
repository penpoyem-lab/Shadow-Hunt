package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.GameSoundEffects
import com.example.ui.components.GlassPanel
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    soundEffects: GameSoundEffects,
    onDismiss: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(soundEffects.isSoundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(soundEffects.isHapticsEnabled) }
    var showAdminInfo by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0xFF383B5E),
            backgroundColor = Color(0xF50E101D),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GAME SETTINGS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound FX Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Sound Effects", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(text = "Synthesized cyber audio & sonar", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            soundEffects.isSoundEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue, checkedThumbColor = Color.White)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                // Haptic Feedback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Vibration & Haptics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(text = "Zone alarms & capture shockwaves", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            soundEffects.isHapticsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = NeonRed, checkedThumbColor = Color.White)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkCardBorder)

                // Admin & Health Status Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Admin Health Monitor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(text = "Server rooms & anti-cheat check", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary)
                        }
                    }
                    Checkbox(
                        checked = showAdminInfo,
                        onCheckedChange = { showAdminInfo = it },
                        colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                    )
                }

                if (showAdminInfo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text = "• Active Rooms: 124 Rooms", color = NeonGreen, style = MaterialTheme.typography.labelSmall)
                            Text(text = "• Server Tickrate: 60 FPS Authoritative", color = ElectricBlue, style = MaterialTheme.typography.labelSmall)
                            Text(text = "• Anti-Cheat Status: ACTIVE (Strict Virtual Physics)", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                            Text(text = "• Privacy Enforcement: Zero GPS / Local Virtual Grid", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy assurance notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2200E676))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shadow Hunt is 100% digital. No real GPS or cameras used.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = Color(0xFFB9F6CA)
                    )
                }
            }
        }
    }
}
