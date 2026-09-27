package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class SupabaseSeriesDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : SeriesDataSource {
    override suspend fun fetchSeries(query: SeriesQuery): List<SeriesRowDto> {
        val client = requireNotNull(clientProvider.clientOrNull) { "Supabase is not configured." }
        val start = query.page.toLong() * query.pageSize
        val end = start + query.pageSize

        return client.from("series").select {
            filter {
                exact("deleted_at", null)
                applyFilter(query.filter)
                val search = query.search.trim()
                if (search.isNotEmpty()) {
                    or {
                        ilike("title", "%$search%")
                        ilike("slug", "%$search%")
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            range(start..end)
        }.decodeList<SeriesRowDto>()
    }

    override suspend fun fetchSeriesById(id: String): SeriesRowDto? {
        val client = requireNotNull(clientProvider.clientOrNull) { "Supabase is not configured." }
        return client.from("series").select {
            filter {
                eq("id", id)
                exact("deleted_at", null)
            }
            limit(1)
        }.decodeList<SeriesRowDto>().firstOrNull()
    }

    private fun io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder.applyFilter(
        filter: SeriesFilter,
    ) {
        when (filter) {
            SeriesFilter.ALL -> Unit
            SeriesFilter.PUBLISHED -> eq("is_published", true)
            SeriesFilter.DRAFT -> {
                eq("is_published", false)
                exact("published_at", null)
            }
            SeriesFilter.FEATURED -> eq("is_featured", true)
        }
    }
}
