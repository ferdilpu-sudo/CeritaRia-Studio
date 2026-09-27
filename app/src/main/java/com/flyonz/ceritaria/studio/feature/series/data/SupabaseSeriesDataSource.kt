package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class SupabaseSeriesDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : SeriesDataSource {
    override suspend fun fetchSeries(query: SeriesQuery): List<SeriesRowDto> {
        val client = requireClient()
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
        return requireClient().from("series").select {
            filter {
                eq("id", id)
                exact("deleted_at", null)
            }
            limit(1)
        }.decodeList<SeriesRowDto>().firstOrNull()
    }

    override suspend fun createSeries(payload: SeriesWriteDto): SeriesRowDto =
        mapConflict {
            requireClient().from("series").insert(payload) {
                select()
            }.decodeSingle<SeriesRowDto>()
        }

    override suspend fun updateSeries(payload: SeriesWriteDto): SeriesRowDto =
        mapConflict {
            requireClient().from("series").update(payload) {
                select()
                filter { eq("id", payload.id) }
            }.decodeSingle<SeriesRowDto>()
        }

    override suspend fun softDeleteSeries(id: String) {
        requireClient().postgrest.rpc(
            function = "soft_delete_series",
            parameters = SoftDeleteSeriesParams(targetId = id),
        )
    }

    private fun requireClient() = requireNotNull(clientProvider.clientOrNull) {
        "Supabase is not configured."
    }

    private suspend fun <T> mapConflict(block: suspend () -> T): T = try {
        block()
    } catch (error: PostgrestRestException) {
        if (error.code == UNIQUE_VIOLATION) throw SeriesSlugConflictException()
        throw error
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

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
