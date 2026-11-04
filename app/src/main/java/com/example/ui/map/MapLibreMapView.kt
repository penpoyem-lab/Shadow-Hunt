package com.example.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PlayerRole
import com.example.model.RealGpsPlayer
import com.example.model.RealGpsSafeZone
import org.json.JSONArray
import org.json.JSONObject

/**
 * JavaScript interface for MapLibre map bridge
 */
class MapLibreBridge(
    private val onReady: () -> Unit,
    private val onMarkerClick: (String) -> Unit
) {
    @JavascriptInterface
    fun onMapReady() {
        onReady()
    }

    @JavascriptInterface
    fun onMarkerClicked(id: String) {
        onMarkerClick(id)
    }
}

/**
 * AndroidView container to display OpenStreetMap using the MapLibre library,
 * applying the custom dark tactical tileset JSON to match the 'Shadow Hunt' aesthetic.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    userLatitude: Double,
    userLongitude: Double,
    userHeading: Float = 0f,
    userRole: PlayerRole = PlayerRole.SEEKER,
    safeZone: RealGpsSafeZone,
    runners: List<RealGpsPlayer> = emptyList(),
    isRadarPulseActive: Boolean = false,
    onRunnerSelected: (RealGpsPlayer) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapInitialized by remember { mutableStateOf(false) }

    val bridge = remember {
        MapLibreBridge(
            onReady = {
                isMapInitialized = true
            },
            onMarkerClick = { id ->
                val target = runners.find { it.id == id }
                if (target != null) {
                    onRunnerSelected(target)
                }
            }
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                // Enable hardware acceleration for MapLibre WebGL vector tile rendering
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(Color.parseColor("#070A13"))

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    databaseEnabled = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }

                addJavascriptInterface(bridge, "AndroidBridge")
                loadUrl("file:///android_asset/maplibre_map.html")
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )

    // Synchronize Player Role and Location
    LaunchedEffect(userLatitude, userLongitude, userHeading, isMapInitialized) {
        if (isMapInitialized && webViewRef != null && userLatitude != 0.0 && userLongitude != 0.0) {
            val roleStr = if (userRole == PlayerRole.SEEKER) "SEEKER" else "RUNNER"
            val js = """
                javascript:(function() {
                    if (window.setPlayerRole) window.setPlayerRole('$roleStr');
                    if (window.updatePlayerLocation) window.updatePlayerLocation($userLatitude, $userLongitude, $userHeading);
                })()
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    // Synchronize Safe Zone
    LaunchedEffect(safeZone.centerLatitude, safeZone.centerLongitude, safeZone.currentRadiusMeters, safeZone.stage, isMapInitialized) {
        if (isMapInitialized && webViewRef != null) {
            val stageName = "Stage ${safeZone.stage}/${safeZone.totalStages}"
            val js = """
                javascript:(function() {
                    if (window.updateSafeZone) {
                        window.updateSafeZone(
                            ${safeZone.centerLatitude},
                            ${safeZone.centerLongitude},
                            ${safeZone.currentRadiusMeters},
                            '$stageName'
                        );
                    }
                })()
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    // Synchronize Runners as GeoJSON
    LaunchedEffect(runners, isMapInitialized) {
        if (isMapInitialized && webViewRef != null) {
            val jsonArray = JSONArray()
            for (runner in runners) {
                val obj = JSONObject().apply {
                    put("id", runner.id)
                    put("name", runner.nickname)
                    put("lat", runner.latitude)
                    put("lng", runner.longitude)
                    put("role", runner.role.name)
                    put("isCaught", runner.isTagged)
                }
                jsonArray.put(obj)
            }

            val js = "javascript:if(window.updateRunners) window.updateRunners($jsonArray);"
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    // Trigger pulse on sonar activation
    LaunchedEffect(isRadarPulseActive) {
        if (isRadarPulseActive && isMapInitialized && webViewRef != null) {
            webViewRef?.evaluateJavascript("javascript:if(window.triggerRadarPulse) window.triggerRadarPulse();", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
