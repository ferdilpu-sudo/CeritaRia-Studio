package com.flyonz.ceritaria.studio.feature.episode.editor

data class EpisodeSeriesOption(
    val id: String,
    val title: String,
)

data class EpisodeEditorUiState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val form: EpisodeEditorForm = EpisodeEditorForm(),
    val initialForm: EpisodeEditorForm = EpisodeEditorForm(),
    val seriesOptions: List<EpisodeSeriesOption> = emptyList(),
    val validationErrors: Map<EpisodeEditorField, EpisodeValidationIssue> = emptyMap(),
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val saveError: EpisodeEditorSaveError? = null,
) {
    val isDirty: Boolean
        get() = form != initialForm
}

enum class EpisodeEditorSaveError {
    CONFLICT,
    GENERIC,
}
