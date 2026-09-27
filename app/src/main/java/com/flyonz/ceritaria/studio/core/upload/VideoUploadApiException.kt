package com.flyonz.ceritaria.studio.core.upload

class VideoUploadApiException(
    val code: String,
    val statusCode: Int,
) : IllegalStateException(code)
