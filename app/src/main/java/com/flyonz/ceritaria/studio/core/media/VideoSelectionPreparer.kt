package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobFactory
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import javax.inject.Inject

class VideoSelectionPreparer @Inject constructor(
    private val sourceAccess: VideoSourceAccess,
    private val inspector: VideoInspector,
    private val jobFactory: VideoJobFactory,
    private val jobs: VideoJobRepository,
) {
    suspend fun prepare(
        sourceUri: String,
        episodeId: String?,
    ): VideoJob {
        sourceAccess.persistReadAccess(sourceUri)
        val metadata = inspector.inspect(sourceUri)
        require(metadata.hasVideo) { "Selected source does not contain a video track." }

        val job = jobFactory.create(
            sourceUri = sourceUri,
            metadata = metadata,
            episodeId = episodeId,
        )
        jobs.upsert(job)
        return job
    }
}
