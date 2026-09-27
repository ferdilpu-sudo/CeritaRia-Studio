package com.flyonz.ceritaria.studio.feature.media.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import com.flyonz.ceritaria.studio.feature.media.domain.ImageMediaSlot
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseImageReferenceDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : ImageReferenceDataSource {
    override suspend fun updateReference(
        ownerId: String,
        slot: ImageMediaSlot,
        publicUrl: String?,
    ) {
        val client = requireClient()
        when (slot) {
            ImageMediaSlot.SERIES_COVER -> client.from("series").update(
                SeriesCoverReferencePatch(publicUrl),
            ) {
                filter { eq("id", ownerId) }
            }
            ImageMediaSlot.SERIES_HERO -> client.from("series").update(
                SeriesHeroReferencePatch(publicUrl),
            ) {
                filter { eq("id", ownerId) }
            }
            ImageMediaSlot.EPISODE_THUMBNAIL -> client.from("episodes").update(
                EpisodeThumbnailReferencePatch(publicUrl),
            ) {
                filter { eq("id", ownerId) }
            }
        }
    }

    private fun requireClient() = requireNotNull(clientProvider.clientOrNull) {
        "Supabase is not configured."
    }
}
