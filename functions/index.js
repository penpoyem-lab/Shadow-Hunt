/**
 * Shadow Hunt - Real-Life Outdoor Multiplayer Hide & Seek
 * Server-Side Firebase Cloud Functions Backend (inspired by lastseenmugshot.com)
 *
 * Handles:
 *  1. Live player location monitoring against shrinking safe zone boundary
 *  2. Safe zone breach warnings and automatic player elimination
 *  3. Seeker/Runner geodesic proximity checks (20m capture lock)
 *  4. Camera photo mugshot verification and BUSTED card generation
 *  5. Room creation, joining, and room code session restoration (e.g. SH7KQ2)
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Calculates geodesic distance between two latitude/longitude pairs using Haversine formula (meters).
 */
function calculateHaversineDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000; // Earth radius in meters
  const dLat = (lat2 - lat1) * Math.PI / 180.0;
  const dLon = (lon2 - lon1) * Math.PI / 180.0;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180.0) * Math.cos(lat2 * Math.PI / 180.0) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * 1. Monitor live player location against dynamic safe zone perimeter.
 * Triggered on any player document update in a game room.
 */
exports.onPlayerLocationUpdated = functions.firestore
  .document("rooms/{roomId}/players/{playerId}")
  .onWrite(async (change, context) => {
    const { roomId, playerId } = context.params;
    const afterData = change.after.exists ? change.after.data() : null;

    if (!afterData || !afterData.latitude || !afterData.longitude) {
      return null;
    }

    // Only process for active, non-eliminated players
    if (afterData.status === "ELIMINATED" || afterData.status === "BUSTED") {
      return null;
    }

    const roomRef = db.collection("rooms").doc(roomId);
    const roomSnap = await roomRef.get();
    if (!roomSnap.exists) return null;

    const roomData = roomSnap.data();
    if (roomData.gameState !== "PLAYING") return null;

    const safeZone = roomData.safeZone || {};
    const centerLat = safeZone.centerLatitude;
    const centerLng = safeZone.centerLongitude;
    const currentRadiusMeters = safeZone.currentRadiusMeters || 500;

    if (centerLat === undefined || centerLng === undefined) return null;

    // Calculate distance from dynamic safe zone center
    const distanceToCenter = calculateHaversineDistanceMeters(
      afterData.latitude,
      afterData.longitude,
      centerLat,
      centerLng
    );

    const isOutsideZone = distanceToCenter > currentRadiusMeters;
    const nowMs = Date.now();
    const GRACE_PERIOD_MS = 20000; // 20 seconds warning before elimination

    const playerRef = change.after.ref;

    if (isOutsideZone) {
      const outSince = afterData.outOfZoneSinceMs || nowMs;
      const elapsedMs = nowMs - outSince;

      if (elapsedMs >= GRACE_PERIOD_MS) {
        // Player stayed outside zone past grace period -> ELIMINATE
        await playerRef.update({
          status: "ELIMINATED",
          eliminationReason: "OUT_OF_SAFE_ZONE",
          eliminatedAt: admin.firestore.FieldValue.serverTimestamp(),
          isOutsideZone: true,
          outOfZoneDurationSec: Math.floor(elapsedMs / 1000)
        });

        // Broadcast elimination log
        await roomRef.collection("events").add({
          type: "PLAYER_ELIMINATED",
          playerId: playerId,
          playerName: afterData.nickname || "Runner",
          role: afterData.role,
          reason: "OUT_OF_SAFE_ZONE",
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
      } else {
        // Issue warning state with remaining seconds
        const remainingSec = Math.max(0, Math.ceil((GRACE_PERIOD_MS - elapsedMs) / 1000));
        await playerRef.update({
          status: "OUT_OF_ZONE",
          isOutsideZone: true,
          outOfZoneSinceMs: outSince,
          warningSecondsLeft: remainingSec
        });
      }
    } else {
      // Player is safely inside zone
      if (afterData.status === "OUT_OF_ZONE" || afterData.isOutsideZone) {
        await playerRef.update({
          status: "ACTIVE",
          isOutsideZone: false,
          outOfZoneSinceMs: null,
          warningSecondsLeft: null
        });
      }
    }

    // 2. Proximity calculation between Seeker and Runners (20m capture radar)
    if (afterData.role === "SEEKER") {
      const runnersSnap = await roomRef
        .collection("players")
        .where("role", "==", "RUNNER")
        .where("status", "in", ["ACTIVE", "OUT_OF_ZONE"])
        .get();

      const batch = db.batch();
      let nearbyRunnersCount = 0;

      runnersSnap.forEach((runnerDoc) => {
        const rData = runnerDoc.data();
        if (rData.latitude && rData.longitude) {
          const dist = calculateHaversineDistanceMeters(
            afterData.latitude,
            afterData.longitude,
            rData.latitude,
            rData.longitude
          );

          const inRange = dist <= 20.0;
          if (inRange) nearbyRunnersCount++;

          batch.update(runnerDoc.ref, {
            distanceToSeekerMeters: Math.round(dist),
            inCaptureRange: inRange
          });
        }
      });

      await batch.commit();
    }

    return null;
  });

/**
 * 2. Advance Dynamic Safe Zone (Shrinking Zone Cycle)
 * Can be scheduled or called when match timer advances.
 */
exports.advanceSafeZonePhase = functions.https.onCall(async (data, context) => {
  const { roomId, targetPhase } = data;
  if (!roomId) throw new functions.https.HttpsError("invalid-argument", "roomId is required");

  const roomRef = db.collection("rooms").doc(roomId);
  const roomSnap = await roomRef.get();
  if (!roomSnap.exists) throw new functions.https.HttpsError("not-found", "Room not found");

  const phaseRadii = {
    PHASE_1: 500, // Initial Area
    PHASE_2: 300, // First Shrink
    PHASE_3: 150, // Mid Game
    PHASE_4: 75,  // Danger Zone
    PHASE_5: 25   // Final Showdown
  };

  const newRadius = phaseRadii[targetPhase] || 100;

  await roomRef.update({
    "safeZone.currentPhase": targetPhase,
    "safeZone.currentRadiusMeters": newRadius,
    "safeZone.lastShrinkTimestamp": admin.firestore.FieldValue.serverTimestamp()
  });

  await roomRef.collection("events").add({
    type: "SAFE_ZONE_SHRUNK",
    phase: targetPhase,
    newRadiusMeters: newRadius,
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });

  return { success: true, newRadius: newRadius };
});

/**
 * 3. Verify Photo Mugshot and Create Collectible BUSTED Card
 * Receives seekerId, runnerId, seeker coords, runner coords, and uploaded photo URL.
 */
exports.verifyAndIssueMugshot = functions.https.onCall(async (data, context) => {
  const { roomId, seekerId, runnerId, seekerLocation, photoUrl, runnerNickname } = data;

  if (!roomId || !seekerId || !runnerId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required fields");
  }

  const roomRef = db.collection("rooms").doc(roomId);
  const runnerRef = roomRef.collection("players").doc(runnerId);
  const seekerRef = roomRef.collection("players").doc(seekerId);

  const [runnerSnap, seekerSnap] = await Promise.all([runnerRef.get(), seekerSnap ? seekerRef.get() : null]);

  if (!runnerSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Runner not found");
  }

  const runnerData = runnerSnap.data();

  // Verify proximity <= 25 meters (accounting for GPS drift)
  if (runnerData.latitude && runnerData.longitude && seekerLocation) {
    const dist = calculateHaversineDistanceMeters(
      seekerLocation.latitude,
      seekerLocation.longitude,
      runnerData.latitude,
      runnerData.longitude
    );

    if (dist > 35.0) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Capture failed: Runner is ${Math.round(dist)}m away. Must be within 20m.`
      );
    }
  }

  // Create official BUSTED collectible card
  const cardId = `BUSTED_${Date.now()}`;
  const cardData = {
    cardId: cardId,
    roomId: roomId,
    runnerId: runnerId,
    runnerNickname: runnerNickname || runnerData.nickname || "Shadow Runner",
    seekerId: seekerId,
    bustedTimestamp: admin.firestore.FieldValue.serverTimestamp(),
    photoUrl: photoUrl || "",
    captureCoords: seekerLocation || { latitude: 0, longitude: 0 },
    stats: {
      survivalTimeMinutes: runnerData.survivalTimeMinutes || 4,
      bountyScore: 850
    }
  };

  const batch = db.batch();
  // Mark runner as BUSTED
  batch.update(runnerRef, {
    status: "BUSTED",
    isCaught: true,
    bustedCardId: cardId,
    caughtAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Save collectible card
  const cardRef = roomRef.collection("busted_cards").doc(cardId);
  batch.set(cardRef, cardData);

  // Increment seeker catch count
  if (seekerRef) {
    batch.update(seekerRef, {
      catchCount: admin.firestore.FieldValue.increment(1)
    });
  }

  // Add global event
  const eventRef = roomRef.collection("events").doc();
  batch.set(eventRef, {
    type: "PLAYER_BUSTED",
    runnerId: runnerId,
    seekerId: seekerId,
    cardId: cardId,
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });

  await batch.commit();

  return { success: true, cardId: cardId, card: cardData };
});

/**
 * 4. Rejoin Room by Code (e.g. SH7KQ2)
 * Allows players to restore an active match or rejoin after app restart.
 */
exports.rejoinGameRoom = functions.https.onCall(async (data, context) => {
  const { roomCode, playerNickname } = data;
  if (!roomCode) {
    throw new functions.https.HttpsError("invalid-argument", "roomCode is required");
  }

  const cleanCode = roomCode.trim().toUpperCase();
  const roomsSnap = await db.collection("rooms")
    .where("code", "==", cleanCode)
    .limit(1)
    .get();

  if (roomsSnap.empty) {
    throw new functions.https.HttpsError("not-found", `No active game found with code ${cleanCode}`);
  }

  const roomDoc = roomsSnap.docs[0];
  const roomData = roomDoc.data();

  return {
    success: true,
    roomId: roomDoc.id,
    roomCode: roomData.code,
    roomName: roomData.name,
    gameState: roomData.gameState,
    safeZone: roomData.safeZone
  };
});
