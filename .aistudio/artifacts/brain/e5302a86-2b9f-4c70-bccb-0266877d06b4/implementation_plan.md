# SHADOW HUNT: Real-Life Live Outdoor Multiplayer Game

Transition Shadow Hunt into a real-life live GPS outdoor hide-and-seek game inspired by Last Seen Mugshot, featuring real GPS tracking, dark OpenStreetMap tactical tiles, GPS-centered shrinking safe zone boundaries, CameraX photo mugshot captures, and real-time Firebase Firestore multiplayer synchronization.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during Phase 1 clarification:

- **Confirmed Decision 1 (Map Engine)**: OpenStreetMap (OSMDroid) with CartoDB Dark Matter / Stadia dark tactical tiles. Zero API key friction, completely offline capable, dark cyberpunk tactical aesthetics matching the Shadow Hunt design.
- **Confirmed Decision 2 (Catch Mechanic)**: Real CameraX photo capture coupled with GPS proximity verification (Seeker must be within 20 meters of the Runner to snap their photo and trigger the BUSTED mugshot card).
- **Confirmed Decision 3 (Multiplayer Backend)**: Firebase Firestore for real-time room creation, live player GPS coordinate synchronization, and snapshot listener updates.

---

### 1. Overview & Core Concept

- **What It Does**: Transforms Shadow Hunt from a fictional virtual arena into an active outdoor battle-royale hide-and-seek game played in real parks, campuses, or neighborhoods.
- **Role Assignment**:
  - **Seekers (Hunters)**: Hunt down runners by tracking GPS "blinks", navigating within 20 meters, and photographing them with the built-in CameraX scanner to generate their official BUSTED mugshot.
  - **Runners (Hiders)**: Stay within the shrinking GPS safe zone while avoiding hunters. When photographed, they switch teams to join the hunt (the signature snowball mechanic).
- **Real-World Shrinking Safe Zone**: The host defines an outdoor game center (either their current GPS location or an interactive map pin) with a radius (e.g. 500m). The boundary visibly and rhythmically shrinks on the live dark map toward a designated center.
- **GPS Blinks**: Periodic temporary reveals (every 30–60s) where runner coordinates briefly pulse on the seekers' tactical radar.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Location & Camera Permission Flow**: 
   - Runtime permission request for `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `CAMERA`.
   - Explanatory tactical briefing: *"Shadow Hunt uses your GPS strictly for active match boundaries and camera for mugshot verification. No background tracking or permanent storage."*
2. **Room Creation (Host)**:
   - Host chooses game duration, initial safe-zone radius (250m, 500m, 1km), shrink speed, and hint blink frequency.
   - Host sets the arena origin at their current live GPS position.
   - Generates a 6-character room code stored in Firestore.
3. **Live Tactical Map HUD**:
   - Full-bleed dark OpenStreetMap tile rendering centered on the player's real-time coordinates.
   - Glowing circular safe-zone perimeter with timer-synced urgency pulsing (cyan/green when calm, rapid red shockwaves when shrinking).
   - Real-time location blips:
     - Local player: Bright glowing crosshair with direction heading.
     - Seekers: Menacing red icons with 20m proximity capture rings.
     - Runners: Blue dots revealed during periodic GPS blinks.
   - Distance-to-safe-zone HUD: Shows live meters to perimeter (e.g., *"SAFE: 120m to edge"* or *"DANGER: OUT OF ZONE by 35m"*).
4. **CameraX Capture & Busted Card**:
   - When a seeker enters within 20m of a runner, the tactical HUD unlocks the **`SNAP MUGSHOT`** camera scanner.
   - Taking the photo captures the runner, records GPS timestamp, updates Firestore to transition the runner into a seeker, and generates the collectible BUSTED card.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: OpenStreetMap / OSMDroid vs. Google Maps SDK**
  - *Chosen Approach*: OSMDroid with CartoDB Dark Matter tile overlay.
  - *Why*: Eliminates Google Maps API key requirements and billing configuration while providing a native dark cyberpunk palette.
  - *Alternative Considered*: Google Maps SDK requires external API keys that fail without user billing setup.
- **Decision 2: Dual Verification Tagging (GPS + Camera)**
  - *Chosen Approach*: Distance calculated using Android `Location.distanceBetween` ($\le 20\text{m}$) before allowing the seeker's CameraX shutter to trigger capture.
  - *Why*: Prevents false captures across town while preserving the physical thrill of photographing runners in person like Last Seen Mugshot.
- **Decision 3: Firebase Firestore Real-Time Architecture**
  - *Chosen Approach*: Document listener on `/game_rooms/{roomCode}/players/{playerId}` with geohash / lat-lng coordinates and room state.
  - *Why*: Provides sub-second real-time multiplayer updates across Android devices with authenticated security rules.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    SHADOW HUNT ARCHITECTURE                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [Android Device]                                           │
│  ┌───────────────────────┐       ┌──────────────────────┐   │
│  │ FusedLocationProvider │       │ CameraX ViewFinder   │   │
│  │ (GPS Updates @ 1Hz)   │       │ (20m Capture Shutter)│   │
│  └───────────┬───────────┘       └──────────┬───────────┘   │
│              │                              │               │
│              ▼                              ▼               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ LiveGameRepository / GameViewModel                   │   │
│  │ - Safe zone geofence math (Haversine distance)       │   │
│  │ - 5-stage timer-synced boundary interpolation        │   │
│  │ - GPS blink timer & role reassignment logic          │   │
│  └──────────────────────────┬───────────────────────────┘   │
│                             │                               │
│                             ▼                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TacticalMapScreen (OSMDroid Dark Tiles + Compose)    │   │
│  │ - Real GPS map tiles with dark theme                 │   │
│  │ - Pulsing safe-zone boundary ring                    │   │
│  │ - Live distance & proximity badge                    │   │
│  │ - Mini-Map corner toggle mode                        │   │
│  └──────────────────────────┬───────────────────────────┘   │
│                             │                               │
│                             ▼                               │
│  [Firebase Firestore Backend]                               │
│  - /game_rooms/{roomCode} (centerLat, centerLng, radius, ..)│
│  - /game_rooms/{roomCode}/players/{uid} (lat, lng, role, ..)│
│  - /game_rooms/{roomCode}/mugshots/{id} (photoUri, time, ..)│
└─────────────────────────────────────────────────────────────┘
```

#### Data Entities in Firestore:
- `game_rooms`: `code`, `hostId`, `status`, `centerLatitude`, `centerLongitude`, `initialRadiusMeters`, `currentRadiusMeters`, `phase`, `createdAt`.
- `players`: `uid`, `nickname`, `avatarId`, `role` (`SEEKER`, `RUNNER`), `latitude`, `longitude`, `isTagged`, `taggedAt`, `lastBlinkAt`.
- `mugshots`: `cardId`, `runnerUid`, `seekerUid`, `runnerName`, `photoUrl`, `captureDistanceMeters`, `timestamp`.
