package dev.argon.client.core

/**
 * The catalog is a PRODUCT ROADMAP, not a set of active Minecraft modifications.
 * NoGameIntegration deliberately keeps every module unavailable.
 */
enum class ModuleCategory(val title: String) {
    HUD("HUD"),
    VISUAL("Visual"),
    CONTROLS("Controls"),
    UTILITY("Utility")
}

enum class GameCapability {
    HUD_DRAW,
    GAME_METRICS,
    INPUT_EVENTS,
    GAME_STATE,
    GAME_NETWORK_STATS,
    CAMERA_CONTROL,
    VISUAL_SETTINGS
}

data class ModuleSpec(
    val id: String,
    val title: String,
    val description: String,
    val category: ModuleCategory,
    val needs: Set<GameCapability>
) {
    init {
        require(Regex("[a-z][a-z0-9_]*").matches(id)) { "Invalid module ID" }
        require(title.isNotBlank() && description.isNotBlank()) { "Module metadata is incomplete" }
        require(needs.isNotEmpty()) { "Modules must require explicit capabilities" }
    }
}

enum class ModuleStatus(val label: String) {
    UNAVAILABLE("Unavailable (not integrated)"),
    AVAILABLE("Integration reports support")
}

interface GameIntegration {
    val readyAndVerified: Boolean
    val capabilities: Set<GameCapability>
}

object NoGameIntegration : GameIntegration {
    override val readyAndVerified: Boolean = false
    override val capabilities: Set<GameCapability> = emptySet()
}

/**
 * This only reports readiness; it never mutates a game.
 * Future bridges must separately implement and test any runtime behavior.
 */
object ModuleGate {
    fun status(spec: ModuleSpec, bridge: GameIntegration): ModuleStatus =
        if (bridge.readyAndVerified && bridge.capabilities.containsAll(spec.needs)) {
            ModuleStatus.AVAILABLE
        } else {
            ModuleStatus.UNAVAILABLE
        }
}

object ModuleCatalog {
    val all: List<ModuleSpec> = listOf(
        ModuleSpec("fps_counter", "FPS Counter", "Display actual in-game frame rate when a supported metrics API exists.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_METRICS)),
        ModuleSpec("cps_counter", "CPS Counter", "Show touch/click input rate with accurate, permitted input events.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.INPUT_EVENTS)),
        ModuleSpec("coordinates", "Coordinates", "Display current game coordinates in a configurable HUD.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_STATE)),
        ModuleSpec("keystrokes", "Input Display", "Visualize permitted movement and action inputs on screen.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.INPUT_EVENTS)),
        ModuleSpec("armor_hud", "Armor HUD", "Keep track of equipped armor in a small HUD element.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_STATE)),
        ModuleSpec("status_effects", "Status Effects", "Show active game effects with accessible labels.", ModuleCategory.HUD, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_STATE)),
        ModuleSpec("ping_display", "Ping Display", "Show actual server latency only if a reliable game network metric is accessible.", ModuleCategory.UTILITY, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_NETWORK_STATS)),
        ModuleSpec("zoom", "Zoom", "Adjust the game camera view where compatible and server-permitted.", ModuleCategory.VISUAL, setOf(GameCapability.CAMERA_CONTROL)),
        ModuleSpec("minimal_hud", "Minimal HUD", "Reduce HUD clutter for smaller touchscreens.", ModuleCategory.VISUAL, setOf(GameCapability.HUD_DRAW)),
        ModuleSpec("touch_visualizer", "Touch Visualizer", "Display touch input feedback as an accessibility aid.", ModuleCategory.CONTROLS, setOf(GameCapability.HUD_DRAW, GameCapability.INPUT_EVENTS)),
        ModuleSpec("daylight_clock", "World Clock", "Display in-world time where the game exposes it.", ModuleCategory.UTILITY, setOf(GameCapability.HUD_DRAW, GameCapability.GAME_STATE)),
        ModuleSpec("brightness", "Brightness Preference", "Expose permitted visual brightness adjustments.", ModuleCategory.VISUAL, setOf(GameCapability.VISUAL_SETTINGS))
    )

    private val byId = all.associateBy { it.id }

    init {
        require(byId.size == all.size) { "Duplicate module IDs" }
    }

    fun find(id: String): ModuleSpec? = byId[id]

    fun search(query: String, category: ModuleCategory? = null): List<ModuleSpec> {
        val term = query.trim()
        return all.filter { spec ->
            (category == null || spec.category == category) &&
                (term.isEmpty() || spec.title.contains(term, ignoreCase = true) ||
                    spec.description.contains(term, ignoreCase = true))
        }
    }
}
