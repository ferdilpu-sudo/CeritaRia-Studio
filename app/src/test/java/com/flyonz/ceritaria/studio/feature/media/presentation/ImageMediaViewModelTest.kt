package com.flyonz.ceritaria.studio.feature.media.presentation

import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaScheduler
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaWorkState
import com.flyonz.ceritaria.studio.feature.media.work.ImageMediaWorkStatus
import com.flyonz.ceritaria.studio.testutil.MainDispatcherRule
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageMediaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun unsavedOwnerDoesNotEnqueueWork() = runTest(mainDispatcherRule.testDispatcher) {
        val scheduler = FakeScheduler()
        val viewModel = ImageMediaViewModel(scheduler)

        viewModel.replace(
            ownerId = null,
            slot = ImageMediaSlot.SERIES_COVER,
            sourceUri = "content://cover",
            oldPublicUrl = null,
        )

        assertEquals(0, scheduler.replaceCalls)
        assertEquals(
            ImageMediaUiStatus.SAVE_FIRST,
            viewModel.state.value[ImageMediaSlot.SERIES_COVER]?.status,
        )
    }

    @Test
    fun successfulReplacementEmitsUpdatedReference() =
        runTest(mainDispatcherRule.testDispatcher) {
            val scheduler = FakeScheduler()
            val viewModel = ImageMediaViewModel(scheduler)
            val effect = async { viewModel.effects.first() }

            viewModel.replace(
                ownerId = "owner-1",
                slot = ImageMediaSlot.SERIES_COVER,
                sourceUri = "content://cover",
                oldPublicUrl = "https://old",
            )
            scheduler.emitSuccess("https://new")
            advanceUntilIdle()

            assertEquals(
                ImageMediaEffect.ReferenceUpdated(
                    ImageMediaSlot.SERIES_COVER,
                    "https://new",
                ),
                effect.await(),
            )
        }

    private class FakeScheduler : ImageMediaScheduler {
        private val id = UUID.randomUUID()
        private val flow = MutableStateFlow<ImageMediaWorkState?>(null)
        var replaceCalls = 0

        override fun replace(
            ownerId: String,
            slot: ImageMediaSlot,
            sourceUri: String,
            oldPublicUrl: String?,
        ): UUID {
            replaceCalls += 1
            return id
        }

        override fun remove(
            ownerId: String,
            slot: ImageMediaSlot,
            oldPublicUrl: String?,
        ): UUID = id

        override fun observe(workId: UUID): Flow<ImageMediaWorkState?> = flow

        override fun cancel(workId: UUID) = Unit

        fun emitSuccess(publicUrl: String) {
            flow.value = ImageMediaWorkState(
                workId = id,
                status = ImageMediaWorkStatus.SUCCEEDED,
                progress = 100,
                publicUrl = publicUrl,
            )
        }
    }
}
