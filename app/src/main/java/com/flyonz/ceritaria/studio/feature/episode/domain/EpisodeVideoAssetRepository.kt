package com.flyonz.ceritaria.studio.feature.episode.domain

import com.flyonz.ceritaria.studio.core.error.AppResult

interface EpisodeVideoAssetRepository {
    suspend fun attachReadyAsset(
        episodeId: String,
        assetId: String,
    ): AppResult<EpisodeVideoAttachment>

    suspend fun getPreview(assetId: String): AppResult<EpisodeVideoPreview>
}
