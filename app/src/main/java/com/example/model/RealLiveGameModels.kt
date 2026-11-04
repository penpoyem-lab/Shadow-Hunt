package com.example.model

data class RealGpsPlayer(
    val id: String,
    val nickname: String,
    val avatarId: String,
    var role: PlayerRole,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var accuracyMeters: Float = 0f,
    var isHost: Boolean = false,
    var isReady: Boolean = true,
    var isTagged: Boolean = false,
    var taggedAtSeconds: Int = 0,
    var isLocalPlayer: Boolean = false,
    var capturedPhotoPath: String? = null,
    var lastBlinkVisible: Boolean = false
)

data class RealGpsSafeZone(
    var centerLatitude: Double = 37.7749,
    var centerLongitude: Double = -122.4194,
    var initialRadiusMeters: Float = 400f,
    var currentRadiusMeters: Float = 400f,
    var targetRadiusMeters: Float = 400f,
    var stage: Int = 1,
    var totalStages: Int = 5,
    var countdownSeconds: Int = 45,
    var isShrinking: Boolean = false
)

data class LiveBustedCard(
    val id: String,
    val runnerName: String,
    val seekerName: String,
    val roomCode: String,
    val captureTimeFormatted: String,
    val captureDistanceMeters: Float,
    val capturedPhotoPath: String?,
    val badgeTitle: String = "CAUGHT OUTDOORS",
    val locationDescription: String = "Live Safe Zone"
)

data class LiveRoomSettings(
    val roomCode: String = "SH7KQ2",
    val roomName: String = "Outdoor Park Hunt",
    val hostId: String = "host",
    var centerLatitude: Double = 37.7749,
    var centerLongitude: Double = -122.4194,
    val initialRadiusMeters: Float = 400f,
    val durationMinutes: Int = 10,
    val shrinkSpeed: String = "Normal",
    val hintBlinkSeconds: Int = 30,
    val isSnowballMode: Boolean = true
)
