package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.LiveBustedCard
import com.example.model.PlayerRole
import com.example.model.RealGpsPlayer
import com.example.model.RealGpsSafeZone
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * Repository for synchronizing live outdoor GPS multiplayer games in real-time with Firebase Firestore.
 * Supports real human player discovery, lobby joining via Room Code & QR, and live GPS streaming.
 */
class FirestoreGameRepository(private val context: Context? = null) {

    private val db: FirebaseFirestore? by lazy {
        try {
            val app = FirebaseApp.getInstance()
            val dbId = context?.getString(R.string.firestore_database_id)
            if (!dbId.isNullOrEmpty()) {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w("FirestoreGameRepo", "Firebase Firestore initialization fallback: ${e.message}")
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                null
            }
        }
    }

    private var playersListener: ListenerRegistration? = null
    private var roomStatusListener: ListenerRegistration? = null

    /**
     * Creates a new live game room on Firestore.
     */
    fun createRoom(
        roomCode: String,
        hostId: String,
        hostPlayer: RealGpsPlayer,
        safeZone: RealGpsSafeZone,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val firestore = db ?: run {
            onSuccess()
            return
        }

        val roomData = hashMapOf(
            "roomCode" to roomCode,
            "hostId" to hostId,
            "status" to "LOBBY",
            "safeZoneLat" to safeZone.centerLatitude,
            "safeZoneLon" to safeZone.centerLongitude,
            "safeZoneRadius" to safeZone.currentRadiusMeters,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastActive" to FieldValue.serverTimestamp()
        )

        firestore.collection("rooms")
            .document(roomCode)
            .set(roomData, SetOptions.merge())
            .addOnSuccessListener {
                // Add host as first player in room
                addPlayerToRoom(roomCode, hostPlayer.copy(isHost = true), onSuccess, onError)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreGameRepo", "Failed to create room: ${e.message}")
                onError(e.message ?: "Failed to create room on cloud")
            }
    }

    /**
     * Joins an existing game room by Room Code.
     */
    fun joinRoom(
        roomCode: String,
        player: RealGpsPlayer,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val firestore = db ?: run {
            onSuccess()
            return
        }

        firestore.collection("rooms")
            .document(roomCode)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    addPlayerToRoom(roomCode, player.copy(isHost = false), onSuccess, onError)
                } else {
                    onError("Room $roomCode not found. Please verify the code or scan the host's QR code.")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to find room $roomCode")
            }
    }

    private fun addPlayerToRoom(
        roomCode: String,
        player: RealGpsPlayer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val firestore = db ?: run {
            onSuccess()
            return
        }

        val playerData = hashMapOf(
            "userId" to player.id,
            "nickname" to player.nickname,
            "avatarId" to player.avatarId,
            "role" to player.role.name,
            "isHost" to player.isHost,
            "isReady" to true,
            "latitude" to player.latitude,
            "longitude" to player.longitude,
            "accuracy" to player.accuracyMeters,
            "isTagged" to player.isTagged,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore.collection("rooms")
            .document(roomCode)
            .collection("players")
            .document(player.id)
            .set(playerData, SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to join room")
            }
    }

    /**
     * Listens to connected real human squad members in the room.
     */
    fun listenToRoomPlayers(
        roomCode: String,
        currentUserId: String,
        onPlayersUpdated: (List<RealGpsPlayer>) -> Unit
    ) {
        val firestore = db ?: return
        playersListener?.remove()

        playersListener = firestore.collection("rooms")
            .document(roomCode)
            .collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.w("FirestoreGameRepo", "Players listener notice: ${error?.message}")
                    return@addSnapshotListener
                }

                val realPlayers = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("userId") ?: doc.id
                    val nickname = doc.getString("nickname") ?: "Player"
                    val avatarId = doc.getString("avatarId") ?: "cyber"
                    val roleStr = doc.getString("role") ?: "RUNNER"
                    val isHost = doc.getBoolean("isHost") ?: false
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lng = doc.getDouble("longitude") ?: 0.0
                    val accuracy = doc.getDouble("accuracy")?.toFloat() ?: 5f
                    val isTagged = doc.getBoolean("isTagged") ?: false

                    RealGpsPlayer(
                        id = id,
                        nickname = nickname,
                        avatarId = avatarId,
                        role = if (roleStr == "SEEKER") PlayerRole.SEEKER else PlayerRole.RUNNER,
                        isHost = isHost,
                        isLocalPlayer = (id == currentUserId),
                        latitude = lat,
                        longitude = lng,
                        accuracyMeters = accuracy,
                        isTagged = isTagged
                    )
                }

                onPlayersUpdated(realPlayers)
            }
    }

    /**
     * Listens to the game room status (e.g. LOBBY -> ACTIVE when Host launches).
     */
    fun listenToRoomStatus(
        roomCode: String,
        onStatusChanged: (String) -> Unit
    ) {
        val firestore = db ?: return
        roomStatusListener?.remove()

        roomStatusListener = firestore.collection("rooms")
            .document(roomCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val status = snapshot.getString("status") ?: "LOBBY"
                onStatusChanged(status)
            }
    }

    /**
     * Host starts the match: updates roles for all real players and transitions room status to ACTIVE.
     */
    fun launchMatch(
        roomCode: String,
        assignedPlayers: List<RealGpsPlayer>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val firestore = db ?: run {
            onSuccess()
            return
        }

        val batch = firestore.batch()
        val roomRef = firestore.collection("rooms").document(roomCode)
        batch.update(roomRef, mapOf(
            "status" to "ACTIVE",
            "lastActive" to FieldValue.serverTimestamp()
        ))

        assignedPlayers.forEach { player ->
            val playerRef = roomRef.collection("players").document(player.id)
            batch.update(playerRef, mapOf(
                "role" to player.role.name,
                "isTagged" to false
            ))
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to start match") }
    }

    /**
     * Updates local player live GPS coordinates and telemetry.
     */
    fun syncPlayerLocation(
        roomCode: String,
        playerId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        role: String,
        isTagged: Boolean
    ) {
        val firestore = db ?: return
        if (latitude == 0.0 && longitude == 0.0) return

        val data = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracy" to accuracy,
            "role" to role,
            "isTagged" to isTagged,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore.collection("rooms")
            .document(roomCode)
            .collection("players")
            .document(playerId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.d("FirestoreGameRepo", "GPS sync background notice: ${e.message}")
            }
    }

    /**
     * Publishes collectible mugshot card when a runner is caught.
     */
    fun publishBustedCard(
        roomCode: String,
        card: LiveBustedCard
    ) {
        val firestore = db ?: return
        val data = hashMapOf(
            "id" to card.id,
            "runnerName" to card.runnerName,
            "seekerName" to card.seekerName,
            "roomCode" to card.roomCode,
            "captureTimeFormatted" to card.captureTimeFormatted,
            "captureDistanceMeters" to card.captureDistanceMeters,
            "badgeTitle" to card.badgeTitle,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("rooms")
            .document(roomCode)
            .collection("busted_cards")
            .document(card.id)
            .set(data)
            .addOnFailureListener { e ->
                Log.d("FirestoreGameRepo", "Busted card notice: ${e.message}")
            }
    }

    fun cleanup() {
        playersListener?.remove()
        playersListener = null
        roomStatusListener?.remove()
        roomStatusListener = null
    }
}
