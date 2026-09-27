package com.flyonz.ceritaria.studio.feature.episode.data

class EpisodeVideoAssetApiException(
    val code: String,
    val statusCode: Int,
) : IllegalStateException(code)
