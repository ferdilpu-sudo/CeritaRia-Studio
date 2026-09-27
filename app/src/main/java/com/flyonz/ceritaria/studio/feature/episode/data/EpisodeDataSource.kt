package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery

interface EpisodeDataSource {
    suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto>
    suspend fun fetchEpisodeById(id: String): EpisodeRowDto?
}
