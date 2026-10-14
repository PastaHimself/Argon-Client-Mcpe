package dev.argon.client.performance

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.view.FrameMetrics
import android.view.Window
import dev.argon.client.core.FrameRateSampler

/**
 * Observes actual frames rendered by Argon's window, never Minecraft frames.
 * Sampling runs off the UI thread, and the UI is updated once per second.
 * Android may suppress callbacks when nothing is drawn: null means idle/no estimate.
 */
class UiFrameRateMonitor(
    private val window: Window,
    private val onSample: (Double?) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var worker: HandlerThread? = null
    private var listener: Window.OnFrameMetricsAvailableListener? = null
    private var sampler: FrameRateSampler? = null
    private var started = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!started) return
            val snapshot = sampler
            val fps = snapshot?.let { synchronized(it) { it.currentFps(System.nanoTime()) } }
            onSample(fps)
            mainHandler.postDelayed(this, 500L)
        }
    }

    fun start() {
        if (started) return
        val fresh = FrameRateSampler()
        val thread = HandlerThread("argon-ui-frame-metrics", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        val observer = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
            val vsync = metrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
            synchronized(fresh) {
                if (dropped > 0) fresh.reset()
                fresh.recordFrame(vsync)
            }
        }
        try {
            window.addOnFrameMetricsAvailableListener(observer, Handler(thread.looper))
        } catch (_: IllegalStateException) {
            thread.quitSafely()
            onSample(null)
            return
        }
        sampler = fresh
        worker = thread
        listener = observer
        started = true
        mainHandler.post(ticker)
    }

    fun stop() {
        started = false
        mainHandler.removeCallbacks(ticker)
        listener?.let { window.removeOnFrameMetricsAvailableListener(it) }
        listener = null
        worker?.quitSafely()
        worker = null
        sampler = null
    }
}
