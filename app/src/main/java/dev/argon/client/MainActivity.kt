package dev.argon.client

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.HorizontalScrollView
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
import dev.argon.client.core.AppModuleRegistry
import dev.argon.client.core.ModuleCatalog
import dev.argon.client.core.ModuleCategory
import dev.argon.client.core.ModuleGate
import dev.argon.client.core.NoGameIntegration
import dev.argon.client.data.ModuleStore
import dev.argon.client.data.AppModuleStore
import dev.argon.client.performance.UiFrameRateMonitor
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight touch-first UI. No dependencies outside the Android framework.
 * "Preview" is deliberately explicit: it does not pretend to modify Minecraft.
 */
class MainActivity : Activity() {
    private enum class Page { DASHBOARD, PROFILES, MODULES, ABOUT }

    // A restrained, high-contrast palette shared by every screen.
    private val backgroundColor = Color.rgb(11, 16, 22)
    private val panel = Color.rgb(21, 29, 38)
    private val elevated = Color.rgb(29, 39, 49)
    private val accent = Color.rgb(168, 235, 200)
    private val primary = Color.rgb(245, 248, 249)
    private val muted = Color.rgb(171, 185, 195)
    private val outline = Color.rgb(48, 63, 75)

    private lateinit var store: ProfileStore
    private lateinit var moduleStore: ModuleStore
    private lateinit var appModuleStore: AppModuleStore
    private var modulePanelOpen = false
    private var bubbleXFraction: Float? = null
    private var bubbleYFraction: Float? = null
    private var foreground = false
    private var lastUiFps: Double? = null
    private var floatingFpsReadout: TextView? = null
    private var moduleQuery: String = ""
    private var moduleFilter: ModuleCategory? = null
    private lateinit var device: DeviceSnapshot
    private lateinit var fpsMonitor: UiFrameRateMonitor
    private var fpsReadout: TextView? = null
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
        moduleStore = ModuleStore(this)
        appModuleStore = AppModuleStore(this)
        fpsMonitor = UiFrameRateMonitor(window) { measured ->
            lastUiFps = measured
            updateFpsLabels()
        }
        savedInstanceState?.getString("argon_page")?.let { saved ->
            page = Page.entries.firstOrNull { it.name == saved } ?: Page.DASHBOARD
        }
        modulePanelOpen = savedInstanceState?.getBoolean("module_panel_open") ?: false
        if (savedInstanceState?.containsKey("bubble_x_fraction") == true) {
            bubbleXFraction = savedInstanceState.getFloat("bubble_x_fraction")
            bubbleYFraction = savedInstanceState.getFloat("bubble_y_fraction")
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("argon_page", page.name)
        outState.putBoolean("module_panel_open", modulePanelOpen)
        bubbleXFraction?.let {
            outState.putFloat("bubble_x_fraction", it)
            outState.putFloat("bubble_y_fraction", bubbleYFraction ?: 0.5f)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        if (appModuleStore.isEnabled(AppModuleRegistry.UI_FPS)) fpsMonitor.start()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) render()
    }

    override fun onStop() {
        foreground = false
        fpsMonitor.stop()
        lastUiFps = null
        updateFpsLabels()
        super.onStop()
    }

    private fun updateFpsLabels() {
        val rate = lastUiFps
        val value = if (rate == null) "—" else "${rate.roundToInt()} FPS"
        val accessible = if (rate == null) "Argon app FPS unavailable while idle" else
            "Argon app ${rate.roundToInt()} frames per second, not Minecraft FPS"
        fpsReadout?.apply {
            if (text.toString() != value) text = value
            contentDescription = accessible
        }
        floatingFpsReadout?.apply {
            if (text.toString() != "APP  $value") text = "APP  $value"
            contentDescription = accessible
        }
    }

    private fun setAppModuleEnabled(id: String, enabled: Boolean) {
        appModuleStore.setEnabled(id, enabled)
        if (id == AppModuleRegistry.UI_FPS) {
            lastUiFps = null
            if (enabled && foreground) fpsMonitor.start() else fpsMonitor.stop()
        }
        render()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun rounded(fill: Int, stroke: Int? = null, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (stroke != null) setStroke(dp(1), stroke)
        }

    /** Native ripple feedback with a clipped touch shape, no extra UI dependency. */
    private fun touchBackground(fill: Int, stroke: Int? = null, radius: Int = 16): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.argb(46, 255, 255, 255)),
            rounded(fill, stroke, radius),
            rounded(Color.WHITE, radius = radius)
        )

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
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
            background = rounded(panel, outline, 20)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        parent.addView(body, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) })
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
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (filled) backgroundColor else if (enabled) primary else muted)
            background = touchBackground(if (filled) accent else elevated, if (filled) null else outline, 14)
            minHeight = dp(50)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            isEnabled = enabled
            isClickable = enabled
            isFocusable = enabled
            alpha = if (enabled) 1f else 0.48f
            if (enabled) setOnClickListener { handler() }
        }
        parent.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(9) })
    }

    private fun render() {
        // Old views must not be retained when switching pages or recreating the root.
        fpsReadout = null
        floatingFpsReadout = null
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
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

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val monogram = TextView(this).apply {
            text = "A"
            textSize = 19f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(accent)
            background = rounded(elevated, outline, 13)
            contentDescription = "Argon"
        }
        identity.addView(monogram, LinearLayout.LayoutParams(dp(42), dp(42)))
        val wordmark = TextView(this).apply {
            text = "ARGON"
            textSize = 15f
            letterSpacing = 0.17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(dp(12), 0, 0, 0)
        }
        identity.addView(wordmark, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val version = TextView(this).apply {
            text = "COMPANION  0.1"
            textSize = 10f
            letterSpacing = 0.05f
            setTextColor(muted)
            gravity = Gravity.CENTER
            background = rounded(panel, outline, 12)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        identity.addView(version)
        content.addView(identity)
        space(content, 28)
        val (headline, subtitle) = when (page) {
            Page.DASHBOARD -> "Your Bedrock space." to "A simpler way to prepare your Android playstyle."
            Page.PROFILES -> "Play your way." to "Shape your local presets. Minecraft stays unchanged."
            Page.MODULES -> "The wishlist." to "Explore future modules and save what matters."
            Page.ABOUT -> "Made for mobile." to "Thoughtful tools, honest capabilities."
        }
        label(content, headline, 28f, primary, true, 8)
        label(content, subtitle, 14f, muted, false, 24)

        when (page) {
            Page.DASHBOARD -> dashboard(content)
            Page.PROFILES -> profiles(content)
            Page.MODULES -> modules(content)
            Page.ABOUT -> about(content)
        }
        root.addView(View(this).apply { setBackgroundColor(outline) }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ))
        root.addView(navigation(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(72)
        ))
        val layer = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        layer.addView(root, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        attachFloatingControls(layer)
        setContentView(layer)
        updateFpsLabels()
    }

    private fun dashboard(parent: LinearLayout) {
        label(parent, "YOUR WORKSPACE", 11f, accent, true, 14)
        card(parent) { body ->
            label(body, "GAME CONNECTION", 11f, accent, true, 11)
            val installed = MinecraftLauncher.isInstalled(this)
            label(body, if (installed) "Minecraft is installed" else "Minecraft is not installed", 19f, primary, true, 7)
            label(body, "Argon can open the official game. It cannot change in-game settings yet.", 13f, muted)
            action(body, if (installed) "Launch Minecraft  ↗" else "Launch unavailable", true, installed) {
                if (!MinecraftLauncher.open(this)) {
                    Toast.makeText(this, "Could not open Minecraft", Toast.LENGTH_SHORT).show()
                }
            }
        }
        card(parent) { body ->
            label(body, "APP FPS  ·  OPTIONAL MODULE", 11f, accent, true, 11)
            if (appModuleStore.isEnabled(AppModuleRegistry.UI_FPS)) {
                fpsReadout = label(body, "—", 30f, primary, true, 7)
                label(body, "Measured from Argon's own rendered frames. Idle windows show a dash. Not Minecraft FPS.", 12f, muted)
            } else {
                label(body, "Off", 22f, primary, true, 7)
                label(body, "Open the floating A icon to enable the app FPS counter.", 12f, muted)
            }
        }
        val style = store.selected()
        val advice = ArgonFlow.recommend(style, device)
        val saved = store.get(style)
        card(parent) { body ->
            label(body, "ARGON FLOW  ·  LOCAL PREVIEW", 11f, accent, true, 11)
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
            label(body, "MODULE WISHLIST", 11f, accent, true, 11)
            val savedCount = ModuleCatalog.all.count { moduleStore.isDesired(it.id) }
            label(body, ModuleCatalog.all.size.toString() + " planned • " + savedCount + " in wishlist", 18f, primary, true, 7)
            label(body, "No modules run in Minecraft yet. Build your wishlist while integration is researched.", 13f, muted)
            action(body, "Browse modules") { page = Page.MODULES; render() }
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
        label(parent, "SELECT A PLAYSTYLE", 11f, accent, true, 14)
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        parent.addView(choices, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ))
        val selected = store.selected()
        PlayStyle.entries.forEach { style ->
            val active = selected == style
            val view = TextView(this).apply {
                text = style.title
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(if (active) backgroundColor else primary)
                gravity = Gravity.CENTER
                background = touchBackground(if (active) accent else panel, if (active) null else outline, 14)
                isClickable = true
                isFocusable = true
                contentDescription = "${style.title} profile, ${if (active) "selected" else "not selected"}"
                setOnClickListener { store.select(style); render() }
            }
            choices.addView(view, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
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


    private fun modules(parent: LinearLayout) {
        label(parent, "NOT ACTIVE IN MINECRAFT", 11f, accent, true, 8)
        label(parent, "Save a wishlist of proposed modules. None can change the game yet.", 13f, muted, false, 17)
        card(parent) { body ->
            label(body, "MODULE INTEGRATION", 11f, accent, true, 6)
            label(body, "0 active  •  ${ModuleCatalog.all.size} planned", 17f, primary, true, 6)
            label(body, "Official Minecraft is launched separately. An in-game integration has not been implemented or verified.", 12f, muted)
        }

        val search = EditText(this).apply {
            hint = "Search modules"
            setSingleLine(true)
            textSize = 14f
            setTextColor(primary)
            setHintTextColor(muted)
            background = rounded(elevated, outline, 14)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            contentDescription = "Search planned modules"
            setText(moduleQuery)
        }
        parent.addView(search, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ))
        space(parent, 10)
        val categories = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categories.addView(chips)
        parent.addView(categories, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ))
        val filters: List<Pair<ModuleCategory?, String>> =
            listOf<Pair<ModuleCategory?, String>>(null to "All") +
                ModuleCategory.entries.map { it to it.title }
        filters.forEach { (category, name) ->
            val active = moduleFilter == category
            val chip = TextView(this).apply {
                text = name
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (active) backgroundColor else primary)
                background = touchBackground(if (active) accent else panel, if (active) null else outline, 13)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                isClickable = true
                isFocusable = true
                contentDescription = "${name} category, ${if (active) "selected" else "not selected"}"
                setOnClickListener { moduleFilter = category; render() }
            }
            chips.addView(chip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)
            ).apply { rightMargin = dp(8) })
        }
        space(parent, 13)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        parent.addView(results)
        renderModuleCards(results)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                moduleQuery = s?.toString() ?: ""
                renderModuleCards(results)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun renderModuleCards(target: LinearLayout) {
        target.removeAllViews()
        val matching = ModuleCatalog.search(moduleQuery, moduleFilter)
        if (matching.isEmpty()) {
            label(target, "No matching modules.", 14f, muted)
            return
        }
        matching.forEach { module ->
            card(target) { body ->
                label(body, module.category.title.uppercase() + "  /  PLANNED", 11f, accent, true, 7)
                label(body, module.title, 18f, primary, true, 5)
                label(body, module.description, 13f, muted, false, 8)
                val status = ModuleGate.status(module, NoGameIntegration)
                label(body, "In-game status: " + status.label, 12f, muted, true)
                action(body, if (moduleStore.isDesired(module.id)) "Remove from wishlist" else "Add to wishlist") {
                    moduleStore.setDesired(module.id, !moduleStore.isDesired(module.id))
                    renderModuleCards(target)
                }
            }
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
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
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
        label(parent, "THE ARGON APPROACH", 11f, accent, true, 14)
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
            setBackgroundColor(backgroundColor)
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        val tabs = listOf(
            Page.DASHBOARD to "Overview",
            Page.PROFILES to "Profiles",
            Page.MODULES to "Modules",
            Page.ABOUT to "About"
        )
        tabs.forEach { (target, title) ->
            val selected = target == page
            val tab = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = touchBackground(if (selected) elevated else backgroundColor, radius = 14)
                isClickable = true
                isFocusable = true
                contentDescription = "$title tab, ${if (selected) "selected" else "not selected"}"
                setOnClickListener {
                    if (page != target) {
                        page = target
                        modulePanelOpen = false
                        render()
                    }
                }
            }
            val indicator = View(this).apply {
                background = rounded(if (selected) accent else Color.TRANSPARENT, radius = 2)
            }
            tab.addView(indicator, LinearLayout.LayoutParams(dp(24), dp(3)).apply {
                bottomMargin = dp(8)
            })
            val caption = TextView(this).apply {
                text = title
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setTextColor(if (selected) primary else muted)
            }
            tab.addView(caption)
            nav.addView(tab, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = dp(2)
                rightMargin = dp(2)
            })
        }
        return nav
    }

    /** In-app floating controls only: no system overlay or Minecraft integration. */
    private fun attachFloatingControls(layer: FrameLayout) {
        val hudOffset = dp(94)
        if (appModuleStore.isEnabled(AppModuleRegistry.UI_FPS)) {
            floatingFpsReadout = floatingPill(layer, "APP  —", hudOffset).apply {
                contentDescription = "Argon app FPS unavailable while idle"
            }
        }
        if (appModuleStore.isEnabled(AppModuleRegistry.DEVICE_STATUS)) {
            val saver = if (device.batterySaver) "Saver on" else "Saver off"
            floatingPill(layer, "APP MEMORY  ${device.memoryClassMb} MB  ·  $saver",
                hudOffset + dp(48)).apply {
                contentDescription = "Android app memory class ${device.memoryClassMb} megabytes, battery saver $saver"
            }
        }

        val bubble = TextView(this).apply {
            text = "A"
            textSize = 21f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(backgroundColor)
            background = touchBackground(accent, radius = 28)
            elevation = dp(8).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Open Argon floating module controls"
            setOnClickListener {
                modulePanelOpen = !modulePanelOpen
                render()
            }
        }
        layer.addView(bubble, FrameLayout.LayoutParams(dp(56), dp(56),
            Gravity.END or Gravity.BOTTOM).apply {
            marginEnd = dp(17)
            bottomMargin = dp(94)
        })
        bubble.post {
            val fx = bubbleXFraction
            val fy = bubbleYFraction
            if (fx != null && fy != null && bubble.parent === layer) {
                val travelX = max(1, layer.width - layer.paddingLeft - layer.paddingRight - bubble.width)
                val travelY = max(1, layer.height - layer.paddingTop - layer.paddingBottom - bubble.height)
                bubble.x = layer.paddingLeft + fx.coerceIn(0f, 1f) * travelX
                bubble.y = layer.paddingTop + fy.coerceIn(0f, 1f) * travelY
            }
        }
        val slop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var dragging = false
        bubble.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = view.x
                    startY = view.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging) {
                        view.x = (startX + dx).coerceIn(layer.paddingLeft.toFloat(),
                            max(layer.paddingLeft, layer.width - layer.paddingRight - view.width).toFloat())
                        view.y = (startY + dy).coerceIn(layer.paddingTop.toFloat(),
                            max(layer.paddingTop, layer.height - layer.paddingBottom - view.height).toFloat())
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val travelX = max(1, layer.width - layer.paddingLeft - layer.paddingRight - view.width)
                        val travelY = max(1, layer.height - layer.paddingTop - layer.paddingBottom - view.height)
                        bubbleXFraction = ((view.x - layer.paddingLeft) / travelX).coerceIn(0f, 1f)
                        bubbleYFraction = ((view.y - layer.paddingTop) / travelY).coerceIn(0f, 1f)
                    } else {
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        if (modulePanelOpen) attachModulePanel(layer)
    }

    private fun floatingPill(layer: FrameLayout, title: String, bottom: Int): TextView {
        val pill = TextView(this).apply {
            text = title
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(primary)
            gravity = Gravity.CENTER
            background = rounded(panel, outline, 12)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            elevation = dp(5).toFloat()
        }
        layer.addView(pill, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(40),
            Gravity.START or Gravity.BOTTOM
        ).apply {
            marginStart = dp(16)
            bottomMargin = bottom
        })
        return pill
    }

    private fun attachModulePanel(layer: FrameLayout) {
        val scrim = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(172, 0, 0, 0))
            isClickable = true
            setOnClickListener { modulePanelOpen = false; render() }
        }
        layer.addView(scrim, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        val availableWidth = resources.displayMetrics.widthPixels - dp(32)
        val panelWidth = min(availableWidth, dp(360))
        val panelHeight = min((resources.displayMetrics.heightPixels * 0.65f).toInt(), dp(590))
        val moduleSheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, outline, 20)
            elevation = dp(14).toFloat()
            setPadding(dp(18), dp(18), dp(18), dp(16))
            isClickable = true
        }
        scrim.addView(moduleSheet, FrameLayout.LayoutParams(panelWidth, panelHeight,
            Gravity.END or Gravity.BOTTOM).apply {
            marginEnd = dp(16)
            bottomMargin = dp(89)
        })
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val heading = TextView(this).apply {
            text = "MODULE CONTROLS"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
        }
        header.addView(heading, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            gravity = Gravity.CENTER_VERTICAL
        })
        val close = TextView(this).apply {
            text = "✕"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(primary)
            background = touchBackground(elevated, outline, 12)
            isClickable = true
            isFocusable = true
            contentDescription = "Close module controls"
            setOnClickListener { modulePanelOpen = false; render() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        moduleSheet.addView(header)
        label(moduleSheet, "Argon app only · not an overlay in Minecraft", 12f, muted, false, 15)
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }
        moduleSheet.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(rows)
        label(rows, "AVAILABLE NOW", 11f, accent, true, 10)
        AppModuleRegistry.all.forEach { spec ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(elevated, outline, 14)
                setPadding(dp(12), dp(11), dp(10), dp(11))
            }
            rows.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
            val descriptions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            row.addView(descriptions, LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            label(descriptions, spec.title, 14f, primary, true, 4)
            label(descriptions, spec.description, 11f, muted)
            val toggle = Switch(this).apply {
                isChecked = appModuleStore.isEnabled(spec.id)
                minHeight = dp(48)
                contentDescription = "Enable ${spec.title} inside Argon"
                thumbTintList = ColorStateList.valueOf(if (isChecked) accent else muted)
                setOnCheckedChangeListener { _, checked -> setAppModuleEnabled(spec.id, checked) }
            }
            row.addView(toggle, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)
            ))
        }
        space(rows, 8)
        label(rows, "MINECRAFT  ·  NOT AVAILABLE", 11f, muted, true, 9)
        ModuleCatalog.all.take(4).forEach { spec ->
            val planned = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(panel, outline, 12)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            rows.addView(planned, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
            label(planned, spec.title + "  ·  Planned", 13f, muted, true, 4)
            label(planned, "No verified Minecraft integration. Cannot enable.", 11f, muted)
        }
        action(moduleSheet, "Browse planned modules") {
            modulePanelOpen = false
            page = Page.MODULES
            render()
        }
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
