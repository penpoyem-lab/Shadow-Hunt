# Live High-Precision GPS Satellite Tracking & Locked Safe Zone

Implement high-precision live location mapping with high-detail street level satellite imagery, a geographically locked safe circle zone, and dynamic glowing accuracy ring visualization.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product behaviors have been established based on your design choices:
> - **Geographically Locked Safe Zone**: The Safe Circle Zone center is permanently fixed at the initial host coordinates (or chosen landmark anchor). The safe perimeter remains fixed on earth while the player moves in real-time within or outside the zone.
> - **High-Detail Street Level Satellite Zoom**: Camera zoom defaults to high-detail street level (~zoom 17.5) rendering sharp satellite building footprints, streets, alleys, and paths.
> - **Live GPS Precision Visualization**: Player dot is surrounded by a translucent glowing neon cyan accuracy ring whose radius scales dynamically with device GPS horizontal accuracy (±X meters).
> - **Strict 1:30 Stealth Protocol Preserved**: Seekers and Hiders remain completely invisible to each other outside the periodic 1:30 minute (90-second) 15s mutual radar reveal window.

---

### 1. Overview & Core Concept

- **What It Delivers**: Transforms the outdoor gameplay into a high-precision live GPS tactical mission on real satellite imagery.
- **Geographical Integrity**: Rather than shifting the safe zone with the user, the safe zone circle is anchored permanently at fixed GPS coordinates. As the user walks, runs, or maneuvers in the physical world, their live GPS marker moves across the real satellite map toward or away from the fixed safe zone boundary.
- **Visual Precision**: Renders real-time GPS accuracy tolerance as a pulsing halo (e.g. ±3m to ±12m) and displays live telemetry (Lat/Lon, Bearing, Altitude, Speed in km/h, Satellites locked).

---

### 2. User Experience & Visual Design

#### Visual Layout & Telemetry HUD
- **Satellite Map Viewport**: Full-bleed ESRI World Imagery high-resolution satellite tiles showing actual streets, buildings, foliage, and paths.
- **Fixed Safe Zone Ring**: A 1.0+ km perimeter line rendered with a vibrant cyan-gold glow, dashed urgency indicator, and expanding perimeter shockwave.
- **Player Marker**:
  - Bright golden core pin with directional heading beam pointing along device azimuth.
  - Surrounding translucent cyan glowing ring whose physical diameter on screen reflects exact GPS accuracy in meters.
  - Real-time coordinates and speed badge docked smoothly on the bottom left.
- **Distance & Boundary Metrics**: Real-time distance indicator showing exact meters to safe zone edge, updating smoothly at 60 FPS without jitter.

---

### 3. Key Technical Decisions & Trade-Offs

- **Decision 1: Fixed Geographical Anchor vs Auto-Anchor**
  - *Chosen Approach*: Anchor the Safe Zone center once on match launch to the host's actual location. The safe zone center coordinates never move with the player; only the player marker moves.
  - *Why*: True hide-and-seek gameplay requires an objective play zone in the real world (park, neighborhood, campus) where players can be pushed toward the center or caught outside the perimeter.
- **Decision 2: Accuracy Ring Scaling Math**
  - *Chosen Approach*: Convert `currentLoc.accuracy` (in meters) into map pixels using Mercator meter-to-pixel projection at the current zoom level.
  - *Why*: Ensures the glowing halo visually represents the actual GPS margin of error on the ground.
- **Decision 3: Tile Caching & Street-Level Resolution**
  - *Chosen Approach*: High-detail zoom level (17.0–18.0) with LRU in-memory tile caching for instant street and building rendering.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Live Location Manager                │
│    (FusedLocationProviderClient / GPS Satellites)      │
└───────────────────────────┬────────────────────────────┘
                            │ (lat, lon, accuracy, bearing, speed)
                            ▼
┌────────────────────────────────────────────────────────┐
│                RealOutdoorGameEngine                   │
│  - Locked Safe Zone: centerLat, centerLon, radius (1km)│
│  - Distance calculation: distToEdge = distCenter - R   │
│  - Proximity tag detection & 1:30 reveal ticker       │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow updates
                            ▼
┌────────────────────────────────────────────────────────┐
│                   DarkTacticalMap                      │
│  - High-res Satellite Tiles (Zoom 17.5)                │
│  - Fixed Safe Zone Ring (Always Showable)              │
│  - Live Player Pin + Directional Azimuth Beam          │
│  - Dynamic Glowing GPS Accuracy Ring (±Accuracy m)     │
│  - Opponents visible ONLY during 1:30 reveal           │
└────────────────────────────────────────────────────────┘
```

#### Verification Steps
1. Verify Safe Zone center remains static at the initial coordinates while player position updates.
2. Verify zoom level defaults to high-detail street level showing building footprints.
3. Verify the glowing cyan accuracy circle scales with GPS accuracy tolerance.
4. Verify Seeker/Hider mutual reveal triggers strictly every 1:30 minutes for 15 seconds.
5. Compile and verify with `compile_applet`.
