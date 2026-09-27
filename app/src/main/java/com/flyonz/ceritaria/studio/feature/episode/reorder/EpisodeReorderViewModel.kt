package com.flyonz.ceritaria.studio.feature.episode.reorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppError
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
class EpisodeReorderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EpisodeRepository,
) : ViewModel() {
    private val seriesId = requireNotNull(savedStateHandle.get<String>("seriesId"))
    private val mutableState = MutableStateFlow(EpisodeReorderUiState())
    val state: StateFlow<EpisodeReorderUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun moveUp(index: Int) = move(index, index - 1)

    fun moveDown(index: Int) = move(index, index + 1)

    fun save() {
        val current = mutableState.value
        if (current.isSaving || !current.isDirty) return

        viewModelScope.launch {
            mutableState.update {
                it.copy(isSaving = true, saveError = null, saveSucceeded = false)
            }
            val ids = current.episodes.map { it.id }
            when (val result = repository.reorderEpisodes(seriesId, ids)) {
                is AppResult.Success -> onSaveSuccess(ids)
                is AppResult.Failure -> mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = result.error.toSaveError(),
                    )
                }
            }
        }
    }

    private fun move(from: Int, to: Int) {
        val current = mutableState.value
        if (current.isSaving || from !in current.episodes.indices || to !in current.episodes.indices) {
            return
        }
        val updated = current.episodes.toMutableList()
        val item = updated.removeAt(from)
        updated.add(to, item)
        mutableState.update {
            it.copy(
                episodes = updated,
                saveError = null,
                saveSucceeded = false,
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isLoading = true,
                    loadFailed = false,
                    saveError = null,
                    saveSucceeded = false,
                )
            }
            when (val result = repository.getEpisodesForReorder(seriesId)) {
                is AppResult.Success -> {
                    val episodes = result.value
                    mutableState.value = EpisodeReorderUiState(
                        isLoading = false,
                        episodes = episodes,
                        originalOrder = episodes.map { it.id },
                    )
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(isLoading = false, loadFailed = true)
                }
            }
        }
    }

    private suspend fun onSaveSuccess(savedIds: List<String>) {
        when (val refreshed = repository.getEpisodesForReorder(seriesId)) {
            is AppResult.Success -> {
                val episodes = refreshed.value
                mutableState.value = EpisodeReorderUiState(
                    isLoading = false,
                    episodes = episodes,
                    originalOrder = episodes.map { it.id },
                    saveSucceeded = true,
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(
                    isSaving = false,
                    originalOrder = savedIds,
                    saveSucceeded = true,
                )
            }
        }
    }

    private fun AppError.toSaveError(): EpisodeReorderSaveError = when (this) {
        AppError.Conflict -> EpisodeReorderSaveError.CONFLICT
        else -> EpisodeReorderSaveError.GENERIC
    }
}
