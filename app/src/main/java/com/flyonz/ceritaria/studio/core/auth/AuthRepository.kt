package com.flyonz.ceritaria.studio.core.auth

import com.flyonz.ceritaria.studio.core.error.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>

    suspend fun restoreSession(): AppResult<Unit>
    suspend fun signIn(email: String, password: String): AppResult<Unit>
    suspend fun signOut(): AppResult<Unit>
}
