package com.example.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
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

import android.webkit.RenderProcessGoneDetail

/**
 * JavaScript interface for two-way communication between Leaflet map and Compose.
 */
class MapJavaScriptBridge(
    private val onReady: () -> Unit,
    private val onRunnerClick: (String) -> Unit
) {
    @JavascriptInterface
    fun onMapReady() {
        onReady()
    }

    @JavascriptInterface
    fun onMarkerClicked(playerId: String) {
        onRunnerClick(playerId)
    }
}

/**
 * Modern Compose Leaflet Map component rendering CartoDB Dark Matter tiles
 * within an Android WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    modifier: Modifier = Modifier,
    userLatitude: Double,
    userLongitude: Double,
    userHeading: Float = 0f,
    userRole: PlayerRole = PlayerRole.SEEKER,
    safeZone: RealGpsSafeZone,
    runners: List<RealGpsPlayer> = emptyList(),
    isRadarPulseActive: Boolean = false,
    recenterTrigger: Int = 0,
    onRunnerSelected: (RealGpsPlayer) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapInitialized by remember { mutableStateOf(false) }

    val jsBridge = remember {
        MapJavaScriptBridge(
            onReady = {
                isMapInitialized = true
            },
            onRunnerClick = { runnerId ->
                val clickedRunner = runners.find { it.id == runnerId }
                if (clickedRunner != null) {
                    onRunnerSelected(clickedRunner)
                }
            }
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.parseColor("#0A0E17"))
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        // Crucial fix: prevents chromium crash from killing the host activity
                        return true
                    }
                }
                addJavascriptInterface(jsBridge, "AndroidBridge")
                loadUrl("file:///android_asset/leaflet_map.html")
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )

    // Update Player Role & Location when location updates
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

    // Update Safe Zone circle
    LaunchedEffect(safeZone.centerLatitude, safeZone.centerLongitude, safeZone.currentRadiusMeters, safeZone.stage, isMapInitialized) {
        if (isMapInitialized && webViewRef != null) {
            val phaseName = "Stage ${safeZone.stage}/${safeZone.totalStages}"
            val js = """
                javascript:(function() {
                    if (window.updateSafeZone) {
                        window.updateSafeZone(
                            ${safeZone.centerLatitude},
                            ${safeZone.centerLongitude},
                            ${safeZone.currentRadiusMeters},
                            '$phaseName'
                        );
                    }
                })()
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    // Update Runners
    LaunchedEffect(runners, isMapInitialized) {
        if (isMapInitialized && webViewRef != null) {
            val aliveCount = runners.count { !it.isTagged }
            val totalCount = runners.size

            val sb = StringBuilder()
            sb.append("javascript:(function() {\n")
            sb.append("if (window.updateHud) window.updateHud($aliveCount, $totalCount);\n")
            for (runner in runners) {
                val isVisible = runner.lastBlinkVisible || runner.isTagged
                val isCaught = runner.isTagged
                val safeName = runner.nickname.replace("'", "\\'")
                sb.append("if (window.updateRunner) window.updateRunner('${runner.id}', '$safeName', ${runner.latitude}, ${runner.longitude}, $isVisible, $isCaught);\n")
            }
            sb.append("})()")
            webViewRef?.evaluateJavascript(sb.toString(), null)
        }
    }

    // Trigger radar pulse wave
    LaunchedEffect(isRadarPulseActive) {
        if (isRadarPulseActive && isMapInitialized && webViewRef != null) {
            webViewRef?.evaluateJavascript("javascript:if(window.triggerRadarPulse) window.triggerRadarPulse();", null)
        }
    }

    // Trigger map camera re-centering on player GPS location
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && isMapInitialized && webViewRef != null) {
            webViewRef?.evaluateJavascript("javascript:if(window.centerOnPlayer) window.centerOnPlayer();", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
