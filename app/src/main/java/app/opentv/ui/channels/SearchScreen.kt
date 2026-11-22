/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.Channel
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.shownName
import app.opentv.data.parser.displayTitle
import app.opentv.ui.ChannelsViewModel
import app.opentv.ui.VodViewModel
import app.opentv.ui.rememberScreenInfo
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Channel search with an on-screen keyboard.
 *
 * Android TV's default text field punts to "type on your phone", which is useless if your phone
 * isn't to hand. This screen draws its own d-pad keyboard so search works with the remote alone,
 * and shows matching channels live as you type.
 */
@Composable
fun SearchScreen(
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelsViewModel = viewModel(),
    vodViewModel: VodViewModel = viewModel(),
) {
    // The ViewModel owns the text so switching the guide's category can clear it, even while a
    // previous search screen instance is still on the back stack.
    val query by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val scopedToCategory = selectedCategory != null
    val channelResults by viewModel.searchResults.collectAsState()
    val movieResults by vodViewModel.movieResults.collectAsState()
    val seriesResults by vodViewModel.seriesResults.collectAsState()
    val anyResults = channelResults.isNotEmpty() ||
        (!scopedToCategory && (movieResults.isNotEmpty() || seriesResults.isNotEmpty()))

    LaunchedEffect(query) {
        viewModel.setSearchQuery(query)
        // A live category has no VOD equivalent: scoping to it means channels only.
        vodViewModel.setVodSearchQuery(if (scopedToCategory) "" else query)
    }
    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.nav_search),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
            )
            Spacer(Modifier.width(20.dp))
            Text(
                query.ifEmpty { stringResource(R.string.search_type_name) },
                style = MaterialTheme.typography.titleLarge,
                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done), maxLines = 1) }
        }

        Spacer(Modifier.height(20.dp))

        val keyboard: @Composable (Modifier) -> Unit = { modifier ->
            OnScreenKeyboard(
                onKey = { if (query.length < 40) viewModel.setSearchQuery(query + it) },
                onSpace = { if (query.length < 40) viewModel.setSearchQuery("$query ") },
                onBackspace = { viewModel.setSearchQuery(query.dropLast(1)) },
                onClear = { viewModel.setSearchQuery("") },
                modifier = modifier,
            )
        }

        // On a phone the keyboard needs the full width, so stack results above it. On a TV or a
        // landscape phone there is room for the original keyboard-left / results-right split.
        if (rememberScreenInfo().isCompact) {
            Column(Modifier.fillMaxSize()) {
                SearchResultsPane(
                    query = query,
                    channelResults = channelResults,
                    movieResults = movieResults,
                    seriesResults = seriesResults,
                    scopedToCategory = scopedToCategory,
                    anyResults = anyResults,
                    onPlayChannel = onPlayChannel,
                    onPlayMovie = onPlayMovie,
                    onOpenSeries = onOpenSeries,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                keyboard(Modifier)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                keyboard(Modifier.weight(1f))
                Spacer(Modifier.width(28.dp))
                SearchResultsPane(
                    query = query,
                    channelResults = channelResults,
                    movieResults = movieResults,
                    seriesResults = seriesResults,
                    scopedToCategory = scopedToCategory,
                    anyResults = anyResults,
                    onPlayChannel = onPlayChannel,
                    onPlayMovie = onPlayMovie,
                    onOpenSeries = onOpenSeries,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SearchResultsPane(
    query: String,
    channelResults: List<ChannelsViewModel.Row>,
    movieResults: List<Movie>,
    seriesResults: List<Series>,
    scopedToCategory: Boolean,
    anyResults: Boolean,
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        when {
            query.isBlank() -> Hint(stringResource(R.string.search_start_hint))
            query.trim().length < 2 -> Hint(stringResource(R.string.common_keep_typing))
            !anyResults -> Hint(stringResource(R.string.search_no_results, query))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (channelResults.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.common_channels)) }
                    items(channelResults, key = { "c${it.key}" }) { row ->
                        SearchResultRow(row = row, onClick = { onPlayChannel(row.primary) })
                    }
                }
                if (!scopedToCategory && movieResults.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.nav_movies)) }
                    items(movieResults, key = { "m${it.id}" }) { movie ->
                        VodResultRow(movie.displayTitle, movie.posterUrl, movie.year?.toString()) {
                            onPlayMovie(movie)
                        }
                    }
                }
                if (!scopedToCategory && seriesResults.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.nav_shows)) }
                    items(seriesResults, key = { "s${it.id}" }) { show ->
                        VodResultRow(show.displayTitle, show.posterUrl, show.year?.toString()) {
                            onOpenSeries(show)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun VodResultRow(name: String, posterUrl: String?, subtitle: String?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 34.dp, height = 48.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchResultRow(row: ChannelsViewModel.Row, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = row.primary.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.primary.shownName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.now?.let { "${searchTime(it.startUtcMillis)}  ${it.title}" } ?: stringResource(R.string.guide_no_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val searchTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun searchTime(utcMillis: Long): String = searchTimeFormat.format(Date(utcMillis))
