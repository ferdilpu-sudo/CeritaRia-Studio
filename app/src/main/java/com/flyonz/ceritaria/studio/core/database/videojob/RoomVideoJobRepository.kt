package com.flyonz.ceritaria.studio.core.database.videojob

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomVideoJobRepository @Inject constructor(
    private val dao: VideoJobDao,
) : VideoJobRepository {
    override suspend fun getById(jobId: String): VideoJob? =
        dao.getById(jobId)?.toDomain()

    override fun observeById(jobId: String): Flow<VideoJob?> =
        dao.observeById(jobId).map { it?.toDomain() }

    override fun observeLatestForEpisode(episodeId: String): Flow<VideoJob?> =
        dao.observeLatestForEpisode(episodeId).map { it?.toDomain() }

    override suspend fun upsert(job: VideoJob) {
        dao.upsert(job.toEntity())
    }

    override suspend fun delete(jobId: String) {
        dao.delete(jobId)
    }
}
