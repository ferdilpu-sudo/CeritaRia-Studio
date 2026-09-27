package com.flyonz.ceritaria.studio.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.app.navigation.StudioShell
import com.flyonz.ceritaria.studio.core.auth.AuthState
import com.flyonz.ceritaria.studio.feature.login.ConfigurationRequiredScreen
import com.flyonz.ceritaria.studio.feature.login.LoginScreen
import com.flyonz.ceritaria.studio.feature.login.UnauthorizedScreen

@Composable
fun CeritariaStudioApp(
    viewModel: AppSessionViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthState.Initializing -> LoadingScreen()
        AuthState.ConfigurationRequired -> ConfigurationRequiredScreen(onRetry = viewModel::retrySession)
        AuthState.SignedOut -> LoginScreen()
        is AuthState.Unauthorized -> UnauthorizedScreen(
            email = state.email,
            onSignOut = viewModel::signOut,
        )
        is AuthState.Authenticated -> StudioShell(
            userEmail = state.user.email,
            onSignOut = viewModel::signOut,
        )
        is AuthState.Failed -> ErrorScreen(onRetry = viewModel::retrySession)
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.generic_error))
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
