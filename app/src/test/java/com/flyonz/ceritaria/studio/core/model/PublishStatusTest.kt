package com.flyonz.ceritaria.studio.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PublishStatusTest {
    @Test
    fun freshUnpublishedRowIsDraft() {
        assertEquals(PublishStatus.DRAFT, resolvePublishStatus(false, null, null))
    }

    @Test
    fun previouslyPublishedRowIsUnpublished() {
        assertEquals(
            PublishStatus.UNPUBLISHED,
            resolvePublishStatus(false, "2026-09-01T00:00:00Z", null),
        )
    }

    @Test
    fun publishedRowIsPublished() {
        assertEquals(
            PublishStatus.PUBLISHED,
            resolvePublishStatus(true, "2026-09-01T00:00:00Z", null),
        )
    }

    @Test
    fun deletedStateWinsOverPublishState() {
        assertEquals(
            PublishStatus.DELETED,
            resolvePublishStatus(true, "2026-09-01T00:00:00Z", "2026-09-02T00:00:00Z"),
        )
    }
}
