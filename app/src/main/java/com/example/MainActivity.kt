package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GameSoundEffects
import com.example.game.ShadowHuntEngine
import com.example.model.*
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen {
    object Home : Screen()
    object CreateRoom : Screen()
    object JoinRoom : Screen()
    object Lobby : Screen()
    object Arena : Screen()
    object GameEnd : Screen()
    object Profile : Screen()
    object LastWall : Screen()
    object Friends : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var soundEffects: GameSoundEffects
    private lateinit var gameEngine: ShadowHuntEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundEffects = GameSoundEffects(applicationContext)
        gameEngine = ShadowHuntEngine(soundEffects)

        setContent {
            ShadowHuntTheme {
                ShadowHuntApp(
                    engine = gameEngine,
                    soundEffects = soundEffects
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameEngine.stopGame()
        soundEffects.release()
    }
}

@Composable
fun ShadowHuntApp(
    engine: ShadowHuntEngine,
    soundEffects: GameSoundEffects
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
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
    var hasActiveRoom by remember { mutableStateOf(false) }

    val players by engine.players.collectAsState()
    val lastTaggedCard by engine.lastTaggedCard.collectAsState()
    val bustedCardsHistory by engine.bustedCardsHistory.collectAsState()
    val gamePhase by engine.gamePhase.collectAsState()

    // When game over, navigate to GameEnd screen
    LaunchedEffect(gamePhase) {
        if (gamePhase == GamePhase.GAME_OVER && currentScreen == Screen.Arena) {
            currentScreen = Screen.GameEnd
        }
    }

    // Handle Back Button navigation
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            Screen.CreateRoom, Screen.JoinRoom -> currentScreen = Screen.Home
            Screen.Lobby -> currentScreen = Screen.Home
            Screen.Arena -> {
                engine.stopGame()
                currentScreen = Screen.Lobby
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
            Screen.Home -> {
                HomeScreen(
                    profile = userProfile,
                    onPlayNow = {
                        soundEffects.playClick()
                        val cfg = RoomConfig(code = "SH7KQ2", name = "Cyber Arena", maxPlayers = 8)
                        activeRoomConfig = cfg
                        hasActiveRoom = true
                        engine.initializeRoom(cfg, userProfile.nickname, userProfile.avatarId)
                        currentScreen = Screen.Lobby
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
                        currentScreen = Screen.Lobby
                    },
                    hasActiveRoom = hasActiveRoom,
                    activeRoomCode = activeRoomConfig.code,
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
                        engine.initializeRoom(config, nickname, avatarId)
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
                        val config = RoomConfig(code = code, name = "Game $code", maxPlayers = 6)
                        activeRoomConfig = config
                        hasActiveRoom = true
                        engine.initializeRoom(config, nickname, avatarId)
                        currentScreen = Screen.Lobby
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
                        engine.startGame()
                        currentScreen = Screen.Arena
                    },
                    onToggleReady = {
                        soundEffects.playClick()
                    },
                    onAddBot = {
                        soundEffects.playClick()
                        val currentList = players.toMutableList()
                        if (currentList.size < activeRoomConfig.maxPlayers) {
                            val botNames = listOf("RexHunter", "ShadowKing", "Devil", "Valkyrie", "Ghost", "Cipher", "Phoenix", "Specter", "Zero")
                            val nextName = botNames[currentList.size % botNames.size]
                            currentList.add(
                                PlayerState(
                                    id = "bot_${System.currentTimeMillis()}",
                                    nickname = nextName,
                                    avatarId = "cyber",
                                    role = PlayerRole.RUNNER,
                                    isHost = false,
                                    isReady = true,
                                    isBot = true
                                )
                            )
                            engine.initializeRoom(activeRoomConfig.copy(maxPlayers = currentList.size), userProfile.nickname, userProfile.avatarId)
                        }
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
            Screen.GameEnd -> {
                GameEndScreen(
                    players = players,
                    onPlayAgain = {
                        engine.initializeRoom(activeRoomConfig, userProfile.nickname, userProfile.avatarId)
                        engine.startGame()
                        currentScreen = Screen.Arena
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
                    onCardClick = { /* open inspected dialog */ },
                    onBack = {
                        selectedTab = NavigationTab.HOME
                        currentScreen = Screen.Home
                    }
                )
            }
            Screen.Friends -> {
                FriendsScreen(
                    onInviteToRoom = { friendName ->
                        // Send room invite toast
                    }
                )
            }
        }

        // BUSTED COLLECTIBLE CARD DIALOG (When runner is captured)
        if (lastTaggedCard != null) {
            BustedCardDialog(
                card = lastTaggedCard!!,
                onDismiss = {
                    engine.dismissBustedCard()
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
        // Visible on main hub screens (Home, Profile, LastWall, Friends)
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
                            // Quick Match trigger
                            val cfg = RoomConfig(code = "SH7KQ2", name = "Cyber Arena", maxPlayers = 8)
                            activeRoomConfig = cfg
                            hasActiveRoom = true
                            engine.initializeRoom(cfg, userProfile.nickname, userProfile.avatarId)
                            currentScreen = Screen.Lobby
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
