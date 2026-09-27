package com.flyonz.ceritaria.studio.feature.series.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter
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
class SeriesListViewModel @Inject constructor(
    private val repository: SeriesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SeriesListUiState())
    val state: StateFlow<SeriesListUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
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

    fun setFilter(filter: SeriesFilter) {
        if (filter == mutableState.value.filter) return
        mutableState.update { it.copy(filter = filter) }
        load(reset = true)
    }

    fun refresh() = load(reset = true, refreshing = true)

    fun loadMore() {
        val current = mutableState.value
        if (!current.hasMore || current.isLoadingMore || current.isLoading) return
        load(reset = false)
    }

    private fun load(reset: Boolean, refreshing: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = mutableState.value
            val page = if (reset) 0 else current.nextPage
            setLoadingState(reset, refreshing)
            val request = SeriesQuery(
                page = page,
                pageSize = PAGE_SIZE,
                search = mutableState.value.query,
                filter = mutableState.value.filter,
            )
            when (val result = repository.getSeries(request)) {
                is AppResult.Success -> mutableState.update { state ->
                    val items = if (reset) result.value.items else state.items + result.value.items
                    state.copy(
                        items = items,
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
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
