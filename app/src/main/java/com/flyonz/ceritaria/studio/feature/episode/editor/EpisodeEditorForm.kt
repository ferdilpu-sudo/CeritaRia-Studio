package com.flyonz.ceritaria.studio.feature.episode.editor

import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProvider

data class EpisodeEditorForm(
    val seriesId: String = "",
    val episodeNumber: String = "1",
    val slug: String = "",
    val title: String = "",
    val shortSynopsis: String = "",
    val recap: String = "",
    val highlights: String = "",
    val videoProvider: String = "youtube",
    val videoUrl: String = "",
    val videoAssetId: String? = null,
    val thumbnailUrl: String = "",
    val durationSeconds: String = "",
    val isPublished: Boolean = false,
    val seoTitle: String = "",
    val seoDescription: String = "",
) {
    companion object {
        fun from(episode: Episode): EpisodeEditorForm = EpisodeEditorForm(
            seriesId = episode.seriesId,
            episodeNumber = episode.episodeNumber.toString(),
            slug = episode.slug,
            title = episode.title,
            shortSynopsis = episode.shortSynopsis.orEmpty(),
            recap = episode.recap.orEmpty(),
            highlights = episode.highlights.joinToString("\n"),
            videoProvider = episode.videoProvider.rawValue(),
            videoUrl = episode.videoUrl.orEmpty(),
            videoAssetId = episode.videoAssetId,
            thumbnailUrl = episode.thumbnailUrl.orEmpty(),
            durationSeconds = episode.durationSeconds?.toString().orEmpty(),
            isPublished = episode.publishStatus == PublishStatus.PUBLISHED,
            seoTitle = episode.seoTitle.orEmpty(),
            seoDescription = episode.seoDescription.orEmpty(),
        )
    }
}

private fun VideoProvider.rawValue(): String = when (this) {
    VideoProvider.YouTube -> "youtube"
    VideoProvider.Facebook -> "facebook"
    VideoProvider.R2 -> "r2"
    is VideoProvider.Unknown -> rawValue
}
