package com.flyonz.ceritaria.studio.feature.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange

@Composable
fun AnalyticsContent(
    contentPadding: PaddingValues,
    state: AnalyticsUiState,
    onRangeChange: (AnalyticsRange) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.analytics),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            AnalyticsRangeSelector(
                selected = state.range,
                onSelected = onRangeChange,
            )
        }

        when {
            state.isLoading -> item { LinearProgressIndicator() }
            state.report == null && state.hasError -> item {
                AnalyticsLoadError(onRetry)
            }
            state.isEmpty -> item {
                Text(
                    text = stringResource(R.string.analytics_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.report != null -> {
                val report = requireNotNull(state.report)
                item { AnalyticsSummarySection(report.summary, report.days) }
                item { AnalyticsHourlyStrip(report.hourly) }
                item { AnalyticsTopPagesSection(report.topPages) }
                item {
                    AnalyticsBreakdownSection(
                        title = stringResource(R.string.analytics_devices),
                        points = report.devices,
                    )
                }
                item {
                    AnalyticsBreakdownSection(
                        title = stringResource(R.string.analytics_referrers),
                        points = report.referrers,
                    )
                }
                item {
                    AnalyticsBreakdownSection(
                        title = stringResource(R.string.analytics_player_events),
                        points = report.events.filterNot {
                            it.label == "page_view" || it.label == "episode_view"
                        },
                    )
                }
                if (state.hasError) {
                    item { AnalyticsRefreshWarning() }
                }
            }
        }
    }
}
