package dev.argon.client.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameRateSamplerTest {
    private val origin = 10_000_000_000L

    @Test fun computes60FpsFromRenderedFrames() {
        val sampler = FrameRateSampler()
        for (i in 0..60) sampler.recordFrame(origin + i * 16_666_667L)
        val rate = sampler.currentFps(origin + 60 * 16_666_667L)
        assertTrue(rate != null && rate in 59.5..60.5)
    }

    @Test fun computes120FpsWithoutUsingDisplayRefreshRate() {
        val sampler = FrameRateSampler()
        for (i in 0..120) sampler.recordFrame(origin + i * 8_333_333L)
        val rate = sampler.currentFps(origin + 120 * 8_333_333L)
        assertTrue(rate != null && rate in 119.0..121.0)
    }

    @Test fun neverGuessesFpsFromTooFewFrames() {
        val sampler = FrameRateSampler()
        repeat(5) { sampler.recordFrame(origin + it * 16_666_667L) }
        assertNull(sampler.currentFps(origin + 4 * 16_666_667L))
    }

    @Test fun idleWindowReturnsNoReading() {
        val sampler = FrameRateSampler()
        repeat(61) { sampler.recordFrame(origin + it * 16_666_667L) }
        assertNull(sampler.currentFps(origin + 60 * 16_666_667L + 1_000_000_001L))
    }

    @Test fun repeatedOrOutOfOrderTimestampsDoNotInflateRate() {
        val sampler = FrameRateSampler()
        repeat(61) { i ->
            val timestamp = origin + i * 16_666_667L
            sampler.recordFrame(timestamp)
            sampler.recordFrame(timestamp)
            sampler.recordFrame(timestamp - 1L)
        }
        assertEquals(60.0, sampler.currentFps(origin + 60 * 16_666_667L)!!, 0.5)
    }

    @Test fun resetDiscardsOldWindow() {
        val sampler = FrameRateSampler()
        repeat(61) { sampler.recordFrame(origin + it * 16_666_667L) }
        sampler.reset()
        assertNull(sampler.currentFps(origin + 60 * 16_666_667L))
    }

    @Test fun negativeAndFutureTimestampsCannotFabricateFps() {
        val sampler = FrameRateSampler()
        sampler.recordFrame(-1L)
        assertNull(sampler.currentFps(origin))
        repeat(61) { sampler.recordFrame(origin + it * 16_666_667L) }
        assertNull(sampler.currentFps(origin))
    }
}
