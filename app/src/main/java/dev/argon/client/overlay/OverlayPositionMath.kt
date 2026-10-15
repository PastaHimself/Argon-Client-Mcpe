package dev.argon.client.overlay

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure coordinate and bounds arithmetic for floating overlay positioning
 * and orientation configuration changes.
 */
object OverlayPositionMath {
    /**
     * Calculates an absolute pixel coordinate within [displayDimension] for a widget of size [widgetDimension]
     * based on a normalized relative ratio [fraction] (0.0 to 1.0).
     */
    fun calculateCoordinate(fraction: Float, displayDimension: Int, widgetDimension: Int): Int {
        val safeFraction = fraction.coerceIn(0f, 1f)
        val maxTravel = max(0, displayDimension - widgetDimension)
        if (maxTravel == 0) return 0
        return (safeFraction * maxTravel).roundToInt().coerceIn(0, maxTravel)
    }

    /**
     * Calculates a normalized relative ratio (0.0 to 1.0) given a pixel [coordinate],
     * total [displayDimension], and [widgetDimension].
     */
    fun calculateFraction(coordinate: Int, displayDimension: Int, widgetDimension: Int): Float {
        val maxTravel = max(1, displayDimension - widgetDimension)
        return (coordinate.toFloat() / maxTravel).coerceIn(0f, 1f)
    }

    /**
     * Clamps a coordinate within the visible display bounds [0, displayDimension - widgetDimension].
     */
    fun clampCoordinate(coordinate: Int, displayDimension: Int, widgetDimension: Int): Int {
        val maxBound = max(0, displayDimension - widgetDimension)
        return coordinate.coerceIn(0, maxBound)
    }
}
