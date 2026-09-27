package com.flyonz.ceritaria.studio.feature.login

import com.flyonz.ceritaria.studio.core.auth.AuthRepository
import com.flyonz.ceritaria.studio.core.auth.AuthState
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun invalidEmailDoesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = LoginViewModel(repository)
            viewModel.setEmail("invalid")
            viewModel.setPassword("secret1")

            viewModel.signIn()
            advanceUntilIdle()

            assertEquals(0, repository.signInCalls)
            assertEquals(LoginValidationResult.InvalidEmail, viewModel.state.value.error)
        }

    @Test
    fun successfulSignInTrimsEmailAndClearsSubmitting() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = LoginViewModel(repository)
            viewModel.setEmail("  admin@example.com  ")
            viewModel.setPassword("secret1")

            viewModel.signIn()
            advanceUntilIdle()

            assertEquals(1, repository.signInCalls)
            assertEquals("admin@example.com", repository.lastEmail)
            assertFalse(viewModel.state.value.isSubmitting)
            assertFalse(viewModel.state.value.submissionFailed)
        }

    @Test
    fun duplicateSignInWhileSubmittingIsIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = LoginViewModel(repository)
            viewModel.setEmail("admin@example.com")
            viewModel.setPassword("secret1")

            viewModel.signIn()
            viewModel.signIn()
            advanceUntilIdle()

            assertEquals(1, repository.signInCalls)
        }

    @Test
    fun failedSignInSurfacesSubmissionFailure() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAuthRepository(failSignIn = true)
            val viewModel = LoginViewModel(repository)
            viewModel.setEmail("admin@example.com")
            viewModel.setPassword("secret1")

            viewModel.signIn()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isSubmitting)
            assertTrue(viewModel.state.value.submissionFailed)
        }

    private class FakeAuthRepository(
        private val failSignIn: Boolean = false,
    ) : AuthRepository {
        private val mutableState = MutableStateFlow<AuthState>(AuthState.SignedOut)
        override val state: StateFlow<AuthState> = mutableState

        var signInCalls = 0
        var lastEmail: String? = null

        override suspend fun restoreSession(): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun signIn(
            email: String,
            password: String,
        ): AppResult<Unit> {
            signInCalls += 1
            lastEmail = email
            return if (failSignIn) {
                AppResult.Failure(AppError.Authentication)
            } else {
                AppResult.Success(Unit)
            }
        }

        override suspend fun signOut(): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}
