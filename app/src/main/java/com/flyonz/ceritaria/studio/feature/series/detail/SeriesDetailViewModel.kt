package com.flyonz.ceritaria.studio.feature.series.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SeriesRepository,
) : ViewModel() {
    private val seriesId = requireNotNull(savedStateHandle.get<String>("seriesId"))
    private val mutableState = MutableStateFlow(SeriesDetailUiState())
    val state: StateFlow<SeriesDetailUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<SeriesDetailEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun retry() = load()

    fun delete() {
        if (mutableState.value.isDeleting) return
        viewModelScope.launch {
            mutableState.update { it.copy(isDeleting = true, deleteError = false) }
            when (repository.softDeleteSeries(seriesId)) {
                is AppResult.Success -> {
                    mutableState.update { it.copy(isDeleting = false) }
                    effectChannel.send(SeriesDetailEffect.Deleted)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(isDeleting = false, deleteError = true)
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, hasError = false) }
            when (val result = repository.getSeriesById(seriesId)) {
                is AppResult.Success -> mutableState.value = SeriesDetailUiState(
                    isLoading = false,
                    series = result.value,
                    hasError = result.value == null,
                )
                is AppResult.Failure -> mutableState.value = SeriesDetailUiState(
                    isLoading = false,
                    hasError = true,
                )
            }
        }
    }
}
