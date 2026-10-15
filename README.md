# Argon Client — Built-in Android Minecraft Bedrock Client

**Argon** is a free, all-in-one Minecraft: Bedrock Edition client built specifically for Android.

Unlike tools that rely on manual resource pack downloads or external companion apps, **Argon features real modules built directly into the client**. Everything runs out of the box with zero external dependencies.

---

## Why Argon?

Mobile Bedrock players deserve a clean, high-performance client experience comparable to modern Java clients, built ground-up for touch screens:

- **100% Built-in Modules:** Real FPS counter, CPS counter, Keystrokes, Coordinates, Armor & Durability HUD, Status Effects, and Direction Compass built directly into Argon. No `.mcpack` files to import, no third-party apps required.
- **In-Game ClickGUI & HUD Editor:** Tap or swipe to open Argon's floating menu over Minecraft. Drag, scale, recolor, and reposition modules on the fly with live preview.
- **Touch Lab:** Ergonomic mobile touch controls, thumb-reach calibration arcs, and customizable Claw presets (2-finger, 3-finger, 4-finger).
- **Argon Flow:** Intelligent, hardware-aware optimization profiles (PvP, Building, Exploration) tailored to your device's RAM, chipset, and display refresh rate (60Hz / 90Hz / 120Hz).
- **Safe & Fair Play First:** Strict anti-cheat compliance. Argon provides competitive quality-of-life and HUD modules without game-breaking hacks, ensuring zero server bans.

---

## Feature Comparison

| Feature | Argon Client (Built-in) | Traditional Mod / Companion |
| :--- | :---: | :---: |
| **Real In-Game FPS Counter** | **Built-in** (Native frame timing) | Requires manual `.mcpack` import |
| **Touch CPS Counter** | **Built-in** (Left & Right tap rates) | Not supported |
| **Keystrokes & Touch Visualizer** | **Built-in** (On-screen touch tracking) | Not supported |
| **In-Game Draggable HUD Editor** | **Built-in** (Live drag-and-drop) | Inflexible / fixed JSON UI |
| **Armor & Item Durability HUD** | **Built-in** | Requires external packs |
| **Coordinates & Compass** | **Built-in** | Bulky vanilla UI |
| **External Resource Packs Needed?** | **None** | Yes |
| **External Apps / Shizuku Needed?** | **None** | Frequently required |
| **Touch Lab Mobile Ergonomics** | **Built-in** | Not supported |
| **Argon Flow Device Optimization** | **Built-in** | Not supported |

---

## Planned Built-in Modules

### Combat & Performance
- **Real-Time FPS Counter:** Accurate frame timing and delta-time measurement with customizable smoothing and colors.
- **Touch CPS Counter:** Measures real tap speeds for left-click (attack) and right-click (use/place) touch zones.
- **Keystrokes:** Visual feedback for movement inputs, jump, and sneak states.
- **Smooth Zoom:** Smooth animated camera FOV zoom for long-range reconnaissance.

### Information & HUD
- **Coordinates & Direction:** Clean X, Y, Z coordinate display with cardinal direction heading (N, NE, E, SE, S, SW, W, NW).
- **Armor & Durability:** Real-time equipment status, remaining durability percentage, and offhand item indicator.
- **Status Effects:** Visual icons showing active potion and status effects with countdown timers.
- **Clock & Session Timer:** Local device clock and world session length.
- **Device Status:** Real-time memory allocation, battery percentage, and thermal indicators.

### Visual & QoL
- **Custom Dynamic Crosshairs:** Customizable styles, colors, thickness, dynamic spread, and chroma rainbow mode.
- **Freelook / 360° Perspective:** Inspect your surroundings in third-person without moving your player's head.
- **Fullbright:** Client-side ambient lighting adjustment for dark caves and night vision.
- **Low Fire & Clean Shield:** Lowered first-person equipment view to preserve combat visibility.

---

## Architecture

- **`core/`:** Client profile models, module catalog, and Argon Flow recommendation engine.
- **`overlay/`:** High-performance Android floating overlay engine (`MinecraftOverlayService`, `OverlayWidgetRegistry`, `OverlayWidgetStore`) running over Minecraft.
- **`performance/`:** Frame-timing sampler (`FrameRateSampler`, `UiFrameRateMonitor`) measuring cadence and rendering performance.
- **`data/`:** Offline profile storage and portable JSON profile schema with schema validation.
- **`MainActivity`:** Mobile dashboard, Touch Lab setup, profiles manager, and client launcher.

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK Platform 35 (`compileSdk = 35`, `minSdk = 26`)
- Gradle 8.13

### Build Command
```sh
# Run tests and assemble debug APK
./gradlew testDebugUnitTest assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Project Boundaries & Fair Play

- **Unofficial Project:** Argon is not affiliated with or endorsed by Mojang, Microsoft, or Lunar Client.
- **No Bundled Game Binaries:** Argon does not redistribute Minecraft game APKs or copyrighted game assets. Users must own and install Minecraft separately.
- **Ban-Free Guarantee:** Argon is built exclusively for competitive fair play, quality of life, and accessibility. Unfair cheats (killaura, fly, speed, reach, hitbox tampering) are strictly forbidden and not included.
