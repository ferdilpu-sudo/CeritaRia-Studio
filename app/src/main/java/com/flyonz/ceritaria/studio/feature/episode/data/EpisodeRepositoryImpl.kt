package com.flyonz.ceritaria.studio.feature.episode.data

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.model.PagedResult
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val dataSource: EpisodeDataSource,
) : EpisodeRepository {
    override suspend fun getEpisodes(query: EpisodeQuery): AppResult<PagedResult<Episode>> =
        runRead {
            val rows = dataSource.fetchEpisodes(query)
            PagedResult(
                items = rows.take(query.pageSize).map(EpisodeRowDto::toDomain),
                page = query.page,
                hasMore = rows.size > query.pageSize,
            )
        }

    override suspend fun getEpisodeById(id: String): AppResult<Episode?> =
        runRead { dataSource.fetchEpisodeById(id)?.toDomain() }

    private suspend fun <T> runRead(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalArgumentException) {
        AppResult.Failure(AppError.Configuration)
    } catch (_: Throwable) {
        AppResult.Failure(AppError.Network)
    }
}
