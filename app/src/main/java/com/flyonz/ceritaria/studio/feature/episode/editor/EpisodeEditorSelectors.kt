package com.flyonz.ceritaria.studio.feature.episode.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.flyonz.ceritaria.studio.R

@Composable
fun EpisodeSeriesSelector(
    options: List<EpisodeSeriesOption>,
    selectedId: String,
    issue: EpisodeValidationIssue?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = options.firstOrNull { it.id == selectedId }?.title
        ?: stringResource(R.string.select_series)

    Box {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(title)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    },
                )
            }
        }
    }
    if (issue != null) {
        Text(
            text = stringResource(R.string.invalid_series),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun EpisodeProviderSelector(
    selected: String,
    issue: EpisodeValidationIssue?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = when (selected) {
        "youtube" -> stringResource(R.string.youtube)
        "facebook" -> stringResource(R.string.facebook)
        else -> stringResource(R.string.provider_unknown, selected)
    }

    Box {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(title)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProviderOption("youtube", stringResource(R.string.youtube), onSelected) {
                expanded = false
            }
            ProviderOption("facebook", stringResource(R.string.facebook), onSelected) {
                expanded = false
            }
        }
    }
    if (issue == EpisodeValidationIssue.UNSUPPORTED_PROVIDER) {
        Text(
            text = stringResource(R.string.unsupported_provider),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ProviderOption(
    value: String,
    label: String,
    onSelected: (String) -> Unit,
    onClose: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            onClose()
            onSelected(value)
        },
    )
}
