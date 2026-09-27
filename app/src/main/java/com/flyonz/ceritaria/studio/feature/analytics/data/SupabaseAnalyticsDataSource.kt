package com.flyonz.ceritaria.studio.feature.analytics.data

import com.flyonz.ceritaria.studio.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAnalyticsDataSource @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : AnalyticsDataSource {
    override suspend fun fetchReport(days: Int): AnalyticsReportDto {
        val result = requireClient().postgrest.rpc(
            function = "get_analytics_dashboard",
            parameters = buildJsonObject {
                put("p_days", days)
                put("p_timezone", TIMEZONE)
            },
        )
        return result.decodeSingle()
    }

    private fun requireClient() = requireNotNull(clientProvider.clientOrNull) {
        "Supabase is not configured."
    }

    private companion object {
        const val TIMEZONE = "Asia/Jakarta"
    }
}
