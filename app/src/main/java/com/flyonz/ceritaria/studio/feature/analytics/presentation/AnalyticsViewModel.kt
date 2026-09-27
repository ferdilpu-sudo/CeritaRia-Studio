package com.flyonz.ceritaria.studio.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(refreshing = false)
    }

    fun setRange(range: AnalyticsRange) {
        if (range == mutableState.value.range) return
        mutableState.update {
            it.copy(
                range = range,
                report = null,
                isLoading = true,
                isRefreshing = false,
                hasError = false,
            )
        }
        load(refreshing = false)
    }

    fun refresh() {
        if (mutableState.value.isRefreshing) return
        load(refreshing = true)
    }

    fun retry() = load(refreshing = false)

    private fun load(refreshing: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val range = mutableState.value.range
            mutableState.update {
                it.copy(
                    isLoading = !refreshing && it.report == null,
                    isRefreshing = refreshing,
                    hasError = false,
                )
            }

            when (val result = repository.getReport(range)) {
                is AppResult.Success -> mutableState.update {
                    if (it.range != range) return@update it
                    it.copy(
                        report = result.value,
                        isLoading = false,
                        isRefreshing = false,
                        hasError = false,
                    )
                }
                is AppResult.Failure -> mutableState.update {
                    if (it.range != range) return@update it
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasError = true,
                    )
                }
            }
        }
    }
}
