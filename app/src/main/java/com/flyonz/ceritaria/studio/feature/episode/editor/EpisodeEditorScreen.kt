package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaEffect
import com.flyonz.ceritaria.studio.feature.media.presentation.ImageMediaViewModel
import com.flyonz.ceritaria.studio.feature.episode.editor.video.EpisodeLocalVideoViewModel
import com.flyonz.ceritaria.studio.feature.episode.editor.video.EpisodeVideoAttachmentEffect
import com.flyonz.ceritaria.studio.feature.episode.editor.video.EpisodeVideoAttachmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeEditorScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: EpisodeEditorViewModel = hiltViewModel(),
    mediaViewModel: ImageMediaViewModel = hiltViewModel(),
    localVideoViewModel: EpisodeLocalVideoViewModel = hiltViewModel(),
    attachmentViewModel: EpisodeVideoAttachmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mediaState by mediaViewModel.state.collectAsStateWithLifecycle()
    val localVideoState by localVideoViewModel.state.collectAsStateWithLifecycle()
    val attachmentState by attachmentViewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is EpisodeEditorEffect.Saved) onSaved(effect.episodeId)
        }
    }
    LaunchedEffect(mediaViewModel, viewModel) {
        mediaViewModel.effects.collect { effect ->
            if (effect is ImageMediaEffect.ReferenceUpdated) {
                viewModel.applyMediaReference(effect.slot, effect.publicUrl)
            }
        }
    }
    LaunchedEffect(attachmentViewModel, viewModel) {
        attachmentViewModel.effects.collect { effect ->
            when (effect) {
                is EpisodeVideoAttachmentEffect.Attached ->
                    viewModel.applyR2Attachment(effect.assetId)
                is EpisodeVideoAttachmentEffect.PreviewReady ->
                    uriHandler.openUri(effect.url)
            }
        }
    }
    BackHandler(enabled = state.isDirty && !state.isSaving) {
        showDiscardDialog = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(
                        if (state.isEdit) R.string.edit_episode else R.string.new_episode,
                    ),
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (state.isDirty) showDiscardDialog = true else onBack()
                    },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
        EpisodeEditorBody(
            state = state,
            viewModel = viewModel,
            mediaStates = mediaState,
            mediaViewModel = mediaViewModel,
            localVideoState = localVideoState,
            localVideoViewModel = localVideoViewModel,
            attachmentState = attachmentState,
            attachmentViewModel = attachmentViewModel,
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = onBack) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
    }
}
