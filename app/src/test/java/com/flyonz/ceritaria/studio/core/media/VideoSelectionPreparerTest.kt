package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobFactory
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSelectionPreparerTest {
    @Test
    fun persistsAccessInspectsAndStoresJob() = runTest {
        val access = FakeAccess()
        val repository = FakeJobs()
        val preparer = VideoSelectionPreparer(
            sourceAccess = access,
            inspector = FakeInspector(metadata()),
            jobFactory = VideoJobFactory(VideoCompatibilityChecker()),
            jobs = repository,
        )

        val job = preparer.prepare("content://video/1", "episode-1")

        assertEquals("content://video/1", access.uri)
        assertEquals("episode-1", job.episodeId)
        assertEquals(job, repository.saved)
    }

    @Test
    fun rejectsSourceWithoutVideoTrack() = runTest {
        val preparer = VideoSelectionPreparer(
            sourceAccess = FakeAccess(),
            inspector = FakeInspector(metadata(hasVideo = false)),
            jobFactory = VideoJobFactory(VideoCompatibilityChecker()),
            jobs = FakeJobs(),
        )

        var failed = false
        try {
            preparer.prepare("content://audio/1", null)
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
    }

    private class FakeAccess : VideoSourceAccess {
        var uri: String? = null
        override fun persistReadAccess(sourceUri: String) {
            uri = sourceUri
        }
    }

    private class FakeInspector(
        private val metadata: VideoMetadata,
    ) : VideoInspector {
        override suspend fun inspect(sourceUri: String): VideoMetadata = metadata
    }

    private class FakeJobs : VideoJobRepository {
        var saved: VideoJob? = null

        override suspend fun getById(jobId: String): VideoJob? = null
        override fun observeById(jobId: String): Flow<VideoJob?> = emptyFlow()
        override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> = emptyFlow()
        override suspend fun upsert(job: VideoJob) {
            saved = job
        }
        override suspend fun delete(jobId: String) = Unit
    }

    private fun metadata(
        hasVideo: Boolean = true,
    ) = VideoMetadata(
        containerMimeType = "video/mp4",
        sizeBytes = 10_000_000,
        durationMs = 30_000,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frameRate = 30f,
        videoMimeType = if (hasVideo) "video/avc" else null,
        audioMimeType = "audio/mp4a-latm",
        audioBitrate = 128_000,
        audioSampleRate = 48_000,
        hasVideo = hasVideo,
        hasAudio = true,
    )
}
