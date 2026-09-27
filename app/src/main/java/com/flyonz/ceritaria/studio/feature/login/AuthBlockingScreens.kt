package com.flyonz.ceritaria.studio.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun ConfigurationRequiredScreen(onRetry: () -> Unit) {
    BlockingMessage(
        title = stringResource(R.string.configuration_required),
        description = stringResource(R.string.configuration_hint),
        actionLabel = stringResource(R.string.retry),
        onAction = onRetry,
    )
}

@Composable
fun UnauthorizedScreen(email: String?, onSignOut: () -> Unit) {
    val account = email?.let { "\n$it" }.orEmpty()
    BlockingMessage(
        title = stringResource(R.string.unauthorized_title),
        description = stringResource(R.string.unauthorized_message) + account,
        actionLabel = stringResource(R.string.sign_out),
        onAction = onSignOut,
    )
}

@Composable
private fun BlockingMessage(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = description,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
