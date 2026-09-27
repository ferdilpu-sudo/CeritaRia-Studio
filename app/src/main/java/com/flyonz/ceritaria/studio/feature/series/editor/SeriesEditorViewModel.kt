package com.flyonz.ceritaria.studio.feature.series.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
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
class SeriesEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SeriesRepository,
) : ViewModel() {
    private val seriesId = savedStateHandle.get<String>("seriesId")
    private var existingPublishedAt: Instant? = null
    private val mutableState = MutableStateFlow(
        SeriesEditorUiState(
            isLoading = seriesId != null,
            isEdit = seriesId != null,
        ),
    )
    val state: StateFlow<SeriesEditorUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<SeriesEditorEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        if (seriesId != null) load()
    }

    fun setForm(form: SeriesEditorForm) {
        mutableState.update {
            it.copy(
                form = form,
                validationErrors = emptyMap(),
                saveError = null,
            )
        }
    }

    fun retry() = load()

    fun save() {
        val current = mutableState.value
        if (current.isSaving || current.isLoading) return

        val errors = SeriesEditorValidator.validate(current.form)
        if (errors.isNotEmpty()) {
            mutableState.update {
                it.copy(validationErrors = errors.associate { error -> error.field to error.issue })
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, saveError = null) }
            val command = current.form.toSaveCommand(seriesId, existingPublishedAt)
            when (val result = repository.saveSeries(command)) {
                is AppResult.Success -> {
                    existingPublishedAt = result.value.publishedAt
                    val normalized = SeriesEditorForm.from(result.value)
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            form = normalized,
                            initialForm = normalized,
                        )
                    }
                    effectChannel.send(SeriesEditorEffect.Saved(result.value.id))
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = result.error.toSaveError(),
                    )
                }
            }
        }
    }

    private fun load() {
        val id = seriesId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, loadFailed = false) }
            when (val result = repository.getSeriesById(id)) {
                is AppResult.Success -> {
                    val series = result.value
                    if (series == null) {
                        mutableState.update { it.copy(isLoading = false, loadFailed = true) }
                    } else {
                        existingPublishedAt = series.publishedAt
                        val form = SeriesEditorForm.from(series)
                        mutableState.value = SeriesEditorUiState(
                            isLoading = false,
                            isEdit = true,
                            form = form,
                            initialForm = form,
                        )
                    }
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(isLoading = false, loadFailed = true)
                }
            }
        }
    }

    private fun AppError.toSaveError(): SeriesEditorSaveError = when (this) {
        AppError.Conflict -> SeriesEditorSaveError.DUPLICATE_SLUG
        else -> SeriesEditorSaveError.GENERIC
    }
}
