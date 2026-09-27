package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeListHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onCreateEpisode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.episodes))
        Button(onClick = onCreateEpisode) {
            Text(stringResource(R.string.new_episode))
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.search_episodes)) },
        singleLine = true,
    )
}
