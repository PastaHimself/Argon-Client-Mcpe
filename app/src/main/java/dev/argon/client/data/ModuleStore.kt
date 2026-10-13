package dev.argon.client.data

import android.content.Context
import dev.argon.client.core.ModuleCatalog

/**
 * Local wishlist only. This does NOT toggle game features.
 * Kept separate from profile JSON v1 to preserve its schema.
 */
class ModuleStore(context: Context) {
    private val prefs = context.getSharedPreferences("argon_module_wishlist_v1", Context.MODE_PRIVATE)

    fun isDesired(id: String): Boolean {
        require(ModuleCatalog.find(id) != null) { "Unknown module: $id" }
        return prefs.getBoolean("desired_$id", false)
    }

    fun setDesired(id: String, desired: Boolean) {
        require(ModuleCatalog.find(id) != null) { "Unknown module: $id" }
        prefs.edit().putBoolean("desired_$id", desired).apply()
    }
}
