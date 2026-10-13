package dev.argon.client.data

import android.content.Context
import dev.argon.client.core.ArgonFlow
import dev.argon.client.core.ClientProfile
import dev.argon.client.core.DeviceSnapshot
import dev.argon.client.core.PlayStyle
import dev.argon.client.core.VisualPreference
import org.json.JSONObject

/**
 * All data stays in Android-private SharedPreferences. JSON uses a versioned,
 * validated and intentionally small format. No account or network dependency.
 */
class ProfileStore(context: Context, private val device: DeviceSnapshot) {
    private val prefs = context.getSharedPreferences("argon_profiles_v1", Context.MODE_PRIVATE)

    fun selected(): PlayStyle = runCatching {
        PlayStyle.valueOf(prefs.getString("selected", PlayStyle.PVP.name) ?: PlayStyle.PVP.name)
    }.getOrDefault(PlayStyle.PVP)

    fun select(style: PlayStyle) {
        prefs.edit().putString("selected", style.name).apply()
    }

    fun get(style: PlayStyle): ClientProfile {
        val proposed = ArgonFlow.recommend(style, device).profile
        val visual = runCatching {
            VisualPreference.valueOf(
                prefs.getString(style.name + "_visual", proposed.visuals.name) ?: proposed.visuals.name
            )
        }.getOrDefault(proposed.visuals)
        return runCatching {
            ClientProfile(
                style,
                prefs.getInt(style.name + "_scale", proposed.controlScale),
                prefs.getInt(style.name + "_opacity", proposed.hudOpacity),
                visual
            ).validated()
        }.getOrDefault(proposed)
    }

    fun save(profile: ClientProfile) {
        profile.validated()
        prefs.edit()
            .putInt(profile.style.name + "_scale", profile.controlScale)
            .putInt(profile.style.name + "_opacity", profile.hudOpacity)
            .putString(profile.style.name + "_visual", profile.visuals.name)
            .apply()
    }

    fun reset(style: PlayStyle) {
        prefs.edit()
            .remove(style.name + "_scale")
            .remove(style.name + "_opacity")
            .remove(style.name + "_visual")
            .apply()
    }

    fun exportJson(): String {
        val profiles = JSONObject()
        for (style in PlayStyle.entries) {
            val p = get(style)
            profiles.put(style.name, JSONObject().apply {
                put("controlScale", p.controlScale)
                put("hudOpacity", p.hudOpacity)
                put("visuals", p.visuals.name)
            })
        }
        return JSONObject().apply {
            put("schema", 1)
            put("selected", selected().name)
            put("profiles", profiles)
        }.toString(2)
    }

    /** Parse and validate the entire document before modifying local storage. */
    fun importJson(input: String) {
        require(input.length <= 64_000) { "Profile file is too large" }
        val root = JSONObject(input)
        require(root.getInt("schema") == 1) { "Unsupported profile schema" }
        val newSelected = PlayStyle.valueOf(root.getString("selected"))
        val profiles = root.getJSONObject("profiles")
        val parsed = PlayStyle.entries.map { style ->
            val obj = profiles.getJSONObject(style.name)
            ClientProfile(
                style,
                obj.getInt("controlScale"),
                obj.getInt("hudOpacity"),
                VisualPreference.valueOf(obj.getString("visuals"))
            ).validated()
        }
        val editor = prefs.edit().clear().putString("selected", newSelected.name)
        parsed.forEach { profile ->
            editor.putInt(profile.style.name + "_scale", profile.controlScale)
                .putInt(profile.style.name + "_opacity", profile.hudOpacity)
                .putString(profile.style.name + "_visual", profile.visuals.name)
        }
        check(editor.commit()) { "Could not persist imported profiles" }
    }
}
