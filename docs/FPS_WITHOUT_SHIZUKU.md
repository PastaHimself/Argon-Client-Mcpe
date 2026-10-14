# Minecraft FPS counter without Shizuku

## Supported path

Argon has a legitimate, non-privileged way to help players obtain a working
Minecraft Bedrock FPS counter: **use a Minecraft resource pack** that measures
in-game frame timing, rather than trying to read frames from another Android
process. Resource packs run inside Minecraft and require **no root, Shizuku,
wireless debugging, accessibility access, or screen recording**.

Microsoft documents `query.average_frame_time` as the average frame duration,
in seconds, across prior rendered frames. Recent community packs use built-in
Bedrock/Molang render information for dynamic in-game FPS displays.

Documentation: https://learn.microsoft.com/minecraft/creator/reference/content/molangreference/examples/molangconcepts/queryfunctions/query_average_frame_time?view=minecraft-bedrock-stable

Argon links to **FPS DISPLAY**, an external resource pack made by PrabhatPie
and distributed with an MIT license on CurseForge:
https://www.curseforge.com/minecraft-bedrock/texture-packs/fps-display

Argon does **not** copy, bundle, download, patch or silently enable this pack.
Review the pack's compatibility, license and creator's instructions yourself.

## How to use

1. Open Argon and select **Get Minecraft FPS resource pack** on Overview, the
   planned FPS module card, or the floating menu.
2. The external browser opens the author's CurseForge page. Download a
   compatible `.mcpack` from that site and open it with Minecraft.
3. Wait for Minecraft to finish importing the resource pack.
4. In Minecraft, go to **Settings → Global Resources** (or a world's Resource
   Packs settings). Activate the imported FPS pack and move it above conflicting
   HUD packs if necessary. Restart the game if it does not appear.
5. Launch Minecraft. The counter, when it works, is drawn by Minecraft's
   resource pack and does **not** depend on Argon's Android system overlay.

## Exact limitations

- **Argon's A menu cannot toggle the imported resource pack.** The user
  enables/disables it through Minecraft's own Resource Pack settings. The
  overlay has a link and instructions, not a fake enable switch.
- Argon cannot read the pack's number into the floating system overlay.
  Minecraft renders it independently, and the output is not exposed through
  the Android app process.
- Visual/HUD resource packs may conflict, and compatibility may change across
  Minecraft versions, worlds, Realms or managed servers.
- Not all FPS resource packs measure the same thing. `query.average_frame_time`
  is a rendering-frame metric, whereas Android `Display.refreshRate` is only
  the panel's nominal refresh rate; Argon never uses it as FPS.
- Prior Shizuku/SurfaceFlinger prototype files, libraries and permission were
  removed from this PR because the user does not want Shizuku.
- No screen capture, media projection, untrusted download, root, DLL injector,
  unverified FPS data or Minecraft APK modifications are introduced.

## Manual verification checklist

- Confirm that the external project page opens from Overview and Modules.
- Install the pack manually on an Android phone and confirm the displayed value
  changes between high-motion and low-motion gameplay.
- Verify the pack counter is present even when Argon's overlay is stopped.
- Confirm that deleting/disabling the resource pack in Minecraft removes the
  counter and does not affect Argon preferences.
- Confirm the floating button's external-link fallback works if Android blocks
  background activity starts.
- Check whether fullscreen HUD packs conflict; do not treat a successful
  Android app build as proof of resource-pack compatibility.
