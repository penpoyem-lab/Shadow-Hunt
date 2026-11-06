# Real GPS Outdoor Hunt: Cloud Multi-Player & Pure Human Protocol

Architectural plan to convert the game into a 100% genuine outdoor real-time multiplayer GPS hunt: eliminating all bot players, removing virtual arena fallbacks, establishing real-time cloud room synchronization across separate phones, and strictly requiring 3 real human players to launch.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during interactive planning:

- **Confirmed Decision 1 (Pure Real GPS Outdoor Arena)**: Completely remove all virtual 2D arena concepts, bot logic, and virtual disclaimer text. The app runs exclusively on hardware GPS telemetry and high-detail satellite maps in the real world.
- **Confirmed Decision 2 (Strictly Zero Bots)**: Bot players are completely eliminated. The lobby starts with only the Host (`1/3`), and will not allow match start until at least 2 other real humans join using the Room Code or QR scanner (`3/3`).
- **Confirmed Decision 3 (Real-Time Cloud Synchronization)**: Rooms, lobby memberships, and live player GPS coordinates will synchronize in real time via Firebase Firestore so friends on separate devices can join instantly and track each other on the satellite map.

---

## 1. Overview & Core Concept

### What It Does
An outdoor tactical game played by real friends on their own mobile devices. The game Host creates a room with a 6-character room code and QR code. Friends open their app, scan the host's QR code or type the code, and join the lobby. The game strictly enforces a 3-human minimum before the host can start the match. Once started, players hunt each other inside a geographically locked 1.0 km safe circle zone using real hardware GPS, high-detail satellite imagery, and tactile proximity alerts.

### Target Audience & Real-World Context
Groups of 3 or more friends playing outdoors in parks, campuses, neighborhoods, or sports fields without simulated bot fillers.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. HOST CREATES ROOM                                                        │
│ Host sets Safe Zone (min 1.0 km) -> Generates Room Code (e.g. SH7921) & QR  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. LOBBY (STRICT 3-HUMAN GATE)                                              │
│ • State 1/3: "WAITING FOR PLAYERS (1/3 Joined - Need 2 real players)" [RED] │
│ • START GAME Button: LOCKED with Lock icon                                  │
│ • Host shares Room Code or presents full-screen QR code                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. SQUAD MEMBERS JOIN ACROSS PHONES                                         │
│ • Friend 2 scans QR code -> Joins lobby -> Lobby auto-updates to 2/3        │
│ • Friend 3 scans QR code -> Joins lobby -> Lobby auto-updates to 3/3 [GREEN]│
│ • START GAME Button UNLOCKS: "START MATCH (3 PLAYERS)"                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. REAL OUTDOOR SATELLITE ARENA                                             │
│ • Host launches game -> All phones transition to Satellite Map              │
│ • 1 Seeker and 2 Hiders randomly and smartly assigned                       │
│ • Safe Zone locked permanently to geographic start center (min 1.0 km)       │
│ • Live GPS precision halo (±Xm) & real-time multi-phone location streaming   │
│ • Proximity alert siren & haptics when Seeker gets within <= 8m of a Hider  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Theme**: Neon Tactical Cyber-Ops (M3 Dark Theme, `Color(0xFF060A14)` background, `ElectricBlue`, `NeonGreen`, `NeonRed`, `GoldAccent`).
- **Lobby Roster**: Real human avatars with live connection status dots ("REAL PLAYER", "CONNECTED").
- **Lockdown Indicator**: Status banner in the lobby clearly indicates exact player counts needed:
  - `1/3`: Red border & background `Need 2 more real players`.
  - `2/3`: Amber border & background `Need 1 more real player`.
  - `3/3+`: Neon green border & background `Squad Ready • 3 Players Connected`.
- **Top Bar in Arena**: Clean layout with `[Pause] [LIVE SATELLITE] [Match Timer] [Runners Count] [Minimap Toggle]` without overcrowding or unwanted buttons.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Total Removal of Bot Generation & Virtual Arena
- **Decision**: Remove bot generation arrays (`botNames`, `botAvatars`) and the 2D simulated arena screen (`GameArenaScreen.kt`).
- **Rationale**: The user explicitly requested: *"It's not ani vertual arena, real gps location is required.......Only real life human player are needed (no bot player ).."*
- **Outcome**: The app is 100% focused on real GPS outdoor play.

### 2. Cloud-Backed Real-Time Room Synchronization
- **Decision**: Use Firebase Firestore for room state, lobby presence, and GPS position streaming.
- **Rationale**: Real human players on different phones need a reliable, serverless way to discover rooms, join via room code/QR, and synchronize coordinates outdoors over cellular/5G data.
- **Security & Structure**: Zero-trust rules where room creators are hosts, players can only update their own participant document, and room status governs match transitions.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FIREBASE FIRESTORE                               │
│                                                                             │
│  Collection: rooms/{roomCode}                                               │
│  ├── roomCode: "SH7921"                                                     │
│  ├── hostId: "user_abc"                                                     │
│  ├── status: "LOBBY" | "ACTIVE" | "FINISHED"                                │
│  ├── safeZone: { lat: 37.7749, lon: -122.4194, radiusMeters: 1000 }         │
│  └── Subcollection: players/{userId}                                        │
│      ├── nickname: "Kishalay"                                               │
│      ├── avatarId: "cyber"                                                  │
│      ├── role: "SEEKER" | "RUNNER"                                          │
│      ├── isHost: true                                                       │
│      ├── lat: 37.7749, lon: -122.4194, accuracy: 4.5                       │
│      └── lastUpdated: Timestamp                                             │
└───────────────────────▲─────────────────────────────▲───────────────────────┘
                        │ Real-Time Snapshot          │ Real-Time Snapshot
                        │ Updates                     │ Updates
        ┌───────────────┴───────────────┐     ┌───────┴───────────────────────┐
        │        PHONE 1 (HOST)         │     │     PHONE 2 & 3 (PARTNERS)    │
        │ • Creates Room SH7921         │     │ • Scan QR or Enter Code       │
        │ • Displays QR / Code          │     │ • Join room in Firestore      │
        │ • Listens to players subcoll  │     │ • Stream GPS coords to cloud  │
        │ • Unlocks Start when >= 3     │     │ • Render real players on map  │
        └───────────────────────────────┘     └───────────────────────────────┘
```

### Key Implementation Steps

1. **Firebase Backend Provisioning**:
   - Provision Firestore for real-time room and player coordination.
   - Configure security rules for rooms and live player tracking.

2. **Real Cloud Room Repository (`RealGameRoomRepository.kt`)**:
   - `createRoom(roomConfig, hostPlayer)`: Creates document in `rooms/{code}`.
   - `joinRoom(code, player)`: Adds player to `rooms/{code}/players/{userId}`.
   - `observeRoom(code)`: Returns a Flow/StateFlow of connected real players and room state.
   - `updatePlayerLocation(code, userId, lat, lon, accuracy)`: Continuously syncs live GPS coordinates during match.
   - `startMatch(code)`: Host updates room status to `"ACTIVE"`, automatically transitioning all connected phones to the live arena.

3. **Lobby Overhaul (`LobbyScreen.kt`)**:
   - Remove bot slots and bot generation buttons.
   - Display real human player cards with live status indicators.
   - Lock match start until `players.size >= 3`.
   - Start button unlocks only when 3 or more real humans are connected.

4. **Pure GPS Arena (`RealOutdoorArenaScreen.kt` & `RealOutdoorGameEngine.kt`)**:
   - Connect live GPS updates of all squad members from Firestore to the `DarkTacticalMap`.
   - Geographically locked 1.0 km safe zone.
   - Dynamic GPS accuracy ring and live telemetry badge.
   - Proximity detection ($\le 8\text{m}$) using real coordinates from all connected players.
