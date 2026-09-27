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
import com.flyonz.ceritaria.studio.core.upload.VideoUploadStatus
import com.flyonz.ceritaria.studio.core.upload.execution.VideoUploadScheduleResult
import com.flyonz.ceritaria.studio.core.upload.execution.VideoUploadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
    private val uploadScheduler: VideoUploadScheduler,
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

    fun uploadVideo() {
        val job = mutableState.value.job ?: return
        if (!job.isReadyForUpload() || job.uploadStatus.isTransferActiveOrReady()) return

        viewModelScope.launch {
            when (uploadScheduler.enqueue(job.jobId, job.totalBytes)) {
                VideoUploadScheduleResult.SCHEDULED -> markUploadQueuedIfNeeded(job.jobId)
                VideoUploadScheduleResult.REJECTED -> markUploadScheduleRejected(job.jobId)
            }
        }
    }

    fun cancelUpload() {
        val job = mutableState.value.job ?: return
        viewModelScope.launch {
            try {
                uploadScheduler.cancel(job.jobId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        status = EpisodeLocalVideoStatus.UPLOAD_FAILED,
                        errorCode = ERROR_UPLOAD_CANCEL_FAILED,
                    )
                }
            }
        }
    }

    private suspend fun markUploadQueuedIfNeeded(jobId: String) {
        val current = jobs.getById(jobId) ?: return
        if (current.uploadStatus !in SCHEDULABLE_UPLOAD_STATUSES) return
        jobs.upsert(
            current.copy(
                uploadStatus = VideoUploadStatus.QUEUED,
                lastErrorCode = null,
                updatedAt = Instant.now(),
            ),
        )
    }

    private suspend fun markUploadScheduleRejected(jobId: String) {
        val current = jobs.getById(jobId) ?: return
        if (current.uploadStatus == VideoUploadStatus.READY) return
        jobs.upsert(
            current.copy(
                uploadStatus = VideoUploadStatus.FAILED,
                lastErrorCode = ERROR_UPLOAD_SCHEDULE_REJECTED,
                updatedAt = Instant.now(),
            ),
        )
    }

    private fun initialState() = EpisodeLocalVideoUiState(
        status = if (episodeId == null) {
            EpisodeLocalVideoStatus.SAVE_FIRST
        } else {
            EpisodeLocalVideoStatus.IDLE
        },
    )

    private fun VideoJob.toUiState(): EpisodeLocalVideoUiState {
        val mapped = when (uploadStatus) {
            VideoUploadStatus.QUEUED -> EpisodeLocalVideoStatus.UPLOAD_QUEUED
            VideoUploadStatus.UPLOADING -> EpisodeLocalVideoStatus.UPLOADING
            VideoUploadStatus.VERIFYING -> EpisodeLocalVideoStatus.VERIFYING
            VideoUploadStatus.READY -> EpisodeLocalVideoStatus.UPLOAD_READY
            VideoUploadStatus.FAILED -> EpisodeLocalVideoStatus.UPLOAD_FAILED
            VideoUploadStatus.CANCELLED -> EpisodeLocalVideoStatus.UPLOAD_CANCELLED
            VideoUploadStatus.NOT_STARTED -> when {
                !needsEncoding -> EpisodeLocalVideoStatus.READY_WITHOUT_ENCODING
                encodingStatus == VideoEncodingStatus.QUEUED ->
                    EpisodeLocalVideoStatus.READY_TO_ENCODE
                encodingStatus == VideoEncodingStatus.ENCODING ->
                    EpisodeLocalVideoStatus.ENCODING
                encodingStatus == VideoEncodingStatus.READY ->
                    EpisodeLocalVideoStatus.ENCODED_READY
                encodingStatus == VideoEncodingStatus.FAILED ->
                    EpisodeLocalVideoStatus.FAILED
                encodingStatus == VideoEncodingStatus.CANCELLED ->
                    EpisodeLocalVideoStatus.CANCELLED
                else -> EpisodeLocalVideoStatus.READY_TO_ENCODE
            }
        }
        return EpisodeLocalVideoUiState(
            status = mapped,
            job = this,
            errorCode = lastErrorCode,
        )
    }

    private fun VideoJob.isReadyForUpload(): Boolean =
        !needsEncoding || encodingStatus == VideoEncodingStatus.READY

    private fun VideoUploadStatus.isTransferActiveOrReady(): Boolean =
        this == VideoUploadStatus.QUEUED ||
            this == VideoUploadStatus.UPLOADING ||
            this == VideoUploadStatus.VERIFYING ||
            this == VideoUploadStatus.READY

    private companion object {
        val SCHEDULABLE_UPLOAD_STATUSES = setOf(
            VideoUploadStatus.NOT_STARTED,
            VideoUploadStatus.FAILED,
            VideoUploadStatus.CANCELLED,
        )
        const val ERROR_UPLOAD_SCHEDULE_REJECTED = "UPLOAD_SCHEDULE_REJECTED"
        const val ERROR_UPLOAD_CANCEL_FAILED = "UPLOAD_CANCEL_FAILED"
    }
}
