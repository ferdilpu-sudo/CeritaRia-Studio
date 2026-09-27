package com.flyonz.ceritaria.studio.feature.episode.editor

sealed interface EpisodeEditorEffect {
    data class Saved(val episodeId: String) : EpisodeEditorEffect
}
