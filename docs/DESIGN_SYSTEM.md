# Argon interface foundations

Argon is a **native Android companion app**, not an in-game Minecraft modification. Its interface must never imply that wishlist items, local presets, or device recommendations are active inside Minecraft.

## Visual direction

- **Dark-first and minimal:** a near-black canvas (`#0B1016`), quiet surfaces (`#151D26`), a single mint accent (`#A8EBC8`), and understated borders (`#303F4B`).
- **Typography:** system sans-serif, restrained weights, a compact wordmark, a clear 28sp page heading, 14sp body text and 11–12sp supporting labels.
- **Hierarchy:** a consistent masthead, one concise description per page, section labels, and compact actionable cards. Avoid decorative meters, simulated performance numbers, misleading badges or extra dependencies.
- **Interaction:** native ripple on buttons and navigation; controls are at least 48dp tall where practical, exceeding the 44px touch-optimized guidance in [Microsoft Learn touch interactions](https://learn.microsoft.com/windows/apps/develop/input/touch-interactions#hit-targets). Android density-independent pixels are not directly equivalent to Windows effective pixels, so follow Android platform guidance during physical-device review.
- **Navigation:** four persistent tabs with a selected-state indicator and spoken accessibility labels. The last selected tab survives Activity state restoration.
- **Transparency:** module cards explicitly say planned/unavailable; Argon Flow is a local preview; the launch button is disabled when the official game is absent.

## Review checklist

- On a small phone and a large tablet, in portrait and landscape, ensure that no labels truncate unexpectedly and that the keyboard does not hide module search.
- Test touch targets, screen-reader labels, selected states, contrast, sliders, and the native Android back button.
- Rotate and recreate the Activity on each page. Verify that profile values and module wishlist persistence are unchanged.
- Confirm both installed and missing-Minecraft launch states and the correct error toast on failed launch.
- Test module search, every category chip, add/remove wishlist, profile selection and edits, JSON import/export including invalid input and cancellation.
- Build and run Android tests; manually smoke-test on a real device before marking the UI production-ready.

No new runtime dependencies, permissions, network requests or Minecraft integration are part of this UI redesign.

## Floating modules (in-app only)

- The mint 56dp **A** launcher can be dragged within the Activity's safe content bounds, and its fractional position survives Activity recreation.
- Tap to open a scrollable, dim-backed module sheet, or tap outside / close to dismiss. Interactive toggles are limited to `AppModuleRegistry` allowlisted **Argon-only** capabilities and persist separately from the game wishlist.
- **App FPS** uses real Window frame callback samples, displays no reading while idle, and stops collecting when disabled or backgrounded. **Device Status** shows app memory class and battery-saver state, not game telemetry.
- Planned Minecraft modules are text-only disabled rows, never toggles, until the capability gate is legitimately implemented.
- **Device checks required:** drag, rotate, scroll behind sheet, Android back navigation, TalkBack focus, toggles after process recreation, lowest supported API 26, landscape clipping, and no HUD over another app.

## Minecraft floating overlay (new opt-in surface)

- Argon's **Launch with floating Argon menu** starts an Android `TYPE_APPLICATION_OVERLAY` only after the user grants `SYSTEM_ALERT_WINDOW` through Settings; **Launch without overlay** needs no permission.
- A 56dp draggable mint button floats above Minecraft and other apps. The compact, bounded sheet includes only supported Android-owned widgets (Clock, Device Status) as switches. Minecraft FPS, CPS, Ping, game coordinates and other game modules remain **unavailable**, not switches.
- The owner foreground service uses Android `specialUse` with an ongoing notification and clear **Stop overlay** action. A stop action also appears in the sheet and in Argon. The menu does not cover the whole screen, so interactions outside its bounds continue to reach the game.
- The HUD is noninteractive, uses a restricted opacity, and displays no fabricated gameplay metrics. The overlay is a system-level window, **not** a Minecraft game modification or a version-specific integration.
- The service does **not** track which app is in the foreground. It may remain over other applications after Minecraft closes. Inform the user; no Usage Access or Accessibility Service is requested. Validate Google Play foreground-service declarations/policy prior to public distribution.
- Device-only test matrix: API 26, 34 and 35; Android 15+, phones/tablets, Minecraft fullscreen; permission denied/granted/revoked, notification stop, app exit/return, screen rotation, drag and touch-through, task killer, and OEM permission limitations.

## Minecraft FPS resource-pack setup

The Overview card and floating panel link to an independent, MIT-licensed Bedrock FPS resource pack. The user imports and enables it **inside Minecraft**; neither Argon nor the Android overlay can activate or read it. No Shizuku, diagnostic service, root or MediaProjection capture are requested. See [FPS_WITHOUT_SHIZUKU.md](FPS_WITHOUT_SHIZUKU.md).
