package com.flyonz.ceritaria.studio.app

import com.flyonz.ceritaria.studio.core.auth.AuthRepository
import com.flyonz.ceritaria.studio.core.auth.AuthState
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initRestoresSession() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        AppSessionViewModel(repository)

        advanceUntilIdle()

        assertEquals(1, repository.restoreCalls)
    }

    @Test
    fun retryAndSignOutDelegateToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = AppSessionViewModel(repository)
            advanceUntilIdle()

            viewModel.retrySession()
            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(2, repository.restoreCalls)
            assertEquals(1, repository.signOutCalls)
        }

    private class FakeAuthRepository : AuthRepository {
        private val mutableState = MutableStateFlow<AuthState>(AuthState.Initializing)
        override val state: StateFlow<AuthState> = mutableState

        var restoreCalls = 0
        var signOutCalls = 0

        override suspend fun restoreSession(): AppResult<Unit> {
            restoreCalls += 1
            return AppResult.Success(Unit)
        }

        override suspend fun signIn(
            email: String,
            password: String,
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun signOut(): AppResult<Unit> {
            signOutCalls += 1
            mutableState.value = AuthState.SignedOut
            return AppResult.Success(Unit)
        }
    }
}
