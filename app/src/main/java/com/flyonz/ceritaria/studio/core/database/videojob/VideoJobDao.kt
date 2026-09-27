package com.flyonz.ceritaria.studio.core.database.videojob

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoJobDao {
    @Query("SELECT * FROM video_jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun getById(jobId: String): VideoJobEntity?

    @Query("SELECT * FROM video_jobs WHERE jobId = :jobId LIMIT 1")
    fun observeById(jobId: String): Flow<VideoJobEntity?>

    @Query(
        """
        SELECT * FROM video_jobs
        WHERE episodeId = :episodeId
        ORDER BY updatedAtEpochMs DESC
        LIMIT 1
        """,
    )
    fun observeLatestForEpisode(episodeId: String): Flow<VideoJobEntity?>

    @Upsert
    suspend fun upsert(entity: VideoJobEntity)

    @Query("DELETE FROM video_jobs WHERE jobId = :jobId")
    suspend fun delete(jobId: String)
}
