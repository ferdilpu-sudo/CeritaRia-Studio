package com.flyonz.ceritaria.studio.feature.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsRange

@Composable
fun AnalyticsRangeSelector(
    selected: AnalyticsRange,
    onSelected: (AnalyticsRange) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelected(range) },
                label = {
                    Text(
                        stringResource(
                            when (range) {
                                AnalyticsRange.DAYS_7 -> R.string.analytics_7_days
                                AnalyticsRange.DAYS_30 -> R.string.analytics_30_days
                                AnalyticsRange.DAYS_90 -> R.string.analytics_90_days
                            },
                        ),
                    )
                },
            )
        }
    }
}
