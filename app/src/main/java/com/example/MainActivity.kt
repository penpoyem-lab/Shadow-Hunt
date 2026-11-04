package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.GameSoundEffects
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
                nickname = "Kishalay_007",
                avatarId = "kishalay",
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

        // Launch real outdoor game
        outdoorEngine.initializeRoom(
            activeOutdoorSettings,
            userProfile.nickname,
            userProfile.avatarId,
            PlayerRole.SEEKER
        )
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

    // Handle Back Button navigation
    BackHandler(enabled = currentScreen != Screen.Home && currentScreen != Screen.Login) {
        when (currentScreen) {
            Screen.CreateRoom, Screen.JoinRoom -> currentScreen = Screen.Home
            Screen.Lobby -> currentScreen = Screen.Home
            Screen.Arena -> {
                engine.stopGame()
                currentScreen = Screen.Lobby
            }
            Screen.OutdoorArena -> {
                outdoorEngine.stopGame()
                currentScreen = Screen.Home
            }
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
                        activeOutdoorSettings = activeOutdoorSettings.copy(roomCode = "SH7KQ2")
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    hasActiveRoom = true,
                    activeRoomCode = "SH7KQ2",
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
                        activeOutdoorSettings = LiveRoomSettings(
                            roomCode = config.code,
                            roomName = config.name,
                            durationMinutes = config.durationMinutes,
                            initialRadiusMeters = 400f,
                            shrinkSpeed = config.shrinkSpeed
                        )
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
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
                        activeOutdoorSettings = LiveRoomSettings(
                            roomCode = code,
                            roomName = "Live Room $code"
                        )
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    }
                )
            }
            Screen.Lobby -> {
                LobbyScreen(
                    roomConfig = activeRoomConfig,
                    players = players,
                    onLeaveLobby = {
                        currentScreen = Screen.Home
                    },
                    onStartGame = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    onToggleReady = {
                        soundEffects.playClick()
                    },
                    onAddBot = {
                        soundEffects.playClick()
                    }
                )
            }
            Screen.Arena -> {
                GameArenaScreen(
                    engine = engine,
                    onPauseOrExit = {
                        engine.stopGame()
                        currentScreen = Screen.Lobby
                    }
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp)),
            color = Color(0xE60E101E),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.2.dp, Color(0xFF282B48))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = selectedTab == NavigationTab.HOME,
                    activeColor = NeonRed,
                    onClick = { onTabSelect(NavigationTab.HOME) }
                )
                BottomNavItem(
                    label = "Play",
                    icon = Icons.Default.SportsEsports,
                    isSelected = selectedTab == NavigationTab.PLAY,
                    activeColor = ElectricBlue,
                    onClick = { onTabSelect(NavigationTab.PLAY) }
                )
                BottomNavItem(
                    label = "Friends",
                    icon = Icons.Default.People,
                    isSelected = selectedTab == NavigationTab.FRIENDS,
                    activeColor = NeonPurple,
                    onClick = { onTabSelect(NavigationTab.FRIENDS) }
                )
                BottomNavItem(
                    label = "Last Wall",
                    icon = Icons.Default.FilterFrames,
                    isSelected = selectedTab == NavigationTab.LAST_WALL,
                    activeColor = GoldAccent,
                    onClick = { onTabSelect(NavigationTab.LAST_WALL) }
                )
                BottomNavItem(
                    label = "Profile",
                    icon = Icons.Default.Person,
                    isSelected = selectedTab == NavigationTab.PROFILE,
                    activeColor = NeonPurple,
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
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(300),
        label = "tabBg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) Color.White else TextMuted
        )
    }
}
