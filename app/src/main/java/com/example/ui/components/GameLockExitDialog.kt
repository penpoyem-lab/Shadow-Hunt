package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

/**
 * Tactical Game Lock Exit Confirmation Dialog.
 * Enforces rule: "No one can exit the app when the game is running".
 * Prevents accidental quits during live matches and warns about forfeit penalties.
 */
@Composable
fun GameLockExitDialog(
    onDismiss: () -> Unit,
    onConfirmForfeit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1422)),
            border = BorderStroke(2.dp, NeonRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Lock Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF1E43)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Game Locked",
                        tint = NeonRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MATCH LOCK ACTIVE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = "ZONE ENGAGEMENT IN PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonRed
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You cannot exit while the game is running! Your squad and hunters are synchronized in real-time.\n\nAbandoning the zone now will forfeit all your match XP, drop your rank rating, and permanently stamp you BUSTED.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary: Stay in Game
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME BATTLE",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary: Forfeit & Exit
                OutlinedButton(
                    onClick = onConfirmForfeit,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x66FF1E43)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FORFEIT & ABANDON MATCH",
                        color = NeonRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
