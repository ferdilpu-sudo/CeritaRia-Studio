package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import javax.inject.Inject

class SupabaseEpisodeDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) {
    suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto> {
        val client = requireNotNull(clientProvider.clientOrNull) { "Supabase is not configured." }
        val start = query.page.toLong() * query.pageSize
        val end = start + query.pageSize

        return client.from("episodes").select(columns = EPISODE_COLUMNS) {
            filter {
                exact("deleted_at", null)
                query.seriesId?.let { eq("series_id", it) }
                applyStatus(query.status)
                applyProvider(query.provider)
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
        }.decodeList<EpisodeRowDto>()
    }

    suspend fun fetchEpisodeById(id: String): EpisodeRowDto? {
        val client = requireNotNull(clientProvider.clientOrNull) { "Supabase is not configured." }
        return client.from("episodes").select(columns = EPISODE_COLUMNS) {
            filter {
                eq("id", id)
                exact("deleted_at", null)
            }
            limit(1)
        }.decodeList<EpisodeRowDto>().firstOrNull()
    }

    private fun PostgrestFilterBuilder.applyStatus(status: EpisodeStatusFilter) {
        when (status) {
            EpisodeStatusFilter.ALL -> Unit
            EpisodeStatusFilter.PUBLISHED -> eq("is_published", true)
            EpisodeStatusFilter.DRAFT -> {
                eq("is_published", false)
                exact("published_at", null)
            }
            EpisodeStatusFilter.UNPUBLISHED -> {
                eq("is_published", false)
                filterNot("published_at", FilterOperator.IS, null)
            }
        }
    }

    private fun PostgrestFilterBuilder.applyProvider(provider: VideoProviderFilter) {
        when (provider) {
            VideoProviderFilter.ALL -> Unit
            VideoProviderFilter.YOUTUBE -> eq("video_provider", "youtube")
            VideoProviderFilter.FACEBOOK -> eq("video_provider", "facebook")
        }
    }

    private companion object {
        val EPISODE_COLUMNS = Columns.raw(
            """
            id,series_id,episode_number,slug,title,short_synopsis,recap,highlights,
            video_provider,video_url,thumbnail_url,duration_seconds,is_published,published_at,
            seo_title,seo_description,created_at,updated_at,deleted_at,
            series(id,title)
            """.trimIndent(),
        )
    }
}
