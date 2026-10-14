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
