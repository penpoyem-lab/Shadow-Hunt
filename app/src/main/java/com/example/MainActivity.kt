package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.GameSoundEffects
import com.example.data.FirestoreGameRepository
import com.example.game.RealOutdoorGameEngine
import com.example.game.ShadowHuntEngine
import com.example.location.LiveLocationManager
import com.example.model.*
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object CreateRoom : Screen()
    object JoinRoom : Screen()
    object Lobby : Screen()
    object Arena : Screen()
    object OutdoorArena : Screen()
    object GameEnd : Screen()
    object Profile : Screen()
    object LastWall : Screen()
    object Friends : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var soundEffects: GameSoundEffects
    private lateinit var gameEngine: ShadowHuntEngine
    private lateinit var locationManager: LiveLocationManager
    private lateinit var outdoorEngine: RealOutdoorGameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundEffects = GameSoundEffects(applicationContext)
        gameEngine = ShadowHuntEngine(soundEffects)
        locationManager = LiveLocationManager(applicationContext)
        outdoorEngine = RealOutdoorGameEngine(locationManager, soundEffects)

        // Check if permissions are already granted
        val hasLoc = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        locationManager.updatePermission(hasLoc)

        setContent {
            ShadowHuntTheme {
                ShadowHuntApp(
                    engine = gameEngine,
                    outdoorEngine = outdoorEngine,
                    locationManager = locationManager,
                    soundEffects = soundEffects
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stopGame()
        outdoorEngine.stopGame()
        locationManager.stopLocationUpdates()
        soundEffects.release()
    }
}

@Composable
fun ShadowHuntApp(
    engine: ShadowHuntEngine,
    outdoorEngine: RealOutdoorGameEngine,
    locationManager: LiveLocationManager,
    soundEffects: GameSoundEffects
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var userProfile by remember {
        mutableStateOf(
            UserProfileData(
                nickname = "Hunter",
                avatarId = "krish",
                level = 12,
                currentXp = 850,
                maxXp = 1200,
                coins = 12750,
                diamonds = 150,
                playerId = "SH-5KV8HG",
                gamesPlayed = 24,
                wins = 11,
                tags = 18,
                survivalFormatted = "6h 42m"
            )
        )
    }

    val firestoreRepo = remember { FirestoreGameRepository(context.applicationContext) }
    var cloudGpsSquad by remember { mutableStateOf<List<RealGpsPlayer>>(emptyList()) }
    var isCurrentPlayerHost by remember { mutableStateOf(true) }

    var activeRoomConfig by remember { mutableStateOf(RoomConfig()) }
    var activeOutdoorSettings by remember { mutableStateOf(LiveRoomSettings(roomCode = "SH7KQ2")) }
    var hasActiveRoom by remember { mutableStateOf(true) }

    val players by engine.players.collectAsState()
    val outdoorPlayers by outdoorEngine.players.collectAsState()
    val lastTaggedCard by engine.lastTaggedCard.collectAsState()
    val lastOutdoorMugshot by outdoorEngine.lastCapturedMugshot.collectAsState()
    val bustedCardsHistory by engine.bustedCardsHistory.collectAsState()
    val gamePhase by engine.gamePhase.collectAsState()
    val outdoorGamePhase by outdoorEngine.gamePhase.collectAsState()

    // Runtime Permission Launcher for GPS and Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val locGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationManager.updatePermission(locGranted)

        // Launch real outdoor game with all 100% real human members from cloud room!
        val squad = if (cloudGpsSquad.isNotEmpty()) {
            cloudGpsSquad
        } else {
            listOf(
                RealGpsPlayer(
                    id = userProfile.playerId.ifEmpty { "player_local" },
                    nickname = userProfile.nickname,
                    avatarId = userProfile.avatarId,
                    role = if (isCurrentPlayerHost) PlayerRole.SEEKER else PlayerRole.RUNNER,
                    isHost = isCurrentPlayerHost,
                    isLocalPlayer = true,
                    isReady = true
                )
            )
        }
        outdoorEngine.initializeRoomWithRealSquad(activeOutdoorSettings, squad)
        outdoorEngine.startGame()
        currentScreen = Screen.OutdoorArena
    }

    // Convert LiveBustedCard to BustedCard for unified display
    val displayBustedCard = lastTaggedCard ?: lastOutdoorMugshot?.let { live ->
        BustedCard(
            id = live.id,
            playerName = live.runnerName,
            playerAvatarId = "cyber",
            playerNumber = 7,
            captureTimeFormatted = live.captureTimeFormatted,
            roomCode = live.roomCode,
            achievementTitle = live.badgeTitle,
            capturedPhotoPath = live.capturedPhotoPath
        )
    }

    // When game over, navigate to GameEnd screen
    LaunchedEffect(gamePhase, outdoorGamePhase) {
        if ((gamePhase == GamePhase.GAME_OVER && currentScreen == Screen.Arena) ||
            (outdoorGamePhase == GamePhase.GAME_OVER && currentScreen == Screen.OutdoorArena)
        ) {
            currentScreen = Screen.GameEnd
        }
    }

    // Handle Back Button navigation (Arena and OutdoorArena manage their own match lockdown)
    BackHandler(
        enabled = currentScreen != Screen.Home &&
                currentScreen != Screen.Login &&
                currentScreen != Screen.Arena &&
                currentScreen != Screen.OutdoorArena
    ) {
        when (currentScreen) {
            Screen.CreateRoom, Screen.JoinRoom -> currentScreen = Screen.Home
            Screen.Lobby -> currentScreen = Screen.Home
            Screen.GameEnd -> currentScreen = Screen.Home
            Screen.Profile, Screen.LastWall, Screen.Friends -> {
                selectedTab = NavigationTab.HOME
                currentScreen = Screen.Home
            }
            else -> currentScreen = Screen.Home
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // CONTENT AREA
        when (currentScreen) {
            Screen.Login -> {
                LoginScreen(
                    onLoginSuccess = { codename ->
                        soundEffects.playClick()
                        userProfile = userProfile.copy(nickname = codename)
                        currentScreen = Screen.Home
                    },
                    onGuestLogin = {
                        soundEffects.playClick()
                        currentScreen = Screen.Home
                    }
                )
            }
            Screen.Home -> {
                HomeScreen(
                    profile = userProfile,
                    onPlayNow = {
                        soundEffects.playClick()
                        // Request GPS and Camera for live outdoor game
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    onCreateRoom = {
                        soundEffects.playClick()
                        currentScreen = Screen.CreateRoom
                    },
                    onJoinRoom = {
                        soundEffects.playClick()
                        currentScreen = Screen.JoinRoom
                    },
                    onRejoinRoom = {
                        soundEffects.playClick()
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    hasActiveRoom = true,
                    activeRoomCode = activeOutdoorSettings.roomCode,
                    onRefreshRoomCode = {
                        val freshCode = "SH" + (1000..9999).random().toString()
                        activeOutdoorSettings = activeOutdoorSettings.copy(roomCode = freshCode)
                        activeRoomConfig = activeRoomConfig.copy(code = freshCode)
                        Toast.makeText(context, "Refreshed: New Room Code & QR ($freshCode)", Toast.LENGTH_SHORT).show()
                    },
                    onOpenProfile = {
                        selectedTab = NavigationTab.PROFILE
                        currentScreen = Screen.Profile
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    }
                )
            }
            Screen.CreateRoom -> {
                CreateRoomScreen(
                    initialNickname = userProfile.nickname,
                    initialAvatarId = userProfile.avatarId,
                    onBack = { currentScreen = Screen.Home },
                    onCreateGame = { config, nickname, avatarId ->
                        soundEffects.playClick()
                        userProfile = userProfile.copy(nickname = nickname, avatarId = avatarId)
                        activeRoomConfig = config
                        hasActiveRoom = true
                        isCurrentPlayerHost = true
                        activeOutdoorSettings = LiveRoomSettings(
                            roomCode = config.code,
                            roomName = config.name,
                            durationMinutes = config.durationMinutes,
                            initialRadiusMeters = config.safeZoneRadiusMeters.coerceAtLeast(1000f),
                            shrinkSpeed = config.shrinkSpeed
                        )

                        val hostGpsPlayer = RealGpsPlayer(
                            id = userProfile.playerId.ifEmpty { "host_" + System.currentTimeMillis() },
                            nickname = nickname,
                            avatarId = avatarId,
                            role = PlayerRole.SEEKER,
                            isHost = true,
                            isLocalPlayer = true,
                            isReady = true
                        )
                        cloudGpsSquad = listOf(hostGpsPlayer)

                        val safeZone = RealGpsSafeZone(
                            centerLatitude = locationManager.currentLocation.value?.latitude ?: 37.7749,
                            centerLongitude = locationManager.currentLocation.value?.longitude ?: -122.4194,
                            initialRadiusMeters = config.safeZoneRadiusMeters.coerceAtLeast(1000f),
                            currentRadiusMeters = config.safeZoneRadiusMeters.coerceAtLeast(1000f)
                        )

                        firestoreRepo.createRoom(
                            roomCode = config.code,
                            hostId = hostGpsPlayer.id,
                            hostPlayer = hostGpsPlayer,
                            safeZone = safeZone,
                            onSuccess = {
                                Toast.makeText(context, "Room ${config.code} live on cloud! Waiting for squad to join.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )

                        firestoreRepo.listenToRoomPlayers(config.code, hostGpsPlayer.id) { squad ->
                            cloudGpsSquad = squad
                        }

                        currentScreen = Screen.Lobby
                    }
                )
            }
            Screen.JoinRoom -> {
                JoinRoomScreen(
                    initialNickname = userProfile.nickname,
                    initialAvatarId = userProfile.avatarId,
                    onBack = { currentScreen = Screen.Home },
                    onJoinGame = { code, nickname, avatarId ->
                        soundEffects.playClick()
                        userProfile = userProfile.copy(nickname = nickname, avatarId = avatarId)
                        val joinedConfig = RoomConfig(code = code, name = "Live Room $code")
                        activeRoomConfig = joinedConfig
                        hasActiveRoom = true
                        isCurrentPlayerHost = false
                        activeOutdoorSettings = LiveRoomSettings(
                            roomCode = code,
                            roomName = "Live Room $code"
                        )

                        val joinGpsPlayer = RealGpsPlayer(
                            id = userProfile.playerId.ifEmpty { "p_" + System.currentTimeMillis() },
                            nickname = nickname,
                            avatarId = avatarId,
                            role = PlayerRole.RUNNER,
                            isHost = false,
                            isLocalPlayer = true,
                            isReady = true
                        )
                        cloudGpsSquad = listOf(joinGpsPlayer)

                        firestoreRepo.joinRoom(
                            roomCode = code,
                            player = joinGpsPlayer,
                            onSuccess = {
                                Toast.makeText(context, "Connected to room $code! Waiting in lobby.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )

                        firestoreRepo.listenToRoomPlayers(code, joinGpsPlayer.id) { squad ->
                            cloudGpsSquad = squad
                        }

                        firestoreRepo.listenToRoomStatus(code) { status ->
                            if (status == "ACTIVE" && currentScreen != Screen.OutdoorArena) {
                                // Match started by Host! Automatically transition to Real Outdoor Arena!
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.CAMERA
                                    )
                                )
                            }
                        }

                        currentScreen = Screen.Lobby
                    }
                )
            }
            Screen.Lobby -> {
                val displaySquad = if (cloudGpsSquad.isNotEmpty()) {
                    cloudGpsSquad.map { gp ->
                        PlayerState(
                            id = gp.id,
                            nickname = gp.nickname,
                            avatarId = gp.avatarId,
                            role = gp.role,
                            isHost = gp.isHost,
                            isReady = gp.isReady,
                            isBot = false,
                            isTagged = gp.isTagged,
                            isLocalPlayer = gp.isLocalPlayer
                        )
                    }
                } else {
                    listOf(
                        PlayerState(
                            id = userProfile.playerId,
                            nickname = userProfile.nickname,
                            avatarId = userProfile.avatarId,
                            role = if (isCurrentPlayerHost) PlayerRole.SEEKER else PlayerRole.RUNNER,
                            isHost = isCurrentPlayerHost,
                            isReady = true,
                            isBot = false,
                            isLocalPlayer = true
                        )
                    )
                }

                LobbyScreen(
                    roomConfig = activeRoomConfig,
                    players = displaySquad,
                    onLeaveLobby = {
                        currentScreen = Screen.Home
                    },
                    onStartGame = {
                        if (cloudGpsSquad.size >= 3) {
                            soundEffects.playClick()
                            firestoreRepo.launchMatch(
                                roomCode = activeRoomConfig.code,
                                assignedPlayers = cloudGpsSquad,
                                onSuccess = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.CAMERA
                                        )
                                    )
                                },
                                onError = { _ ->
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.CAMERA
                                        )
                                    )
                                }
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Minimum 3 real players required! Currently connected: ${cloudGpsSquad.size}/3. Share Room Code ${activeRoomConfig.code} or QR scanner.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onToggleReady = {
                        soundEffects.playClick()
                    },
                    onAddBot = {
                        Toast.makeText(
                            context,
                            "Pure real human players only (0 bots). Share Room Code ${activeRoomConfig.code} or scan QR code to invite friends!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            Screen.Arena -> {
                // Completely eliminate virtual arena in favor of pure Real GPS Outdoor Satellite Arena
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                    )
                )
            }
            Screen.OutdoorArena -> {
                RealOutdoorArenaScreen(
                    engine = outdoorEngine,
                    locationManager = locationManager,
                    onPauseOrExit = {
                        outdoorEngine.stopGame()
                        currentScreen = Screen.Home
                    }
                )
            }
            Screen.GameEnd -> {
                GameEndScreen(
                    players = if (outdoorPlayers.isNotEmpty()) {
                        outdoorPlayers.map {
                            PlayerState(
                                id = it.id,
                                nickname = it.nickname,
                                avatarId = it.avatarId,
                                role = it.role,
                                isTagged = it.isTagged,
                                tagTimeSeconds = it.taggedAtSeconds
                            )
                        }
                    } else players,
                    onPlayAgain = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    onReturnHome = {
                        currentScreen = Screen.Home
                    }
                )
            }
            Screen.Profile -> {
                ProfileScreen(
                    profile = userProfile,
                    onBack = {
                        selectedTab = NavigationTab.HOME
                        currentScreen = Screen.Home
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    },
                    onCustomizeAvatar = {
                        currentScreen = Screen.CreateRoom
                    }
                )
            }
            Screen.LastWall -> {
                LastWallScreen(
                    bustedCards = bustedCardsHistory,
                    onCardClick = { /* preview dialog */ },
                    onBack = {
                        selectedTab = NavigationTab.HOME
                        currentScreen = Screen.Home
                    }
                )
            }
            Screen.Friends -> {
                FriendsScreen(
                    onInviteToRoom = { _ -> }
                )
            }
        }

        // BUSTED COLLECTIBLE CARD DIALOG (With real camera photo!)
        if (displayBustedCard != null) {
            BustedCardDialog(
                card = displayBustedCard,
                onDismiss = {
                    engine.dismissBustedCard()
                    outdoorEngine.dismissMugshot()
                }
            )
        }

        // SETTINGS & AUDIO DIALOG
        if (showSettingsDialog) {
            SettingsDialog(
                soundEffects = soundEffects,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // LIQUID GLASS BOTTOM NAVIGATION BAR
        val showBottomNav = currentScreen in listOf(Screen.Home, Screen.Profile, Screen.LastWall, Screen.Friends)
        if (showBottomNav) {
            LiquidGlassBottomBar(
                selectedTab = selectedTab,
                onTabSelect = { tab ->
                    selectedTab = tab
                    soundEffects.playClick()
                    when (tab) {
                        NavigationTab.HOME -> currentScreen = Screen.Home
                        NavigationTab.PLAY -> {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.CAMERA
                                )
                            )
                        }
                        NavigationTab.FRIENDS -> currentScreen = Screen.Friends
                        NavigationTab.PROFILE -> currentScreen = Screen.Profile
                        NavigationTab.LAST_WALL -> currentScreen = Screen.LastWall
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun LiquidGlassBottomBar(
    selectedTab: NavigationTab,
    onTabSelect: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
    ) {
        // Outer Fluid Water Shadow Pool
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = WaterShadowAqua.copy(alpha = 0.5f),
                    spotColor = WaterShadowCyan.copy(alpha = 0.75f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE60D1224), // Translucent deep sapphire crystal
                            Color(0xF0070A14)
                        )
                    )
                )
                // Dual Specular Glass Border (Light refraction on surface tension)
                .border(
                    width = 1.6.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCCFFFFFF),       // Top specular rim glint
                            Color(0x5500F2FE),       // Mid aqua caustic
                            Color(0x22FFFFFF),
                            Color(0x10000000)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
        ) {
            // Top Glass Specular Arc / Lens Refraction Highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x35FFFFFF),
                                Color(0x08FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Top Specular Thin Edge Sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x88FFFFFF),
                                Color(0xCC00F2FE),
                                Color(0x88FFFFFF),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = selectedTab == NavigationTab.HOME,
                    activeColor = LiquidCrimsonCoral,
                    activeWaterGlow = WaterShadowCrimson,
                    onClick = { onTabSelect(NavigationTab.HOME) }
                )
                BottomNavItem(
                    label = "Play",
                    icon = Icons.Default.SportsEsports,
                    isSelected = selectedTab == NavigationTab.PLAY,
                    activeColor = LiquidAuroraCyan,
                    activeWaterGlow = WaterShadowAqua,
                    onClick = { onTabSelect(NavigationTab.PLAY) }
                )
                BottomNavItem(
                    label = "Friends",
                    icon = Icons.Default.People,
                    isSelected = selectedTab == NavigationTab.FRIENDS,
                    activeColor = RoyalCyberIris,
                    activeWaterGlow = WaterShadowPurple,
                    onClick = { onTabSelect(NavigationTab.FRIENDS) }
                )
                BottomNavItem(
                    label = "Last Wall",
                    icon = Icons.Default.FilterFrames,
                    isSelected = selectedTab == NavigationTab.LAST_WALL,
                    activeColor = SolarAmberGold,
                    activeWaterGlow = WaterShadowAmber,
                    onClick = { onTabSelect(NavigationTab.LAST_WALL) }
                )
                BottomNavItem(
                    label = "Profile",
                    icon = Icons.Default.Person,
                    isSelected = selectedTab == NavigationTab.PROFILE,
                    activeColor = LiquidEmeraldJade,
                    activeWaterGlow = WaterShadowEmerald,
                    onClick = { onTabSelect(NavigationTab.PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    activeWaterGlow: Color,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tabScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        if (isSelected) {
            // Liquid Droplet Capsule Background Pill with Water Glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = activeWaterGlow.copy(alpha = 0.5f),
                        spotColor = activeWaterGlow
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.28f),
                                activeColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99FFFFFF),
                                activeColor.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    letterSpacing = 0.3.sp
                ),
                color = if (isSelected) Color.White else TextMuted
            )
            if (isSelected) {
                // Micro Liquid Glow Dot Indicator
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}
