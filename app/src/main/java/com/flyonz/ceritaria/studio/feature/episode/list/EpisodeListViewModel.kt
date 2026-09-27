package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeQuery
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val seriesRepository: SeriesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EpisodeListUiState())
    val state: StateFlow<EpisodeListUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadSeriesOptions()
        load(reset = true)
    }

    fun setQuery(value: String) {
        mutableState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
    }

    fun setSeries(id: String?) {
        if (id == mutableState.value.seriesId) return
        mutableState.update { it.copy(seriesId = id) }
        load(reset = true)
    }

    fun setStatus(status: EpisodeStatusFilter) {
        if (status == mutableState.value.status) return
        mutableState.update { it.copy(status = status) }
        load(reset = true)
    }

    fun setProvider(provider: VideoProviderFilter) {
        if (provider == mutableState.value.provider) return
        mutableState.update { it.copy(provider = provider) }
        load(reset = true)
    }

    fun refresh() = load(reset = true, refreshing = true)

    fun loadMore() {
        val current = mutableState.value
        if (!current.hasMore || current.isLoadingMore || current.isLoading) return
        load(reset = false)
    }

    private fun loadSeriesOptions() {
        viewModelScope.launch {
            when (val result = seriesRepository.getSeries(SeriesQuery(pageSize = SERIES_OPTION_LIMIT))) {
                is AppResult.Success -> mutableState.update { state ->
                    state.copy(
                        seriesOptions = result.value.items.map { SeriesOption(it.id, it.title) },
                    )
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun load(reset: Boolean, refreshing: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = mutableState.value
            val page = if (reset) 0 else current.nextPage
            setLoadingState(reset, refreshing)
            val request = EpisodeQuery(
                page = page,
                pageSize = PAGE_SIZE,
                search = mutableState.value.query,
                seriesId = mutableState.value.seriesId,
                status = mutableState.value.status,
                provider = mutableState.value.provider,
            )
            when (val result = episodeRepository.getEpisodes(request)) {
                is AppResult.Success -> mutableState.update { state ->
                    state.copy(
                        items = if (reset) result.value.items else state.items + result.value.items,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasMore = result.value.hasMore,
                        nextPage = page + 1,
                        hasError = false,
                    )
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasError = true,
                    )
                }
            }
        }
    }

    private fun setLoadingState(reset: Boolean, refreshing: Boolean) {
        mutableState.update {
            it.copy(
                isLoading = reset && !refreshing && it.items.isEmpty(),
                isRefreshing = refreshing,
                isLoadingMore = !reset,
                hasError = false,
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SERIES_OPTION_LIMIT = 100
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
