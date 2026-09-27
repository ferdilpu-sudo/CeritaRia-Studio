package com.flyonz.ceritaria.studio.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.auth.AuthRepository
import com.flyonz.ceritaria.studio.core.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.state

    init {
        viewModelScope.launch { authRepository.restoreSession() }
    }

    fun retrySession() {
        viewModelScope.launch { authRepository.restoreSession() }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
