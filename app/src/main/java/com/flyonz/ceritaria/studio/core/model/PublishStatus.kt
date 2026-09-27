package com.flyonz.ceritaria.studio.core.model

enum class PublishStatus {
    DRAFT,
    PUBLISHED,
    UNPUBLISHED,
    DELETED,
}

fun resolvePublishStatus(
    isPublished: Boolean,
    publishedAt: String?,
    deletedAt: String?,
): PublishStatus = when {
    deletedAt != null -> PublishStatus.DELETED
    isPublished -> PublishStatus.PUBLISHED
    publishedAt != null -> PublishStatus.UNPUBLISHED
    else -> PublishStatus.DRAFT
}
