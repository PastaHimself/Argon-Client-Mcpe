package dev.argon.client.overlay

/**
 * Allowed cross-app UI widgets. These use Android-owned data, not Minecraft internals.
 * FPS and other Minecraft telemetry remain outside this allowlist.
 */
data class OverlayWidget(val id: String, val title: String, val description: String)

object OverlayWidgetRegistry {
    const val CLOCK = "system_clock"
    const val DEVICE = "android_device_status"
    val supported = listOf(
        OverlayWidget(CLOCK, "Clock", "Android local time, not Minecraft world time."),
        OverlayWidget(DEVICE, "Device Status", "Android app memory class and battery saver.")
    )
    fun supports(id: String): Boolean = supported.any { it.id == id }
}

