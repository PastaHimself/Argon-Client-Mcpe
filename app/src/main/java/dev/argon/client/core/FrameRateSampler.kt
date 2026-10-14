package dev.argon.client.core

import java.util.ArrayDeque

/**
 * Estimates Argon's *own* rendered-window FPS from real Android frame-vsync timestamps.
 * It does not measure Minecraft, the device refresh rate, or game performance.
 *
 * No result is returned for idle windows or insufficient samples. Frame callback
 * reports can be dropped by Android, so callers must reset after a reported drop.
 */
class FrameRateSampler {
    private val timestamps = ArrayDeque<Long>()

    fun reset() = timestamps.clear()

    fun recordFrame(vsyncNanos: Long) {
        if (vsyncNanos <= 0L) return
        if (timestamps.isNotEmpty() && vsyncNanos <= timestamps.peekLast()) return
        timestamps.addLast(vsyncNanos)
        prune(vsyncNanos)
    }

    fun currentFps(nowNanos: Long): Double? {
        if (timestamps.isEmpty()) return null
        val newest = timestamps.peekLast()
        if (nowNanos < newest || nowNanos - newest > IDLE_NANOS) return null
        prune(nowNanos)
        if (timestamps.size < MIN_SAMPLES) return null
        val duration = timestamps.peekLast() - timestamps.peekFirst()
        if (duration < MIN_SPAN_NANOS) return null
        return (timestamps.size - 1).toDouble() * 1_000_000_000.0 / duration
    }

    private fun prune(nowNanos: Long) {
        val cutoff = nowNanos - WINDOW_NANOS
        while (timestamps.isNotEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.removeFirst()
        }
    }

    private companion object {
        const val WINDOW_NANOS = 1_000_000_000L
        const val IDLE_NANOS = 500_000_000L
        const val MIN_SPAN_NANOS = 250_000_000L
        const val MIN_SAMPLES = 8
    }
}
