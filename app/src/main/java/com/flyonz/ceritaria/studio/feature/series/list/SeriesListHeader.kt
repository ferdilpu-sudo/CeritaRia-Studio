package com.flyonz.ceritaria.studio.feature.series.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.series.domain.SeriesFilter

@Composable
fun SeriesListHeader(
    query: String,
    filter: SeriesFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SeriesFilter) -> Unit,
    onCreateSeries: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.series))
        Button(onClick = onCreateSeries) {
            Text(stringResource(R.string.new_series))
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.search_series)) },
        singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SeriesFilter.entries.forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = { onFilterChange(item) },
                label = { Text(item.label()) },
            )
        }
    }
}

@Composable
private fun SeriesFilter.label(): String = when (this) {
    SeriesFilter.ALL -> stringResource(R.string.filter_all)
    SeriesFilter.PUBLISHED -> stringResource(R.string.status_published)
    SeriesFilter.DRAFT -> stringResource(R.string.status_draft)
    SeriesFilter.FEATURED -> stringResource(R.string.filter_featured)
}
