package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeRepository
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesQuery
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodeRepository: EpisodeRepository,
    private val seriesRepository: SeriesRepository,
) : ViewModel() {
    private val episodeId = savedStateHandle.get<String>("episodeId")
    private val preselectedSeriesId = savedStateHandle.get<String>("seriesId")
    private var existingPublishedAt: Instant? = null

    private val mutableState = MutableStateFlow(
        EpisodeEditorUiState(isEdit = episodeId != null),
    )
    val state: StateFlow<EpisodeEditorUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<EpisodeEditorEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun setForm(form: EpisodeEditorForm) {
        mutableState.update {
            it.copy(
                form = form,
                validationErrors = emptyMap(),
                saveError = null,
            )
        }
    }

    fun retry() = load()

    fun applyMediaReference(slot: ImageMediaSlot, publicUrl: String?) {
        if (slot != ImageMediaSlot.EPISODE_THUMBNAIL) return
        val value = publicUrl.orEmpty()
        mutableState.update { current ->
            current.copy(
                form = current.form.copy(thumbnailUrl = value),
                initialForm = current.initialForm.copy(thumbnailUrl = value),
            )
        }
    }

    fun save() {
        val current = mutableState.value
        if (current.isLoading || current.isSaving) return

        val errors = EpisodeEditorValidator.validate(current.form)
        if (errors.isNotEmpty()) {
            mutableState.update {
                it.copy(validationErrors = errors.associate { error -> error.field to error.issue })
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, saveError = null) }
            val command = current.form.toSaveCommand(episodeId, existingPublishedAt)
            when (val result = episodeRepository.saveEpisode(command)) {
                is AppResult.Success -> onSaveSuccess(result.value)
                is AppResult.Failure -> mutableState.update {
                    it.copy(isSaving = false, saveError = result.error.toSaveError())
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, loadFailed = false) }

            val episode = if (episodeId != null) loadEpisode() else null
            if (episodeId != null && episode == null) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            val requiredSeriesId = episode?.seriesId ?: preselectedSeriesId
            val options = loadSeriesOptions(requiredSeriesId)
            if (options == null) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            existingPublishedAt = episode?.publishedAt
            val form = episode?.let(EpisodeEditorForm::from)
                ?: EpisodeEditorForm(seriesId = requiredSeriesId ?: options.firstOrNull()?.id.orEmpty())
            mutableState.value = EpisodeEditorUiState(
                isLoading = false,
                isEdit = episode != null,
                recordId = episode?.id,
                form = form,
                initialForm = form,
                seriesOptions = options,
            )
        }
    }

    private suspend fun loadEpisode(): Episode? =
        when (val result = episodeRepository.getEpisodeById(requireNotNull(episodeId))) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> null
        }

    private suspend fun loadSeriesOptions(requiredId: String?): List<EpisodeSeriesOption>? {
        val base = when (val result = seriesRepository.getSeries(SeriesQuery(pageSize = OPTION_LIMIT))) {
            is AppResult.Success -> result.value.items.map { EpisodeSeriesOption(it.id, it.title) }
            is AppResult.Failure -> return null
        }
        if (requiredId.isNullOrBlank() || base.any { it.id == requiredId }) return base

        return when (val result = seriesRepository.getSeriesById(requiredId)) {
            is AppResult.Success -> {
                val required = result.value
                if (required == null) base else base + EpisodeSeriesOption(required.id, required.title)
            }
            is AppResult.Failure -> base
        }
    }

    private suspend fun onSaveSuccess(episode: Episode) {
        existingPublishedAt = episode.publishedAt
        val normalized = EpisodeEditorForm.from(episode)
        mutableState.update {
            it.copy(
                isSaving = false,
                isEdit = true,
                recordId = episode.id,
                form = normalized,
                initialForm = normalized,
            )
        }
        effectChannel.send(EpisodeEditorEffect.Saved(episode.id))
    }

    private fun AppError.toSaveError(): EpisodeEditorSaveError = when (this) {
        AppError.Conflict -> EpisodeEditorSaveError.CONFLICT
        else -> EpisodeEditorSaveError.GENERIC
    }

    private companion object {
        const val OPTION_LIMIT = 100
    }
}
