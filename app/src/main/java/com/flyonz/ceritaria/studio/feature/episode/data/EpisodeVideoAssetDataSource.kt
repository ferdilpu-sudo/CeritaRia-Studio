package com.flyonz.ceritaria.studio.feature.episode.data

interface EpisodeVideoAssetDataSource {
    suspend fun attachReadyAsset(
        episodeId: String,
        assetId: String,
    ): EpisodeVideoAttachmentDto

    suspend fun getPreview(assetId: String): EpisodeVideoPreviewDto
}
