# "Last Seen Mugshot" Interface Redesign & Pure Human Protocol

Implementation plan to build the exact "Last Seen Mugshot" interface shown in the user's reference screenshots, blending the signature Neon Lime palette with liquid glass capsule effects and enforcing the strict 3-real-human player requirement.

## 1. Visual & Architectural Overview

Based on the 4 provided reference screenshots:
1. **Lobby Screen (`IMG_20261106_143302_2db62686.jpg`, `IMG_20261106_143604_39b6ab50.jpg`)**:
   - Header: "LAST SEEN MUGSHOT" title, "LOBBY" subtitle, and top-right "Leave Lobby" wine-colored rounded button.
   - Game Code Card: "GAME CODE" label, large white bold room code (e.g. `E8SYEC`), copy icon button, and dual "SHARE INVITE" + "SHOW QR" actions.
   - Requirement Banner:
     - When `< 3` players: Dark wine/crimson pill banner "🔴 Need X more to start / Waiting for players · minimum 3."
     - When `3+` players: Dark olive/green pill banner "🟢 Squad Ready / X real players in lobby"
   - "Your crew ✳" Section:
     - Player count "01 / 30 players".
     - Cute "Oddball" creature avatar cards with star host badge, nickname, and "You · Host" / "In lobby" status. Host card glows with `NeonLime` border.
     - Action buttons: "Change Oddball" text link and "👥 SEE ALL PLAYERS" pill.
   - Game Mode Selector Card:
     - Target icon with "STANDARD / 20 min · The full game" or "CUSTOM / X min · Y m circle".
     - "EDIT ->" button in olive green with bright neon lime text that opens the "GAME FEEL" bottom sheet.
   - "Last Wall" link.
   - Sticky Bottom Button:
     - When `< 3` players: Disabled olive/charcoal pill "NEED X MORE".
     - When `3+` players: Full-width vibrant Neon Lime (`#C8FF00`) capsule button "START GAME ->" with bold black text, water shadow, and tactile bounce.

2. **Game Feel Settings Bottom Sheet (`IMG_20261106_143321_4a4bde20.jpg`)**:
   - "GAME FEEL" header with close "✕" button.
   - 3 Preset Cards: "INTENSE" (12 min / 120 m), "STANDARD" (20 min / 160 m), and "CHILL" (30 min / 220 m).
   - "YOUR OWN SETTINGS" in neon lime with live summary and "Back to recommended".
   - Custom parameters panel:
     - Round length dropdown.
     - Circle radius input with quick chips: 50m, 100m, 250m, 500m, 1km, 2km, 5km, 10km.
     - Head start dropdown (15s, 30s, 45s, 60s) with explanatory rule text.
     - Zone physics & area calculations.

3. **Scan to Join QR Bottom Sheet (`IMG_20261106_143312_ffe9b7eb.jpg`)**:
   - "SCAN TO JOIN" header with close "✕" button.
   - Subtitle: "Scan this code with your phone camera to open the invite link."
   - Large QR Code canvas framed with vibrant Neon Lime scanner corner brackets.
   - "GAME CODE" subtitle and large glowing Neon Lime room code.

4. **Oddball Avatar System**:
   - Custom vector illustrations representing the distinctive creatures seen in the screenshots (Cyclops alien with teal body, purple teardrop creature with hooded eyes, spiky star creature, etc.).

---

## 2. Step-by-Step Implementation

1. **Color Tokens (`Color.kt`)**:
   - Add `MugshotNeonLime = Color(0xFFC8FF00)`, `MugshotDarkBg = Color(0xFF0A0E18)`, `MugshotCardBg = Color(0xFF111724)`, `MugshotCardBorder = Color(0xFF1E283A)`, `MugshotWineButton = Color(0xFF2C141B)`, `MugshotRedBg = Color(0xFF2A1116)`, `MugshotRedBorder = Color(0xFF551C25)`, `WaterShadowLime = Color(0x66C8FF00)`.

2. **Oddball Avatar Component (`OddballAvatars.kt`)**:
   - Render vector-drawn Oddballs with distinct character expressions, vibrant neon fills, and customizable colors.

3. **Game Feel Bottom Sheet (`GameFeelBottomSheet.kt`)**:
   - Build the interactive modal bottom sheet with presets, circle radius chips, custom numeric inputs, and live validation.

4. **Scan to Join Modal (`ScanToJoinBottomSheet.kt`)**:
   - Build the QR invite sheet with neon lime corner scanner brackets and room code.

5. **Lobby Screen Overhaul (`LobbyScreen.kt`)**:
   - Reconstruct the layout to match `IMG_20261106_143302_2db62686.jpg` and `IMG_20261106_143604_39b6ab50.jpg`.
   - Wire all buttons, sheets, and the strict 3-player start requirement.

6. **Real-Life GPS Map & UI Polish**:
   - Fixed the grey map tile grid showing "Map data not yet available": replaced limited ArcGIS imagery with Google Hybrid Satellite, CartoDB Dark Matter, and OpenStreetMap tiles with automated resilient fallback and duplicate-request suppression (`inFlight`).
   - Centered tactical map on user's live physical GPS coordinates (`26.06139, 89.29616`) with real hardware GPS tracking, live precision halo (±6m), speed, heading, and boundary proximity calculation.
   - Removed all hardcoded demo names ("Kishalay007") and password input barriers, replacing them with fluid codename entry and Oddball creature selection.
   - Implemented high quality liquid glass UI effect with neon lime highlights, specular shine, and water shadows across the "CREATE A GAME" section and gameplay HUD.

