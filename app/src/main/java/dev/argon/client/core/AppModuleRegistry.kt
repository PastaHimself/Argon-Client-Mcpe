package dev.argon.client.core

/**
 * Modules with real, observable behavior inside Argon's Android window.
 * This is intentionally separate from ModuleCatalog's unavailable in-game modules.
 */
data class AppModuleSpec(
    val id: String,
    val title: String,
    val description: String
)

object AppModuleRegistry {
    const val UI_FPS = "argon_ui_fps"
    const val DEVICE_STATUS = "argon_device_status"

    val all = listOf(
        AppModuleSpec(UI_FPS, "App FPS Counter", "Shows measured frame rate of Argon's own window."),
        AppModuleSpec(DEVICE_STATUS, "Device Status", "Shows Android app memory class and battery-saver state.")
    )

    init {
        require(all.map { it.id }.toSet().size == all.size)
    }

    fun find(id: String): AppModuleSpec? = all.firstOrNull { it.id == id }
    fun isSupported(id: String): Boolean = find(id) != null
}
