# Argon Client — TODO

> **Goal:** Build a free, Android-first Minecraft Bedrock client with a polished experience inspired by Lunar Client, but differentiated by **Argon Flow** (explainable device-aware presets), **Touch Lab** (comfortable mobile controls), and a lightweight, privacy-first design.
>
> **Current status:** v0.1.0 native Android **companion foundation**, not a modified Minecraft client. Presets currently affect Argon's own settings only. Nothing here should imply in-game functionality before integration is proven.
>
> **Priorities:** P0 = required for a usable release; P1 = important next; P2 = optional. Work through the phase gates in order, keep this checklist updated in the same PR as each implementation, and verify claims with tests. See [docs/ROADMAP.md](docs/ROADMAP.md) for the higher-level roadmap.

## Now — finish and validate v0.1.0 (P0)

- [x] Initialize native Android Kotlin application and basic Argon branding.
- [x] Build Overview, Profiles and About screens.
- [x] Add PvP, Building and Exploration playstyle profiles.
- [x] Add Argon Flow suggestions based on low-RAM mode, app memory class, refresh rate and battery-saver state.
- [x] Save settings locally; support JSON import/export with schema validation.
- [x] Detect and launch separately installed Minecraft (launch capability implemented, physical-device behavior still unverified).
- [x] Add JVM unit tests and GitHub Actions debug build.
- [x] Produce a debug APK as a GitHub Actions artifact.
- [ ] **P0:** Install the latest debug APK on a real Android phone and smoke-test launch, tabs, sliders, reset and game handoff.
- [ ] **P0:** Test JSON export/import round trip, corrupt JSON, oversized files and cancelled Android file-picker flows.
- [ ] **P0:** Test at least a small phone, large phone/tablet, low-memory device/emulator, Android 8+ and a recent Android release.
- [ ] **P0:** Fix discovered crashes, accessibility issues and state-restoration bugs; document exact tested devices.
- [x] Review PR #1, confirm CI passed at its final commit, and merge it into `main`.
- [ ] **P1:** Add a Gradle wrapper, lock/pin build dependencies and document reproducible local builds.
- [ ] **P1:** Add version/changelog and release notes with clear **companion-only** labeling.

**Done when:** The APK builds in CI and runs without known critical errors on tested Android devices; profile operations work offline.

## Phase 1 — Argon's unique mobile experience (P0 / P1)

### Touch Lab: the standout feature

- [ ] **P0:** Create a landscape-mode touch-control preview/editor **inside Argon** (not in Minecraft yet).
- [ ] **P0:** Let users calibrate left/right thumb reach and choose handedness.
- [ ] **P0:** Allow dragging/resizing virtual buttons with snap guides, safe-area handling and undo/reset.
- [ ] **P1:** Design different presets for compact phones, tall phones and tablets.
- [ ] **P1:** Provide side-by-side comparison of PvP / Building / Exploration layouts.
- [ ] **P1:** Export and import shareable, versioned Touch Lab layouts.
- [ ] **P1:** Add high-contrast, large-button and reduced-motion accessibility options.
- [ ] **P1:** Test multitouch UI interactions and different aspect ratios.

### Argon Flow

- [ ] **P0:** Show the specific device signals and the reasons behind every recommendation.
- [ ] **P0:** Clearly distinguish a **suggestion**, an **app-only preference**, and an **applied in-game setting**.
- [ ] **P1:** Support performance, balanced and quality presets with explicit trade-offs.
- [ ] **P1:** Provide manual overrides; never silently overwrite user settings.
- [ ] **P1:** Handle unreliable/unavailable device metrics safely.
- [ ] **P2:** Offer optional on-device calibration; keep results offline unless the user explicitly exports them.

### Modules — the planned Lunar-style customization system

- [x] **P0:** Add a native module catalog, categories, search and a saved local wishlist (no in-game behavior).
- [x] **P0:** Give modules stable IDs, capability requirements and a fail-closed availability guard.
- [x] **P0:** Show clear `planned/unavailable` labels; never imply wishlist toggles activate modules.
- [ ] **P0:** Smoke-test floating in-app module launcher, drag and persisted Argon FPS/Device Status toggles on a real Android device (separate from Minecraft overlay).
- [ ] **P0:** Define capability-based module configuration schema (positions, scale, opacity, hotkeys) with validation and migration.
- [ ] **P0:** Add module-specific controls, presets and a layout preview **inside Argon**.
- [ ] **P0:** Extend JSON import/export to include module preferences safely.
- [ ] **P1:** Add search, favorites and default preset bundles aligned with PvP / Building / Exploration.
- [ ] **P1:** Add accessible module descriptions and screenshot previews.
- [ ] **P1:** After the integration feasibility gate, implement and individually verify these proposed modules where supported:
  - [ ] FPS Counter and performance metrics (real measurements, not promises)
  - [ ] CPS Counter and Keystrokes
  - [ ] Coordinates, Armor HUD, Status Effects and Clock
  - [ ] Ping Display (only if accurate gameplay network metrics can be read)
  - [ ] Zoom / adjustable FOV and Minimal HUD
  - [ ] Touch Input Visualizer and readable HUD
- [ ] **P1:** Check each module against server rules and use compatibility flags; keep unsupported functionality disabled.
- [ ] **P2:** Consider a sandboxed community module ecosystem only after a stable runtime, API and security model exist.

**Done when:** A player can create a useful, understandable mobile control layout in Argon without Minecraft integration.

## Phase 2 — feasibility gate: becoming a real Bedrock client (P0)

**Do not skip this gate.** Research legal, technical and distribution constraints before writing invasive game hooks. Official Minecraft must be independently owned and installed.

- [ ] **P0:** Document permitted distribution and modification approaches, including Minecraft Usage Guidelines, app-store rules and third-party server policies; obtain legal review before commercialization.
- [ ] **P0:** Investigate supported resource packs, add-ons and documented Bedrock APIs as integration options.
- [ ] **P0:** Build a compatibility matrix of Android versions, device architectures and actual Minecraft Bedrock versions.
- [ ] **P0:** Document what each integration option can and **cannot** do (touch controls, overlays, HUD elements, visual settings).
- [ ] **P0:** Introduce a `GameIntegration` interface with capabilities, version detection, errors and a safe no-op fallback.
- [ ] **P0:** Implement one proof of concept using a maintainable, legally distributable method.
- [ ] **P0:** Prove the integration on a physical Android device and explicitly identify the supported versions.
- [ ] **P0:** If no acceptable method exists, **keep Argon as an honest companion** and revise the roadmap instead of pretending it modifies the game.
- [ ] **P1:** Add automated compatibility checks and graceful behavior after Minecraft updates.

**Done when:** There is a demonstrated and documented way to apply at least one intended feature to supported Minecraft installs, or a documented no-go decision.

## Phase 3 — in-game client MVP (conditional on Phase 2) (P0 / P1)

- [ ] **P0:** Integrate one Touch Lab layout with the game **only where technically supported**.
- [ ] **P0:** Expose an understandable on/off and reset path for every working module.
- [x] **P0:** Add an **app-only** module catalog UI with search and categories (wishlist, not in-game).
- [ ] **P0:** Add real in-game module settings and activation, only after a supported integration exists.
- [ ] **P0:** Prevent unsupported modules from appearing as available.
- [ ] **P0:** Make backups and recovery straightforward; never overwrite Minecraft files silently.
- [ ] **P1:** Add user-requested HUD elements (e.g. coordinates, clock or input display) only where supported and allowed.
- [ ] **P1:** Add compatibility/failure diagnostics without exposing account or personal information.
- [ ] **P1:** Add an explicit safe mode and respect each server's rules.
- [ ] **P1:** Benchmark any claimed FPS, thermal, battery or latency improvements on controlled devices; publish methodology and regressions.
- [ ] **P2:** Explore further customization and purely cosmetic improvements after compatibility review.

**Out of scope:** unfair automation, anti-cheat bypasses, account-token collection, hidden APK modification, proprietary Minecraft binaries/assets, fake FPS claims, and paid access gates inside Minecraft without appropriate approval.

**Done when:** Supported in-game features work reliably, clearly advertise compatibility, and degrade safely after an unsupported Minecraft update.

## Phase 4 — shipping quality (P0 / P1)

- [ ] **P0:** Implement crash handling and **opt-in** diagnostic export without private account data.
- [ ] **P0:** Add automated tests for profile migration, unsupported versions, recovery and feature flags.
- [ ] **P0:** Audit Android permissions, dependencies, supply chain and exported components.
- [ ] **P0:** Define supported Android versions and supported Minecraft versions in each release.
- [ ] **P0:** Add a release signing/key-backup procedure; never commit signing secrets.
- [ ] **P0:** Establish a verified download channel, checksums and a consistent changelog.
- [ ] **P1:** Reduce cold-start time, UI jank, memory use and package size using measured results.
- [ ] **P1:** Set up reproducible beta testing and a bug report template.
- [ ] **P1:** Create update compatibility and rollback instructions.

**Done when:** A new user can install, understand and safely update Argon without developer assistance.

## Phase 5 — open-source community (P1)

- [ ] Decide whether Argon is fully open source, source-available or partly proprietary **before** accepting outside contributions.
- [ ] Select and add a suitable `LICENSE` (GPLv3 is one option, not yet a decision).
- [ ] Add `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md` and issue/PR templates.
- [ ] Separate original Argon branding/trademarks from code licensing; document permitted use.
- [ ] Publish a development board and label beginner-friendly issues.
- [ ] Start a moderated Discord/community hub with clear support channels.
- [ ] Document architecture and how to add new supported modules and tests.
- [ ] Make major technical decisions in public GitHub discussions/issues.

## Phase 6 — sustainable funding (P1; only after user value)

**Keep the base client free.** Do not build the roadmap around paid competitive advantages or features that violate Minecraft/platform rules.

- [ ] Publish estimated monthly project costs (hosting, domain, signing/release tools, test devices and other actual expenses).
- [ ] Set up voluntary GitHub Sponsors or a comparable donation option and clearly disclose what support funds.
- [ ] Offer optional **non-gameplay** supporter recognition only after verifying applicable platform rules.
- [ ] Start transparent monthly development and spending updates.
- [ ] Create development tutorials and demo videos; investigate appropriate sponsorships only with disclosure.
- [ ] Consider hosting or other genuinely separate services **only** after proving demand and reviewing compliance.
- [ ] Before any store, subscription, cosmetics or premium features: review Minecraft policies, app-store billing requirements, taxes and applicable consumer rules.
- [ ] Track recurring costs, recurring donations and remaining funding runway; never promise guaranteed future service without resources.

**Done when:** Costs are visible, users can voluntarily support development, and revenue decisions do not undermine fair play or legal distribution.

## Release gates

- **v0.1.x — Companion alpha:** Android foundation tested on devices, stable local profiles, APK and documentation.
- **v0.2 — Touch Lab beta:** Touch editor, module catalog/preferences, presets and accessible mobile UI work **inside Argon**.
- **v0.3 — Integration feasibility:** One real supported integration demonstrated **or** an explicit no-go decision.
- **v0.5 — Client alpha (conditional):** At least one verified in-game feature, safe fallback and compatibility checks.
- **v1.0 — Public stable (conditional):** Reliable integration, honest feature claims, security/release checks, supported-version matrix and real-device QA.

## Rules for future development

1. Do not mark a task complete based on a mock-up, API stub or a passing build alone. Verify the described behavior.
2. Prefer small PRs with tests, screenshots where UI changes, and clear release notes.
3. Keep the project functional without accounts, telemetry or network access unless a feature genuinely needs them and the user opts in.
4. Never bundle or redistribute Minecraft, imply Mojang/Microsoft endorsement, or claim performance gains without measurements.
5. Prioritize **Android-specific usability and compatibility** over copying another client's feature count.
