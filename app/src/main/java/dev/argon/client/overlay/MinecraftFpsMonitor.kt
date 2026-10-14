package dev.argon.client.overlay

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Optional, user-authorized Minecraft SurfaceFlinger present-frame sampler.
 * Runs privileged diagnostics only through Shizuku, never as the app itself.
 */
class MinecraftFpsMonitor(
    context: Context,
    private val display: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { job ->
        Thread(job, "argon-minecraft-fps").apply { isDaemon = true }
    }
    private val busy = AtomicBoolean(false)
    private val args = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, SurfaceFlingerUserService::class.java.name)
    ).daemon(false).processNameSuffix("fps").tag("argon-fps-surface").version(1)

    @Volatile private var remote: IFrameStatsService? = null
    private var running = false
    private var bound = false
    private var layer: String? = null
    private var lastLayerLookup = 0L

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (!running) return
            remote = IFrameStatsService.Stub.asInterface(binder)
            layer = null
            display("GAME FPS  —  Sampling")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            remote = null
            layer = null
            if (running) display("GAME FPS  —  Shizuku disconnected")
        }
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (!authorized()) {
                display("GAME FPS  —  Shizuku unavailable")
                stopBinding()
            } else {
                if (!bound) bind()
                if (remote != null && busy.compareAndSet(false, true)) {
                    executor.execute {
                        val text = try {
                            val api = remote ?: throw IllegalStateException("Disconnected")
                            val now = System.nanoTime()
                            if (layer == null || now - lastLayerLookup > 10_000_000_000L) {
                                layer = SurfaceFps.selectLayer(api.listLayers())
                                lastLayerLookup = now
                            }
                            val selected = layer
                            if (selected == null) {
                                "GAME FPS  —  Minecraft layer unavailable"
                            } else {
                                val rate = SurfaceFps.readFps(api.readLayerLatency(selected), System.nanoTime())
                                if (rate == null) "GAME FPS  —  Idle / unsupported"
                                else "GAME FPS  ≈${rate.roundToInt()}"
                            }
                        } catch (_: Exception) {
                            layer = null
                            "GAME FPS  —  Sampling unavailable"
                        }
                        main.post { if (running) display(text) }
                        busy.set(false)
                    }
                }
            }
            main.postDelayed(this, 2_000L)
        }
    }

    fun start() {
        if (running) return
        running = true
        display("GAME FPS  —  Waiting for Shizuku")
        main.post(tick)
    }

    private fun authorized(): Boolean = try {
        Shizuku.pingBinder() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: RuntimeException) {
        false
    }

    private fun bind() {
        try {
            Shizuku.bindUserService(args, connection)
            bound = true
        } catch (_: RuntimeException) {
            display("GAME FPS  —  Could not bind Shizuku")
        }
    }

    private fun stopBinding() {
        if (bound) {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: RuntimeException) {
                // Shizuku may have been stopped; no privileged work continues.
            }
            bound = false
        }
        remote = null
        layer = null
    }

    fun stop() {
        running = false
        main.removeCallbacks(tick)
        stopBinding()
    }

    fun destroy() {
        stop()
        executor.shutdownNow()
    }
}
