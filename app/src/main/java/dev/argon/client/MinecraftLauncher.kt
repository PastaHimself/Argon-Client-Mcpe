package dev.argon.client

import android.content.ActivityNotFoundException
import android.content.Context

/**
 * Launches the user's separately installed game only.
 * Does not inject, repackage, patch or control Minecraft.
 */
object MinecraftLauncher {
    private const val PACKAGE = "com.mojang.minecraftpe"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    fun open(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
