package com.flyonz.ceritaria.studio.feature.episode.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeStatusFilter
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProviderFilter

@Composable
fun EpisodeSeriesFilter(
    options: List<SeriesOption>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = options.firstOrNull { it.id == selectedId }?.title

    Box {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedTitle ?: stringResource(R.string.all_series))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_series)) },
                onClick = {
                    expanded = false
                    onSelected(null)
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    },
                )
            }
        }
    }
}

@Composable
fun EpisodeStatusFilters(
    selected: EpisodeStatusFilter,
    onSelected: (EpisodeStatusFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(EpisodeStatusFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
fun EpisodeProviderFilters(
    selected: VideoProviderFilter,
    onSelected: (VideoProviderFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(VideoProviderFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun EpisodeStatusFilter.label(): String = when (this) {
    EpisodeStatusFilter.ALL -> stringResource(R.string.filter_all)
    EpisodeStatusFilter.PUBLISHED -> stringResource(R.string.status_published)
    EpisodeStatusFilter.DRAFT -> stringResource(R.string.status_draft)
    EpisodeStatusFilter.UNPUBLISHED -> stringResource(R.string.status_unpublished)
}

@Composable
private fun VideoProviderFilter.label(): String = when (this) {
    VideoProviderFilter.ALL -> stringResource(R.string.filter_all)
    VideoProviderFilter.YOUTUBE -> stringResource(R.string.youtube)
    VideoProviderFilter.FACEBOOK -> stringResource(R.string.facebook)
}
