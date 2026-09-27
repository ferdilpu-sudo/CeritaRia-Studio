package com.flyonz.ceritaria.studio.feature.media.source

import com.flyonz.ceritaria.studio.feature.media.domain.ImageValidationIssue

class ImageSourceException(
    val issue: ImageValidationIssue?,
    message: String,
) : IllegalArgumentException(message)
