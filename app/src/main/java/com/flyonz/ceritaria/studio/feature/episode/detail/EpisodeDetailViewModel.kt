package com.flyonz.ceritaria.studio.feature.episode.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EpisodeRepository,
) : ViewModel() {
    private val episodeId = requireNotNull(savedStateHandle.get<String>("episodeId"))
    private val mutableState = MutableStateFlow(EpisodeDetailUiState())
    val state: StateFlow<EpisodeDetailUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, hasError = false) }
            when (val result = repository.getEpisodeById(episodeId)) {
                is AppResult.Success -> mutableState.value = EpisodeDetailUiState(
                    isLoading = false,
                    episode = result.value,
                    hasError = result.value == null,
                )
                is AppResult.Failure -> mutableState.value = EpisodeDetailUiState(
                    isLoading = false,
                    hasError = true,
                )
            }
        }
    }
}
