package com.flyonz.ceritaria.studio.core.auth

import com.flyonz.ceritaria.studio.core.error.AppError

sealed interface AuthState {
    data object Initializing : AuthState
    data object ConfigurationRequired : AuthState
    data object SignedOut : AuthState
    data class Unauthorized(val email: String?) : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Failed(val error: AppError) : AuthState
}
