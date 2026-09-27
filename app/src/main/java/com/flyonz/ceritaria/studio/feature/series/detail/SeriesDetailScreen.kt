package com.flyonz.ceritaria.studio.feature.series.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.series.list.displayName

@Composable
fun SeriesDetailScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = { Text(state.series?.title ?: stringResource(R.string.series_detail)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )

        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.hasError || state.series == null -> Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.series_detail_error))
                Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            else -> {
                val series = requireNotNull(state.series)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(series.title, style = MaterialTheme.typography.headlineSmall)
                    Text(series.publishStatus.displayName(), color = MaterialTheme.colorScheme.primary)
                    if (series.isFeatured) Text(stringResource(R.string.filter_featured))
                    DetailField(stringResource(R.string.slug), series.slug)
                    DetailField(stringResource(R.string.short_synopsis), series.shortSynopsis)
                    DetailField(stringResource(R.string.synopsis), series.synopsis)
                    DetailField(stringResource(R.string.genres), series.genres.joinToString(", "))
                    DetailField(stringResource(R.string.cover_url), series.coverUrl)
                    DetailField(stringResource(R.string.hero_url), series.heroUrl)
                    HorizontalDivider()
                    DetailField(stringResource(R.string.seo_title), series.seoTitle)
                    DetailField(stringResource(R.string.seo_description), series.seoDescription)
                }
            }
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
