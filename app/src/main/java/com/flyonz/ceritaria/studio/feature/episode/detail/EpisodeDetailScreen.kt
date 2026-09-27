package com.flyonz.ceritaria.studio.feature.episode.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.flyonz.ceritaria.studio.core.designsystem.component.RemoteArtwork
import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.episode.domain.Episode
import com.flyonz.ceritaria.studio.feature.episode.domain.VideoProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: EpisodeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = { Text(state.episode?.title ?: stringResource(R.string.episode_detail)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )

        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.hasError || state.episode == null -> ErrorContent(onRetry = viewModel::retry)
            else -> EpisodeContent(episode = requireNotNull(state.episode))
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(stringResource(R.string.episode_detail_error))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EpisodeContent(episode: Episode) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RemoteArtwork(
            imageUrl = episode.thumbnailUrl,
            fallbackText = episode.episodeNumber.toString(),
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
        Text(episode.title, style = MaterialTheme.typography.headlineSmall)
        DetailField(stringResource(R.string.status), episode.publishStatus.label())
        DetailField(stringResource(R.string.parent_series), episode.seriesTitle ?: episode.seriesId)
        DetailField(stringResource(R.string.episode_number), episode.episodeNumber.toString())
        DetailField(stringResource(R.string.slug), episode.slug)
        DetailField(stringResource(R.string.short_synopsis), episode.shortSynopsis)
        DetailField(stringResource(R.string.recap), episode.recap)
        DetailField(stringResource(R.string.highlights), episode.highlights.joinToString("\n"))
        HorizontalDivider()
        DetailField(stringResource(R.string.video_provider), episode.videoProvider.label())
        DetailField(stringResource(R.string.video_url), episode.videoUrl)
        DetailField(stringResource(R.string.duration), episode.durationSeconds?.toString())
        DetailField(stringResource(R.string.thumbnail_url), episode.thumbnailUrl)
        HorizontalDivider()
        DetailField(stringResource(R.string.seo_title), episode.seoTitle)
        DetailField(stringResource(R.string.seo_description), episode.seoDescription)
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

@Composable
private fun PublishStatus.label(): String = when (this) {
    PublishStatus.DRAFT -> stringResource(R.string.status_draft)
    PublishStatus.PUBLISHED -> stringResource(R.string.status_published)
    PublishStatus.UNPUBLISHED -> stringResource(R.string.status_unpublished)
    PublishStatus.DELETED -> stringResource(R.string.status_deleted)
}

@Composable
private fun VideoProvider.label(): String = when (this) {
    VideoProvider.YouTube -> stringResource(R.string.youtube)
    VideoProvider.Facebook -> stringResource(R.string.facebook)
    is VideoProvider.Unknown -> stringResource(R.string.provider_unknown, rawValue)
}
