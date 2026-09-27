package com.flyonz.ceritaria.studio.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoOutputPlannerTest {
    private val planner = VideoOutputPlanner()

    @Test
    fun keepsPortrait1080By1920WithoutScaling() {
        val plan = requireNotNull(planner.plan(metadata(1080, 1920)))

        assertEquals(1080, plan.outputWidth)
        assertEquals(1920, plan.outputHeight)
        assertNull(plan.targetShortSide)
    }

    @Test
    fun keepsLandscape1920By1080WithoutScaling() {
        val plan = requireNotNull(planner.plan(metadata(1920, 1080)))

        assertEquals(1920, plan.outputWidth)
        assertEquals(1080, plan.outputHeight)
        assertNull(plan.targetShortSide)
    }

    @Test
    fun downscalesFourKPortraitPreservingAspectRatio() {
        val plan = requireNotNull(planner.plan(metadata(2160, 3840)))

        assertEquals(1080, plan.outputWidth)
        assertEquals(1920, plan.outputHeight)
        assertEquals(1080, plan.targetShortSide)
    }

    @Test
    fun longSideLimitHandlesUltraWideVideo() {
        val plan = requireNotNull(planner.plan(metadata(2160, 900)))

        assertEquals(1920, plan.outputWidth)
        assertEquals(800, plan.outputHeight)
        assertEquals(800, plan.targetShortSide)
    }

    private fun metadata(width: Int, height: Int) = VideoMetadata(
        containerMimeType = "video/mp4",
        sizeBytes = 10_000_000,
        durationMs = 30_000,
        width = width,
        height = height,
        rotationDegrees = 0,
        frameRate = 30f,
        videoMimeType = "video/avc",
        audioMimeType = "audio/mp4a-latm",
        audioBitrate = 128_000,
        audioSampleRate = 48_000,
        hasVideo = true,
        hasAudio = true,
    )
}
