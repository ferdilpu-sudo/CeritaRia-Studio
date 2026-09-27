package com.flyonz.ceritaria.studio.feature.series.editor

data class SeriesEditorUiState(
    val isLoading: Boolean = true,
    val isEdit: Boolean = false,
    val recordId: String? = null,
    val form: SeriesEditorForm = SeriesEditorForm(),
    val initialForm: SeriesEditorForm = SeriesEditorForm(),
    val validationErrors: Map<SeriesEditorField, SeriesValidationIssue> = emptyMap(),
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val saveError: SeriesEditorSaveError? = null,
) {
    val isDirty: Boolean
        get() = form != initialForm
}

enum class SeriesEditorSaveError {
    DUPLICATE_SLUG,
    GENERIC,
}
