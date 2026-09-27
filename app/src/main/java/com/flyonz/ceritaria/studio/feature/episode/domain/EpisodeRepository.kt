package com.flyonz.ceritaria.studio.feature.episode.domain

import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult

interface EpisodeRepository {
    suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>>
    suspend fun getEpisodeById(id: String): AppResult<Episode?>
    suspend fun saveEpisode(command: EpisodeSaveCommand): AppResult<Episode>
    suspend fun softDeleteEpisode(id: String): AppResult<Unit>
}
