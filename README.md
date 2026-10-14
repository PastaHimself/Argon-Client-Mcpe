# Argon Client — Android foundation

**Argon** is an independent, Android-first Minecraft: Bedrock Edition companion and planned client runtime.

> **Current status: 0.1.0 foundation.** This is a native Android companion application, **not** yet a replacement Minecraft APK, injector, FPS booster, or functional in-game mod. Presets are saved locally and previewed in Argon, but are **not applied to Minecraft** until a supported integration exists. No elevated privileges, APK patching, or Minecraft game binaries are included.

## Why Argon?

Existing Bedrock clients already offer HUD widgets, cosmetics and toggles. Argon's proposed differentiator is **Argon Flow**: explainable, device-aware, playstyle-specific touch layouts and performance profiles, with privacy-first offline storage. The goal is to make the Android experience comfortable on lower-end devices and large phones without overwhelming players.

**Modules:** Argon lets you browse and wishlist proposed modules, but **does not activate any in-game module**. Module wishlist preferences are stored locally and are not yet included in the JSON profile export.

Current foundation:
- Native Android app with a dark, mobile-first dashboard.
- **Opt-in cross-app overlay:** launching Minecraft with the overlay option creates a draggable mint **A** icon above Minecraft through Android's user-granted display-over-other-apps capability. The menu supports Android local time and device status. It also links to an independently installed Minecraft FPS resource pack; Argon does not receive that FPS value or control the pack. The overlay remains active over other apps until stopped via menu/notification.
- **Argon-only UI modules:** the previous in-app module editor is still on the Modules page; its App FPS uses Argon's frame timestamps only, not Minecraft frames.
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
| Floating menu over Minecraft/other apps | Implemented via user-approved Android overlay; requires on-device validation |
| Overlay clock and Android device widget | Implemented, no game telemetry |
| Floating in-app module launcher and saved module toggles | Available on Argon's Modules screen |
| Argon window render FPS | Optional Argon-only HUD module (not Minecraft) |
| Device Status pill | Optional Argon-only HUD module |
| JSON import and export | Implemented in Argon |
| Launch an installed Minecraft | Implemented via Android package manager |
| Module catalog, filtering and wishlist | Implemented inside Argon (no in-game effect) |
| Minecraft FPS resource pack setup link | Implemented: opens third-party pack page; pack must be imported and enabled inside Minecraft |
| Direct Minecraft FPS telemetry and toggling in Argon's overlay | **Not implemented** |
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
- `overlay/MinecraftOverlayService.kt`: opt-in cross-app system overlay, special-use foreground service, small floating menu, widget HUD and explicit Stop controls.
- `overlay/OverlayWidgetStore.kt` and `OverlayWidgetRegistry.kt`: persisted allowlist for actual Android-only widgets; no coupling to game module capability gate.
- `MinecraftLauncher`: checks and opens the installed official app.
- `docs/ROADMAP.md`: compatibility-aware path toward a real modded client.

The Android UI intentionally uses platform widgets and zero third-party runtime libraries to minimize overhead and simplify early testing.

### Floating menu over Minecraft (opt-in)

1. Install Minecraft independently, open Argon and choose **Launch with floating Argon menu**.
2. Grant Android's **Display over other apps** permission on the Settings screen. Argon will attempt to start an ongoing foreground service and then launch Minecraft.
3. Drag the floating **A** icon to reposition it. Tap for the module menu: Android **Clock** and **Device Status** can be toggled on/off. The menu also provides a **Get Minecraft FPS Pack** link for a separate in-game resource pack; other game telemetry remains unavailable.
4. Stop the overlay from **Stop overlay** in the floating menu, the notification's **Stop overlay** action, or Argon's **Stop floating overlay** button. You can also launch Minecraft *without* the overlay from Argon.

**Privacy and behavior:** this is an Android `TYPE_APPLICATION_OVERLAY` over other apps, not a Minecraft mod. Android does not automatically tell this app when Minecraft exits, so the user-started overlay **can remain visible over other apps** until explicitly stopped. It neither records the screen nor accesses Minecraft's process, saved data or accounts. Draw-over-other-apps is optional and must be explicitly approved. The special-use foreground service has an ongoing notification; Google Play distribution of `specialUse` services requires separate policy review. Some versions of Android, OEM policies and games may restrict external overlays. Real Minecraft FPS is **not available** from Android's system overlay permission.

The prior Argon-only floating module editor is now accessible on **Modules**, rather than covering the Overview screen. Its FPS counter still measures Argon's own window only.

## Project boundaries

Independent, unofficial project. Not affiliated with Mojang or Microsoft. Users must own and install Minecraft separately. Do not distribute Minecraft binaries or copyrighted assets through Argon. Server rules may prohibit client modifications; compatibility and allowed modifications must be reviewed per game version and server.

### Actual Minecraft FPS without Shizuku

Argon links to a separately distributed Bedrock FPS resource pack that renders frame timing **inside Minecraft**. Install the `.mcpack` manually, then enable it in Minecraft's **Settings → Global Resources** or the world's Resource Packs. This does not use Shizuku, root, wireless debugging or screen capture. It is **not** a toggle that Argon can control or a reading that Argon's overlay can access. The original resource pack remains owned by its creator and is **not bundled** with Argon.

See [the setup and compatibility guide](docs/FPS_WITHOUT_SHIZUKU.md) for installation steps and limitations.
