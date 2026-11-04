package com.example.game

import android.os.SystemClock
import com.example.audio.GameSoundEffects
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*
import kotlin.random.Random

class ShadowHuntEngine(
    private val soundEffects: GameSoundEffects
) {
    private val _players = MutableStateFlow<List<PlayerState>>(emptyList())
    val players: StateFlow<List<PlayerState>> = _players.asStateFlow()

    private val _safeZone = MutableStateFlow(SafeZoneState())
    val safeZone: StateFlow<SafeZoneState> = _safeZone.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.PREPARATION)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(600)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _sonarPingActive = MutableStateFlow(false)
    val sonarPingActive: StateFlow<Boolean> = _sonarPingActive.asStateFlow()

    private val _sonarPingProgress = MutableStateFlow(0f)
    val sonarPingProgress: StateFlow<Float> = _sonarPingProgress.asStateFlow()

    private val _lastTaggedCard = MutableStateFlow<BustedCard?>(null)
    val lastTaggedCard: StateFlow<BustedCard?> = _lastTaggedCard.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _bustedCardsHistory = MutableStateFlow<List<BustedCard>>(emptyList())
    val bustedCardsHistory: StateFlow<List<BustedCard>> = _bustedCardsHistory.asStateFlow()

    var currentRoomConfig = RoomConfig()
    var isMiniMapExpanded = false

    private var engineScope: CoroutineScope? = null
    private var isRunning = false

    // Arena Obstacles (0..1000 coordinate space)
    val obstacles = listOf(
        Obstacle(180f, 180f, 120f, 120f, "Tower A"),
        Obstacle(700f, 180f, 130f, 110f, "Server Hub"),
        Obstacle(200f, 680f, 140f, 130f, "Warehouse"),
        Obstacle(690f, 670f, 130f, 140f, "Substation"),
        Obstacle(440f, 440f, 120f, 120f, "Central Core"),
        Obstacle(430f, 150f, 140f, 60f, "Barricade North"),
        Obstacle(430f, 790f, 140f, 60f, "Barricade South")
    )

    // Joystick input vector (-1..1)
    private var inputX = 0f
    private var inputY = 0f

    fun setInputVector(x: Float, y: Float) {
        inputX = x.coerceIn(-1f, 1f)
        inputY = y.coerceIn(-1f, 1f)
    }

    fun startSprint(enable: Boolean) {
        val list = _players.value.toMutableList()
        val local = list.find { it.isLocalPlayer } ?: return
        local.isSprinting = enable
        _players.value = list
    }

    fun triggerStealth() {
        val list = _players.value.toMutableList()
        val local = list.find { it.isLocalPlayer } ?: return
        if (local.stamina >= 30f) {
            local.stamina -= 30f
            local.stealthUntilMillis = SystemClock.uptimeMillis() + 5000L
            _players.value = list
            soundEffects.playClick()
            postAlert("Stealth activated (5s hidden)")
        }
    }

    fun tagNearbyRunners(): Boolean {
        val currentPlayers = _players.value.toMutableList()
        val local = currentPlayers.find { it.isLocalPlayer } ?: return false
        if (local.role != PlayerRole.SEEKER || local.isTagged) return false

        var taggedAny = false
        currentPlayers.forEachIndexed { index, runner ->
            if (runner.role == PlayerRole.RUNNER && !runner.isTagged) {
                val dist = hypot(runner.x - local.x, runner.y - local.y)
                if (dist <= 75f) {
                    currentPlayers[index] = runner.copy(
                        isTagged = true,
                        tagTimeSeconds = currentRoomConfig.durationMinutes * 60 - _remainingSeconds.value
                    )
                    taggedAny = true
                    onPlayerCaptured(runner)
                }
            }
        }

        if (taggedAny) {
            _players.value = currentPlayers
            checkWinCondition()
        }
        return taggedAny
    }

    fun initializeRoom(config: RoomConfig, localPlayerNickname: String, localAvatarId: String, preferredRole: PlayerRole? = null) {
        currentRoomConfig = config
        val initialPlayers = mutableListOf<PlayerState>()

        val totalToCreate = config.maxPlayers.coerceIn(2, 10)
        // Smart random role assignment: 1 seeker for <= 4 players, 2 for 5-8 players, 3 for 9-10
        val seekerCount = when {
            totalToCreate <= 4 -> 1
            totalToCreate <= 8 -> 2
            else -> 3
        }
        val randomSeekerIndices = (0 until totalToCreate).shuffled(Random(System.currentTimeMillis())).take(seekerCount).toSet()

        // Local Player
        val assignedRole = preferredRole ?: if (randomSeekerIndices.contains(0)) PlayerRole.SEEKER else PlayerRole.RUNNER
        initialPlayers.add(
            PlayerState(
                id = "p_local",
                nickname = localPlayerNickname.ifEmpty { "Kishalay_007" },
                avatarId = localAvatarId,
                role = assignedRole,
                isHost = true,
                isReady = true,
                isBot = false,
                x = 480f + Random.nextInt(-40, 40),
                y = 480f + Random.nextInt(-40, 40),
                isLocalPlayer = true
            )
        )

        // Bot participants
        val botNames = listOf("RexHunter", "ShadowKing", "Devil", "Valkyrie", "Ghost", "Cipher", "Phoenix", "Specter", "Zero")
        val botAvatars = listOf("hunter", "cyber", "phantom", "reaper", "demon")

        for (i in 1 until totalToCreate) {
            val role = if (randomSeekerIndices.contains(i)) PlayerRole.SEEKER else PlayerRole.RUNNER

            val spawnAngle = (i * 2 * PI / (totalToCreate - 1)).toFloat()
            val spawnDist = Random.nextFloat() * 200f + 120f
            val bx = (500f + cos(spawnAngle) * spawnDist).coerceIn(100f, 900f)
            val by = (500f + sin(spawnAngle) * spawnDist).coerceIn(100f, 900f)

            initialPlayers.add(
                PlayerState(
                    id = "bot_$i",
                    nickname = botNames[(i - 1) % botNames.size],
                    avatarId = botAvatars[(i - 1) % botAvatars.size],
                    role = role,
                    isHost = false,
                    isReady = true,
                    isBot = true,
                    x = bx,
                    y = by
                )
            )
        }

        _players.value = initialPlayers
        _safeZone.value = SafeZoneState(
            centerX = 500f,
            centerY = 500f,
            currentRadius = 450f,
            targetRadius = 450f,
            stage = 1,
            totalStages = 5,
            countdownSeconds = 45,
            isShrinking = false
        )
        _remainingSeconds.value = config.durationMinutes * 60
        _gamePhase.value = GamePhase.PREPARATION
        _lastTaggedCard.value = null
    }

    fun startGame() {
        engineScope?.cancel()
        engineScope = CoroutineScope(Dispatchers.Default + Job())
        isRunning = true

        engineScope?.launch {
            // Preparation Countdown 3... 2... 1... HUNT BEGINS!
            _gamePhase.value = GamePhase.PREPARATION
            postAlert("3... 2... 1... HUNT BEGINS!")
            soundEffects.playCountdownBeep(false)
            delay(1000)
            soundEffects.playCountdownBeep(false)
            delay(1000)
            soundEffects.playCountdownBeep(true)
            delay(1000)
            _gamePhase.value = GamePhase.SAFE_CALM
            postAlert("FIND THEM BEFORE THE ZONE DISAPPEARS!")

            launchGameLoops()
        }
    }

    private fun CoroutineScope.launchGameLoops() {
        // 60 FPS physics tick
        launch {
            var lastTime = SystemClock.uptimeMillis()
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                val now = SystemClock.uptimeMillis()
                val dt = ((now - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                updatePhysics(dt)
                delay(16) // ~60fps
            }
        }

        // 1-second game clock & safe-zone ticker
        launch {
            var pingTimer = 0
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                delay(1000)
                if (_remainingSeconds.value > 0) {
                    _remainingSeconds.value -= 1
                } else {
                    onTimeExpired()
                    break
                }

                pingTimer++
                if (pingTimer >= currentRoomConfig.hintFrequencySeconds) {
                    pingTimer = 0
                    triggerSonarPing()
                }

                updateSafeZoneClock()
            }
        }

        // Sonar wave animation loop
        launch {
            while (isRunning && _gamePhase.value != GamePhase.GAME_OVER) {
                if (_sonarPingActive.value) {
                    var progress = 0f
                    while (progress < 1f && _sonarPingActive.value) {
                        progress += 0.035f
                        _sonarPingProgress.value = progress.coerceAtMost(1f)
                        delay(25)
                    }
                    _sonarPingActive.value = false
                    _sonarPingProgress.value = 0f
                }
                delay(100)
            }
        }
    }

    private fun updateSafeZoneClock() {
        val currentZone = _safeZone.value
        if (currentZone.countdownSeconds > 0) {
            currentZone.countdownSeconds -= 1
            if (currentZone.countdownSeconds == 10) {
                _gamePhase.value = GamePhase.ZONE_WARNING
                postAlert("WARNING: Safe zone begins shrinking in 10s!")
                soundEffects.playZoneWarning()
            }
        } else {
            // Transition to shrinking or next stage
            if (!currentZone.isShrinking && currentZone.stage < currentZone.totalStages) {
                currentZone.isShrinking = true
                _gamePhase.value = GamePhase.ZONE_SHRINKING
                // Pick smaller target radius and slightly shift center
                val newRadius = when (currentZone.stage) {
                    1 -> 360f
                    2 -> 270f
                    3 -> 180f
                    4 -> 90f
                    else -> 60f
                }
                currentZone.targetRadius = newRadius
                currentZone.centerX = (currentZone.centerX + Random.nextInt(-40, 40)).coerceIn(300f, 700f)
                currentZone.centerY = (currentZone.centerY + Random.nextInt(-40, 40)).coerceIn(300f, 700f)
                postAlert("Danger zone expanding! Move inside the circle!")
                soundEffects.playZoneWarning()
            } else if (currentZone.isShrinking) {
                // Shrinking finished, rest for next stage
                currentZone.isShrinking = false
                currentZone.stage += 1
                currentZone.countdownSeconds = when (currentRoomConfig.shrinkSpeed) {
                    "Fast" -> 25
                    "Blitz" -> 15
                    else -> 40
                }
                _gamePhase.value = if (currentZone.stage >= currentZone.totalStages) GamePhase.SUDDEN_DEATH else GamePhase.SAFE_CALM
            }
        }
        _safeZone.value = currentZone.copy()
    }

    private fun updatePhysics(dt: Float) {
        val currentPlayers = _players.value.map { it.copy() }
        val zone = _safeZone.value

        // Smoothly shrink radius if active
        if (zone.isShrinking && zone.currentRadius > zone.targetRadius) {
            val shrinkSpeed = when (currentRoomConfig.shrinkSpeed) {
                "Fast" -> 20f
                "Blitz" -> 30f
                else -> 12f
            }
            zone.currentRadius = (zone.currentRadius - shrinkSpeed * dt).coerceAtLeast(zone.targetRadius)
            _safeZone.value = zone.copy()
        }

        // Find seeker to coordinate bot movements
        val activeSeekers = currentPlayers.filter { it.role == PlayerRole.SEEKER && !it.isTagged }
        val activeRunners = currentPlayers.filter { it.role == PlayerRole.RUNNER && !it.isTagged }

        val now = SystemClock.uptimeMillis()

        currentPlayers.forEach { player ->
            if (player.isTagged) return@forEach

            if (player.isLocalPlayer) {
                // Local player control
                var baseSpeed = 160f
                if (player.isSprinting && player.stamina > 0) {
                    baseSpeed = 260f
                    player.stamina = (player.stamina - 28f * dt).coerceAtLeast(0f)
                } else {
                    player.stamina = (player.stamina + 14f * dt).coerceAtMost(100f)
                }

                val moveDist = hypot(inputX, inputY)
                if (moveDist > 0.05f) {
                    val nx = inputX / moveDist
                    val ny = inputY / moveDist
                    var nextX = player.x + nx * baseSpeed * dt
                    var nextY = player.y + ny * baseSpeed * dt

                    // Obstacle collision
                    if (!checkObstacleCollision(nextX, player.y)) {
                        player.x = nextX.coerceIn(50f, 950f)
                    }
                    if (!checkObstacleCollision(player.x, nextY)) {
                        player.y = nextY.coerceIn(50f, 950f)
                    }
                }
            } else if (player.isBot) {
                // AI Bot logic
                updateBotMovement(player, activeSeekers, activeRunners, zone, dt)
            }

            // Tagging Check: Any seeker tags nearby runners
            if (player.role == PlayerRole.SEEKER) {
                currentPlayers.forEach { runner ->
                    if (runner.role == PlayerRole.RUNNER && !runner.isTagged) {
                        val isStealthed = runner.stealthUntilMillis > now
                        if (!isStealthed) {
                            val dist = hypot(player.x - runner.x, player.y - runner.y)
                            if (dist < 55f) {
                                runner.isTagged = true
                                runner.tagTimeSeconds = currentRoomConfig.durationMinutes * 60 - _remainingSeconds.value
                                onPlayerCaptured(runner)
                            }
                        }
                    }
                }
            }

            // Safe zone check: Warn if player is outside
            val distToCenter = hypot(player.x - zone.centerX, player.y - zone.centerY)
            if (distToCenter > zone.currentRadius) {
                if (player.isLocalPlayer && Random.nextFloat() < 0.04f) {
                    postAlert("DANGER: You are outside the safe zone!")
                    soundEffects.playZoneWarning()
                }
            }
        }

        _players.value = currentPlayers
        checkWinCondition()
    }

    private fun updateBotMovement(
        bot: PlayerState,
        seekers: List<PlayerState>,
        runners: List<PlayerState>,
        zone: SafeZoneState,
        dt: Float
    ) {
        val speed = 135f
        var dirX = 0f
        var dirY = 0f

        if (bot.role == PlayerRole.SEEKER) {
            // Seek nearest runner or move towards center of safe zone
            val nearestRunner = runners.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
            if (nearestRunner != null) {
                val dx = nearestRunner.x - bot.x
                val dy = nearestRunner.y - bot.y
                val dist = hypot(dx, dy)
                if (dist > 1f) {
                    dirX = dx / dist
                    dirY = dy / dist
                }
            } else {
                val dx = zone.centerX - bot.x
                val dy = zone.centerY - bot.y
                val dist = hypot(dx, dy)
                if (dist > 1f) {
                    dirX = dx / dist
                    dirY = dy / dist
                }
            }
        } else {
            // Runner AI: Flee from nearest seeker
            val nearestSeeker = seekers.minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
            val distToSeeker = nearestSeeker?.let { hypot(it.x - bot.x, it.y - bot.y) } ?: 9999f

            if (nearestSeeker != null && distToSeeker < 220f) {
                // Run opposite direction
                val fx = bot.x - nearestSeeker.x
                val fy = bot.y - nearestSeeker.y
                val dist = hypot(fx, fy)
                if (dist > 1f) {
                    dirX = fx / dist
                    dirY = fy / dist
                }
            } else {
                // Wander inside safe zone
                val distToZone = hypot(bot.x - zone.centerX, bot.y - zone.centerY)
                if (distToZone > zone.currentRadius * 0.75f) {
                    // Head towards zone center
                    val dx = zone.centerX - bot.x
                    val dy = zone.centerY - bot.y
                    val dist = hypot(dx, dy)
                    if (dist > 1f) {
                        dirX = dx / dist
                        dirY = dy / dist
                    }
                } else {
                    // Slight random wander
                    bot.vx += (Random.nextFloat() - 0.5f) * 0.4f
                    bot.vy += (Random.nextFloat() - 0.5f) * 0.4f
                    val mag = hypot(bot.vx, bot.vy)
                    if (mag > 1f) {
                        bot.vx /= mag
                        bot.vy /= mag
                    }
                    dirX = bot.vx
                    dirY = bot.vy
                }
            }
        }

        val nextX = bot.x + dirX * speed * dt
        val nextY = bot.y + dirY * speed * dt

        if (!checkObstacleCollision(nextX, bot.y)) {
            bot.x = nextX.coerceIn(50f, 950f)
        } else {
            bot.vx = -bot.vx
        }
        if (!checkObstacleCollision(bot.x, nextY)) {
            bot.y = nextY.coerceIn(50f, 950f)
        } else {
            bot.vy = -bot.vy
        }
    }

    private fun checkObstacleCollision(x: Float, y: Float): Boolean {
        val playerRadius = 18f
        return obstacles.any { obs ->
            x + playerRadius >= obs.x && x - playerRadius <= obs.x + obs.width &&
            y + playerRadius >= obs.y && y - playerRadius <= obs.y + obs.height
        }
    }

    private fun triggerSonarPing() {
        _sonarPingActive.value = true
        soundEffects.playSonarPing()
        postAlert("SONAR PING: Temporary runner signals detected!")
    }

    private fun onPlayerCaptured(runner: PlayerState) {
        soundEffects.playTagCapture()
        postAlert("${runner.nickname} was TAGGED!")

        val card = BustedCard(
            id = "busted_${System.currentTimeMillis()}",
            playerName = runner.nickname,
            playerAvatarId = runner.avatarId,
            playerNumber = Random.nextInt(1, 99),
            captureTimeFormatted = formatTimer(runner.tagTimeSeconds),
            roomCode = currentRoomConfig.code,
            achievementTitle = if (runner.tagTimeSeconds > 240) "Elite Evader" else "Rookie Runner"
        )
        _lastTaggedCard.value = card
        _bustedCardsHistory.value = listOf(card) + _bustedCardsHistory.value
    }

    private fun checkWinCondition() {
        val runners = _players.value.filter { it.role == PlayerRole.RUNNER }
        val remainingRunners = runners.filter { !it.isTagged }

        if (runners.isNotEmpty() && remainingRunners.isEmpty()) {
            _gamePhase.value = GamePhase.GAME_OVER
            soundEffects.playVictory()
            postAlert("SEEKER VICTORY! All runners eliminated!")
        }
    }

    private fun onTimeExpired() {
        val remainingRunners = _players.value.filter { it.role == PlayerRole.RUNNER && !it.isTagged }
        _gamePhase.value = GamePhase.GAME_OVER
        if (remainingRunners.isNotEmpty()) {
            soundEffects.playVictory()
            postAlert("RUNNER VICTORY! Surviving players outsmarted the hunters!")
        } else {
            postAlert("MATCH CONCLUDED!")
        }
    }

    fun dismissBustedCard() {
        _lastTaggedCard.value = null
    }

    fun stopGame() {
        isRunning = false
        engineScope?.cancel()
        engineScope = null
    }

    private fun postAlert(msg: String) {
        _alertMessage.value = msg
    }

    fun clearAlert() {
        _alertMessage.value = null
    }

    private fun formatTimer(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%02d:%02d".format(m, s)
    }
}
