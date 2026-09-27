package com.flyonz.ceritaria.studio.feature.series.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.core.designsystem.component.RemoteArtwork
import com.flyonz.ceritaria.studio.feature.series.domain.Series
import com.flyonz.ceritaria.studio.feature.series.list.displayName

@Composable
fun SeriesDetailContent(
    series: Series,
    isDeleting: Boolean,
    deleteError: Boolean,
    onEpisodesClick: (String) -> Unit,
    onReorderClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RemoteArtwork(
            imageUrl = series.coverUrl,
            fallbackText = series.title,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        Text(series.title, style = MaterialTheme.typography.headlineSmall)
        Text(series.publishStatus.displayName(), color = MaterialTheme.colorScheme.primary)
        if (series.isFeatured) Text(stringResource(R.string.featured))

        Button(onClick = { onEditClick(series.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.edit_series))
        }
        OutlinedButton(
            onClick = { onEpisodesClick(series.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.view_series_episodes))
        }
        OutlinedButton(
            onClick = { onReorderClick(series.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.reorder_episodes))
        }

        DetailField(stringResource(R.string.slug), series.slug)
        DetailField(stringResource(R.string.short_synopsis), series.shortSynopsis)
        DetailField(stringResource(R.string.synopsis), series.synopsis)
        DetailField(stringResource(R.string.genres), series.genres.joinToString(", "))
        DetailField(stringResource(R.string.cover_url), series.coverUrl)
        DetailField(stringResource(R.string.hero_url), series.heroUrl)
        HorizontalDivider()
        DetailField(stringResource(R.string.seo_title), series.seoTitle)
        DetailField(stringResource(R.string.seo_description), series.seoDescription)
        HorizontalDivider()

        if (deleteError) {
            Text(
                text = stringResource(R.string.series_delete_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(
            onClick = onDeleteClick,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (isDeleting) R.string.deleting else R.string.delete,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
