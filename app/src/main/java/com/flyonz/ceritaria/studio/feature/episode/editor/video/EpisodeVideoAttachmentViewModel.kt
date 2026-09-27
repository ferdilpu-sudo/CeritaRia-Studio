package com.flyonz.ceritaria.studio.feature.episode.editor.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flyonz.ceritaria.studio.core.error.AppError
import com.flyonz.ceritaria.studio.core.error.AppResult
import com.flyonz.ceritaria.studio.feature.episode.domain.EpisodeVideoAssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeVideoAttachmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EpisodeVideoAssetRepository,
) : ViewModel() {
    private val episodeId = savedStateHandle.get<String>("episodeId")

    private val mutableState = MutableStateFlow(EpisodeVideoAttachmentUiState())
    val state: StateFlow<EpisodeVideoAttachmentUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<EpisodeVideoAttachmentEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun attach(assetId: String) {
        val ownerId = episodeId ?: return
        if (assetId.isBlank() || mutableState.value.status == EpisodeVideoAttachmentStatus.ATTACHING) {
            return
        }

        viewModelScope.launch {
            mutableState.value = EpisodeVideoAttachmentUiState(
                status = EpisodeVideoAttachmentStatus.ATTACHING,
                assetId = assetId,
            )
            when (val result = repository.attachReadyAsset(ownerId, assetId)) {
                is AppResult.Success -> {
                    mutableState.value = EpisodeVideoAttachmentUiState(
                        status = EpisodeVideoAttachmentStatus.ATTACHED,
                        assetId = result.value.assetId,
                    )
                    effectChannel.send(
                        EpisodeVideoAttachmentEffect.Attached(
                            assetId = result.value.assetId,
                            replacedAssetId = result.value.replacedAssetId,
                        ),
                    )
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(
                        status = EpisodeVideoAttachmentStatus.FAILED,
                        errorCode = result.error.errorCode(),
                    )
                }
            }
        }
    }

    private fun AppError.errorCode(): String = when (this) {
        AppError.Configuration -> "CONFIGURATION"
        AppError.Authentication -> "AUTHENTICATION"
        AppError.Authorization -> "AUTHORIZATION"
        AppError.Network -> "NETWORK"
        AppError.Conflict -> "NOT_ATTACHABLE"
        is AppError.Validation -> "INVALID_RESPONSE"
        AppError.Unknown -> "UNKNOWN"
    }
}
