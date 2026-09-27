package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeSaveCommand
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val dataSource: EpisodeDataSource,
) : EpisodeRepository {
    override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> =
        runOperation {
            val rows = dataSource.fetchEpisodes(query)
            PagedResult(
                items = rows.take(query.pageSize).map(EpisodeRowDto::toDomain),
                page = query.page,
                hasMore = rows.size > query.pageSize,
            )
        }

    override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
        runOperation { dataSource.fetchEpisodeById(id)?.toDomain() }

    override suspend fun saveEpisode(command: EpisodeSaveCommand): AppResult<Episode> =
        runOperation {
            val id = command.id ?: UUID.randomUUID().toString()
            val payload = command.toWriteDto(id)
            val row = if (command.id == null) {
                dataSource.createEpisode(payload)
            } else {
                dataSource.updateEpisode(payload)
            }
            row.toDomain()
        }

    override suspend fun softDeleteEpisode(id: String): AppResult<Unit> =
        runOperation { dataSource.softDeleteEpisode(id) }

    private fun EpisodeSaveCommand.toWriteDto(id: String): EpisodeWriteDto {
        val publishedAt = when {
            isPublished -> existingPublishedAt ?: Instant.now()
            else -> existingPublishedAt
        }
        return EpisodeWriteDto(
            id = id,
            seriesId = seriesId,
            episodeNumber = episodeNumber,
            slug = slug,
            title = title,
            shortSynopsis = shortSynopsis,
            recap = recap,
            highlights = highlights,
            videoProvider = videoProvider.rawValue,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            isPublished = isPublished,
            publishedAt = publishedAt?.toString(),
            seoTitle = seoTitle,
            seoDescription = seoDescription,
        )
    }

    private suspend fun <T> runOperation(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (_: EpisodeConflictException) {
        AppResult.Failure(AppError.Conflict)
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Configuration)
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }
}
