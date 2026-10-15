# Argon Client — Master Roadmap & Development TODO

> **Mission:** Build a full-fledged, Android-first Minecraft Bedrock client with **real built-in modules**. No external resource packs to import, no third-party companion apps or tools to install. Argon is an all-in-one client featuring real in-game HUD modules, **Touch Lab** (ergonomic mobile control editor), and **Argon Flow** (device-adaptive performance tuning).

---

## Core Client Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                       ARGON CLIENT (Android)                           │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Built-in In-Game HUD & Module Engine                                │
│    - Real-time FPS Counter (native frame-delta timing)                 │
│    - Touch CPS Counter (Left/Right tap frequency)                      │
│    - Dynamic Keystrokes & Touch Input Visualizer                       │
│    - Armor & Offhand Durability HUD                                    │
│    - Coordinates & Cardinal Direction Compass                         │
│    - In-Game Draggable ClickGUI / HUD Customizer                       │
├────────────────────────────────────────────────────────────────────────┤
│ 2. Touch Lab (Mobile Ergonomics Engine)                                │
│    - Thumb reach calibration arc & safe-area margin detection          │
│    - Custom touch controls, claw layout presets (2, 3, 4 fingers)     │
│    - Quick Action Radial Menu over gameplay                            │
├────────────────────────────────────────────────────────────────────────┤
│ 3. Native Game Runtime & Engine Hooks                                  │
│    - Direct interaction with Minecraft Bedrock process                │
│    - GLES / RenderDragon swapchain frame hook                          │
│    - Camera FOV hook for Smooth Zoom                                   │
│    - Zero external apps, zero resource pack dependencies               │
├────────────────────────────────────────────────────────────────────────┤
│ 4. Argon Flow Performance Suite                                        │
│    - 60 / 90 / 120 Hz display refresh rate synchronization             │
│    - Memory class & thermal-aware background optimization              │
│    - Offline, privacy-first profiles (PvP, Building, Exploration)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Current Status & Phased Roadmap

### Phase 0: Android Core Foundation (Completed / Current)
- [x] Native Kotlin Android application with dark modern theme.
- [x] Dashboard, Profiles, and Module Catalog UI.
- [x] Local profile storage (PvP, Building, Exploration) with JSON import/export.
- [x] Device signals detection: app memory class, display refresh rate, low-RAM flag, battery-saver state.
- [x] System-level floating overlay service (`MinecraftOverlayService`) with ongoing notification and stop controls.
- [x] Gradle wrapper, automated build script, and GitHub Actions CI.
- [ ] **P0:** Smoke-test floating overlay permission grant/revoke flow and orientation handling on real physical devices.
- [ ] **P0:** Ensure zero memory leaks when overlay service restarts or tears down.

---

### Phase 1: Built-in In-Game Modules Engine (P0 — High Priority)
*Eliminate all external resource packs and external tools. Implement real modules running directly within Argon's in-game client engine.*

- [ ] **1.1 Real-Time In-Game FPS Counter:**
  - [ ] Measure exact frame cadence and 1% lows directly in the client runtime without third-party apps or external packs.
  - [ ] Configurable update frequency (0.5s, 1s, real-time smoothing).
  - [ ] Customizable styling: background color, text size, font, borders, and color coding (green/yellow/red).
- [ ] **1.2 Native Touch CPS Counter (Clicks Per Second):**
  - [ ] Built-in touch input listener to track left/right attack and place tap rates with millisecond precision.
  - [ ] Display sliding window average CPS (last 1 second) and peak CPS indicator.
- [ ] **1.3 Built-in Keystrokes & Touch Visualizer:**
  - [ ] Visual HUD tiles displaying active movement inputs and jump/sneak status.
  - [ ] Animated keypress feedback, customizable press animations, and touch position markers.
- [ ] **1.4 Built-in Status & Utility HUDs:**
  - [ ] **Clock & Session Timer:** Local time and active gameplay session duration.
  - [ ] **Coordinates & Cardinal Facing:** Clean X, Y, Z coordinate display with cardinal direction (N, NE, E, SE, S, SW, W, NW).
  - [ ] **Armor & Hand Durability:** Real-time visual display of helmet, chestplate, leggings, boots, and held items.
  - [ ] **Status Effects HUD:** Visual effect icons with countdown timers.
  - [ ] **Ping Display:** Built-in connection latency meter.

---

### Phase 2: In-Game ClickGUI & Real-Time Customizer (P0)
*Allow players to toggle, position, and customize client modules without leaving their game.*

- [ ] **2.1 In-Game ClickGUI Overlay:**
  - [ ] Swipe-gesture or quick-tap trigger to open the Argon Client HUD Editor directly over Minecraft.
  - [ ] Searchable module drawer with instant on/off switches for every module.
  - [ ] Category tabs: **Combat**, **Movement**, **Visual**, **HUD**, **Utility**.
- [ ] **2.2 Real-Time Drag-and-Drop HUD Positioning:**
  - [ ] Interactive layout editor: tap and drag any HUD module to position it on screen.
  - [ ] Smart snap-to-grid and alignment guides (top-left, center, top-right, bottom-right).
  - [ ] Individual module scale, opacity, border, and chroma color sliders.
  - [ ] Auto-save layout configurations to local offline profiles.

---

### Phase 3: Touch Lab — Native Mobile Ergonomics (P1)
*Argon's premier mobile feature: solving Bedrock touch control friction.*

- [ ] **3.1 Thumb-Reach Calibration:**
  - [ ] Interactive screen calibration tool to map natural left and right thumb reach arcs.
  - [ ] Screen safe-area compensation for camera cutouts, rounded corners, and punch holes.
- [ ] **3.2 Custom Touch Layout Presets:**
  - [ ] **2-Finger Casual:** Relaxed thumb positions with enlarged hitboxes.
  - [ ] **3-Finger Claw:** Dedicated top-corner trigger for jump or attack.
  - [ ] **4-Finger Pro Claw:** Full multi-finger competitive PvP layout.
  - [ ] **Tablet / Large Screen Mode:** Centered ergonomics preventing thumb strain.
- [ ] **3.3 Quick Action Radial Wheel:**
  - [ ] On-screen draggable floating touch wheel for instant actions: Perspective toggle (F5), Hide GUI (F1), Chat, Zoom, and Profile switch.

---

### Phase 4: Native Client Runtime & Direct Game Hooks (P1 / P2)
*Deep engine integration into Minecraft Bedrock's native libraries (`libminecraftpe.so`).*

- [ ] **4.1 In-Process Graphics Hook:**
  - [ ] Hook GLES3 / RenderDragon presentation pipeline (`eglSwapBuffers`) for true sub-millisecond frame rendering.
  - [ ] Integrate lightweight native UI renderer (e.g., Dear ImGui or NanoVG) drawn directly into the game's render thread.
- [ ] **4.2 Client Camera & Visual Enhancements:**
  - [ ] **Smooth Cinematic Zoom:** Dynamic FOV reduction with smooth interpolation on button tap or dedicated gesture.
  - [ ] **Freelook / 360° Perspective:** Orbit camera freely in third-person view without altering player orientation.
  - [ ] **Fullbright / Dynamic Gamma:** Client-side brightness boost for caves and night gameplay.
  - [ ] **Custom Dynamic Crosshairs:** Customizable size, thickness, gap, dynamic recoil, and color themes.
  - [ ] **Low Fire & Clean Shield:** Lowered first-person item rendering to maximize PvP field of view.

---

### Phase 5: Argon Flow & Device Performance Suite (P1)
*Make the game run faster and smoother on Android devices.*

- [ ] **5.1 Display Refresh Rate Lock:**
  - [ ] Force game surface rendering to match native 90Hz / 120Hz / 144Hz high-refresh displays.
- [ ] **5.2 Thermal & Battery Safeguards:**
  - [ ] Monitor device temperature in real-time and provide thermal throttle prevention.
  - [ ] Low-power background standby mode when Minecraft loses focus.
- [ ] **5.3 Profile Auto-Switching:**
  - [ ] Automatically apply competitive PvP settings when entering arena play, and battery-saver settings when traveling or idle.

---

### Phase 6: Fair Play, Safety & Release (P0 / P1)
*Ensure Argon is 100% safe, ban-free, and legally compliant.*

- [ ] **Anti-Cheat Safe Mode:**
  - [ ] Hardcoded blacklist against unfair hacks (no speed, fly, killaura, reach, or hitbox expansion).
  - [ ] Strict compliance with third-party server guidelines (Hive, CubeCraft, etc.) to guarantee zero bans.
- [ ] **100% Self-Contained:**
  - [ ] Zero requirement for external companion apps, Shizuku, or third-party resource packs.
  - [ ] Clean APK install with no bloat or tracking.
- [ ] **Release Engineering:**
  - [ ] Automated GitHub Release workflow building signed release APKs with SHA256 verification.
