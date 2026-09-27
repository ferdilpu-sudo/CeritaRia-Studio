package com.flyonz.ceritaria.studio.feature.series.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesSaveCommand
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val dataSource: SeriesDataSource,
) : SeriesRepository {
    override suspend fun getSeries(query: SeriesQuery): AppResult<PagedResult<Series>> =
        runOperation {
            val rows = dataSource.fetchSeries(query)
            val visibleRows = rows.take(query.pageSize)
            PagedResult(
                items = visibleRows.map(SeriesRowDto::toDomain),
                page = query.page,
                hasMore = rows.size > query.pageSize,
            )
        }

    override suspend fun getSeriesById(id: String): AppResult<Series?> =
        runOperation { dataSource.fetchSeriesById(id)?.toDomain() }

    override suspend fun saveSeries(command: SeriesSaveCommand): AppResult<Series> =
        runOperation {
            val id = command.id ?: UUID.randomUUID().toString()
            val payload = command.toWriteDto(id)
            val row = if (command.id == null) {
                dataSource.createSeries(payload)
            } else {
                dataSource.updateSeries(payload)
            }
            row.toDomain()
        }

    override suspend fun softDeleteSeries(id: String): AppResult<Unit> =
        runOperation { dataSource.softDeleteSeries(id) }

    private fun SeriesSaveCommand.toWriteDto(id: String): SeriesWriteDto {
        val publishedAt = when {
            isPublished -> existingPublishedAt ?: Instant.now()
            else -> existingPublishedAt
        }
        return SeriesWriteDto(
            id = id,
            slug = slug,
            title = title,
            shortSynopsis = shortSynopsis,
            synopsis = synopsis,
            genres = genres,
            coverUrl = coverUrl,
            heroUrl = heroUrl,
            isFeatured = isFeatured,
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
    } catch (_: SeriesSlugConflictException) {
        AppResult.Failure(AppError.Conflict)
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Configuration)
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }
}
