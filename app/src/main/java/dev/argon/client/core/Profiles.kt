package dev.argon.client.core

enum class PlayStyle(val title: String, val detail: String) {
    PVP("PvP", "Responsive and uncluttered"),
    BUILD("Building", "More room to see your work"),
    EXPLORE("Exploration", "Balanced for longer sessions")
}

enum class VisualPreference(val title: String) {
    BATTERY("Battery first"),
    BALANCED("Balanced"),
    QUALITY("Visual quality")
}

/**
 * Stored preferences for a future supported Minecraft integration.
 * Changing these values does NOT change the Minecraft game today.
 */
data class ClientProfile(
    val style: PlayStyle,
    val controlScale: Int,
    val hudOpacity: Int,
    val visuals: VisualPreference
) {
    fun validated(): ClientProfile {
        require(controlScale in 70..140) { "Control scale must be 70..140" }
        require(hudOpacity in 20..100) { "HUD opacity must be 20..100" }
        return this
    }
}

data class DeviceSnapshot(
    val isLowRam: Boolean,
    val memoryClassMb: Int,
    val refreshRateHz: Int,
    val batterySaver: Boolean
)

data class Advice(
    val profile: ClientProfile,
    val reasons: List<String>
)

/** Deterministic, offline guidance; never claims to benchmark or modify Minecraft. */
object ArgonFlow {
    fun recommend(style: PlayStyle, device: DeviceSnapshot): Advice {
        val visuals = when {
            device.batterySaver || device.isLowRam || device.memoryClassMb < 192 ->
                VisualPreference.BATTERY
            device.memoryClassMb >= 256 && device.refreshRateHz >= 90 ->
                VisualPreference.QUALITY
            else -> VisualPreference.BALANCED
        }
        val scale = when (style) {
            PlayStyle.PVP -> 112
            PlayStyle.BUILD -> 96
            PlayStyle.EXPLORE -> 104
        }
        val opacity = when (style) {
            PlayStyle.PVP -> 92
            PlayStyle.BUILD -> 70
            PlayStyle.EXPLORE -> 80
        }
        val reasons = mutableListOf(
            when (style) {
                PlayStyle.PVP -> "Larger proposed touch targets for fast inputs."
                PlayStyle.BUILD -> "Smaller proposed controls leave more screen space."
                PlayStyle.EXPLORE -> "Moderate proposed controls for longer sessions."
            }
        )
        when {
            device.batterySaver -> reasons.add("Android battery saver is active: prefer lighter visuals.")
            device.isLowRam -> reasons.add("Android reports a low-RAM device: prefer lighter visuals.")
            device.memoryClassMb < 192 -> reasons.add("Limited app memory class: prefer lighter visuals.")
            visuals == VisualPreference.QUALITY ->
                reasons.add("Higher app memory class and refresh rate: quality may be practical.")
            else -> reasons.add("Conservative default when device capacity is uncertain.")
        }
        reasons.add("Suggestions are previews only; Minecraft settings are unchanged.")
        return Advice(ClientProfile(style, scale, opacity, visuals).validated(), reasons)
    }
}
