package com.flyonz.ceritaria.studio.core.database.videojob

import kotlinx.coroutines.flow.Flow

interface VideoJobRepository {
    suspend fun getById(jobId: String): VideoJob?
    fun observeById(jobId: String): Flow<VideoJob?>
    fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?>
    suspend fun upsert(job: VideoJob)
    suspend fun delete(jobId: String)
}
