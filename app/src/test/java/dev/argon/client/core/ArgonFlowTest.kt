package dev.argon.client.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgonFlowTest {
    @Test fun lowRamAlwaysPrefersBattery() {
        val advice = ArgonFlow.recommend(
            PlayStyle.PVP,
            DeviceSnapshot(isLowRam = true, memoryClassMb = 512, refreshRateHz = 120, batterySaver = false)
        )
        assertEquals(VisualPreference.BATTERY, advice.profile.visuals)
        assertEquals(112, advice.profile.controlScale)
        assertTrue(advice.reasons.any { it.contains("low-RAM") })
    }

    @Test fun powerSaverWinsOverRefreshRate() {
        val advice = ArgonFlow.recommend(
            PlayStyle.BUILD,
            DeviceSnapshot(false, 512, 120, true)
        )
        assertEquals(VisualPreference.BATTERY, advice.profile.visuals)
        assertEquals(70, advice.profile.hudOpacity)
    }

    @Test fun qualityRequiresMemoryAndRefreshRate() {
        assertEquals(
            VisualPreference.QUALITY,
            ArgonFlow.recommend(PlayStyle.EXPLORE, DeviceSnapshot(false, 256, 90, false)).profile.visuals
        )
        assertEquals(
            VisualPreference.BALANCED,
            ArgonFlow.recommend(PlayStyle.EXPLORE, DeviceSnapshot(false, 256, 60, false)).profile.visuals
        )
    }

    @Test fun invalidProfileIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ClientProfile(PlayStyle.PVP, 500, 80, VisualPreference.BALANCED).validated()
        }
    }
}
