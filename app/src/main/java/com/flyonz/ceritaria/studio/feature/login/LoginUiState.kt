package com.flyonz.ceritaria.studio.feature.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: LoginValidationResult? = null,
    val submissionFailed: Boolean = false,
)
