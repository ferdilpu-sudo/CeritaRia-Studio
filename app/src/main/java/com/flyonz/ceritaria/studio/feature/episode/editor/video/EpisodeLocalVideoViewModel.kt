package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJob
import com.flyonz.ceritaria.studio.core.database.videojob.VideoJobRepository
import com.flyonz.ceritaria.studio.core.media.VideoEncodingCoordinator
import com.flyonz.ceritaria.studio.core.media.VideoEncodingRecovery
import com.flyonz.ceritaria.studio.core.media.VideoEncodingStatus
import com.flyonz.ceritaria.studio.core.media.VideoSelectionPreparer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeLocalVideoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectionPreparer: VideoSelectionPreparer,
    private val encodingCoordinator: VideoEncodingCoordinator,
    private val encodingRecovery: VideoEncodingRecovery,
    private val jobs: VideoJobRepository,
) : ViewModel() {
    private val episodeId = savedStateHandle.get<String>("episodeId")
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<EpisodeLocalVideoUiState> = mutableState.asStateFlow()

    private var encodeJob: Job? = null
    private var initialRecoveryPending = true

    init {
        episodeId?.let { ownerId ->
            viewModelScope.launch {
                jobs.observeLatestForEpisode(ownerId).collect { job ->
                    if (job != null) {
                        val effective = if (initialRecoveryPending) {
                            initialRecoveryPending = false
                            encodingRecovery.recover(job)
                        } else {
                            job
                        }
                        mutableState.value = effective.toUiState()
                    }
                }
            }
        }
    }

    fun select(sourceUri: String) {
        val ownerId = episodeId
        if (ownerId == null) {
            mutableState.value = EpisodeLocalVideoUiState(
                status = EpisodeLocalVideoStatus.SAVE_FIRST,
            )
            return
        }

        viewModelScope.launch {
            mutableState.update {
                it.copy(status = EpisodeLocalVideoStatus.INSPECTING, errorCode = null)
            }
            try {
                mutableState.value = selectionPreparer.prepare(sourceUri, ownerId).toUiState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        status = EpisodeLocalVideoStatus.FAILED,
                        errorCode = error::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun prepareVideo() {
        val job = mutableState.value.job ?: return
        if (!job.needsEncoding || encodeJob?.isActive == true) return

        encodeJob = viewModelScope.launch {
            try {
                encodingCoordinator.encode(job.jobId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                Unit
            }
        }
    }

    fun cancelEncoding() {
        encodeJob?.cancel()
    }

    private fun initialState() = EpisodeLocalVideoUiState(
        status = if (episodeId == null) {
            EpisodeLocalVideoStatus.SAVE_FIRST
        } else {
            EpisodeLocalVideoStatus.IDLE
        },
    )

    private fun VideoJob.toUiState(): EpisodeLocalVideoUiState {
        val mapped = when {
            !needsEncoding -> EpisodeLocalVideoStatus.READY_WITHOUT_ENCODING
            encodingStatus == VideoEncodingStatus.QUEUED -> EpisodeLocalVideoStatus.READY_TO_ENCODE
            encodingStatus == VideoEncodingStatus.ENCODING -> EpisodeLocalVideoStatus.ENCODING
            encodingStatus == VideoEncodingStatus.READY -> EpisodeLocalVideoStatus.ENCODED_READY
            encodingStatus == VideoEncodingStatus.FAILED -> EpisodeLocalVideoStatus.FAILED
            encodingStatus == VideoEncodingStatus.CANCELLED -> EpisodeLocalVideoStatus.CANCELLED
            else -> EpisodeLocalVideoStatus.READY_TO_ENCODE
        }
        return EpisodeLocalVideoUiState(
            status = mapped,
            job = this,
            errorCode = lastErrorCode,
        )
    }
}
