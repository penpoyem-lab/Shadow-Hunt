package com.example.ui.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun QrScannerDialog(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101D)),
            border = BorderStroke(1.5.dp, ElectricBlue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TACTICAL QR SCANNER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CAMERA SCANNING VIEWFINDER
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF04060C))
                        .border(1.5.dp, Color(0xFF232D48), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Corner targeting brackets
                        val bracketLen = 28.dp.toPx()
                        val strokeW = 3.dp.toPx()

                        // Top-Left
                        drawLine(ElectricBlue, Offset(16f, 16f), Offset(16f + bracketLen, 16f), strokeW)
                        drawLine(ElectricBlue, Offset(16f, 16f), Offset(16f, 16f + bracketLen), strokeW)

                        // Top-Right
                        drawLine(ElectricBlue, Offset(w - 16f, 16f), Offset(w - 16f - bracketLen, 16f), strokeW)
                        drawLine(ElectricBlue, Offset(w - 16f, 16f), Offset(w - 16f, 16f + bracketLen), strokeW)

                        // Bottom-Left
                        drawLine(ElectricBlue, Offset(16f, h - 16f), Offset(16f + bracketLen, h - 16f), strokeW)
                        drawLine(ElectricBlue, Offset(16f, h - 16f), Offset(16f, h - 16f - bracketLen), strokeW)

                        // Bottom-Right
                        drawLine(ElectricBlue, Offset(w - 16f, h - 16f), Offset(w - 16f - bracketLen, h - 16f), strokeW)
                        drawLine(ElectricBlue, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - bracketLen), strokeW)

                        // Animated Green Laser Line
                        val laserY = 24f + (h - 48f) * laserProgress
                        drawLine(
                            color = NeonGreen,
                            start = Offset(20f, laserY),
                            end = Offset(w - 20f, laserY),
                            strokeWidth = 2.5.dp.toPx()
                        )
                        // Laser glow rect
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x3300E676), Color.Transparent),
                                startY = laserY - 14f,
                                endY = laserY + 14f
                            ),
                            topLeft = Offset(20f, laserY - 14f),
                            size = Size(w - 40f, 28f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0x5500E5FF),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "POINT CAMERA AT QR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0x88FFFFFF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "QUICK DETECT FOUND SQUADS:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Optical Scan Simulations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SH7KQ2", "HUNT99", "VALK07").forEach { code ->
                        Surface(
                            onClick = {
                                Toast.makeText(context, "QR Recognized: $code", Toast.LENGTH_SHORT).show()
                                onCodeScanned(code)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "Scan",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) GoldAccent else DarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Auto-scanned host room SH7KQ2", Toast.LENGTH_SHORT).show()
                            onCodeScanned("SH7KQ2")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DETECT QR", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomQrCodeDialog(
    roomCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101D)),
            border = BorderStroke(1.5.dp, NeonPurple),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM QR CODE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HIGH-TECH PROCEDURAL QR MATRIX
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 15
                        val cellSize = size.width / gridSize

                        // Finder Patterns (Top-Left, Top-Right, Bottom-Left)
                        fun drawFinder(startX: Int, startY: Int) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(startX * cellSize, startY * cellSize),
                                size = Size(5 * cellSize, 5 * cellSize)
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
                                size = Size(3 * cellSize, 3 * cellSize)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
                                size = Size(1 * cellSize, 1 * cellSize)
                            )
                        }

                        drawFinder(0, 0)
                        drawFinder(gridSize - 5, 0)
                        drawFinder(0, gridSize - 5)

                        // Pseudo-random deterministic QR payload cells based on roomCode hash
                        val hash = roomCode.hashCode()
                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                val inFinder = (r < 5 && c < 5) || (r < 5 && c >= gridSize - 5) || (r >= gridSize - 5 && c < 5)
                                if (!inFinder) {
                                    val shouldFill = ((r * 31 + c * 17 + hash) % 3) == 0
                                    if (shouldFill) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize - 0.5f, cellSize - 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = ElectricBlue
                )

                Text(
                    text = "Scan with camera or Shadow Hunt app to join",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Shadow Hunt Room", "https://shadowhunt.app/room/$roomCode")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY LINK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DONE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
