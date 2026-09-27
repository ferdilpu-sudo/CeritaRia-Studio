package com.flyonz.ceritaria.studio.core.auth

import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Initializing)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()

    override suspend fun restoreSession(): AppResult<Unit> = runAuthOperation {
        val client = requireClient()
        val session = client.auth.currentSessionOrNull()
        if (session == null) {
            mutableState.value = AuthState.SignedOut
        } else {
            resolveAdminState(session.user?.id, session.user?.email)
        }
    }

    override suspend fun signIn(email: String, password: String): AppResult<Unit> = runAuthOperation(
        failureState = AuthState.SignedOut,
    ) {
        val client = requireClient()
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = client.auth.currentSessionOrNull()
        resolveAdminState(session?.user?.id, session?.user?.email)
    }

    override suspend fun signOut(): AppResult<Unit> = runAuthOperation(failureState = null) {
        val client = requireClient()
        client.auth.signOut()
        mutableState.value = AuthState.SignedOut
    }

    private suspend fun resolveAdminState(userId: String?, email: String?) {
        if (userId.isNullOrBlank()) {
            mutableState.value = AuthState.SignedOut
            return
        }
        val memberships = requireClient()
            .from("admin_users")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<AdminMembershipDto>()

        mutableState.value = if (memberships.isNotEmpty()) {
            AuthState.Authenticated(AuthUser(id = userId, email = email))
        } else {
            AuthState.Unauthorized(email)
        }
    }

    private fun requireClient() = clientProvider.clientOrNull
        ?: throw MissingSupabaseConfigurationException()

    private suspend fun runAuthOperation(
        failureState: AuthState? = AuthState.Failed(AppError.Authentication),
        block: suspend () -> Unit,
    ): AppResult<Unit> = try {
        block()
        AppResult.Success(Unit)
    } catch (_: MissingSupabaseConfigurationException) {
        mutableState.value = AuthState.ConfigurationRequired
        AppResult.Failure(AppError.Configuration)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        failureState?.let { mutableState.value = it }
        AppResult.Failure(AppError.Authentication)
    }
}

private class MissingSupabaseConfigurationException : IllegalStateException()
