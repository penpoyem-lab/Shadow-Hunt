package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoomConfig
import com.example.ui.theme.*

/**
 * "GAME FEEL" Modal Bottom Sheet matching IMG_20261106_143321_4a4bde20.jpg
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameFeelBottomSheet(
    currentConfig: RoomConfig,
    onSaveConfig: (RoomConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember {
        mutableStateOf(
            when {
                currentConfig.durationMinutes == 12 && currentConfig.safeZoneRadiusMeters.toInt() == 120 -> "INTENSE"
                currentConfig.durationMinutes == 30 && currentConfig.safeZoneRadiusMeters.toInt() == 220 -> "CHILL"
                currentConfig.durationMinutes == 20 && currentConfig.safeZoneRadiusMeters.toInt() == 160 -> "STANDARD"
                else -> "CUSTOM"
            }
        )
    }

    var roundLengthMinutes by remember { mutableIntStateOf(currentConfig.durationMinutes) }
    var circleRadiusMeters by remember { mutableIntStateOf(currentConfig.safeZoneRadiusMeters.toInt()) }
    var headStartSeconds by remember { mutableIntStateOf(30) }
    var radiusInputText by remember { mutableStateOf(currentConfig.safeZoneRadiusMeters.toInt().toString()) }

    var expandedRoundLength by remember { mutableStateOf(false) }
    var expandedHeadStart by remember { mutableStateOf(false) }

    val roundLengths = listOf(10, 12, 15, 20, 25, 30, 40, 60)
    val headStarts = listOf(15, 30, 45, 60, 90)
    val radiusPresets = listOf(
        "50 m" to 50,
        "100 m" to 100,
        "250 m" to 250,
        "500 m" to 500,
        "1 km" to 1000,
        "2 km" to 2000,
        "5 km" to 5000,
        "10 km" to 10000
    )

    // Calculate approximate area: pi * r^2
    val areaSqMeters = (Math.PI * circleRadiusMeters * circleRadiusMeters).toInt()
    val areaPerPlayer = areaSqMeters / 3

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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GAME FEEL",
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

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Preset Cards in Row: INTENSE, STANDARD, CHILL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // INTENSE Card
                PresetCard(
                    title = "INTENSE",
                    duration = "12 min",
                    radius = "120 m",
                    description = "Fast, tight and loud",
                    isSelected = selectedPreset == "INTENSE",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPreset = "INTENSE"
                        roundLengthMinutes = 12
                        circleRadiusMeters = 120
                        radiusInputText = "120"
                        headStartSeconds = 15
                    }
                )

                // STANDARD Card
                PresetCard(
                    title = "STANDARD",
                    duration = "20 min",
                    radius = "160 m",
                    description = "The full game",
                    isSelected = selectedPreset == "STANDARD",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPreset = "STANDARD"
                        roundLengthMinutes = 20
                        circleRadiusMeters = 160
                        radiusInputText = "160"
                        headStartSeconds = 30
                    }
                )

                // CHILL Card
                PresetCard(
                    title = "CHILL",
                    duration = "30 min",
                    radius = "220 m",
                    description = "A longer wander",
                    isSelected = selectedPreset == "CHILL",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedPreset = "CHILL"
                        roundLengthMinutes = 30
                        circleRadiusMeters = 220
                        radiusInputText = "220"
                        headStartSeconds = 45
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: YOUR OWN SETTINGS in Neon Lime
            Text(
                text = "YOUR OWN SETTINGS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = MugshotNeonLime
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The host set the round length, circle size and head start by hand.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0ABBE)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$roundLengthMinutes min · circle radius $circleRadiusMeters m · $headStartSeconds s head start",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your settings lock when the host starts the game.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A8A9E)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Back to recommended",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MugshotNeonLime
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        selectedPreset = "STANDARD"
                        roundLengthMinutes = 20
                        circleRadiusMeters = 160
                        radiusInputText = "160"
                        headStartSeconds = 30
                    }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dashed / Outlined Box with Custom Fields
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, Color(0xFF26344A), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. Round Length Dropdown
                    Column {
                        Text(
                            text = "Round length",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFA0ABBE)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF172033))
                                    .border(1.dp, Color(0xFF2D3C52), RoundedCornerShape(12.dp))
                                    .clickable { expandedRoundLength = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$roundLengthMinutes minutes",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = expandedRoundLength,
                                onDismissRequest = { expandedRoundLength = false },
                                modifier = Modifier.background(Color(0xFF172033))
                            ) {
                                roundLengths.forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes", color = Color.White) },
                                        onClick = {
                                            roundLengthMinutes = mins
                                            selectedPreset = "CUSTOM"
                                            expandedRoundLength = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Circle Radius Input
                    Column {
                        Text(
                            text = "Circle radius (20 m to 10 km)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFA0ABBE)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = radiusInputText,
                            onValueChange = { newVal ->
                                radiusInputText = newVal.filter { it.isDigit() }
                                val parsed = radiusInputText.toIntOrNull() ?: 160
                                circleRadiusMeters = parsed.coerceIn(20, 10000)
                                selectedPreset = "CUSTOM"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF172033),
                                unfocusedContainerColor = Color(0xFF172033),
                                focusedBorderColor = MugshotNeonLime,
                                unfocusedBorderColor = Color(0xFF2D3C52),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Radius Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            radiusPresets.take(4).forEach { (label, meters) ->
                                RadiusChip(
                                    label = label,
                                    isSelected = circleRadiusMeters == meters,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        circleRadiusMeters = meters
                                        radiusInputText = meters.toString()
                                        selectedPreset = "CUSTOM"
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            radiusPresets.drop(4).forEach { (label, meters) ->
                                RadiusChip(
                                    label = label,
                                    isSelected = circleRadiusMeters == meters,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        circleRadiusMeters = meters
                                        radiusInputText = meters.toString()
                                        selectedPreset = "CUSTOM"
                                    }
                                )
                            }
                        }
                    }

                    // 3. Head Start Dropdown
                    Column {
                        Text(
                            text = "Head start",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFA0ABBE)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF172033))
                                    .border(1.dp, Color(0xFF2D3C52), RoundedCornerShape(12.dp))
                                    .clickable { expandedHeadStart = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$headStartSeconds s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = expandedHeadStart,
                                onDismissRequest = { expandedHeadStart = false },
                                modifier = Modifier.background(Color(0xFF172033))
                            ) {
                                headStarts.forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("$sec s", color = Color.White) },
                                        onClick = {
                                            headStartSeconds = sec
                                            expandedHeadStart = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Runners get this long to run before the seeker can move or catch. Blinks begin after it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A8A9E)
                        )
                    }

                    // 4. Physics & Area Explanatory Note
                    Column {
                        Text(
                            text = "Recommended range (${"%,d".format(areaPerPlayer)} m² per player)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MugshotNeonLime
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The zone always closes at a walking pace, so a bigger circle needs a longer round. For $roundLengthMinutes minutes the biggest circle is 1.5 km. Picking a bigger circle lengthens the round for you (up to 40 minutes).\n\n" +
                                    "2 shrinks down to a final circle about 50 m across (sized for the players who start). Each zone waits about 7 min, then closes at a steady walking pace (the first closing takes about 46 s). The last circle lasts about 14 min for the hiding showdown.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = Color(0xFF8A9AAF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save / Apply Button
            LiquidGlassButton(
                text = "APPLY SETTINGS",
                onClick = {
                    val newConfig = currentConfig.copy(
                        durationMinutes = roundLengthMinutes,
                        safeZoneRadiusMeters = circleRadiusMeters.toFloat()
                    )
                    onSaveConfig(newConfig)
                    onDismiss()
                },
                gradientColors = listOf(MugshotNeonLime, MugshotNeonLimeDark),
                textColor = Color.Black,
                waterShadowColor = WaterShadowLime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}

@Composable
private fun PresetCard(
    title: String,
    duration: String,
    radius: String,
    description: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF162232) else Color(0xFF101624))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MugshotNeonLime else Color(0xFF26344A),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = if (isSelected) MugshotNeonLime else Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = radius,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFA0ABBE)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF7A8A9E),
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun RadiusChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF263720) else Color(0xFF172033))
            .border(
                width = 1.dp,
                color = if (isSelected) MugshotNeonLime else Color(0xFF2D3C52),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) MugshotNeonLime else Color.White
        )
    }
}
