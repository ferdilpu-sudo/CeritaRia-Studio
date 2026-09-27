package com.flyonz.ceritaria.studio.feature.media.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesCoverReferencePatch(
    @SerialName("cover_url") val publicUrl: String?,
)

@Serializable
data class SeriesHeroReferencePatch(
    @SerialName("hero_url") val publicUrl: String?,
)

@Serializable
data class EpisodeThumbnailReferencePatch(
    @SerialName("thumbnail_url") val publicUrl: String?,
)
