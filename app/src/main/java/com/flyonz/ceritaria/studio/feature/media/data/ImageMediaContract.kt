package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot

data class ImageMediaContract(
    val bucket: String,
)

fun ImageMediaSlot.contract(): ImageMediaContract = when (this) {
    ImageMediaSlot.SERIES_COVER -> ImageMediaContract(bucket = "series-media")
    ImageMediaSlot.SERIES_HERO -> ImageMediaContract(bucket = "series-media")
    ImageMediaSlot.EPISODE_THUMBNAIL -> ImageMediaContract(bucket = "episode-media")
}
