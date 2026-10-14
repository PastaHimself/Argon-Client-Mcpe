package dev.argon.client.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceFpsTest {
    private val now = 100_000_000_000L

    private fun latency(fps: Int, count: Int = fps): String = buildString {
        appendLine("16666667") // display refresh period deliberately unrelated to samples
        val frameStep = 1_000_000_000L / fps
        repeat(count) { i ->
            val t = now - (count - 1 - i) * frameStep
            appendLine("${t - 1_000_000} $t ${t + 1_000_000}")
        }
    }

    @Test fun identifiesOnlyMinecraftSurface() {
        val list = """
            Argon overlay
            SurfaceView[com.other.game/com.other.Main]#1
            com.mojang.minecraftpe/com.mojang.minecraftpe.MainActivity#1
            SurfaceView[com.mojang.minecraftpe/com.mojang.minecraftpe.MainActivity](BLAST)#2
        """.trimIndent()
        assertEquals(
            "SurfaceView[com.mojang.minecraftpe/com.mojang.minecraftpe.MainActivity](BLAST)#2",
            SurfaceFps.selectLayer(list)
        )
        assertNull(SurfaceFps.selectLayer("SurfaceView[com.other.game]#1"))
    }

    @Test fun derivesReal30FpsEvenOn60HzScreen() {
        val value = SurfaceFps.readFps(latency(30), now)
        assertTrue(value != null && value in 29.0..31.0)
    }

    @Test fun derivesReal60FpsFromTimestamps() {
        val value = SurfaceFps.readFps(latency(60), now)
        assertTrue(value != null && value in 59.0..61.0)
    }

    @Test fun refusesRefreshRateWithoutFrames() {
        assertNull(SurfaceFps.readFps("8333333\n", now))
        assertNull(SurfaceFps.readFps(latency(60, 3), now))
    }

    @Test fun ignoresDuplicatedAndPendingFenceTimestamps() {
        val base = latency(60)
        val extra = "${now - 16_666_666} ${now - 16_666_666} ${now - 16_666_666}\n0 9223372036854775807 0"
        assertTrue(SurfaceFps.readFps(base + extra, now)!! in 59.0..61.0)
    }

    @Test fun reportsNoRateForIdleLayerOrFutureFrames() {
        assertNull(SurfaceFps.readFps(latency(60), now + 4_000_000_000L))
        assertNull(SurfaceFps.readFps(latency(60), now - 2_000_000_000L))
    }

    @Test fun malformedOrUnreasonableReportsAreUnavailable() {
        assertNull(SurfaceFps.readFps("unknown\nnope", now))
        assertNull(SurfaceFps.readFps("16666667\n1 2 3\n", now))
        assertNull(SurfaceFps.readFps(latency(600), now))
    }
}
