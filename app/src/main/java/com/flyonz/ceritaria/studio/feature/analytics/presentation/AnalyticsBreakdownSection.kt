package com.flyonz.ceritaria.studio.feature.analytics.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.feature.analytics.domain.AnalyticsPoint

@Composable
fun AnalyticsBreakdownSection(
    title: String,
    points: List<AnalyticsPoint>,
) {
    if (points.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        points.forEachIndexed { index, point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatAnalyticsLabel(point.label),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = point.value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (index != points.lastIndex) HorizontalDivider()
        }
    }
}

private fun formatAnalyticsLabel(value: String): String =
    value.replace('_', ' ')
        .split(' ')
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
