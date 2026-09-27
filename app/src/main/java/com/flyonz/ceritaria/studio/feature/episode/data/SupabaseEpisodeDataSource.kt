package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseEpisodeDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : EpisodeDataSource {
    override suspend fun fetchEpisodes(query: EpisodeQuery): List<EpisodeRowDto> {
        val client = requireClient()
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

    override suspend fun fetchEpisodeById(id: String): EpisodeRowDto? =
        requireClient().from("episodes").select(columns = EPISODE_COLUMNS) {
            filter {
                eq("id", id)
                exact("deleted_at", null)
            }
            limit(1)
        }.decodeList<EpisodeRowDto>().firstOrNull()

    override suspend fun fetchEpisodesForReorder(seriesId: String): List<EpisodeRowDto> =
        requireClient().from("episodes").select(columns = EPISODE_COLUMNS) {
            filter {
                eq("series_id", seriesId)
                exact("deleted_at", null)
            }
            order("episode_number", Order.ASCENDING)
        }.decodeList<EpisodeRowDto>()

    override suspend fun createEpisode(payload: EpisodeWriteDto): EpisodeRowDto =
        mapConflict {
            requireClient().from("episodes").insert(payload) {
                select()
            }.decodeSingle<EpisodeRowDto>()
        }

    override suspend fun updateEpisode(payload: EpisodeWriteDto): EpisodeRowDto =
        mapConflict {
            requireClient().from("episodes").update(payload) {
                select()
                filter { eq("id", payload.id) }
            }.decodeSingle<EpisodeRowDto>()
        }

    override suspend fun softDeleteEpisode(id: String) {
        requireClient().from("episodes").update(
            EpisodeDeleteDto(deletedAt = Instant.now().toString()),
        ) {
            filter { eq("id", id) }
        }
    }

    override suspend fun reorderEpisodes(
        seriesId: String,
        orderedEpisodeIds: List<String>,
    ) {
        mapConflict {
            requireClient().postgrest.rpc(
                function = "reorder_episodes",
                parameters = buildJsonObject {
                    put("target_series_id", seriesId)
                    put(
                        "ordered_episode_ids",
                        JsonArray(orderedEpisodeIds.map(::JsonPrimitive)),
                    )
                },
            )
        }
    }

    private fun requireClient() = requireNotNull(clientProvider.clientOrNull) {
        "Supabase is not configured."
    }

    private suspend fun <T> mapConflict(block: suspend () -> T): T = try {
        block()
    } catch (error: PostgrestRestException) {
        if (error.code == UNIQUE_VIOLATION || error.code == INVALID_PARAMETER) {
            throw EpisodeConflictException()
        }
        throw error
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
        const val UNIQUE_VIOLATION = "23505"
        const val INVALID_PARAMETER = "22023"
        val EPISODE_COLUMNS = Columns.raw(
            """
            id,series_id,episode_number,slug,title,short_synopsis,recap,highlights,
            video_provider,video_url,video_asset_id,thumbnail_url,duration_seconds,is_published,published_at,
            seo_title,seo_description,created_at,updated_at,deleted_at,
            series(id,title)
            """.trimIndent(),
        )
    }
}
