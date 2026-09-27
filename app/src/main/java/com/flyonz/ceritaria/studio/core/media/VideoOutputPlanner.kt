package com.flyonz.ceritaria.studio.core.media

import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class VideoOutputPlanner @Inject constructor() {
    private val preset = StreamingPreset()

    fun plan(metadata: VideoMetadata): VideoOutputPlan? {
        val width = metadata.width ?: return null
        val height = metadata.height ?: return null
        if (width <= 0 || height <= 0) return null

        val rotation = normalizeRotation(metadata.rotationDegrees ?: 0)
        val displayWidth = if (rotation == 90 || rotation == 270) height else width
        val displayHeight = if (rotation == 90 || rotation == 270) width else height
        val shortSide = min(displayWidth, displayHeight)
        val longSide = max(displayWidth, displayHeight)

        val scale = min(
            1.0,
            min(
                preset.maxShortSide.toDouble() / shortSide,
                preset.maxLongSide.toDouble() / longSide,
            ),
        )
        val outputShortSide = evenFloor(shortSide * scale)
        val outputLongSide = evenFloor(longSide * scale)
        val portrait = displayHeight >= displayWidth

        return VideoOutputPlan(
            outputWidth = if (portrait) outputShortSide else outputLongSide,
            outputHeight = if (portrait) outputLongSide else outputShortSide,
            targetShortSide = if (scale < 1.0) outputShortSide else null,
        )
    }

    fun fitsPreset(metadata: VideoMetadata): Boolean {
        val plan = plan(metadata) ?: return false
        return plan.targetShortSide == null
    }

    private fun evenFloor(value: Double): Int {
        val floored = floor(value).toInt().coerceAtLeast(2)
        return if (floored % 2 == 0) floored else floored - 1
    }

    private fun normalizeRotation(rotationDegrees: Int): Int =
        ((rotationDegrees % 360) + 360) % 360
}

data class VideoOutputPlan(
    val outputWidth: Int,
    val outputHeight: Int,
    val targetShortSide: Int?,
)
