package com.flyonz.ceritaria.studio.feature.login

object LoginValidator {
    fun validate(email: String, password: String): LoginValidationResult {
        if (!EMAIL_PATTERN.matches(email.trim())) {
            return LoginValidationResult.InvalidEmail
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return LoginValidationResult.InvalidPassword
        }
        return LoginValidationResult.Valid
    }

    private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private const val MIN_PASSWORD_LENGTH = 6
}

sealed interface LoginValidationResult {
    data object Valid : LoginValidationResult
    data object InvalidEmail : LoginValidationResult
    data object InvalidPassword : LoginValidationResult
}
