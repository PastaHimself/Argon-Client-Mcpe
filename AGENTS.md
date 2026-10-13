# AGENTS.md — Argon Client engineering standards

These instructions apply to **all AI coding agents and contributors** working in this repository. Read this file, `README.md`, `TODO.md`, and relevant code before making changes. The goal is a reliable, recognizable Android-first Minecraft Bedrock project — **not a collection of impressive-looking screens with nonfunctional features**.

## 1. Ground truth: what Argon is today

- Argon v0.1.x is a **Kotlin Android companion app**, not a modified Minecraft client. Minecraft is installed independently.
- Playstyle profiles, local settings, import/export, device suggestions, Minecraft launching, and the module **catalog/wishlist** belong to the companion app.
- The catalog does **not** activate Minecraft modules. In-game HUD changes, touch injection, FPS gains, native hooks, and Minecraft integration are **not implemented** unless a specific change proves otherwise.
- Proposed differentiators: **Argon Flow** (explainable device-aware recommendations), **Touch Lab** (mobile-first controls), and a small, dependable module ecosystem.
- `TODO.md` is the implementation checklist; `docs/ROADMAP.md` provides phase gates. Do not skip the Minecraft-integration feasibility gate to make a demo look impressive.
- Current Android configuration: minSdk 26, compileSdk/targetSdk 35, Kotlin/JVM 17. Verify these in `app/build.gradle.kts` before changing tooling.

## 2. Non-negotiable anti-slop rules

1. **Working behavior over feature count.** Finish and verify one thin vertical slice before starting unrelated features.
2. **Never fake functionality.** No decorative toggles presented as active, hard-coded "live" FPS/ping numbers, simulated benchmarks labeled real, dead buttons, or mock backend connections marketed as integrations. Previews must say *preview*; unavailable features must say *planned/unavailable* and remain disabled.
3. **Do not mark TODO boxes complete for scaffolding.** A feature is done only when its stated user-visible behavior works, passes the relevant checks, and limitations are documented.
4. **Avoid speculative architecture.** No giant frameworks, unnecessary interfaces, new dependencies, rewrites, or premature plugin systems. Add abstractions when at least one real use case needs them.
5. **Preserve working code and data.** Prefer small, reviewable changes. Do not silently remove features, rename persisted identifiers, break exported JSON, or replace architecture merely to satisfy a new prompt.
6. **No copy-paste identity.** Use Lunar/Flarial only as product inspiration. Do not clone their branding, proprietary assets, layouts, or code. Keep Argon visibly Android-native and internally consistent.
7. **Be truthful in every report.** Distinguish implemented, locally tested, CI-tested, device-tested, blocked, and proposed. Never claim a build passed or a Minecraft feature works without evidence.
8. **No change solely for output volume.** If the existing code is sufficient, explain why rather than generating unnecessary files.

## 3. Development workflow for every task

1. Read the affected files, tests, current `TODO.md` task, and latest relevant CI failures. Identify the smallest concrete outcome.
2. State the acceptance criteria in plain language before introducing broad changes. For a module, define exactly what the player can observe.
3. Implement that slice end-to-end: UI + state + validation + error handling + persistence as applicable. Do not leave connected parts as pretend implementations.
4. Add or update focused tests, including invalid inputs and regressions. Test migration/backward compatibility when touching saved data or exported JSON.
5. Run the relevant build/tests. The repository's documented command is:
   `gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest`
   Use JDK 17, Android SDK Platform 35, and the documented compatible Gradle version; a wrapper may be added later. If the toolchain is unavailable, state that explicitly and do not claim success.
6. For UI/input changes, validate on an emulator or real Android device when available; check portrait/landscape where relevant, small/large screens, readable labels, touch target sizes, state restoration, and accessibility. CI unit tests **do not** substitute for device testing.
7. Review your own diff for dead code, duplication, unreachable UI, misleading copy, permissions, excessive allocations, and unrelated churn.
8. Update documentation and `TODO.md` **only for verified milestones**. Report what was changed, commands and results, remaining risks, and any manual checks not performed.

## 4. Module engineering contract

Every module must have:
- A stable ID, category, readable purpose, owned settings schema/defaults, and a clear availability state.
- An explicit required capability and **fail-closed** behavior if Minecraft integration, permissions, or compatible versions are absent.
- A real source for any displayed data. For example, an FPS counter must report a measured, correctly identified frame rate — **not** estimated device refresh rate presented as Minecraft FPS.
- Validated settings, persistence where promised, and a reset/default path.
- An explanation of what it changes and whether changes apply **only in Argon** or **inside Minecraft**.
- Tests for disabled/unsupported states and settings behavior. Once in-game support exists, a tested Minecraft/version/device compatibility note and safe failure path.

The **module catalog/wishlist is not the module runtime**. Wishlist selection must not bypass capability gates or claim an in-game effect.

## 5. Android, UI, and performance bar

- Prioritize touchscreen ergonomics: thumb reach, landscape constraints, safe areas/cutouts, touch target size, and comfortable interactions on low-end phones.
- Maintain coherent typography, spacing, naming, interaction feedback, empty/loading/error states, and accessibility labels. Avoid random visual effects, excessive animations, and inconsistent component patterns.
- Keep expensive I/O off the main thread where needed. Avoid avoidable redraws, memory leaks, large bitmap allocations, and background work that hurts battery life.
- Make Argon Flow recommendations explain *which available device signals* caused the suggestion. Recommendations are not benchmarks; users control overrides.
- Do not advertise "FPS boost", "zero lag", or battery/latency improvements without reproducible baseline and post-change measurements on documented devices.
- Favor Android platform APIs and the project's existing lightweight dependencies. Any new dependency needs a concrete justification.

## 6. Safety, privacy, and Minecraft boundaries

- Minecraft ownership and installation remain separate; never bundle Minecraft APKs, files, account tokens, paid assets, or copyrighted game binaries.
- Check Minecraft usage rules, Android/platform rules, and server policies before any game-integration approach. Do not bypass anti-cheat, account protection, DRM, or silently modify the installed game.
- No hidden analytics, credential collection, unnecessary permissions, or network requests. Request permission only when a real feature needs it, explain why, and minimize collected data.
- Use version checks, explicit user control, safe fallbacks, and clear error messages. Unsupported Minecraft versions should disable affected features, not crash or pretend to work.
- Never commit secrets, signing keys, local credentials, or test users' private data.

## 7. Definition of done (all applicable conditions required)

A task is **done** only when:
- [ ] The promised outcome is implemented and observable, not merely mocked.
- [ ] Tests cover core behavior and important failure cases; relevant tests/build actually ran or the inability to run them is clearly reported.
- [ ] No known critical crashes, misleading UI labels, dead controls, or unsupported activation paths were introduced.
- [ ] Saved data and previous behaviors remain compatible, or migration/recovery is implemented.
- [ ] Mobile UX and accessibility were reviewed; device-testing status is reported accurately.
- [ ] Any performance claims have measurements and methodology.
- [ ] Documentation, compatibility labels, and `TODO.md` match verified reality.
- [ ] The diff is focused and explainable, without unrelated rewrites or generated clutter.

If a condition cannot be met, **leave the feature incomplete and state the blocker**. Prefer a smaller genuinely finished feature over a large half-working release.

## 8. Agent handoff / final response format

At the end of each coding task, report:
- **Implemented:** concrete, observable behavior; distinguish app-only from in-game.
- **Verified:** exact commands, tests, CI outcome, and device/version evidence.
- **Not verified / blocked:** specific missing integration, tool, device, or test.
- **Changed files and follow-up:** concise diff summary and next smallest priority from `TODO.md`.

**Ship fewer features, but make every shipped feature real, maintainable, and worth trusting.**
