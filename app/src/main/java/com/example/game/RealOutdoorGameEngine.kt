package com.example.game

import android.location.Location
import android.os.SystemClock
import com.example.audio.GameSoundEffects
import com.example.data.FirestoreGameRepository
import com.example.location.LiveLocationManager
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

class RealOutdoorGameEngine(
    private val locationManager: LiveLocationManager,
    private val soundEffects: GameSoundEffects
) {
    private val firestoreRepo = FirestoreGameRepository()

    private val _players = MutableStateFlow<List<RealGpsPlayer>>(emptyList())
    val players: StateFlow<List<RealGpsPlayer>> = _players.asStateFlow()

    private val _safeZone = MutableStateFlow(RealGpsSafeZone())
    val safeZone: StateFlow<RealGpsSafeZone> = _safeZone.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.PREPARATION)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(600)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _sonarActive = MutableStateFlow(false)
    val sonarActive: StateFlow<Boolean> = _sonarActive.asStateFlow()

    private val _sonarProgress = MutableStateFlow(0f)
    val sonarProgress: StateFlow<Float> = _sonarProgress.asStateFlow()

    private val _lastCapturedMugshot = MutableStateFlow<LiveBustedCard?>(null)
    val lastCapturedMugshot: StateFlow<LiveBustedCard?> = _lastCapturedMugshot.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _nearbyTarget = MutableStateFlow<RealGpsPlayer?>(null)
    val nearbyTarget: StateFlow<RealGpsPlayer?> = _nearbyTarget.asStateFlow()

    private val _nearbyDistance = MutableStateFlow(999f)
    val nearbyDistance: StateFlow<Float> = _nearbyDistance.asStateFlow()

    private val _radarCountdown = MutableStateFlow(90) // 1:30 minutes between pings
    val radarCountdown: StateFlow<Int> = _radarCountdown.asStateFlow()

    private val _radarRevealActive = MutableStateFlow(false)
    val radarRevealActive: StateFlow<Boolean> = _radarRevealActive.asStateFlow()

    private val _radarRevealSecondsLeft = MutableStateFlow(0)
    val radarRevealSecondsLeft: StateFlow<Int> = _radarRevealSecondsLeft.asStateFlow()

    private val _localHealthHp = MutableStateFlow(92)
    val localHealthHp: StateFlow<Int> = _localHealthHp.asStateFlow()

    private val _headStartSecondsLeft = MutableStateFlow(30)
    val headStartSecondsLeft: StateFlow<Int> = _headStartSecondsLeft.asStateFlow()

    private val _isHeadStartActive = MutableStateFlow(true)
    val isHeadStartActive: StateFlow<Boolean> = _isHeadStartActive.asStateFlow()

    private val _gameFeedMessages = MutableStateFlow<List<String>>(
        listOf("John is the seeker. Runners have 30 s head start - run!")
    )
    val gameFeedMessages: StateFlow<List<String>> = _gameFeedMessages.asStateFlow()

    fun addGameFeedMessage(msg: String) {
        _gameFeedMessages.value = _gameFeedMessages.value + msg
    }

    var roomSettings = LiveRoomSettings()
    private var engineScope: CoroutineScope? = null
    private var isRunning = false
    private var hasAnchoredToRealGps = false
    private var lastProximityAlertMillis: Long = 0L

    fun initializeRoom(
        settings: LiveRoomSettings,
        localNickname: String,
        localAvatarId: String,
        preferredRole: PlayerRole
    ) {
        roomSettings = settings
        val currentLoc = locationManager.currentLocation.value
        val hasLoc = currentLoc != null && currentLoc.latitude != 0.0 && currentLoc.longitude != 0.0
        hasAnchoredToRealGps = hasLoc
        val lat = if (hasLoc) currentLoc!!.latitude else settings.centerLatitude
        val lon = if (hasLoc) currentLoc!!.longitude else settings.centerLongitude

        settings.centerLatitude = lat
        settings.centerLongitude = lon

        val list = mutableListOf<RealGpsPlayer>()

        // Local Player only - Pure real human players, NO BOTS
        list.add(
            RealGpsPlayer(
                id = "p_local",
                nickname = localNickname.ifEmpty { "Hunter" },
                avatarId = localAvatarId,
                role = preferredRole,
                latitude = lat,
                longitude = lon,
                accuracyMeters = currentLoc?.accuracy ?: 5f,
                isHost = true,
                isReady = true,
                isLocalPlayer = true
            )
        )

        _players.value = list
        _safeZone.value = RealGpsSafeZone(
            centerLatitude = lat,
            centerLongitude = lon,
            initialRadiusMeters = settings.initialRadiusMeters,
            currentRadiusMeters = settings.initialRadiusMeters,
            targetRadiusMeters = settings.initialRadiusMeters,
            stage = 1,
            totalStages = 5,
            countdownSeconds = 45,
            isShrinking = false
        )
        _remainingSeconds.value = settings.durationMinutes * 60
        _gamePhase.value = GamePhase.PREPARATION
        _lastCapturedMugshot.value = null
    }

    fun initializeRoomFromLobby(
        settings: LiveRoomSettings,
        lobbyPlayers: List<PlayerState>
    ) {
        roomSettings = settings
        val currentLoc = locationManager.currentLocation.value
        val hasLoc = currentLoc != null && currentLoc.latitude != 0.0 && currentLoc.longitude != 0.0
        hasAnchoredToRealGps = hasLoc
        val lat = if (hasLoc) currentLoc!!.latitude else settings.centerLatitude
        val lon = if (hasLoc) currentLoc!!.longitude else settings.centerLongitude

        settings.centerLatitude = lat
        settings.centerLongitude = lon

        val list = mutableListOf<RealGpsPlayer>()
        val total = lobbyPlayers.size.coerceAtLeast(1)

        // Smart random role assignment for all lobby players
        val seekerCount = when {
            total <= 4 -> 1
            total <= 8 -> 2
            else -> 3
        }
        val randomSeekers = (0 until total).shuffled(Random(System.currentTimeMillis())).take(seekerCount).toSet()

        lobbyPlayers.forEachIndexed { index, p ->
            val smartRole = if (randomSeekers.contains(index)) PlayerRole.SEEKER else PlayerRole.RUNNER
            val angle = (index * 2.0 * Math.PI / total)
            val distMeters = if (p.isLocalPlayer) 0.0 else (60.0 + Random.nextDouble(20.0, 110.0))
            val dLat = (distMeters * Math.cos(angle)) / 111320.0
            val dLon = (distMeters * Math.sin(angle)) / (111320.0 * Math.cos(Math.toRadians(lat)))

            list.add(
                RealGpsPlayer(
                    id = p.id,
                    nickname = p.nickname,
                    avatarId = p.avatarId,
                    role = smartRole,
                    latitude = lat + dLat,
                    longitude = lon + dLon,
                    accuracyMeters = if (p.isLocalPlayer) (currentLoc?.accuracy ?: 5f) else 8f,
                    isHost = p.isHost,
                    isReady = p.isReady,
                    isLocalPlayer = p.isLocalPlayer
                )
            )
        }

        _players.value = list
        _safeZone.value = RealGpsSafeZone(
            centerLatitude = lat,
            centerLongitude = lon,
            initialRadiusMeters = settings.initialRadiusMeters,
            currentRadiusMeters = settings.initialRadiusMeters,
            targetRadiusMeters = settings.initialRadiusMeters,
            stage = 1,
            totalStages = 5,
            countdownSeconds = 45,
            isShrinking = false
        )
        _remainingSeconds.value = settings.durationMinutes * 60
        _gamePhase.value = GamePhase.PREPARATION
        _lastCapturedMugshot.value = null
    }

    /**
     * Initializes room with 100% real human players synced via Firestore cloud.
     * Preserves real GPS satellite coordinates from each connected phone.
     */
    fun initializeRoomWithRealSquad(
        settings: LiveRoomSettings,
        squad: List<RealGpsPlayer>
    ) {
        roomSettings = settings
        val currentLoc = locationManager.currentLocation.value
        val hasLoc = currentLoc != null && currentLoc.latitude != 0.0 && currentLoc.longitude != 0.0
        hasAnchoredToRealGps = hasLoc
        val lat = if (hasLoc) currentLoc!!.latitude else settings.centerLatitude
        val lon = if (hasLoc) currentLoc!!.longitude else settings.centerLongitude

        settings.centerLatitude = lat
        settings.centerLongitude = lon

        val updatedSquad = squad.map { p ->
            if (p.isLocalPlayer && hasLoc) {
                p.copy(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = currentLoc?.accuracy ?: 5f
                )
            } else {
                p
            }
        }

        _players.value = updatedSquad
        _safeZone.value = RealGpsSafeZone(
            centerLatitude = lat,
            centerLongitude = lon,
            initialRadiusMeters = settings.initialRadiusMeters,
            currentRadiusMeters = settings.initialRadiusMeters,
            targetRadiusMeters = settings.initialRadiusMeters,
            stage = 1,
            totalStages = 5,
            countdownSeconds = 45,
            isShrinking = false
        )
        _remainingSeconds.value = settings.durationMinutes * 60
        _gamePhase.value = GamePhase.PREPARATION
        _lastCapturedMugshot.value = null
    }

    fun startGame() {
        engineScope?.cancel()
        engineScope = CoroutineScope(Dispatchers.Default + Job())
        isRunning = true

        engineScope?.launch {
            _gamePhase.value = GamePhase.PREPARATION
            postAlert("3... 2... 1... HUNT BEGINS!")
            soundEffects.playCountdownBeep(false)
            delay(1000)
            soundEffects.playCountdownBeep(false)
            delay(1000)
            soundEffects.playCountdownBeep(true)
            delay(1000)
            _gamePhase.value = GamePhase.SAFE_CALM

            val local = _players.value.find { it.isLocalPlayer }
            val roleName = if (local?.role == PlayerRole.SEEKER) "SEEKER (HUNTER)" else "HIDER (RUNNER)"
            postAlert("YOUR ROLE: $roleName! Stay inside the Safe Zone!")

            // Listen to remote real human squad members from cloud
            firestoreRepo.listenToRoomPlayers(
                roomCode = roomSettings.roomCode,
                currentUserId = local?.id ?: "p_local"
            ) { remotePlayers ->
                val current = _players.value.toMutableList()
                remotePlayers.forEach { remote ->
                    if (!remote.isLocalPlayer) {
                        val idx = current.indexOfFirst { it.id == remote.id }
                        if (idx != -1) {
                            current[idx] = remote.copy(isLocalPlayer = false)
                        } else {
                            current.add(remote.copy(isLocalPlayer = false))
                        }
                    }
                }
                _players.value = current
            }

            launchLoops()
        }
    }

    private fun CoroutineScope.launchLoops() {
        // GPS & Proximity loop (every 500ms)
        launch {
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                updateLocations()
                delay(500)
            }
        }

        // Match Timer & Safe Zone Ticker (every 1s)
        launch {
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                delay(1000)
                if (_remainingSeconds.value > 0) {
                    _remainingSeconds.value -= 1
                } else {
                    onTimeExpired()
                    break
                }

                if (_headStartSecondsLeft.value > 0) {
                    _headStartSecondsLeft.value -= 1
                    if (_headStartSecondsLeft.value == 0) {
                        _isHeadStartActive.value = false
                        postAlert("HEAD START OVER! Hunt begins!")
                    }
                }

                updateSafeZoneClock()
            }
        }

        // Sonar wave animation loop
        launch {
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                if (_sonarActive.value) {
                    var progress = 0f
                    while (progress < 1f && _sonarActive.value) {
                        progress += 0.04f
                        _sonarProgress.value = progress.coerceAtMost(1f)
                        delay(25)
                    }
                    _sonarActive.value = false
                    _sonarProgress.value = 0f
                }
                delay(100)
            }
        }
    }

    fun anchorGameToLocation(lat: Double, lon: Double) {
        val currentList = _players.value.map { it.copy() }.toMutableList()
        val local = currentList.find { it.isLocalPlayer }
        if (local != null) {
            local.latitude = lat
            local.longitude = lon
        }

        // Scatter other players realistically around the user's real location (50m - 140m)
        val otherPlayers = currentList.filter { !it.isLocalPlayer }
        val count = otherPlayers.size
        otherPlayers.forEachIndexed { index, runner ->
            val angle = (index * 2.0 * Math.PI / count.coerceAtLeast(1))
            val distMeters = 50.0 + Random.nextDouble(20.0, 90.0)
            val dLat = (distMeters * Math.cos(angle)) / 111320.0
            val dLon = (distMeters * Math.sin(angle)) / (111320.0 * Math.cos(Math.toRadians(lat)))
            runner.latitude = lat + dLat
            runner.longitude = lon + dLon
        }

        _players.value = currentList

        // Re-anchor safe zone center to player's real coordinates
        val zone = _safeZone.value
        _safeZone.value = zone.copy(
            centerLatitude = lat,
            centerLongitude = lon
        )
        postAlert("📍 GPS LOCKED: Arena centered on your real location!")
    }

    private fun updateLocations() {
        val currentLoc = locationManager.currentLocation.value ?: return
        if (currentLoc.latitude == 0.0 && currentLoc.longitude == 0.0) return

        val zoneCenterDist = LiveLocationManager.distanceMeters(
            currentLoc.latitude, currentLoc.longitude,
            _safeZone.value.centerLatitude, _safeZone.value.centerLongitude
        )

        // Anchor the safe circle zone ONCE on initial GPS fix.
        // Afterward, the safe circle zone stays permanently locked to its fixed geographical position.
        // The player moves in real life inside/outside this fixed zone.
        if (!hasAnchoredToRealGps) {
            hasAnchoredToRealGps = true
            anchorGameToLocation(currentLoc.latitude, currentLoc.longitude)
            return
        }

        val currentList = _players.value.map { it.copy() }.toMutableList()
        val local = currentList.find { it.isLocalPlayer } ?: return

        local.latitude = currentLoc.latitude
        local.longitude = currentLoc.longitude
        local.accuracyMeters = currentLoc.accuracy

        // Stream local GPS to Firestore cloud so other phones see real location
        firestoreRepo.syncPlayerLocation(
            roomCode = roomSettings.roomCode,
            playerId = local.id,
            latitude = local.latitude,
            longitude = local.longitude,
            accuracy = local.accuracyMeters,
            role = local.role.name,
            isTagged = local.isTagged
        )

        // Proximity detection for accurate same location (Haptics + Siren alert)
        if (local.role == PlayerRole.SEEKER) {
            val runners = currentList.filter { it.role == PlayerRole.RUNNER && !it.isTagged }
            var closest: RealGpsPlayer? = null
            var minDist = 9999f

            runners.forEach { runner ->
                val dist = LiveLocationManager.distanceMeters(
                    local.latitude, local.longitude,
                    runner.latitude, runner.longitude
                )
                if (dist < minDist) {
                    minDist = dist
                    closest = runner
                }
            }

            _nearbyTarget.value = if (minDist <= 20f) closest else null
            _nearbyDistance.value = minDist

            // When Seeker finds Hider at accurate same location (<= 8m)
            if (minDist <= 8f && closest != null) {
                val now = System.currentTimeMillis()
                if (now - lastProximityAlertMillis > 3500L) {
                    lastProximityAlertMillis = now
                    soundEffects.playTargetLockedHaptic()
                    postAlert("🚨 TARGET FOUND! ${closest.nickname} is at your exact location (${minDist.toInt()}m)!")
                }
            }
        } else {
            // Local player is RUNNER (Hider) - detect if Seeker is at accurate same location
            val seekers = currentList.filter { it.role == PlayerRole.SEEKER }
            var minSeekerDist = 9999f
            var nearestSeeker: RealGpsPlayer? = null

            seekers.forEach { seeker ->
                val dist = LiveLocationManager.distanceMeters(
                    local.latitude, local.longitude,
                    seeker.latitude, seeker.longitude
                )
                if (dist < minSeekerDist) {
                    minSeekerDist = dist
                    nearestSeeker = seeker
                }
            }

            if (minSeekerDist <= 8f && nearestSeeker != null) {
                val now = System.currentTimeMillis()
                if (now - lastProximityAlertMillis > 3500L) {
                    lastProximityAlertMillis = now
                    soundEffects.playTargetLockedHaptic()
                    postAlert("⚠️ YOU ARE FOUND! Hunter ${nearestSeeker.nickname} is at your exact spot (${minSeekerDist.toInt()}m)!")
                }
            }
        }

        // Safe zone check for local player
        val zone = _safeZone.value
        val distToCenter = LiveLocationManager.distanceMeters(
            local.latitude, local.longitude,
            zone.centerLatitude, zone.centerLongitude
        )
        if (distToCenter > zone.currentRadiusMeters) {
            if (local.role == PlayerRole.RUNNER) {
                _localHealthHp.value = (_localHealthHp.value - 1).coerceAtLeast(0)
                if (_localHealthHp.value <= 0) {
                    local.role = PlayerRole.SEEKER
                    postAlert("0 HP! You are out of health and became a Seeker!")
                    soundEffects.playTargetLockedHaptic()
                }
            }
            if (Random.nextFloat() < 0.12f) {
                val outDist = (distToCenter - zone.currentRadiusMeters).toInt()
                val formattedDist = if (outDist >= 1000) "%.1fkm".format(outDist / 1000f) else "${outDist}m"
                postAlert("DANGER: OUT OF SAFE ZONE by $formattedDist! Losing Health!")
                soundEffects.playZoneWarning()
            }
        } else {
            if (local.role == PlayerRole.RUNNER && _localHealthHp.value < 100) {
                _localHealthHp.value = (_localHealthHp.value + 1).coerceAtMost(100)
            }
        }

        // Live backend sync with Firestore
        firestoreRepo.syncPlayerLocation(
            roomCode = roomSettings.roomCode,
            playerId = local.id,
            latitude = local.latitude,
            longitude = local.longitude,
            accuracy = local.accuracyMeters,
            role = local.role.name,
            isTagged = local.isTagged
        )

        _players.value = currentList
    }

    private fun updateSafeZoneClock() {
        val zone = _safeZone.value
        if (zone.countdownSeconds > 0) {
            zone.countdownSeconds -= 1
            if (zone.countdownSeconds == 10) {
                _gamePhase.value = GamePhase.ZONE_WARNING
                postAlert("WARNING: Safe Zone shrinks in 10s!")
                soundEffects.playZoneWarning()
            }
        } else {
            if (!zone.isShrinking && zone.stage < zone.totalStages) {
                zone.isShrinking = true
                _gamePhase.value = GamePhase.ZONE_SHRINKING
                val newRadius = when (zone.stage) {
                    1 -> zone.initialRadiusMeters * 0.75f
                    2 -> zone.initialRadiusMeters * 0.50f
                    3 -> zone.initialRadiusMeters * 0.30f
                    4 -> zone.initialRadiusMeters * 0.15f
                    else -> 25f
                }
                zone.targetRadiusMeters = newRadius
                postAlert("Safe zone is shrinking! Head toward the center!")
                soundEffects.playZoneWarning()
            } else if (zone.isShrinking) {
                // Shrink complete
                zone.currentRadiusMeters = zone.targetRadiusMeters
                zone.isShrinking = false
                zone.stage += 1
                zone.countdownSeconds = when (roomSettings.shrinkSpeed) {
                    "Fast" -> 20
                    "Blitz" -> 12
                    else -> 35
                }
                _gamePhase.value = if (zone.stage >= zone.totalStages) GamePhase.SUDDEN_DEATH else GamePhase.SAFE_CALM
            }
        }

        if (zone.isShrinking && zone.currentRadiusMeters > zone.targetRadiusMeters) {
            val step = when (roomSettings.shrinkSpeed) {
                "Fast" -> 4f
                "Blitz" -> 7f
                else -> 2f
            }
            zone.currentRadiusMeters = (zone.currentRadiusMeters - step).coerceAtLeast(zone.targetRadiusMeters)
        }

        _safeZone.value = zone.copy()
    }

    fun triggerSonarPing() {
        triggerGpsBlink()
    }

    private fun triggerGpsBlink() {
        _sonarActive.value = true
        soundEffects.playSonarPing()
        postAlert("GPS BLINK: Runner coordinates detected!")
    }

    fun onMugshotPhotoTaken(photoFile: File) {
        val target = _nearbyTarget.value ?: return
        val currentList = _players.value.toMutableList()
        val runnerIndex = currentList.indexOfFirst { it.id == target.id }

        if (runnerIndex != -1) {
            val capturedRunner = currentList[runnerIndex]
            soundEffects.playTagCapture()

            // Last Seen Mugshot Snowball Rule: Captured runner joins the hunters!
            currentList[runnerIndex] = capturedRunner.copy(
                isTagged = true,
                role = if (roomSettings.isSnowballMode) PlayerRole.SEEKER else PlayerRole.RUNNER,
                taggedAtSeconds = roomSettings.durationMinutes * 60 - _remainingSeconds.value,
                capturedPhotoPath = photoFile.absolutePath
            )

            val card = LiveBustedCard(
                id = "mugshot_${System.currentTimeMillis()}",
                runnerName = capturedRunner.nickname,
                seekerName = currentList.find { it.isLocalPlayer }?.nickname ?: "Hunter",
                roomCode = roomSettings.roomCode,
                captureTimeFormatted = formatTimer(currentList[runnerIndex].taggedAtSeconds),
                captureDistanceMeters = _nearbyDistance.value,
                capturedPhotoPath = photoFile.absolutePath,
                badgeTitle = "CAUGHT IN 4K!"
            )

            _lastCapturedMugshot.value = card
            _nearbyTarget.value = null
            _players.value = currentList
            postAlert("${capturedRunner.nickname} BUSTED! Now joined the hunters!")

            // Publish official BUSTED card to Firestore
            firestoreRepo.publishBustedCard(roomSettings.roomCode, card)

            checkWinCondition()
        }
    }

    fun triggerCatch(targetPlayer: RealGpsPlayer? = null) {
        val currentList = _players.value.toMutableList()
        val candidate = targetPlayer ?: _nearbyTarget.value ?: currentList.filter { it.role == PlayerRole.RUNNER && !it.isTagged }
            .minByOrNull {
                val local = currentList.find { p -> p.isLocalPlayer } ?: return@minByOrNull 9999f
                LiveLocationManager.distanceMeters(local.latitude, local.longitude, it.latitude, it.longitude)
            }

        if (candidate != null) {
            val runnerIndex = currentList.indexOfFirst { it.id == candidate.id }
            if (runnerIndex != -1) {
                val capturedRunner = currentList[runnerIndex]
                val local = currentList.find { it.isLocalPlayer }
                val dist = if (local != null) {
                    LiveLocationManager.distanceMeters(local.latitude, local.longitude, capturedRunner.latitude, capturedRunner.longitude)
                } else 10f

                if (dist <= 30f) {
                    soundEffects.playTagCapture()
                    currentList[runnerIndex] = capturedRunner.copy(
                        isTagged = true,
                        role = PlayerRole.SEEKER,
                        taggedAtSeconds = roomSettings.durationMinutes * 60 - _remainingSeconds.value
                    )
                    _players.value = currentList
                    _nearbyTarget.value = null
                    postAlert("🎯 CAUGHT! ${capturedRunner.nickname} was tagged (${dist.toInt()}m) and joined the seekers!")
                    soundEffects.playTargetLockedHaptic()
                    checkWinCondition()
                } else {
                    postAlert("Too far to catch! Runner is ${dist.toInt()}m away (get within 30m)")
                }
            }
        } else {
            postAlert("No runners within catch range.")
        }
    }

    private fun checkWinCondition() {
        val activeRunners = _players.value.count { it.role == PlayerRole.RUNNER && !it.isTagged }
        if (activeRunners == 0) {
            _gamePhase.value = GamePhase.GAME_OVER
            soundEffects.playVictory()
            postAlert("SEEKER VICTORY! All runners captured!")
        }
    }

    private fun onTimeExpired() {
        _gamePhase.value = GamePhase.GAME_OVER
        val survivingRunners = _players.value.count { it.role == PlayerRole.RUNNER && !it.isTagged }
        if (survivingRunners > 0) {
            soundEffects.playVictory()
            postAlert("RUNNER VICTORY! Surviving runners outlasted the hunters!")
        } else {
            postAlert("MATCH CONCLUDED!")
        }
    }

    fun dismissMugshot() {
        _lastCapturedMugshot.value = null
    }

    fun stopGame() {
        isRunning = false
        engineScope?.cancel()
        engineScope = null
        firestoreRepo.cleanup()
    }

    private fun postAlert(msg: String) {
        _alertMessage.value = msg
    }

    private fun formatTimer(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%02d:%02d".format(m, s)
    }
}
