package com.example.data

import android.util.Log
import com.example.model.LiveBustedCard
import com.example.model.RealGpsPlayer
import com.example.model.RealGpsSafeZone
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository for synchronizing live outdoor GPS games with Firebase Firestore backend.
 */
class FirestoreGameRepository {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirestoreGameRepo", "Firebase not initialized, operating in local-first mode: ${e.message}")
            null
        }
    }

    private var playersListener: ListenerRegistration? = null
    private var safeZoneListener: ListenerRegistration? = null

    /**
     * Updates player live GPS location and status in Firestore.
     */
    fun syncPlayerLocation(
        roomId: String,
        playerId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        role: String,
        isTagged: Boolean
    ) {
        val firestore = db ?: return
        val data = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracyMeters" to accuracy,
            "role" to role,
            "isTagged" to isTagged,
            "lastSeenTimestamp" to System.currentTimeMillis()
        )

        firestore.collection("rooms")
            .document(roomId)
            .collection("players")
            .document(playerId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.d("FirestoreGameRepo", "GPS sync silent notice: ${e.message}")
            }
    }

    /**
     * Publishes an official BUSTED collectible mugshot card to Firestore.
     */
    fun publishBustedCard(
        roomId: String,
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
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("rooms")
            .document(roomId)
            .collection("busted_cards")
            .document(card.id)
            .set(data)
            .addOnFailureListener { e ->
                Log.d("FirestoreGameRepo", "Mugshot publish silent notice: ${e.message}")
            }
    }

    /**
     * Listens for remote players in the multiplayer room.
     */
    fun listenToRoomPlayers(
        roomId: String,
        onPlayersUpdated: (List<RealGpsPlayer>) -> Unit
    ) {
        val firestore = db ?: return
        playersListener?.remove()

        playersListener = firestore.collection("rooms")
            .document(roomId)
            .collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val remotePlayers = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val nickname = doc.getString("nickname") ?: "Hunter"
                    val avatarId = doc.getString("avatarId") ?: "cyber"
                    val roleStr = doc.getString("role") ?: "RUNNER"
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lng = doc.getDouble("longitude") ?: 0.0
                    val accuracy = doc.getDouble("accuracyMeters")?.toFloat() ?: 5f
                    val isTagged = doc.getBoolean("isTagged") ?: false

                    if (lat == 0.0 && lng == 0.0) null else {
                        RealGpsPlayer(
                            id = id,
                            nickname = nickname,
                            avatarId = avatarId,
                            role = if (roleStr == "SEEKER") com.example.model.PlayerRole.SEEKER else com.example.model.PlayerRole.RUNNER,
                            latitude = lat,
                            longitude = lng,
                            accuracyMeters = accuracy,
                            isTagged = isTagged
                        )
                    }
                }

                if (remotePlayers.isNotEmpty()) {
                    onPlayersUpdated(remotePlayers)
                }
            }
    }

    fun cleanup() {
        playersListener?.remove()
        playersListener = null
        safeZoneListener?.remove()
        safeZoneListener = null
    }
}
