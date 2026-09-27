package com.flyonz.ceritaria.studio.feature.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsSummary

@Composable
fun AnalyticsSummarySection(
    summary: AnalyticsSummary,
    days: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.analytics_summary),
            style = MaterialTheme.typography.titleMedium,
        )
        MetricPair(
            leftLabel = stringResource(R.string.analytics_today_pageviews),
            leftValue = summary.todayPageviews,
            rightLabel = stringResource(R.string.analytics_today_visitors),
            rightValue = summary.todayVisitors,
        )
        MetricPair(
            leftLabel = stringResource(R.string.analytics_period_pageviews, days),
            leftValue = summary.periodPageviews,
            rightLabel = stringResource(R.string.analytics_period_visitors, days),
            rightValue = summary.periodVisitors,
        )
        MetricPair(
            leftLabel = stringResource(R.string.analytics_sessions),
            leftValue = summary.periodSessions,
            rightLabel = stringResource(R.string.analytics_total_events),
            rightValue = summary.totalEvents,
        )
    }
}

@Composable
private fun MetricPair(
    leftLabel: String,
    leftValue: Long,
    rightLabel: String,
    rightValue: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Metric(leftLabel, leftValue, Modifier.weight(1f))
        Metric(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(
    label: String,
    value: Long,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
