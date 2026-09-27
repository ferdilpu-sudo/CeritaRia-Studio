package com.flyonz.ceritaria.studio.feature.episode.data

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeVideoAttachmentDto(
    val episodeId: String,
    val assetId: String,
    val status: String,
    val replacedAssetId: String? = null,
)
