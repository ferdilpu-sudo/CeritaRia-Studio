package com.flyonz.ceritaria.studio.feature.episode.domain

sealed interface VideoProvider {
    data object YouTube : VideoProvider
    data object Facebook : VideoProvider
    data class Unknown(val rawValue: String) : VideoProvider
}

fun videoProviderFrom(rawValue: String): VideoProvider = when (rawValue.lowercase()) {
    "youtube" -> VideoProvider.YouTube
    "facebook" -> VideoProvider.Facebook
    else -> VideoProvider.Unknown(rawValue)
}
