package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EpisodeErrorState(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.episodes_load_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
fun EpisodeEmptyState(hasQuery: Boolean) {
    Text(
        if (hasQuery) {
            stringResource(R.string.episodes_search_empty)
        } else {
            stringResource(R.string.episodes_empty)
        },
    )
}
