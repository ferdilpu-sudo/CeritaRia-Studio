package com.flyonz.ceritaria.studio.feature.episode.domain

data class EpisodeVideoAttachment(
    val episodeId: String,
    val assetId: String,
    val replacedAssetId: String?,
)
