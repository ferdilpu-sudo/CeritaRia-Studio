package com.flyonz.ceritaria.studio.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.auth.AuthRepository
import com.flyonz.ceritaria.studio.core.error.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun setEmail(value: String) = mutableState.update { it.copy(email = value, error = null, submissionFailed = false) }

    fun setPassword(value: String) = mutableState.update { it.copy(password = value, error = null, submissionFailed = false) }

    fun signIn() {
        val current = mutableState.value
        val validation = LoginValidator.validate(current.email, current.password)
        if (validation != LoginValidationResult.Valid) {
            mutableState.update { it.copy(error = validation) }
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isSubmitting = true, error = null, submissionFailed = false) }
            when (authRepository.signIn(current.email.trim(), current.password)) {
                is AppResult.Success -> mutableState.update { it.copy(isSubmitting = false) }
                is AppResult.Failure -> mutableState.update { it.copy(isSubmitting = false, submissionFailed = true) }
            }
        }
    }
}
