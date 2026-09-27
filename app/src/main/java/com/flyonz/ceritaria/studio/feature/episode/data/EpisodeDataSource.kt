package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery

interface EpisodeDataSource {
    suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto>
    suspend fun fetchEpisodeById(id: String): EpisodeRowDto?
    suspend fun fetchEpisodesForReorder(seriesId: String): List<EpisodeRowDto>
    suspend fun createEpisode(payload: EpisodeWriteDto): EpisodeRowDto
    suspend fun updateEpisode(payload: EpisodeWriteDto): EpisodeRowDto
    suspend fun softDeleteEpisode(id: String)
    suspend fun reorderEpisodes(seriesId: String, orderedEpisodeIds: List<String>)
}
