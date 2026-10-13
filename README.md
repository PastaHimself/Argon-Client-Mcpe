# Argon Client — Android foundation

**Argon** is an independent, Android-first Minecraft: Bedrock Edition companion and planned client runtime.

> **Current status: 0.1.0 foundation.** This is a native Android companion application, **not** yet a replacement Minecraft APK, injector, FPS booster, or functional in-game mod. Presets are saved locally and previewed in Argon, but are **not applied to Minecraft** until a supported integration exists. No elevated privileges, APK patching, or Minecraft game binaries are included.

## Why Argon?

Existing Bedrock clients already offer HUD widgets, cosmetics and toggles. Argon's proposed differentiator is **Argon Flow**: explainable, device-aware, playstyle-specific touch layouts and performance profiles, with privacy-first offline storage. The goal is to make the Android experience comfortable on lower-end devices and large phones without overwhelming players.

Current foundation:
- Native Android app with a dark, mobile-first dashboard.
- Playstyle profiles (PvP, Building, Exploration), customizable control scale / HUD visibility / visual preference.
- **Argon Flow** recommendations based on Android's low-RAM flag, application memory class, refresh rate and battery-saver mode, with clear explanations.
- Offline persistence and JSON import/export through Android's Storage Access Framework.
- Detection and launch of an already installed, legitimate Minecraft Android app.
- Separate recommendation, storage, UI and future game-integration boundaries.
- Unit tests and a GitHub Actions Android CI workflow.
- No INTERNET permission, analytics, account requests, accessibility service, root or game-package rewriting.

### Honest feature status

| Feature | 0.1.0 state |
| --- | --- |
| Profile selection, edits and saved preferences | Implemented in Argon |
| Device-based recommendations | Implemented in Argon |
| JSON import and export | Implemented in Argon |
| Launch an installed Minecraft | Implemented via Android package manager |
| Apply HUD/control changes **inside** Minecraft | **Not implemented** |
| FPS optimization and graphics engine mods | **Not implemented** |
| In-game overlay or native hooks | **Not implemented** |

## Build

Prerequisites: Android Studio with JDK 17, Android SDK Platform 35 and Gradle 8.13. Import this repository as an Android project, sync, then run the \`app\` configuration on an Android 8.0+ device. In a CLI environment with Gradle installed:

\`\`\`sh
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
\`\`\`

The GitHub Actions workflow performs the same build and JVM tests. This minimal foundation does not bundle Gradle's wrapper JAR; use Android Studio or an installed Gradle 8.13 distribution. The produced debug APK is under \`app/build/outputs/apk/debug/\`. The Android app is **not** Minecraft itself; Minecraft must be installed independently.

## Architecture

- \`core/\`: platform-independent profiles and explainable recommendations.
- \`data/\`: local profiles, validation and portable JSON schema.
- \`MainActivity\`: native Android UI, dashboard, profiles and import/export.
- \`MinecraftLauncher\`: checks and opens the installed official app.
- \`docs/ROADMAP.md\`: compatibility-aware path toward a real modded client.

The Android UI intentionally uses platform widgets and zero third-party runtime libraries to minimize overhead and simplify early testing.

## Project boundaries

Independent, unofficial project. Not affiliated with Mojang or Microsoft. Users must own and install Minecraft separately. Do not distribute Minecraft binaries or copyrighted assets through Argon. Server rules may prohibit client modifications; compatibility and allowed modifications must be reviewed per game version and server.
