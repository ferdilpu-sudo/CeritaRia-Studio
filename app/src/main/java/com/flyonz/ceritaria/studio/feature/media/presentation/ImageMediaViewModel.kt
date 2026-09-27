package com.flyonz.ceritaria.studio.feature.media.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaScheduler
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaWorkState
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaWorkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ImageMediaViewModel @Inject constructor(
    private val scheduler: ImageMediaScheduler,
) : ViewModel() {
    private val mutableState = MutableStateFlow<Map<ImageMediaSlot, ImageMediaUiState>>(emptyMap())
    val state: StateFlow<Map<ImageMediaSlot, ImageMediaUiState>> = mutableState.asStateFlow()

    private val effectChannel = Channel<ImageMediaEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private val observationJobs = mutableMapOf<ImageMediaSlot, Job>()

    fun replace(
        ownerId: String?,
        slot: ImageMediaSlot,
        sourceUri: String,
        oldPublicUrl: String?,
    ) {
        if (ownerId.isNullOrBlank()) {
            update(slot) {
                ImageMediaUiState(
                    status = ImageMediaUiStatus.SAVE_FIRST,
                    lastSourceUri = sourceUri,
                    oldPublicUrl = oldPublicUrl,
                    lastAction = ImageMediaAction.REPLACE,
                )
            }
            return
        }

        val workId = scheduler.replace(ownerId, slot, sourceUri, oldPublicUrl)
        update(slot) {
            ImageMediaUiState(
                status = ImageMediaUiStatus.QUEUED,
                workId = workId,
                lastSourceUri = sourceUri,
                oldPublicUrl = oldPublicUrl,
                lastAction = ImageMediaAction.REPLACE,
            )
        }
        observe(slot, workId)
    }

    fun remove(
        ownerId: String?,
        slot: ImageMediaSlot,
        oldPublicUrl: String?,
    ) {
        if (ownerId.isNullOrBlank()) {
            update(slot) {
                ImageMediaUiState(
                    status = ImageMediaUiStatus.SAVE_FIRST,
                    oldPublicUrl = oldPublicUrl,
                    lastAction = ImageMediaAction.REMOVE,
                )
            }
            return
        }

        val workId = scheduler.remove(ownerId, slot, oldPublicUrl)
        update(slot) {
            ImageMediaUiState(
                status = ImageMediaUiStatus.QUEUED,
                workId = workId,
                oldPublicUrl = oldPublicUrl,
                lastAction = ImageMediaAction.REMOVE,
            )
        }
        observe(slot, workId)
    }

    fun retry(ownerId: String?, slot: ImageMediaSlot) {
        val current = mutableState.value[slot] ?: return
        when (current.lastAction) {
            ImageMediaAction.REPLACE -> current.lastSourceUri?.let {
                replace(ownerId, slot, it, current.oldPublicUrl)
            }
            ImageMediaAction.REMOVE -> remove(ownerId, slot, current.oldPublicUrl)
            null -> Unit
        }
    }

    fun cancel(slot: ImageMediaSlot) {
        val workId = mutableState.value[slot]?.workId ?: return
        scheduler.cancel(workId)
    }

    private fun observe(slot: ImageMediaSlot, workId: UUID) {
        observationJobs.remove(slot)?.cancel()
        observationJobs[slot] = viewModelScope.launch {
            scheduler.observe(workId).collect { work ->
                work?.let { applyWorkState(slot, it) }
            }
        }
    }

    private suspend fun applyWorkState(
        slot: ImageMediaSlot,
        work: ImageMediaWorkState,
    ) {
        val current = mutableState.value[slot] ?: ImageMediaUiState()
        val status = when (work.status) {
            ImageMediaWorkStatus.QUEUED -> ImageMediaUiStatus.QUEUED
            ImageMediaWorkStatus.RUNNING -> ImageMediaUiStatus.RUNNING
            ImageMediaWorkStatus.SUCCEEDED -> ImageMediaUiStatus.SUCCEEDED
            ImageMediaWorkStatus.FAILED -> ImageMediaUiStatus.FAILED
            ImageMediaWorkStatus.CANCELLED -> ImageMediaUiStatus.CANCELLED
        }
        update(slot) {
            current.copy(
                status = status,
                progress = work.progress,
                errorCode = work.errorCode,
            )
        }

        if (work.status == ImageMediaWorkStatus.SUCCEEDED) {
            effectChannel.send(
                ImageMediaEffect.ReferenceUpdated(
                    slot = slot,
                    publicUrl = if (work.removed) null else work.publicUrl,
                ),
            )
        }
    }

    private fun update(
        slot: ImageMediaSlot,
        transform: (ImageMediaUiState?) -> ImageMediaUiState,
    ) {
        mutableState.update { states ->
            states + (slot to transform(states[slot]))
        }
    }
}
