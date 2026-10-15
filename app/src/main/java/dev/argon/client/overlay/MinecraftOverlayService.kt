package dev.argon.client.overlay

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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
        private const val TAG = "ArgonOverlay"
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
    private var notificationManager: NotificationManager? = null
    private var activityManager: ActivityManager? = null
    private var powerManager: PowerManager? = null
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
            if (!canDrawOverlaysSafely()) {
                Log.w(TAG, "Overlay permission revoked during tick. Stopping overlay service.")
                teardownOverlay()
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
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
        powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        widgets = OverlayWidgetStore(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            teardownOverlay()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || !canDrawOverlaysSafely()) {
            teardownOverlay()
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
            Log.e(TAG, "Could not create permitted overlay", error)
            teardownOverlay()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun canDrawOverlaysSafely(): Boolean {
        return try {
            Settings.canDrawOverlays(this)
        } catch (_: Exception) {
            false
        }
    }

    private fun startPersistentNotification() {
        val nm = notificationManager ?: (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
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

    private fun screenSize(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            Point().also {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealSize(it)
            }
        }
    }

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
        val x = OverlayPositionMath.calculateCoordinate(
            locationPrefs.getFloat("x", 0.91f), size.x, diameter
        )
        val y = OverlayPositionMath.calculateCoordinate(
            locationPrefs.getFloat("y", 0.45f), size.y, diameter
        )
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
                        params.x = OverlayPositionMath.clampCoordinate(
                            startX + dx.roundToInt(), currentSize.x, diameter
                        )
                        params.y = OverlayPositionMath.clampCoordinate(
                            startY + dy.roundToInt(), currentSize.y, diameter
                        )
                        try {
                            if (v.isAttachedToWindow) {
                                wm.updateViewLayout(v, params)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update bubble layout during drag", e)
                            if (!canDrawOverlaysSafely()) {
                                teardownOverlay()
                                stopSelf()
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val currentSize = screenSize()
                        val fx = OverlayPositionMath.calculateFraction(params.x, currentSize.x, diameter)
                        val fy = OverlayPositionMath.calculateFraction(params.y, currentSize.y, diameter)
                        locationPrefs.edit()
                            .putFloat("x", fx)
                            .putFloat("y", fy)
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
        if (sheet != null || !canDrawOverlaysSafely()) return
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
            ).apply { bottomMargin = dp(9) })\
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
        rows.addView(label("MINECRAFT FPS  ·  RESOURCE PACK", 11f, mint))
        rows.addView(label(
            "A game-side FPS counter is possible with a Bedrock resource pack. " +
                "Install and enable the pack in Minecraft; this panel cannot toggle it.",
            12f, secondary
        ))
        val installPack = TextView(this).apply {
            text = "Get Minecraft FPS Pack  ↗"
            textSize = 13f
            setTextColor(fg)
            gravity = Gravity.CENTER
            minHeight = dp(48)
            background = ripple(elevated)
            isClickable = true
            isFocusable = true
            contentDescription = "Open independent Minecraft FPS resource pack page in browser"
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FpsPackInfo.PROJECT_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: RuntimeException) {
                    Toast.makeText(this@MinecraftOverlayService,
                        "Open Argon Overview to get the FPS pack", Toast.LENGTH_LONG).show()
                }
            }
        }
        rows.addView(installPack, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ).apply { bottomMargin = dp(12) })
        rows.addView(label("OTHER GAME MODULES  ·  NOT INTEGRATED", 11f, secondary))
        ModuleCatalog.all.filter { it.id != "fps_counter" }.take(4).forEach { spec ->
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
            setOnClickListener {
                teardownOverlay()
                stopSelf()
            }
        }
        container.addView(stop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ))
        try {
            wm.addView(container, layout(width, height, x, y))
            sheet = container
        } catch (error: RuntimeException) {
            Log.w(TAG, "Overlay panel rejected by Android", error)
            removeSheet()
            if (!canDrawOverlaysSafely()) {
                teardownOverlay()
                stopSelf()
            }
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
        if (!canDrawOverlaysSafely()) return
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

        val size = screenSize()
        val hudX = dp(12).coerceIn(0, max(0, size.x - dp(100)))
        val hudY = dp(76).coerceIn(0, max(0, size.y - dp(50)))

        try {
            wm.addView(stack, layout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                hudX, hudY
            ).apply {
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                alpha = 0.7f
            })
            hud = stack
        } catch (error: RuntimeException) {
            Log.w(TAG, "Overlay widget rejected by Android", error)
            removeHud()
            if (!canDrawOverlaysSafely()) {
                teardownOverlay()
                stopSelf()
            }
        }
    }

    private fun updateWidgetLabels() {
        if (!active) return
        clockText?.text = "LOCAL  " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val memoryClass = activityManager?.memoryClass
            ?: (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.memoryClass
            ?: 0
        val saver = powerManager?.isPowerSaveMode
            ?: (getSystemService(POWER_SERVICE) as? PowerManager)?.isPowerSaveMode
            ?: false
        deviceText?.text = "APP MEMORY ${memoryClass} MB  ·  Saver ${if (saver) "on" else "off"}"
    }

    private fun safeRemoveView(view: View?) {
        if (view == null) return
        view.setOnClickListener(null)
        view.setOnTouchListener(null)
        if (view is ViewGroup) {
            view.removeAllViews()
        }
        if (view.isAttachedToWindow) {
            try {
                wm.removeViewImmediate(view)
            } catch (_: IllegalArgumentException) {
                // View was not attached or already removed
            } catch (_: Exception) {
                runCatching { wm.removeView(view) }
            }
        }
    }

    private fun removeSheet() {
        safeRemoveView(sheet)
        sheet = null
    }

    private fun removeHud() {
        safeRemoveView(hud)
        hud = null
        clockText = null
        deviceText = null
    }

    private fun teardownOverlay() {
        active = false
        handler.removeCallbacksAndMessages(null)
        removeSheet()
        removeHud()
        safeRemoveView(bubble)
        bubble = null
        bubbleLayout = null
        clockText = null
        deviceText = null
        notificationManager = null
        activityManager = null
        powerManager = null
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!canDrawOverlaysSafely()) {
            teardownOverlay()
            stopSelf()
            return
        }
        removeSheet()
        bubbleLayout?.let { params ->
            val size = screenSize()
            val diameter = dp(56)
            val relX = locationPrefs.getFloat("x", 0.91f)
            val relY = locationPrefs.getFloat("y", 0.45f)
            params.x = OverlayPositionMath.calculateCoordinate(relX, size.x, diameter)
            params.y = OverlayPositionMath.calculateCoordinate(relY, size.y, diameter)
            bubble?.let { b ->
                if (b.isAttachedToWindow) {
                    try {
                        wm.updateViewLayout(b, params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update bubble layout after orientation change", e)
                        if (!canDrawOverlaysSafely()) {
                            teardownOverlay()
                            stopSelf()
                        }
                    }
                }
            }
        }
        rebuildHud()
    }

    override fun onDestroy() {
        teardownOverlay()
        super.onDestroy()
    }
}
