package com.flyonz.ceritaria.studio.feature.episode.domain

sealed interface VideoProvider {
    data object YouTube : VideoProvider
    data object Facebook : VideoProvider
    data object R2 : VideoProvider
    data class Unknown(val rawValue: String) : VideoProvider
}

fun videoProviderFrom(rawValue: String): VideoProvider = when (rawValue.lowercase()) {
    "youtube" -> VideoProvider.YouTube
    "facebook" -> VideoProvider.Facebook
    "r2" -> VideoProvider.R2
    else -> VideoProvider.Unknown(rawValue)
}
