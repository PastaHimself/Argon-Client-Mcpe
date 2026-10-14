package dev.argon.client.overlay

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.content.pm.ServiceInfo
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import dev.argon.client.MainActivity
import dev.argon.client.R
import dev.argon.client.core.ModuleCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Opt-in TYPE_APPLICATION_OVERLAY controls visible over independently launched apps,
 * including Minecraft when the OS allows it. No capture, injection, or game FPS data.
 *
 * The service cannot know which external app is foreground without further privileges.
 * The ongoing notification and menu Stop button terminate the overlay at any time.
 */
class MinecraftOverlayService : Service() {
    companion object {
        const val ACTION_START = "dev.argon.client.overlay.START"
        const val ACTION_STOP = "dev.argon.client.overlay.STOP"
        private const val CHANNEL = "argon_overlay_v1"
        private const val NOTIFICATION_ID = 4202

        @Volatile
        var active: Boolean = false
            private set
    }

    private val bg = Color.rgb(11, 16, 22)
    private val panelColor = Color.rgb(21, 29, 38)
    private val elevated = Color.rgb(29, 39, 49)
    private val border = Color.rgb(48, 63, 75)
    private val mint = Color.rgb(168, 235, 200)
    private val fg = Color.rgb(245, 248, 249)
    private val secondary = Color.rgb(171, 185, 195)

    private lateinit var wm: WindowManager
    private lateinit var widgets: OverlayWidgetStore
    private val handler = Handler(Looper.getMainLooper())
    private val locationPrefs by lazy {
        getSharedPreferences("argon_overlay_position_v1", Context.MODE_PRIVATE)
    }
    private var bubble: TextView? = null
    private var bubbleLayout: WindowManager.LayoutParams? = null
    private var sheet: View? = null
    private var hud: LinearLayout? = null
    private var clockText: TextView? = null
    private var deviceText: TextView? = null

    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            if (!Settings.canDrawOverlays(this@MinecraftOverlayService)) {
                stopSelf()
                return
            }
            updateWidgetLabels()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        widgets = OverlayWidgetStore(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (active) return START_NOT_STICKY
        try {
            startPersistentNotification()
            addBubble()
            active = true
            rebuildHud()
            handler.post(tick)
        } catch (error: RuntimeException) {
            android.util.Log.e("ArgonOverlay", "Could not create permitted overlay", error)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startPersistentNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Argon floating controls", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = Intent(this, MinecraftOverlayService::class.java).setAction(ACTION_STOP)
        val stopAction = PendingIntent.getService(
            this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_argon)
            .setContentTitle("Argon floating menu active")
            .setContentText("Visible over other apps until you stop it")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Stop overlay", stopAction
            ).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun background(color: Int, radius: Int = 16, stroke: Int? = border) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun ripple(color: Int, radius: Int = 16): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.argb(48, 255, 255, 255)),
            background(color, radius, null),
            background(Color.WHITE, radius, null)
        )

    @Suppress("DEPRECATION")
    private fun screenSize(): Point = Point().also { wm.defaultDisplay.getSize(it) }

    private fun layout(width: Int, height: Int, screenX: Int, screenY: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = screenX
            this.y = screenY
        }

    private fun addBubble() {
        val size = screenSize()
        val diameter = dp(56)
        val x = (locationPrefs.getFloat("x", 0.91f) * max(1, size.x - diameter))
            .roundToInt().coerceIn(0, max(0, size.x - diameter))
        val y = (locationPrefs.getFloat("y", 0.45f) * max(1, size.y - diameter))
            .roundToInt().coerceIn(0, max(0, size.y - diameter))
        val params = layout(diameter, diameter, x, y)
        val view = TextView(this).apply {
            text = "A"
            textSize = 23f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(bg)
            gravity = Gravity.CENTER
            background = ripple(mint, 28)
            elevation = dp(7).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Argon: open floating module menu"
            setOnClickListener { if (sheet == null) showSheet() else removeSheet() }
        }
        val slop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging) {
                        val currentSize = screenSize()
                        params.x = (startX + dx.roundToInt()).coerceIn(
                            0, max(0, currentSize.x - diameter)
                        )
                        params.y = (startY + dy.roundToInt()).coerceIn(
                            0, max(0, currentSize.y - diameter)
                        )
                        runCatching { wm.updateViewLayout(v, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val currentSize = screenSize()
                        locationPrefs.edit()
                            .putFloat("x", params.x.toFloat() / max(1, currentSize.x - diameter))
                            .putFloat("y", params.y.toFloat() / max(1, currentSize.y - diameter))
                            .apply()
                        removeSheet()
                    } else {
                        v.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        wm.addView(view, params)
        bubble = view
        bubbleLayout = params
    }

    /** Only the panel area intercepts touch; Minecraft remains touchable outside it. */
    private fun showSheet() {
        if (sheet != null || !Settings.canDrawOverlays(this)) return
        val size = screenSize()
        val width = min(dp(334), max(dp(220), size.x - dp(24)))
            .coerceAtMost(max(1, size.x))
        val height = min(dp(420), max(dp(220), size.y - dp(36)))
            .coerceAtMost(max(1, size.y))
        val bx = bubbleLayout?.x ?: 0
        val by = bubbleLayout?.y ?: 0
        val x = (bx - width + dp(56)).coerceIn(0, max(0, size.x - width))
        val y = by.coerceIn(0, max(0, size.y - height))
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = background(panelColor, 20)
            elevation = dp(12).toFloat()
            isClickable = true
        }
        val heading = TextView(this).apply {
            text = "ARGON  ·  MODULES"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(fg)
        }
        container.addView(heading)
        val subtitle = label("Over Minecraft · Android overlay", 12f, secondary)
        container.addView(subtitle)
        val scroll = ScrollView(this)
        container.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(rows)
        rows.addView(label("WORKING OVERLAY WIDGETS", 11f, mint))
        OverlayWidgetRegistry.supported.forEach { widget ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = background(elevated, 12)
            }
            rows.addView(line, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) })
            val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            text.addView(label(widget.title, 14f, fg))
            text.addView(label(widget.description, 11f, secondary))
            line.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val toggle = Switch(this).apply {
                isChecked = widgets.enabled(widget.id)
                minWidth = dp(52)
                minHeight = dp(48)
                contentDescription = "Toggle ${widget.title}, Android overlay only"
                setOnCheckedChangeListener { _, enabled ->
                    widgets.setEnabled(widget.id, enabled)
                    rebuildHud()
                }
            }
            line.addView(toggle)
        }
        rows.addView(label("MINECRAFT DATA  ·  UNAVAILABLE", 11f, secondary))
        ModuleCatalog.all.take(5).forEach { spec ->
            rows.addView(label("${spec.title}  ·  not supported", 12f, secondary))
        }
        val close = TextView(this).apply {
            text = "Close menu"
            textSize = 14f
            setTextColor(fg)
            gravity = Gravity.CENTER
            background = ripple(elevated)
            isClickable = true
            isFocusable = true
            contentDescription = "Close floating module menu"
            setOnClickListener { removeSheet() }
        }
        container.addView(close, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ))
        val stop = TextView(this).apply {
            text = "Stop overlay"
            textSize = 14f
            setTextColor(mint)
            gravity = Gravity.CENTER
            background = ripple(panelColor)
            isClickable = true
            isFocusable = true
            contentDescription = "Stop Argon overlay and notification"
            setOnClickListener { stopSelf() }
        }
        container.addView(stop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ))
        try {
            wm.addView(container, layout(width, height, x, y))
            sheet = container
        } catch (error: RuntimeException) {
            android.util.Log.w("ArgonOverlay", "Overlay panel rejected by Android", error)
            stopSelf()
        }
    }

    private fun label(value: String, size: Float, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun rebuildHud() {
        removeHud()
        if (!Settings.canDrawOverlays(this)) return
        val showClock = widgets.enabled(OverlayWidgetRegistry.CLOCK)
        val showDevice = widgets.enabled(OverlayWidgetRegistry.DEVICE)
        if (!showClock && !showDevice) return
        val stack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = background(panelColor, 12)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            elevation = dp(4).toFloat()
        }
        if (showClock) {
            clockText = label("", 14f, fg)
            stack.addView(clockText)
        }
        if (showDevice) {
            deviceText = label("", 12f, secondary)
            stack.addView(deviceText)
        }
        updateWidgetLabels()
        // A small noninteractive HUD; it never captures the game's touch events.
        try {
            wm.addView(stack, layout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                dp(12), dp(76)
            ).apply {
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                // Android 12+ blocks touch-through on highly obscuring overlays.
                alpha = 0.7f
            })
            hud = stack
        } catch (error: RuntimeException) {
            android.util.Log.w("ArgonOverlay", "Overlay widget rejected by Android", error)
            stopSelf()
        }
    }

    private fun updateWidgetLabels() {
        clockText?.text = "LOCAL  " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val memoryClass = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).memoryClass
        val saver = (getSystemService(POWER_SERVICE) as PowerManager).isPowerSaveMode
        deviceText?.text = "APP MEMORY ${memoryClass} MB  ·  Saver ${if (saver) "on" else "off"}"
    }

    private fun removeSheet() {
        sheet?.let { runCatching { wm.removeView(it) } }
        sheet = null
    }

    private fun removeHud() {
        hud?.let { runCatching { wm.removeView(it) } }
        hud = null
        clockText = null
        deviceText = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        removeSheet()
        bubbleLayout?.let { params ->
            val size = screenSize()
            params.x = (locationPrefs.getFloat("x", 0.91f) * max(1, size.x - dp(56))).roundToInt()
            params.y = (locationPrefs.getFloat("y", 0.45f) * max(1, size.y - dp(56))).roundToInt()
            bubble?.let { runCatching { wm.updateViewLayout(it, params) } }
        }
        rebuildHud()
    }

    override fun onDestroy() {
        active = false
        handler.removeCallbacks(tick)
        removeSheet()
        removeHud()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        bubbleLayout = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
