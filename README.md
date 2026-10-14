# Argon Client — Android foundation

**Argon** is an independent, Android-first Minecraft: Bedrock Edition companion and planned client runtime.

> **Current status: 0.1.0 foundation.** This is a native Android companion application, **not** yet a replacement Minecraft APK, injector, FPS booster, or functional in-game mod. Presets are saved locally and previewed in Argon, but are **not applied to Minecraft** until a supported integration exists. No elevated privileges, APK patching, or Minecraft game binaries are included.

## Why Argon?

Existing Bedrock clients already offer HUD widgets, cosmetics and toggles. Argon's proposed differentiator is **Argon Flow**: explainable, device-aware, playstyle-specific touch layouts and performance profiles, with privacy-first offline storage. The goal is to make the Android experience comfortable on lower-end devices and large phones without overwhelming players.

**Modules:** Argon lets you browse and wishlist proposed modules, but **does not activate any in-game module**. Module wishlist preferences are stored locally and are not yet included in the JSON profile export.

Current foundation:
- Native Android app with a dark, mobile-first dashboard.
- **Floating in-app module launcher:** a draggable mint **A** button opens a compact panel to enable or disable real Argon-only modules. App FPS uses Android's own frame timestamps (dash when idle/insufficient data); Device Status shows Android app memory class and battery saver. No Minecraft screen overlay or game FPS reading.
- Playstyle profiles (PvP, Building, Exploration), customizable control scale / HUD visibility / visual preference.
- **Argon Flow** recommendations based on Android's low-RAM flag, application memory class, refresh rate and battery-saver mode, with clear explanations.
- Offline persistence and JSON import/export through Android's Storage Access Framework.
- Browsable module catalog (HUD, visual, controls, utility), category filters/search, and locally saved module wishlist; **none of these modules are active in Minecraft yet**.
- Detection and launch of an already installed, legitimate Minecraft Android app.
- Separate recommendation, storage, UI and future game-integration boundaries.
- Unit tests and a GitHub Actions Android CI workflow.
- No INTERNET permission, analytics, account requests, accessibility service, root or game-package rewriting.

### Honest feature status

| Feature | 0.1.0 state |
| --- | --- |
| Profile selection, edits and saved preferences | Implemented in Argon |
| Device-based recommendations | Implemented in Argon |
| Floating in-app module launcher and saved module toggles | Implemented inside Argon only |
| Argon window render FPS | Optional Argon-only HUD module (not Minecraft) |
| Device Status pill | Optional Argon-only HUD module |
| JSON import and export | Implemented in Argon |
| Launch an installed Minecraft | Implemented via Android package manager |
| Module catalog, filtering and wishlist | Implemented inside Argon (no in-game effect) |
| Minecraft module execution, including in-game FPS Counter | **Not implemented** |
| Apply HUD/control changes **inside** Minecraft | **Not implemented** |
| FPS optimization and graphics engine mods | **Not implemented** |
| In-game overlay or native hooks | **Not implemented** |

## Build

Prerequisites: Android Studio with JDK 17, Android SDK Platform 35 and Gradle 8.13. Import this repository as an Android project, sync, then run the `app` configuration on an Android 8.0+ device. In a CLI environment with Gradle installed:

```sh
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

The GitHub Actions workflow performs the same build and JVM tests. This minimal foundation does not bundle Gradle's wrapper JAR; use Android Studio or an installed Gradle 8.13 distribution. The produced debug APK is under `app/build/outputs/apk/debug/`. The Android app is **not** Minecraft itself; Minecraft must be installed independently.

## Architecture

- `core/`: platform-independent profiles and explainable recommendations.
- `data/`: local profiles, wishlist state, validation and portable JSON schema.
- `core/ModuleCatalog.kt`: stable proposed *game* module IDs, capabilities and activation guard (no compatible game integration is available).
- `core/AppModuleRegistry.kt` + `data/AppModuleStore.kt`: allowlisted app-only modules and independently persisted on/off state. No migration from the game wishlist.
- `MainActivity`: native Android UI, dashboard, profiles and import/export.
- `performance/UiFrameRateMonitor.kt` + `core/FrameRateSampler.kt`: observed window frames, recent-rate calculation, idle handling, and lifecycle cleanup. Android may drop frame-metric reports, so FPS is an estimate of Argon's own reported render cadence, not a device benchmark.
- `MinecraftLauncher`: checks and opens the installed official app.
- `docs/ROADMAP.md`: compatibility-aware path toward a real modded client.

The Android UI intentionally uses platform widgets and zero third-party runtime libraries to minimize overhead and simplify early testing.

### Floating module launcher

1. Open Argon and tap the floating **A** at the edge of the screen (drag it to reposition).
2. Enable **App FPS Counter** to show `APP  —` / measured Argon window FPS, or **Device Status** to show real Android device state. Both toggles persist across app relaunches and can be switched off at any time.
3. The panel lists planned Minecraft modules as **unavailable**; `Browse planned modules` opens the existing wishlist, not a game module runtime.

The floating bubble and HUD pills are **inside Argon's Activity**, not a system-wide overlay. They disappear when Minecraft or another app takes focus. This feature requests no draw-over-other-apps, accessibility or foreground-service permission. Accurate in-game FPS and overlays require a separately verified game integration and are not implemented here.

## Project boundaries

Independent, unofficial project. Not affiliated with Mojang or Microsoft. Users must own and install Minecraft separately. Do not distribute Minecraft binaries or copyrighted assets through Argon. Server rules may prohibit client modifications; compatibility and allowed modifications must be reviewed per game version and server.
