package com.example.model

import com.example.R

enum class PlayerRole(val title: String, val badgeColorHex: Long) {
    SEEKER("Seeker", 0xFFFF1E43),
    RUNNER("Runner", 0xFF00E5FF)
}

enum class GamePhase {
    PREPARATION,   // 3... 2... 1... HUNT BEGINS!
    SAFE_CALM,     // Normal movement, safe zone stable
    ZONE_WARNING,  // Safe zone about to shrink!
    ZONE_SHRINKING,// Shrinking circle moving inwards
    SUDDEN_DEATH,  // Final small circle
    GAME_OVER      // Seeker or Runner victory
}

enum class NavigationTab(val label: String) {
    HOME("Home"),
    PLAY("Play"),
    FRIENDS("Friends"),
    PROFILE("Profile"),
    LAST_WALL("Last Wall")
}

data class AvatarOption(
    val id: String,
    val name: String,
    val drawableRes: Int,
    val glowColorHex: Long
)

object AvatarRegistry {
    val options = listOf(
        AvatarOption("hunter", "Shadow Hunter", R.drawable.avatar_hunter, 0xFFFF1E43),
        AvatarOption("cyber", "Cyber Agent", R.drawable.avatar_cyber, 0xFF00E5FF),
        AvatarOption("phantom", "Phantom Ninja", R.drawable.avatar_phantom, 0xFF9D00FF),
        AvatarOption("reaper", "Neon Reaper", R.drawable.avatar_reaper, 0xFF00E5FF),
        AvatarOption("demon", "Demon Warrior", R.drawable.avatar_demon, 0xFFFF3D00),
        AvatarOption("kishalay", "Kishalay 007", R.drawable.avatar_anime_kishalay, 0xFF9D00FF)
    )

    fun getAvatar(id: String): AvatarOption {
        return options.find { it.id == id } ?: options.first()
    }
}

data class ArenaInfo(
    val id: String,
    val name: String,
    val description: String,
    val previewRes: Int
)

object ArenaRegistry {
    val arenas = listOf(
        ArenaInfo("neon_city", "Neon City", "Metropolitan alleys with skyscrapers and neon signs", R.drawable.thumb_neon_city),
        ArenaInfo("cyber_ruins", "Cyber Ruins", "Abandoned industrial tech complex with deep shadows", R.drawable.thumb_neon_city),
        ArenaInfo("shadow_docks", "Shadow Docks", "Harbor warehouses with shipping containers and fog", R.drawable.thumb_neon_city)
    )
}

data class PlayerState(
    val id: String,
    val nickname: String,
    val avatarId: String,
    var role: PlayerRole,
    var isHost: Boolean = false,
    var isReady: Boolean = true,
    var isBot: Boolean = false,
    var x: Float = 500f,         // Virtual arena coordinates: 0..1000
    var y: Float = 500f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isTagged: Boolean = false,
    var tagTimeSeconds: Int = 0,
    var stamina: Float = 100f,   // 0..100 for sprinting
    var isSprinting: Boolean = false,
    var stealthUntilMillis: Long = 0L,
    var lastPingMillis: Long = 0L,
    var isLocalPlayer: Boolean = false
)

data class SafeZoneState(
    var centerX: Float = 500f,
    var centerY: Float = 500f,
    var currentRadius: Float = 420f,
    var targetRadius: Float = 420f,
    var stage: Int = 1,
    var totalStages: Int = 5,
    var countdownSeconds: Int = 45,
    var isShrinking: Boolean = false
)

data class Obstacle(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String = "Building"
)

data class RoomConfig(
    val code: String = "SH7KQ2",
    val name: String = "Shadow Room",
    val maxPlayers: Int = 6,
    val seekersCount: Int = 1,
    val durationMinutes: Int = 10,
    val arenaId: String = "neon_city",
    val shrinkSpeed: String = "Normal",
    val hintFrequencySeconds: Int = 30,
    val isPrivate: Boolean = true,
    val safeZoneRadiusMeters: Float = 1000f,
    val safeZoneStages: Int = 5
)

data class BustedCard(
    val id: String,
    val playerName: String,
    val playerAvatarId: String,
    val playerNumber: Int,
    val captureTimeFormatted: String,
    val roomCode: String,
    val achievementTitle: String = "Rookie Runner",
    val dateText: String = "Today",
    val capturedPhotoPath: String? = null
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val iconName: String,
    val glowColorHex: Long
)

data class FriendItem(
    val id: String,
    val nickname: String,
    val avatarId: String,
    val status: String,
    val isOnline: Boolean,
    val level: Int
)

data class UserProfileData(
    val nickname: String = "Kishalay_007",
    val avatarId: String = "kishalay",
    val level: Int = 12,
    val currentXp: Int = 850,
    val maxXp: Int = 1200,
    val coins: Int = 12750,
    val diamonds: Int = 150,
    val playerId: String = "SH-5KV8HG",
    val gamesPlayed: Int = 24,
    val wins: Int = 11,
    val tags: Int = 18,
    val survivalFormatted: String = "6h 42m"
)
