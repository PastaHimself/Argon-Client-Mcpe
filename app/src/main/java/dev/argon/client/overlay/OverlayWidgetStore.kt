package dev.argon.client.overlay

import android.content.Context

/** Cross-app overlay settings are separate from app-only modules and Minecraft wishlist. */
class OverlayWidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("argon_overlay_widgets_v1", Context.MODE_PRIVATE)

    fun enabled(id: String): Boolean {
        require(OverlayWidgetRegistry.supports(id)) { "Unsupported overlay widget: $id" }
        return prefs.getBoolean("enabled_$id", false)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        require(OverlayWidgetRegistry.supports(id)) { "Unsupported overlay widget: $id" }
        prefs.edit().putBoolean("enabled_$id", enabled).apply()
    }
}
