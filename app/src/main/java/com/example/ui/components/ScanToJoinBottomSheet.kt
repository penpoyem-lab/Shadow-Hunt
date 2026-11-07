package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * "SCAN TO JOIN" Modal Bottom Sheet matching IMG_20261106_143312_ffe9b7eb.jpg
 * Features:
 *  - Header with close '✕' button
 *  - Explanatory subtitle
 *  - QR Code with Neon Lime corner scanner brackets
 *  - Prominent Neon Lime GAME CODE
 *  - Share button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToJoinBottomSheet(
    roomCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D121F),
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Row with Title and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCAN TO JOIN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA0ABBE),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan this code with your phone camera to open the invite link.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA0ABBE),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Centered QR Code with Vibrant Neon Lime Scanner Brackets
            Box(
                modifier = Modifier
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // QR White Surface
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Procedural high-contrast QR Matrix representation based on roomCode
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val matrixSize = 25
                        val cellSize = size.width / matrixSize

                        // Draw corner finder patterns (top-left, top-right, bottom-left)
                        fun drawFinder(cx: Float, cy: Float) {
                            drawRoundRect(
                                color = Color.Black,
                                topLeft = Offset(cx, cy),
                                size = Size(cellSize * 7, cellSize * 7),
                                cornerRadius = CornerRadius(cellSize * 1.5f, cellSize * 1.5f)
                            )
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(cx + cellSize, cy + cellSize),
                                size = Size(cellSize * 5, cellSize * 5),
                                cornerRadius = CornerRadius(cellSize, cellSize)
                            )
                            drawRoundRect(
                                color = Color.Black,
                                topLeft = Offset(cx + cellSize * 2, cy + cellSize * 2),
                                size = Size(cellSize * 3, cellSize * 3),
                                cornerRadius = CornerRadius(cellSize * 0.8f, cellSize * 0.8f)
                            )
                        }

                        drawFinder(0f, 0f)
                        drawFinder(size.width - cellSize * 7, 0f)
                        drawFinder(0f, size.height - cellSize * 7)

                        // Data modules seeded by roomCode hash
                        val seed = roomCode.hashCode()
                        val random = java.util.Random(seed.toLong())

                        for (r in 0 until matrixSize) {
                            for (c in 0 until matrixSize) {
                                // Skip finder patterns
                                val inTopLeft = r < 8 && c < 8
                                val inTopRight = r < 8 && c >= matrixSize - 8
                                val inBottomLeft = r >= matrixSize - 8 && c < 8

                                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                                    val isBlack = (r % 2 == 0 && c % 2 == 1) ||
                                            (random.nextFloat() > 0.45f) ||
                                            ((r + c) % 3 == 0)

                                    if (isBlack) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize, cellSize)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Neon Lime Scanner Corner Brackets [  ]
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bracketLen = 38.dp.toPx()
                    val strokeW = 4.5.dp.toPx()
                    val cornerR = 24.dp.toPx()
                    val color = MugshotNeonLime

                    // Top-Left Bracket
                    val tlPath = Path().apply {
                        moveTo(0f, bracketLen)
                        lineTo(0f, cornerR)
                        quadraticBezierTo(0f, 0f, cornerR, 0f)
                        lineTo(bracketLen, 0f)
                    }
                    drawPath(tlPath, color, style = Stroke(width = strokeW))

                    // Top-Right Bracket
                    val trPath = Path().apply {
                        moveTo(size.width - bracketLen, 0f)
                        lineTo(size.width - cornerR, 0f)
                        quadraticBezierTo(size.width, 0f, size.width, cornerR)
                        lineTo(size.width, bracketLen)
                    }
                    drawPath(trPath, color, style = Stroke(width = strokeW))

                    // Bottom-Left Bracket
                    val blPath = Path().apply {
                        moveTo(0f, size.height - bracketLen)
                        lineTo(0f, size.height - cornerR)
                        quadraticBezierTo(0f, size.height, cornerR, size.height)
                        lineTo(bracketLen, size.height)
                    }
                    drawPath(blPath, color, style = Stroke(width = strokeW))

                    // Bottom-Right Bracket
                    val brPath = Path().apply {
                        moveTo(size.width - bracketLen, size.height)
                        lineTo(size.width - cornerR, size.height)
                        quadraticBezierTo(size.width, size.height, size.width, size.height - cornerR)
                        lineTo(size.width, size.height - bracketLen)
                    }
                    drawPath(brPath, color, style = Stroke(width = strokeW))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle GAME CODE
            Text(
                text = "GAME CODE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF8A99AD)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Glowing Neon Lime Game Code
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Game Code", roomCode))
                        Toast.makeText(context, "Code copied: $roomCode", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = MugshotNeonLime
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MugshotNeonLime,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Share Invite Button with Liquid Glass and Water Shadow
            LiquidGlassButton(
                text = "SHARE INVITE LINK",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my Last Seen Mugshot game! Code: $roomCode\nDownload and enter the code to join.")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Game Code"))
                },
                icon = Icons.Default.Share,
                trailingText = null,
                gradientColors = listOf(Color(0xFF263520), Color(0xFF1B2816)),
                waterShadowColor = WaterShadowLime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}
