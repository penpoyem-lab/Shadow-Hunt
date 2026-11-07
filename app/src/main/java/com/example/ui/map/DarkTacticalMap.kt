package com.example.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LiveLocationManager
import com.example.model.PlayerRole
import com.example.model.RealGpsPlayer
import com.example.model.RealGpsSafeZone
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.*

enum class MapLayerType(val displayName: String, val shortName: String) {
    SATELLITE("SATELLITE HYBRID", "SATELLITE"),
    TACTICAL_NEON("DARK TACTICAL", "TACTICAL"),
    STREET("OPENSTREETMAP", "STREET"),
    TERRAIN("TERRAIN", "TERRAIN")
}

object MapTileCache {
    val cache = ConcurrentHashMap<String, ImageBitmap>()
    val inFlight = ConcurrentHashMap<String, Boolean>()
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .build()
}

private fun getTileCandidateUrls(layer: MapLayerType, z: Int, x: Int, y: Int): List<String> {
    val serverNum = (abs(x + y) % 4)
    return when (layer) {
        MapLayerType.SATELLITE -> listOf(
            // 1. Google Hybrid Satellite (Photo + Roads + Labels) - 100% global coverage down to zoom 20
            "https://mt$serverNum.google.com/vt/lyrs=y&x=$x&y=$y&z=$z",
            // 2. Google Pure Satellite
            "https://mt$serverNum.google.com/vt/lyrs=s&x=$x&y=$y&z=$z",
            // 3. CartoDB Dark Matter fallback
            "https://basemaps.cartocdn.com/dark_all/$z/$x/$y.png",
            // 4. OpenStreetMap fallback
            "https://tile.openstreetmap.org/$z/$x/$y.png"
        )
        MapLayerType.TACTICAL_NEON -> listOf(
            // Dark Tactical Basemap from CartoDB (perfect dark tactical look)
            "https://basemaps.cartocdn.com/dark_all/$z/$x/$y.png",
            "https://tile.openstreetmap.org/$z/$x/$y.png",
            "https://mt$serverNum.google.com/vt/lyrs=y&x=$x&y=$y&z=$z"
        )
        MapLayerType.STREET -> listOf(
            "https://tile.openstreetmap.org/$z/$x/$y.png",
            "https://basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
            "https://mt$serverNum.google.com/vt/lyrs=m&x=$x&y=$y&z=$z"
        )
        MapLayerType.TERRAIN -> listOf(
            "https://mt$serverNum.google.com/vt/lyrs=p&x=$x&y=$y&z=$z",
            "https://basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
            "https://tile.openstreetmap.org/$z/$x/$y.png"
        )
    }
}

@Composable
fun DarkTacticalMap(
    centerLatitude: Double,
    centerLongitude: Double,
    userLatitude: Double,
    userLongitude: Double,
    userBearing: Float,
    safeZone: RealGpsSafeZone,
    players: List<RealGpsPlayer>,
    isSeeker: Boolean,
    sonarActive: Boolean,
    urgencyLevel: Float,
    breathingOffsetPx: Float,
    pulseProgress: Float,
    shockwaveAlpha: Float,
    shockwaveExpansionDp: androidx.compose.ui.unit.Dp,
    recenterTrigger: Int = 0,
    userAccuracyMeters: Float = 6f,
    userSpeedMps: Float = 0f,
    modifier: Modifier = Modifier
) {
    var selectedLayer by remember { mutableStateOf(MapLayerType.SATELLITE) }
    // High-clarity outdoor tactical zoom level showing houses, fields, alleys and streets
    var zoomLevel by remember { mutableFloatStateOf(16.5f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    // Cyan glowing tactical color matrix for TACTICAL_NEON mode (Matches Screenshot 4)
    val tacticalCyanMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1.4f, -1.4f, -1.4f, 0f, 255f, // Invert light land into dark slate
                -0.3f,  1.1f,  0.5f, 0f,  45f, // Glowing cyan-green
                 0.2f,  0.9f,  1.9f, 0f,  85f, // Electric blue-cyan streets
                 0f,    0f,    0f,   1f,   0f
            )
        )
    }
    val tacticalColorFilter = remember { ColorFilter.colorMatrix(tacticalCyanMatrix) }

    // Recenter camera onto user location
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            panOffsetX = 0f
            panOffsetY = 0f
            zoomLevel = 16.5f
        }
    }

    // Auto-recenter smoothly if camera hasn't been panned manually
    LaunchedEffect(userLatitude, userLongitude) {
        if (abs(panOffsetX) < 1f && abs(panOffsetY) < 1f) {
            // Keep centered on player
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A14))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomLevel = (zoomLevel * zoom).coerceIn(13.0f, 19.5f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
    ) {
        // 1. PRIMARY MAP CANVAS: ZERO-API-KEY TILES + GEOMETRY HUD
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val viewCenterLat = if (userLatitude != 0.0) userLatitude else safeZone.centerLatitude
            val viewCenterLon = if (userLongitude != 0.0) userLongitude else safeZone.centerLongitude

            val screenCenterX = canvasW / 2f + panOffsetX
            val screenCenterY = canvasH / 2f + panOffsetY

            // Flat-earth Mercator projection for local 5km gaming arena
            fun latLonToScreen(lat: Double, lon: Double): Offset {
                val metersPerPixel = (156543.03 * cos(Math.toRadians(viewCenterLat)) / 2.0.pow(zoomLevel.toDouble())).toFloat()
                val latDistanceMeters = ((lat - viewCenterLat) * 111320.0).toFloat()
                val lonDistanceMeters = ((lon - viewCenterLon) * (111320.0 * cos(Math.toRadians(viewCenterLat)))).toFloat()

                val screenX = screenCenterX + (lonDistanceMeters / metersPerPixel)
                val screenY = screenCenterY - (latDistanceMeters / metersPerPixel)
                return Offset(screenX, screenY)
            }

            fun metersToPixels(meters: Float): Float {
                val metersPerPixel = (156543.03 * cos(Math.toRadians(viewCenterLat)) / 2.0.pow(zoomLevel.toDouble())).toFloat()
                return meters / metersPerPixel
            }

            // Draw base dark tactical grid canvas background
            drawRect(color = Color(0xFF060A14))

            // RASTER TILE DOWNLOAD & DRAW ENGINE (Google Hybrid Satellite / CartoDB Dark / OSM)
            val z = zoomLevel.toInt().coerceIn(12, 19)
            val centerTileX = lonToTileX(viewCenterLon, z)
            val centerTileY = latToTileY(viewCenterLat, z)

            // Render a wide 5x5 tile grid centered around viewport
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tileX = centerTileX + dx
                    val tileY = centerTileY + dy
                    val tileKey = "${selectedLayer.name}/$z/$tileX/$tileY"

                    val tileLon = tileXToLon(tileX, z)
                    val tileLat = tileYToLat(tileY, z)
                    val nextTileLon = tileXToLon(tileX + 1, z)
                    val nextTileLat = tileYToLat(tileY + 1, z)

                    val topLeft = latLonToScreen(tileLat, tileLon)
                    val bottomRight = latLonToScreen(nextTileLat, nextTileLon)
                    val tileWidth = abs(bottomRight.x - topLeft.x)
                    val tileHeight = abs(bottomRight.y - topLeft.y)

                    val cached = MapTileCache.cache[tileKey]
                    if (cached != null) {
                        drawImage(
                            image = cached,
                            dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(tileWidth.toInt() + 1, tileHeight.toInt() + 1),
                            colorFilter = if (selectedLayer == MapLayerType.TACTICAL_NEON) tacticalColorFilter else null
                        )
                    } else {
                        // Cyberpunk placeholder grid cell with subtle coordinates
                        drawRect(
                            color = Color(0xFF090D1A),
                            topLeft = topLeft,
                            size = Size(tileWidth, tileHeight)
                        )
                        drawRect(
                            color = Color(0x1800E5FF),
                            topLeft = topLeft,
                            size = Size(tileWidth, tileHeight),
                            style = Stroke(1f)
                        )

                        // Asynchronous tile fetch from high-resolution, global tile candidates
                        if (MapTileCache.inFlight.putIfAbsent(tileKey, true) == null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val candidateUrls = getTileCandidateUrls(selectedLayer, z, tileX, tileY)
                                    for (candidateUrl in candidateUrls) {
                                        try {
                                            val req = Request.Builder()
                                                .url(candidateUrl)
                                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                                .header("Referer", "https://maps.google.com/")
                                                .build()

                                            val resp = MapTileCache.httpClient.newCall(req).execute()
                                            if (resp.isSuccessful) {
                                                val bytes = resp.body?.bytes()
                                                // Ensure valid image size (> 1000 bytes) rather than error placeholder
                                                if (bytes != null && bytes.size > 1000) {
                                                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                    if (bmp != null) {
                                                        MapTileCache.cache[tileKey] = bmp.asImageBitmap()
                                                        break
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                } finally {
                                    MapTileCache.inFlight.remove(tileKey)
                                }
                            }
                        }
                    }
                }
            }

            // 2. CONCENTRIC DISTANCE RADAR RINGS (100m, 200m, 300m)
            val userScreen = latLonToScreen(userLatitude, userLongitude)
            listOf(100f, 200f, 300f).forEach { radiusMeters ->
                val rPx = metersToPixels(radiusMeters)
                drawCircle(
                    color = Color(0x2200E5FF),
                    radius = rPx,
                    center = userScreen,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                )
            }

            // 3. GLOWING CYAN CIRCULAR SAFE ZONE (Exact aesthetic of Screenshot 4)
            val zoneCenterScreen = latLonToScreen(safeZone.centerLatitude, safeZone.centerLongitude)
            val baseRadiusPx = metersToPixels(safeZone.currentRadiusMeters)
            val animatedRadiusPx = (baseRadiusPx + breathingOffsetPx).coerceAtLeast(10f)

            // Outer danger tint outside zone
            drawRect(color = Color(0x24FF1744))

            // Inner safe zone cyan aura
            val safeAreaTint = if (urgencyLevel > 0.7f) Color(0x25FF1E43) else Color(0x2000E5FF)
            drawCircle(
                color = safeAreaTint,
                radius = animatedRadiusPx,
                center = zoneCenterScreen
            )

            // Expanding shockwave pulse ring on boundary
            val shockwaveRadius = animatedRadiusPx + (shockwaveExpansionDp.toPx() * pulseProgress)
            val shockwaveColor = if (urgencyLevel > 0.5f) Color(0xFFFF1E43) else Color(0xFF00E5FF)
            drawCircle(
                color = shockwaveColor.copy(alpha = shockwaveAlpha),
                radius = shockwaveRadius,
                center = zoneCenterScreen,
                style = Stroke(width = (3.dp).toPx())
            )

            // Neon Glowing Perimeter Ring (Thick cyan border matching Screenshot 4)
            val glowColor = if (urgencyLevel > 0.6f) Color(0xFFFF1E43) else Color(0xFF00E5FF)
            // Outer soft bloom
            drawCircle(
                color = glowColor.copy(alpha = 0.45f),
                radius = animatedRadiusPx + 3.dp.toPx(),
                center = zoneCenterScreen,
                style = Stroke(width = 7.dp.toPx())
            )
            // Intense core ring
            drawCircle(
                color = glowColor,
                radius = animatedRadiusPx,
                center = zoneCenterScreen,
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Dashed urgency ring when zone is shrinking
            if (safeZone.isShrinking || urgencyLevel > 0.6f) {
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = animatedRadiusPx + 8.dp.toPx(),
                    center = zoneCenterScreen,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), pulseProgress * 20f)
                    )
                )
            }

            // 4. PLAYER MARKERS (Green runners inside, red hunters/seekers, matching Screenshot 4)
            players.forEach { player ->
                if (!player.isTagged && !player.isLocalPlayer) {
                    val pScreen = latLonToScreen(player.latitude, player.longitude)

                    if (player.role == PlayerRole.SEEKER) {
                        // Seeker/Hunter: Visible ONLY to other Seekers/Hunters on the same team. Seeker location is NEVER revealed to Hiders!
                        val isSeekerVisible = isSeeker

                        if (isSeekerVisible) {
                            val captureHaloRadiusPx = metersToPixels(20f)
                            drawCircle(
                                color = Color(0x35FF1E43),
                                radius = captureHaloRadiusPx,
                                center = pScreen
                            )
                            drawCircle(
                                color = Color(0xFFFF1E43),
                                radius = captureHaloRadiusPx,
                                center = pScreen,
                                style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                            )
                            // Red pin marker
                            drawCircle(
                                color = Color(0x55FF1E43),
                                radius = 16.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color(0xFFFF1744),
                                radius = 10.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = pScreen
                            )
                        }
                    } else {
                        // Runner/Hider: Visible to fellow runners on their team
                        val isRunnerVisible = !isSeeker
                        if (isRunnerVisible) {
                            // Green glowing pin
                            drawCircle(
                                color = Color(0x4400E676),
                                radius = 16.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color(0xFF00E676),
                                radius = 10.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = pScreen
                            )
                        }
                    }
                }
            }

            // Active 1:30 Radar Reveal Sweep Wave Animation
            if (sonarActive) {
                val sweepRadius = metersToPixels(350f * pulseProgress)
                drawCircle(
                    color = Color(0x5500E5FF),
                    radius = sweepRadius,
                    center = userScreen,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0x1000E5FF),
                    radius = sweepRadius,
                    center = userScreen
                )
            }

            // 5. DYNAMIC GLOWING ACCURACY RING (Real GPS Tolerance Halo)
            val accuracyPx = metersToPixels(userAccuracyMeters.coerceIn(3f, 45f))
            // Soft cyan-blue accuracy fill
            drawCircle(
                color = Color(0x1800E5FF),
                radius = accuracyPx,
                center = userScreen
            )
            // Glowing dashed accuracy perimeter
            drawCircle(
                color = Color(0x8800E5FF),
                radius = accuracyPx,
                center = userScreen,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), pulseProgress * 14f)
                )
            )

            // 6. CENTRAL USER MARKER (Bright Yellow Glowing Dot + Heading Beam, Screenshot 4)
            // Outer golden aura
            drawCircle(
                color = Color(0x44FFEA00),
                radius = 22.dp.toPx(),
                center = userScreen
            )
            // Intermediate yellow glow
            drawCircle(
                color = Color(0x88FFEA00),
                radius = 14.dp.toPx(),
                center = userScreen
            )
            // Bright solid yellow core
            drawCircle(
                color = Color(0xFFFFEA00),
                radius = 8.dp.toPx(),
                center = userScreen
            )
            // White center reflection
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = userScreen
            )

            // Compass heading ray
            val rad = Math.toRadians((userBearing - 90).toDouble())
            val coneLength = 32.dp.toPx()
            val tipX = userScreen.x + cos(rad).toFloat() * coneLength
            val tipY = userScreen.y + sin(rad).toFloat() * coneLength
            drawLine(
                color = Color(0xFFFFEA00),
                start = userScreen,
                end = Offset(tipX, tipY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. TOP LAYER SELECTOR HUD PILL (TACTICAL, STREET, SATELLITE, TERRAIN)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xDD090D18))
                .border(1.dp, Color(0xFF232A42), RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MapLayerType.values().forEach { layer ->
                val isSelected = selectedLayer == layer
                Surface(
                    onClick = { selectedLayer = layer },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ElectricBlue else Color.Transparent
                ) {
                    Text(
                        text = layer.shortName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isSelected) Color.Black else Color(0xFFB0B7C3),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 3. FLOATING ON-MAP CONTROLS (Compass, Zoom In/Out)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compass Rose Widget
            Surface(
                onClick = { panOffsetX = 0f; panOffsetY = 0f },
                shape = CircleShape,
                color = Color(0xEE090D18),
                border = BorderStroke(1.5.dp, ElectricBlue),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Compass",
                        tint = NeonRed,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(userBearing)
                    )
                }
            }

            // Zoom In (+) Button
            Surface(
                onClick = { zoomLevel = (zoomLevel + 0.6f).coerceAtMost(20.0f) },
                shape = CircleShape,
                color = Color(0xEE090D18),
                border = BorderStroke(1.5.dp, Color(0xFF28324E)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Zoom Out (-) Button
            Surface(
                onClick = { zoomLevel = (zoomLevel - 0.6f).coerceAtMost(20.0f).coerceAtLeast(14.0f) },
                shape = CircleShape,
                color = Color(0xEE090D18),
                border = BorderStroke(1.5.dp, Color(0xFF28324E)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. BOTTOM-LEFT LIVE GPS PRECISION BADGE
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 120.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xDD090D18),
            border = BorderStroke(1.2.dp, Color(0xFF28324E))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (userAccuracyMeters <= 10f) NeonGreen else GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS PRECISION ±${userAccuracyMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }
                Text(
                    text = if (userLatitude != 0.0) String.format("%.5f° N, %.5f° E", userLatitude, userLongitude) else "SEARCHING FIX...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = Color(0xFF90A4AE)
                )
                if (userSpeedMps > 0.5f) {
                    Text(
                        text = "SPEED: ${String.format("%.1f", userSpeedMps * 3.6f)} km/h",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        ),
                        color = ElectricBlue
                    )
                }
            }
        }
    }
}

// Slippy Map Web Mercator tile projection utilities
private fun lonToTileX(lon: Double, zoom: Int): Int {
    return floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
}

private fun latToTileY(lat: Double, zoom: Int): Int {
    var sinLat = sin(Math.toRadians(lat))
    sinLat = sinLat.coerceIn(-0.9999, 0.9999)
    return floor((0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * (1 shl zoom)).toInt()
}

private fun tileXToLon(x: Int, zoom: Int): Double {
    return x.toDouble() / (1 shl zoom) * 360.0 - 180.0
}

private fun tileYToLat(y: Int, zoom: Int): Double {
    val n = PI - 2.0 * PI * y.toDouble() / (1 shl zoom)
    return Math.toDegrees(atan(sinh(n)))
}
