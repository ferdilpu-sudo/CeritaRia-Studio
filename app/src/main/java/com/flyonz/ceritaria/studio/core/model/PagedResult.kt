package com.flyonz.ceritaria.studio.core.model

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val hasMore: Boolean,
)
