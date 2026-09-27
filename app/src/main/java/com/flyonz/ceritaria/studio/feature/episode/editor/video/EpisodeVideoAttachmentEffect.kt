package com.flyonz.ceritaria.studio.feature.episode.editor.video

sealed interface EpisodeVideoAttachmentEffect {
    data class Attached(
        val assetId: String,
        val replacedAssetId: String?,
    ) : EpisodeVideoAttachmentEffect

    data class PreviewReady(
        val url: String,
    ) : EpisodeVideoAttachmentEffect
}
