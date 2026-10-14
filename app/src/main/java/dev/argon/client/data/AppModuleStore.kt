package dev.argon.client.data

import android.content.Context
import dev.argon.client.core.AppModuleRegistry

/** Enabled *Argon app* modules, separate from the future Minecraft module wishlist. */
class AppModuleStore(context: Context) {
    private val prefs = context.getSharedPreferences("argon_app_modules_v1", Context.MODE_PRIVATE)

    fun isEnabled(id: String): Boolean {
        require(AppModuleRegistry.isSupported(id)) { "Unsupported app module: $id" }
        return prefs.getBoolean("enabled_$id", false)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        require(AppModuleRegistry.isSupported(id)) { "Unsupported app module: $id" }
        prefs.edit().putBoolean("enabled_$id", enabled).apply()
    }
}
