package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlin.math.PI

/**
 * Interactive Adjust Safe Zone Dialog for Create Game section.
 * Allows the game host to customize and visually preview the initial safe zone radius,
 * shrink stages, and coverage metrics before creating the room.
 */
@Composable
fun AdjustSafeZoneDialog(
    initialRadiusMeters: Float,
    initialStages: Int,
    initialSpeed: String,
    onDismiss: () -> Unit,
    onApply: (radiusMeters: Float, stages: Int, speed: String) -> Unit
) {
    var radius by remember { mutableFloatStateOf(initialRadiusMeters) }
    var stages by remember { mutableIntStateOf(initialStages) }
    var speed by remember { mutableStateOf(initialSpeed) }

    // Pulsing preview animation
    val infiniteTransition = rememberInfiniteTransition(label = "zoneAdjustPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101D)),
            border = BorderStroke(1.5.dp, ElectricBlue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ADJUST SAFE ZONE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "CUSTOMIZE ARENA BOUNDARY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ElectricBlue
                            )
                        }
                    }

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

                // LIVE RADAR CANVAS PREVIEW
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF050811))
                        .border(1.5.dp, Color(0xFF232D48), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = size.width / 2f - 12f

                        // Grid crosshairs
                        drawLine(Color(0x1800E5FF), Offset(cx, 0f), Offset(cx, size.height), 1f)
                        drawLine(Color(0x1800E5FF), Offset(0f, cy), Offset(size.width, cy), 1f)

                        // Outer max range ring (1000m)
                        drawCircle(
                            color = Color(0x1A00E5FF),
                            radius = maxR,
                            center = Offset(cx, cy),
                            style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        )

                        // Scaled safe zone circle (maps 1000m..3000m to 35%..100% of maxR)
                        val norm = ((radius - 1000f) / 2000f).coerceIn(0f, 1f)
                        val circleR = (maxR * 0.35f + maxR * 0.65f * norm).coerceIn(25f, maxR)

                        // Shockwave ring
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = (1f - pulse) * 0.6f),
                            radius = circleR + 14f * pulse,
                            center = Offset(cx, cy),
                            style = Stroke(2.dp.toPx())
                        )

                        // Glowing safe zone area
                        drawCircle(
                            color = Color(0x2000E5FF),
                            radius = circleR,
                            center = Offset(cx, cy)
                        )

                        // Safe zone thick cyan perimeter
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = circleR,
                            center = Offset(cx, cy),
                            style = Stroke(3.dp.toPx())
                        )

                        // Center player marker (Bright yellow)
                        drawCircle(Color(0xFFFFEA00), radius = 6.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))

                        // A few simulated runners inside zone
                        val r1 = circleR * 0.5f
                        drawCircle(Color(0xFF00E676), radius = 4.dp.toPx(), center = Offset(cx + r1 * 0.7f, cy - r1 * 0.4f))
                        drawCircle(Color(0xFF00E676), radius = 4.dp.toPx(), center = Offset(cx - r1 * 0.5f, cy + r1 * 0.6f))
                    }

                    // Floating Radius Pill (Min 1.0 km)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xDD0C101D),
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "%.1f km (${radius.toInt()}m)".format(radius / 1000f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = ElectricBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RADIUS PRESET CHIPS (MINIMUM 1KM)
                Text(
                    text = "INITIAL SAFE ZONE RADIUS (MINIMUM 1 KM)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1000f, 1500f, 2000f, 2500f, 3000f).forEach { rVal ->
                        val isSel = (radius == rVal)
                        Surface(
                            onClick = { radius = rVal },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) ElectricBlue else DarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) ElectricBlue else DarkCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "%.1fkm".format(rVal / 1000f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) Color.Black else Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Slider (1000m to 3000m)
                Slider(
                    value = radius.coerceAtLeast(1000f),
                    onValueChange = { radius = (it / 50).toInt() * 50f },
                    valueRange = 1000f..3000f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SHRINK STAGES (3, 4, 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SHRINK PHASES",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Total collapse stages",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 4, 5).forEach { st ->
                            val isSel = (stages == st)
                            Surface(
                                onClick = { stages = st },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) NeonPurple else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSel) NeonPurple else DarkCardBorder)
                            ) {
                                Text(
                                    text = "$st Stages",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ESTIMATED METRICS CARD
                val areaKm2 = (PI * radius * radius / 1_000_000.0)
                val sprintSec = (radius / 4.2).toInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF13182A),
                    border = BorderStroke(1.dp, Color(0xFF263152)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COVERAGE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                            Text("%.2f km²".format(areaKm2), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                        }
                        Box(modifier = Modifier.size(1.dp, 24.dp).background(DarkCardBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DIAMETER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                            Text("${(radius * 2).toInt()}m", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = ElectricBlue)
                        }
                        Box(modifier = Modifier.size(1.dp, 24.dp).background(DarkCardBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RUN TIME", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                            Text("~${sprintSec}s", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NeonGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Apply Button
                NeonGamingButton(
                    text = "APPLY SAFE ZONE",
                    onClick = {
                        onApply(radius, stages, speed)
                        onDismiss()
                    },
                    icon = Icons.Default.Check,
                    gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF0072FF)),
                    borderColor = Color(0xFF80D8FF),
                    height = 50.dp
                )
            }
        }
    }
}
