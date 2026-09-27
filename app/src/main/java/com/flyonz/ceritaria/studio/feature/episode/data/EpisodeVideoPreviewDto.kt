package com.flyonz.ceritaria.studio.feature.episode.data

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeVideoPreviewDto(
    val assetId: String,
    val status: String,
    val url: String,
    val expiresInSeconds: Int,
)
