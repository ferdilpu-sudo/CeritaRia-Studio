package com.flyonz.ceritaria.studio.feature.episode.detail

sealed interface EpisodeDetailEffect {
    data object Deleted : EpisodeDetailEffect
}
