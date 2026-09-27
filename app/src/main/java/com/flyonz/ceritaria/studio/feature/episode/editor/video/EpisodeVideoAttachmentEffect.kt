package com.flyonz.ceritaria.studio.feature.episode.editor.video

sealed interface EpisodeVideoAttachmentEffect {
    data class Attached(
        val assetId: String,
        val replacedAssetId: String?,
    ) : EpisodeVideoAttachmentEffect
}
