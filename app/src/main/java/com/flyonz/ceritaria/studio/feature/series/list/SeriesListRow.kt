package com.flyonz.ceritaria.studio.feature.series.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flyonz.ceritaria.studio.R
import com.flyonz.ceritaria.studio.core.designsystem.component.RemoteArtwork
import com.flyonz.ceritaria.studio.core.model.PublishStatus
import com.flyonz.ceritaria.studio.feature.series.domain.Series

@Composable
fun SeriesListRow(
    series: Series,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteArtwork(
                imageUrl = series.coverUrl,
                fallbackText = series.title,
                modifier = Modifier.size(width = 56.dp, height = 80.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = series.slug,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val featuredSuffix = if (series.isFeatured) {
                    " · " + stringResource(R.string.featured)
                } else {
                    ""
                }
                Text(
                    text = series.publishStatus.displayName() + featuredSuffix,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun PublishStatus.displayName(): String = when (this) {
    PublishStatus.DRAFT -> stringResource(R.string.status_draft)
    PublishStatus.PUBLISHED -> stringResource(R.string.status_published)
    PublishStatus.UNPUBLISHED -> stringResource(R.string.status_unpublished)
    PublishStatus.DELETED -> stringResource(R.string.status_deleted)
}
