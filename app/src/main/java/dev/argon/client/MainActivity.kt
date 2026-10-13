package dev.argon.client

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import dev.argon.client.core.ArgonFlow
import dev.argon.client.core.ClientProfile
import dev.argon.client.core.DeviceSnapshot
import dev.argon.client.core.PlayStyle
import dev.argon.client.core.VisualPreference
import dev.argon.client.data.ProfileStore
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

/**
 * Lightweight touch-first UI. No dependencies outside the Android framework.
 * "Preview" is deliberately explicit: it does not pretend to modify Minecraft.
 */
class MainActivity : Activity() {
    private enum class Page { DASHBOARD, PROFILES, ABOUT }

    private val backgroundColor = Color.rgb(10, 16, 27)
    private val panel = Color.rgb(23, 32, 48)
    private val accent = Color.rgb(109, 221, 192)
    private val primary = Color.rgb(239, 245, 250)
    private val muted = Color.rgb(163, 181, 196)
    private val outline = Color.rgb(47, 65, 80)

    private lateinit var store: ProfileStore
    private lateinit var device: DeviceSnapshot
    private var page = Page.DASHBOARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val refreshRate = windowManager.defaultDisplay.refreshRate.roundToInt()
        device = DeviceSnapshot(
            isLowRam = activityManager.isLowRamDevice,
            memoryClassMb = activityManager.memoryClass,
            refreshRateHz = refreshRate,
            batterySaver = powerManager.isPowerSaveMode
        )
        store = ProfileStore(this, device)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) render()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun rounded(fill: Int, stroke: Int? = null, radius: Int = 15): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun space(parent: LinearLayout, height: Int = 12) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun label(
        parent: LinearLayout,
        content: String,
        size: Float = 14f,
        color: Int = primary,
        bold: Boolean = false,
        bottom: Int = 0
    ): TextView {
        val view = TextView(this).apply {
            text = content
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        parent.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottom) })
        return view
    }

    private fun card(parent: LinearLayout, draw: (LinearLayout) -> Unit) {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, outline)
            setPadding(dp(17), dp(17), dp(17), dp(17))
        }
        parent.addView(body, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) })
        draw(body)
    }

    private fun action(
        parent: LinearLayout,
        title: String,
        filled: Boolean = false,
        enabled: Boolean = true,
        handler: () -> Unit
    ) {
        val view = TextView(this).apply {
            text = title
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (filled) backgroundColor else if (enabled) accent else muted)
            background = rounded(if (filled) accent else panel, if (filled) null else outline, 12)
            setPadding(dp(15), dp(13), dp(15), dp(13))
            isClickable = enabled
            isFocusable = enabled
            alpha = if (enabled) 1f else 0.5f
            if (enabled) setOnClickListener { handler() }
        }
        parent.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(9) })
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(21), dp(20), dp(14))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        label(content, "A R G O N", 14f, accent, true, 4)
        label(content, "Built for Bedrock on Android.", 24f, primary, true, 6)
        label(content, "FOUNDATION 0.1  •  LOCAL ONLY", 11f, muted, true, 24)

        when (page) {
            Page.DASHBOARD -> dashboard(content)
            Page.PROFILES -> profiles(content)
            Page.ABOUT -> about(content)
        }
        root.addView(navigation(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(68)
        ))
        setContentView(root)
    }

    private fun dashboard(parent: LinearLayout) {
        label(parent, "Your Android. Your playstyle.", 20f, primary, true, 12)
        card(parent) { body ->
            label(body, "GAME STATUS", 11f, accent, true, 8)
            val installed = MinecraftLauncher.isInstalled(this)
            label(body, if (installed) "Minecraft detected" else "Minecraft not detected", 18f, primary, true, 5)
            label(body, "Argon can open the official game. It cannot change in-game settings yet.", 13f, muted)
            action(body, "Open installed Minecraft", true, installed) {
                if (!MinecraftLauncher.open(this)) {
                    Toast.makeText(this, "Could not open Minecraft", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val style = store.selected()
        val advice = ArgonFlow.recommend(style, device)
        val saved = store.get(style)
        card(parent) { body ->
            label(body, "ARGON FLOW  /  PROFILE PREVIEW", 11f, accent, true, 8)
            label(body, style.title + " • " + saved.visuals.title, 18f, primary, true, 8)
            label(body, "Control scale " + saved.controlScale + "%  |  HUD opacity " + saved.hudOpacity + "%", 13f, muted, false, 12)
            label(body, "Based on your device:", 14f, primary, true, 7)
            advice.reasons.forEach { label(body, "• " + it, 12f, muted, false, 5) }
            action(body, "Edit touch profile") {
                page = Page.PROFILES
                render()
            }
        }
        card(parent) { body ->
            label(body, "DEVICE SIGNALS", 11f, accent, true, 9)
            label(body, "App memory class: " + device.memoryClassMb + " MB", 14f, primary, false, 4)
            label(body, "Display refresh: approximately " + device.refreshRateHz + " Hz", 14f, primary, false, 4)
            label(body, "Low-RAM mode: " + if (device.isLowRam) "Yes" else "No", 14f, primary, false, 4)
            label(body, "Battery saver: " + if (device.batterySaver) "On" else "Off", 14f, primary)
            space(body, 7)
            label(body, "App memory class is not total physical RAM or a game FPS benchmark.", 12f, muted)
        }
    }

    private fun profiles(parent: LinearLayout) {
        label(parent, "Playstyle profiles", 20f, primary, true, 7)
        label(parent, "Tune preferences now; game integration comes later.", 13f, muted, false, 15)
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        parent.addView(choices, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        ))
        val selected = store.selected()
        PlayStyle.entries.forEach { style ->
            val active = selected == style
            val view = TextView(this).apply {
                text = style.title
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (active) backgroundColor else primary)
                gravity = Gravity.CENTER
                background = rounded(if (active) accent else panel, if (active) null else outline, 12)
                isClickable = true
                isFocusable = true
                setOnClickListener { store.select(style); render() }
            }
            choices.addView(view, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
            })
        }
        space(parent, 16)
        val current = store.get(selected)
        card(parent) { body ->
            label(body, current.style.detail, 16f, primary, true, 8)
            label(body, "Not applied to Minecraft — interface preview only.", 12f, muted, false, 15)
            slider(body, "Suggested control size", 70, 140, current.controlScale) { value ->
                store.save(store.get(selected).copy(controlScale = value))
            }
            space(body, 13)
            slider(body, "Suggested HUD opacity", 20, 100, current.hudOpacity) { value ->
                store.save(store.get(selected).copy(hudOpacity = value))
            }
            space(body, 16)
            label(body, "Visual preference", 13f, primary, true)
            action(body, current.visuals.title + "  ↻") {
                val values = VisualPreference.entries
                val next = values[(values.indexOf(store.get(selected).visuals) + 1) % values.size]
                store.save(store.get(selected).copy(visuals = next))
                render()
            }
            action(body, "Restore device recommendation") {
                store.reset(selected)
                render()
            }
        }
        card(parent) { body ->
            label(body, "PORTABLE OFFLINE PROFILES", 11f, accent, true, 7)
            label(body, "Share all three profiles as JSON. No cloud account or tracking.", 13f, muted)
            action(body, "Export profiles as JSON") { exportProfiles() }
            action(body, "Import profiles from JSON") { importProfiles() }
        }
    }

    private fun slider(
        parent: LinearLayout,
        title: String,
        min: Int,
        max: Int,
        selected: Int,
        onSave: (Int) -> Unit
    ) {
        val readout = label(parent, title + ": " + selected + "%", 13f, primary, true, 4)
        val control = SeekBar(this).apply {
            this.max = max - min
            progress = selected - min
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(accent)
        }
        parent.addView(control, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(38)
        ))
        control.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                readout.text = title + ": " + (min + progress) + "%"
            }
            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = onSave(min + bar.progress)
        })
    }

    private fun about(parent: LinearLayout) {
        label(parent, "An Android-first direction", 20f, primary, true, 12)
        card(parent) { body ->
            label(body, "WHY ARGON EXISTS", 11f, accent, true, 8)
            label(body, "Bedrock clients already have plenty of toggles. Argon is being designed around comfortable touch controls, explainable hardware-aware suggestions, and a lightweight offline experience.", 14f, primary)
            space(body, 12)
            label(body, "Next: Touch Lab for thumb reach calibration, better previews and a tested per-version game integration.", 13f, muted)
        }
        card(parent) { body ->
            label(body, "WHAT THIS BUILD DOES NOT DO", 11f, accent, true, 9)
            label(body, "No Minecraft patching, injection, game overlays, FPS boosts, cheats, game file access or account collection. Profiles only change preferences stored by Argon.", 13f, primary)
            space(body, 10)
            label(body, "Argon is unofficial and not affiliated with Mojang or Microsoft.", 12f, muted)
        }
    }

    private fun navigation(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(panel)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val labels = listOf(
            Page.DASHBOARD to "Overview",
            Page.PROFILES to "Profiles",
            Page.ABOUT to "About"
        )
        labels.forEach { (target, text) ->
            val selected = target == page
            val tab = TextView(this).apply {
                this.text = text
                textSize = 14f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (selected) accent else muted)
                background = if (selected) rounded(backgroundColor) else null
                isClickable = true
                isFocusable = true
                setOnClickListener { page = target; render() }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, dp(50), 1f))
        }
        return nav
    }

    private fun exportProfiles() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "argon-profiles.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private fun importProfiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    @Deprecated("Using the framework activity result API to avoid third-party runtime dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            when (requestCode) {
                REQUEST_EXPORT -> {
                    val output = contentResolver.openOutputStream(uri)
                        ?: throw IOException("Cannot write selected document")
                    output.bufferedWriter(StandardCharsets.UTF_8).use { it.write(store.exportJson()) }
                    Toast.makeText(this, "Profiles exported", Toast.LENGTH_SHORT).show()
                }
                REQUEST_IMPORT -> {
                    val input = contentResolver.openInputStream(uri)
                        ?: throw IOException("Cannot read selected document")
                    val bytes = input.use { stream ->
                        val buffer = ByteArray(4096)
                        val out = java.io.ByteArrayOutputStream()
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            if (out.size() + count > 64_000) throw IOException("Profile file exceeds 64 KB")
                            out.write(buffer, 0, count)
                        }
                        out.toByteArray()
                    }
                    store.importJson(String(bytes, StandardCharsets.UTF_8))
                    Toast.makeText(this, "Profiles imported", Toast.LENGTH_SHORT).show()
                    render()
                }
            }
        } catch (error: Exception) {
            Toast.makeText(this, "Profile operation failed: " + (error.message ?: "unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val REQUEST_EXPORT = 1001
        const val REQUEST_IMPORT = 1002
    }
}
