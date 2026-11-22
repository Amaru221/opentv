/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.opentv.core.AppSettings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.ui.components.HeaderTitle
import app.opentv.data.model.LiveStreamFormat
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.ui.SourcesViewModel

/**
 * The provider list: what's connected, with the plumbing to add another or remove one. Removing
 * a source deletes its channels and guide with it — the confirm step is here because that is a
 * lot of data to lose to a stray click on a remote.
 */
@Composable
fun ProvidersScreen(
    onAddSource: () -> Unit,
    onEditSource: (Source) -> Unit,
    onOpenWebManager: () -> Unit,
    onBack: () -> Unit,
    viewModel: SourcesViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    var pendingRemove by remember { mutableStateOf<Source?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderTitle(stringResource(R.string.settings_providers_title))
            OutlinedButton(onClick = onAddSource) { Text(stringResource(R.string.providers_add)) }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
        }

        Spacer(Modifier.height(16.dp))

        if (ui.sources.isEmpty()) {
            Text(
                stringResource(R.string.providers_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            LiveStatusSection(onOpenWebManager)
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 760.dp),
        ) {
            items(ui.sources, key = { it.id }) { source ->
                ProviderRow(
                    source = source,
                    confirming = pendingRemove?.id == source.id,
                    onEdit = { onEditSource(source) },
                    onAskRemove = { pendingRemove = source },
                    onCancelRemove = { pendingRemove = null },
                    onConfirmRemove = {
                        viewModel.delete(source)
                        pendingRemove = null
                    },
                    onSetLiveFormat = { viewModel.setLiveFormat(source, it) },
                    onSetEnabled = { viewModel.setSourceEnabled(source, it) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text(
                    stringResource(R.string.providers_disabled_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
            item { LiveStatusSection(onOpenWebManager) }
        }
    }
}

/**
 * Optional wiring for "Live now": where the Dispatcharr instance is and the admin API key used to
 * read its status. Empty fields mean the feature is simply absent — nothing polls, nothing shows.
 */
@Composable
private fun LiveStatusSection(onOpenWebManager: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val url by settings.dispatcharrUrl.collectAsState()
    val apiKey by settings.dispatcharrKey.collectAsState()
    val slots by settings.dispatcharrSlots.collectAsState()
    val configured = url.isNotBlank() && apiKey.isNotBlank()

    Column(Modifier.widthIn(max = 760.dp)) {
        Text(
            stringResource(R.string.live_status_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.live_status_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (configured) {
                stringResource(R.string.live_status_summary_configured, slots)
            } else {
                stringResource(R.string.live_status_summary_not_configured)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (configured) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        // An API key is a 40-character admin secret: typing it with a TV remote is the whole
        // reason this screen hands off to the phone form (and its real keyboard) instead.
        OutlinedButton(onClick = onOpenWebManager) {
            Text(stringResource(R.string.live_status_configure_phone))
        }
    }
}

@Composable
private fun ProviderRow(
    source: Source,
    confirming: Boolean,
    onEdit: () -> Unit,
    onAskRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onSetLiveFormat: (LiveStreamFormat) -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = source.enabled,
                    onCheckedChange = onSetEnabled,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        source.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (source.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${source.kind.name} · ${hostOf(source.url)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (confirming) {
                    Text(
                        stringResource(R.string.providers_remove_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onConfirmRemove) { Text(stringResource(R.string.providers_yes_remove)) }
                    TextButton(onClick = onCancelRemove) { Text(stringResource(R.string.common_cancel)) }
                } else {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                    TextButton(onClick = onAskRemove) { Text(stringResource(R.string.common_remove)) }
                }
            }

            // Stream format is an Xtream-panel concept — M3U sources carry their own URLs, so the
            // choice is meaningless there and hidden.
            if (source.kind == SourceKind.XTREAM) {
                Spacer(Modifier.height(14.dp))
                StreamFormatSelector(selected = source.liveFormat, onSelect = onSetLiveFormat)
            }
        }
    }
}

/**
 * The per-source HLS / MPEG-TS picker. A compact two-option segmented control (the selected
 * container is a filled button, the other outlined) with a one-line hint on when to reach for it.
 */
@Composable
private fun StreamFormatSelector(
    selected: LiveStreamFormat,
    onSelect: (LiveStreamFormat) -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.provider_stream_format),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatSegment(
                label = stringResource(R.string.provider_stream_format_hls),
                selected = selected == LiveStreamFormat.HLS,
                onClick = { onSelect(LiveStreamFormat.HLS) },
            )
            FormatSegment(
                label = stringResource(R.string.provider_stream_format_ts),
                selected = selected == LiveStreamFormat.MPEG_TS,
                onClick = { onSelect(LiveStreamFormat.MPEG_TS) },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.provider_stream_format_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One option of the stream-format control: filled when chosen, outlined otherwise. */
@Composable
private fun FormatSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

/** Host only — the full URL carries the login and has no place on a settings list. */
private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)
