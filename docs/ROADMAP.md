# Argon Roadmap

## Positioning: Argon Flow

Argon is designed for Android touch players. Its proposed differentiator is **explainable adaptation**, not the number of injected features: device-aware PvP/Builder/Explorer presets, thumb-friendly layout recommendations, transparent performance trade-offs, and shareable configurations. No account or telemetry required.

## Stage 0 — Foundation (this PR)

- [x] Android application, profile navigation, Minecraft launch affordance
- [x] Hardware signals + deterministic recommendations
- [x] Locally editable profiles and JSON import/export
- [x] Profile-model validation and unit tests
- [x] CI Android build (runs once the PR is opened)
- [ ] Verify on real Android devices and different screen sizes
- [ ] In-game modification engine (intentionally absent)

## Stage 1 — Tangible Android differentiation

- [ ] Thumb-reach calibration in landscape mode ("Touch Lab")
- [ ] Preset comparison and simulated HUD mock-up in app
- [ ] Readable accessibility modes, one-handed variants and left-handed controls
- [ ] Profiler for app launch time, memory and thermal state **without** promising Minecraft FPS improvements
- [ ] User-exportable diagnostics that contain no account credentials

## Stage 2 — Minecraft compatibility research

Before claiming a client rather than a companion, investigate a legally distributable and maintainable integration method for supported, legitimately installed Minecraft Android versions. Compare sanctioned resource packs/add-ons and supported APIs before considering invasive hooks. Never ship proprietary Minecraft binaries.

Define and test a \`GameIntegration\` boundary with capability detection, per-version support and clear error reporting. Keep unsupported modules disabled.

## Stage 3 — If technically feasible

- [ ] Supported touch HUD profiles inside game
- [ ] Accessibility-oriented visual HUD and on-device diagnostics
- [ ] Per-server safe-mode rules and no unfair automation
- [ ] Explicit supported-version matrix with regression tests

**Not in scope now:** spoofed FPS numbers, claimed FPS boosts without benchmarks, cheat modules, anti-cheat evasion, silent APK tampering, credential collection, or bundling Minecraft.
