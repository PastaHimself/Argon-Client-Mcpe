package dev.argon.client.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPositionMathTest {
    @Test
    fun calculateCoordinateClampsWithinDisplayBounds() {
        val x = OverlayPositionMath.calculateCoordinate(0.91f, 1080, 160)
        assertTrue(x in 0..(1080 - 160))

        val y = OverlayPositionMath.calculateCoordinate(0.45f, 2400, 160)
        assertTrue(y in 0..(2400 - 160))
    }

    @Test
    fun orientationFlipMaintainsRelativeFractionAndClampsProperly() {
        val relX = 0.91f
        val relY = 0.45f
        val diameter = 160

        val landscapeX = OverlayPositionMath.calculateCoordinate(relX, 2400, diameter)
        val landscapeY = OverlayPositionMath.calculateCoordinate(relY, 1080, diameter)

        assertTrue(landscapeX in 0..(2400 - diameter))
        assertTrue(landscapeY in 0..(1080 - diameter))

        val derivedRatioX = OverlayPositionMath.calculateFraction(landscapeX, 2400, diameter)
        assertEquals(relX, derivedRatioX, 0.01f)
    }

    @Test
    fun handlesZeroOrTinyDisplaysWithoutCrashing() {
        val zeroCoord = OverlayPositionMath.calculateCoordinate(0.5f, 0, 100)
        assertEquals(0, zeroCoord)

        val smallDisplayCoord = OverlayPositionMath.calculateCoordinate(0.5f, 50, 100)
        assertEquals(0, smallDisplayCoord)

        val fraction = OverlayPositionMath.calculateFraction(10, 50, 100)
        assertEquals(0f, fraction, 0.01f)
    }

    @Test
    fun clampCoordinateKeepsWidgetVisible() {
        val clampedNegative = OverlayPositionMath.clampCoordinate(-50, 1080, 160)
        assertEquals(0, clampedNegative)

        val clampedOverflow = OverlayPositionMath.clampCoordinate(2000, 1080, 160)
        assertEquals(1080 - 160, clampedOverflow)

        val clampedValid = OverlayPositionMath.clampCoordinate(500, 1080, 160)
        assertEquals(500, clampedValid)
    }
}
