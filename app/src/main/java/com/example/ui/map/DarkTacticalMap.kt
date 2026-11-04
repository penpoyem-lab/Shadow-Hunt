package com.example.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.PlayerRole
import com.example.model.RealGpsPlayer
import com.example.model.RealGpsSafeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

object MapTileCache {
    val cache = ConcurrentHashMap<String, ImageBitmap>()
    val httpClient = OkHttpClient.Builder().build()
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
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(16.5f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    // Smoothly follow user when within default tracking
    LaunchedEffect(userLatitude, userLongitude) {
        if (abs(panOffsetX) < 1f && abs(panOffsetY) < 1f) {
            // Keep centered
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070811))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomLevel = (zoomLevel * zoom).coerceIn(14f, 19f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val viewCenterLat = userLatitude
            val viewCenterLon = userLongitude

            val screenCenterX = canvasW / 2f + panOffsetX
            val screenCenterY = canvasH / 2f + panOffsetY

            // Coordinate conversion helper: Lat/Lon -> Screen X/Y
            fun latLonToScreen(lat: Double, lon: Double): Offset {
                val metersPerPixel = (156543.03 * cos(Math.toRadians(viewCenterLat)) / 2.0.pow(zoomLevel.toDouble())).toFloat()

                // Approximate flat-earth projection for local game area (<5km)
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

            // 1. Draw Map Tiles (CartoDB Dark Matter / OSM)
            val z = zoomLevel.toInt()
            val centerTileX = lonToTileX(viewCenterLon, z)
            val centerTileY = latToTileY(viewCenterLat, z)

            // Render a 4x4 tile grid around current view
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tileX = centerTileX + dx
                    val tileY = centerTileY + dy
                    val tileKey = "$z/$tileX/$tileY"

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
                            dstSize = androidx.compose.ui.unit.IntSize(tileWidth.toInt(), tileHeight.toInt())
                        )
                    } else {
                        // Background placeholder grid tile
                        drawRect(
                            color = Color(0xFF0B0D18),
                            topLeft = topLeft,
                            size = Size(tileWidth, tileHeight)
                        )
                        drawRect(
                            color = Color(0x1500E5FF),
                            topLeft = topLeft,
                            size = Size(tileWidth, tileHeight),
                            style = Stroke(1f)
                        )

                        // Request tile asynchronously
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = "https://a.basemaps.cartocdn.com/dark_all/$z/$tileX/$tileY.png"
                                val req = Request.Builder().url(url).header("User-Agent", "ShadowHuntAndroid/1.0").build()
                                val resp = MapTileCache.httpClient.newCall(req).execute()
                                if (resp.isSuccessful) {
                                    val bytes = resp.body?.bytes()
                                    if (bytes != null) {
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (bmp != null) {
                                            MapTileCache.cache[tileKey] = bmp.asImageBitmap()
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            // 2. Real Shrinking Safe Zone Geofence Circle
            val zoneCenterScreen = latLonToScreen(safeZone.centerLatitude, safeZone.centerLongitude)
            val baseRadiusPx = metersToPixels(safeZone.currentRadiusMeters)
            val animatedRadiusPx = baseRadiusPx + breathingOffsetPx

            // Outer Danger Zone Overlay
            drawRect(color = Color(0x28FF1744))

            // Inner Safe Zone Tint (Green/Cyan)
            val safeAreaTint = if (urgencyLevel > 0.7f) Color(0x22FF1E43) else Color(0x1E00E676)
            drawCircle(
                color = safeAreaTint,
                radius = animatedRadiusPx,
                center = zoneCenterScreen
            )

            // Expanding Shockwave Ring on Safe Zone Boundary
            val shockwaveRadius = animatedRadiusPx + (shockwaveExpansionDp.toPx() * pulseProgress)
            val shockwaveColor = if (urgencyLevel > 0.5f) Color(0xFFFF1E43) else Color(0xFF00E5FF)
            drawCircle(
                color = shockwaveColor.copy(alpha = shockwaveAlpha),
                radius = shockwaveRadius,
                center = zoneCenterScreen,
                style = Stroke(width = (2.5f + urgencyLevel * 2f).dp.toPx())
            )

            // Main Pulsing Perimeter Ring
            val ringColors = if (urgencyLevel > 0.6f) {
                listOf(Color(0xFFFF1E43), Color(0xFFFFD700), Color(0xFFFF1E43))
            } else {
                listOf(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFF00E5FF))
            }

            drawCircle(
                brush = Brush.sweepGradient(ringColors, zoneCenterScreen),
                radius = animatedRadiusPx,
                center = zoneCenterScreen,
                style = Stroke(width = (3.5f + urgencyLevel * 2.5f).dp.toPx())
            )

            // Dashed Warning Ring when Shrinking
            if (safeZone.isShrinking || urgencyLevel > 0.6f) {
                drawCircle(
                    color = Color(0xFFFF1E43).copy(alpha = 0.85f),
                    radius = animatedRadiusPx + 6.dp.toPx(),
                    center = zoneCenterScreen,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), pulseProgress * 24f)
                    )
                )
            }

            // 3. Render Players on Live Map
            players.forEach { player ->
                if (!player.isTagged) {
                    val pScreen = latLonToScreen(player.latitude, player.longitude)

                    if (player.role == PlayerRole.SEEKER) {
                        // SEEKER: Menacing Red Marker + 20-Meter Capture Halo
                        val captureHaloRadiusPx = metersToPixels(20f)
                        drawCircle(
                            color = Color(0x33FF1E43),
                            radius = captureHaloRadiusPx,
                            center = pScreen
                        )
                        drawCircle(
                            color = Color(0x88FF1E43),
                            radius = captureHaloRadiusPx,
                            center = pScreen,
                            style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                        )
                        drawCircle(
                            color = Color(0xFFFF1E43),
                            radius = 12.dp.toPx(),
                            center = pScreen
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pScreen
                        )
                    } else {
                        // RUNNER: Cyan Marker (Revealed during Sonar GPS Blink or for local player)
                        val isVisible = player.isLocalPlayer || sonarActive || !isSeeker
                        if (isVisible) {
                            drawCircle(
                                color = Color(0x4400E5FF),
                                radius = 16.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 10.dp.toPx(),
                                center = pScreen
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = pScreen
                            )
                        } else {
                            // Faint blip
                            drawCircle(
                                color = Color(0x2200E5FF),
                                radius = 6.dp.toPx(),
                                center = pScreen
                            )
                        }
                    }
                }
            }

            // 4. Local User Location Crosshair & Compass Heading
            val userScreen = latLonToScreen(userLatitude, userLongitude)
            drawCircle(
                color = Color(0x33FFD700),
                radius = 20.dp.toPx(),
                center = userScreen
            )
            drawCircle(
                color = Color.Yellow,
                radius = 8.dp.toPx(),
                center = userScreen
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = userScreen
            )

            // Heading indicator cone
            val rad = Math.toRadians((userBearing - 90).toDouble())
            val coneLength = 26.dp.toPx()
            val tipX = userScreen.x + cos(rad).toFloat() * coneLength
            val tipY = userScreen.y + sin(rad).toFloat() * coneLength
            drawLine(
                color = Color.Yellow,
                start = userScreen,
                end = Offset(tipX, tipY),
                strokeWidth = 2.5.dp.toPx()
            )
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
