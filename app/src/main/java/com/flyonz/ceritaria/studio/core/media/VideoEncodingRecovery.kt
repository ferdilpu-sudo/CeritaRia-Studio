package com.flyonz.ceritaria.studio.core.media

import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import java.time.Instant
import javax.inject.Inject

class VideoEncodingRecovery @Inject constructor(
    private val jobs: VideoJobRepository,
    private val temporaryMediaStore: TemporaryMediaStore,
) {
    suspend fun recover(job: VideoJob): VideoJob = when {
        job.encodingStatus == VideoEncodingStatus.ENCODING -> {
            temporaryMediaStore.deleteEncodedOutput(job.jobId)
            job.resetForRetry(ERROR_INTERRUPTED)
        }
        job.encodingStatus == VideoEncodingStatus.READY &&
            !temporaryMediaStore.hasEncodedOutput(job.jobId) -> {
            job.resetForRetry(ERROR_OUTPUT_MISSING)
        }
        else -> job
    }

    private suspend fun VideoJob.resetForRetry(errorCode: String): VideoJob {
        val recovered = copy(
            encodedLocalUri = null,
            encodingStatus = VideoEncodingStatus.QUEUED,
            encodingProgress = 0,
            lastErrorCode = errorCode,
            updatedAt = Instant.now(),
        )
        jobs.upsert(recovered)
        return recovered
    }

    private companion object {
        const val ERROR_INTERRUPTED = "ENCODING_INTERRUPTED"
        const val ERROR_OUTPUT_MISSING = "ENCODED_OUTPUT_MISSING"
    }
}
