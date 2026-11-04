package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class LiveLocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var locationCallback: LocationCallback? = null

    fun updatePermission(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        stopLocationUpdates()

        // Fetch last known location immediately from both Fused and System LocationManager
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _currentLocation.value == null) {
                    _currentLocation.value = loc
                }
            }
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        _currentLocation.value = loc
                    }
                }

            val sysLocManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (sysLocManager != null) {
                val gpsLoc = sysLocManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                val netLoc = sysLocManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                val best = gpsLoc ?: netLoc
                if (best != null && _currentLocation.value == null) {
                    _currentLocation.value = best
                }
            }
        } catch (_: Exception) {}

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateIntervalMillis(800L)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (_: Exception) {}
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    fun getLastKnownLocation(): Location? {
        return _currentLocation.value
    }

    companion object {
        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0f
            if (lat1.isInfinite() || lon1.isInfinite() || lat2.isInfinite() || lon2.isInfinite()) return 0f
            if (lat1 < -90.0 || lat1 > 90.0 || lat2 < -90.0 || lat2 > 90.0) return 0f
            if (lon1 < -180.0 || lon1 > 180.0 || lon2 < -180.0 || lon2 > 180.0) return 0f
            // If either point is uninitialized (0.0, 0.0), distance is 0 to prevent runaway 173990m errors
            if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return 0f

            val results = FloatArray(1)
            try {
                Location.distanceBetween(lat1, lon1, lat2, lon2, results)
                return results[0]
            } catch (_: Exception) {
                return 0f
            }
        }

        fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val dLon = Math.toRadians(lon2 - lon1)
            val y = sin(dLon) * cos(Math.toRadians(lat2))
            val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                    sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
            val brng = Math.toDegrees(atan2(y, x))
            return ((brng + 360) % 360).toFloat()
        }
    }
}
