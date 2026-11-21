/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.opentv.R
import app.opentv.data.model.Channel
import app.opentv.data.model.Programme
import app.opentv.data.model.shownName
import app.opentv.ui.ChannelsViewModel
import app.opentv.ui.GuideMetrics
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The programme guide: channels down the left, a scrolling time-line to the right, with
 * each programme drawn as a block whose width is its duration. This is the "grid" a TV guide
 * is supposed to be — you can see what is on now, what is next, and read across the evening.
 *
 * ## How it lays out without a custom Layout
 *
 * Every row and the time header share one [horizontalScroll] state, so scrolling any of them
 * scrolls all of them in lock-step and the columns stay time-aligned. Within a row, blocks
 * are placed left to right at [GuideMetrics.minuteDp] width per minute; a leading spacer covers
 * any gap before the first programme, and gaps between programmes get their own spacer. No
 * absolute positioning, no measuring pass — just widths, which is cheap enough for a lazy list
 * of hundreds of channels on a weak TV box.
 *
 * ## Block widths are exact
 *
 * An earlier version floored a short programme's width to keep its title readable, then repaid
 * the borrowed width out of the following gap. That made the title legible at the cost of the one
 * thing a guide must get right: the block no longer lined up with the time header, and neither did
 * anything after it. Blocks are now drawn at their true width, and one too narrow for a title is
 * drawn as a bare pill — the highlighted programme's name is on screen in the preview pane anyway.
 *
 * ## Sizes come from the screen
 *
 * Nothing here is a constant any more. [metrics] carries the channel column, row height, pixels
 * per minute and whether to draw grid lines, resolved from the form factor and the user's font
 * size in [app.opentv.ui.rememberScreenInfo].
 */
@Composable
fun GuideGrid(
    rows: List<ChannelsViewModel.Row>,
    windowStartMillis: Long,
    selectedKey: Any?,
    metrics: GuideMetrics,
    onSelectRow: (ChannelsViewModel.Row) -> Unit,
    onFocusRow: (ChannelsViewModel.Row) -> Unit,
    onProgramme: (ChannelsViewModel.Row, Programme) -> Unit = { _, _ -> },
    onToggleFavourite: (ChannelsViewModel.Row) -> Unit = {},
    // Returns true if it handled the key (rail was hidden, so consume it); false to let normal
    // left-navigation carry focus into the already-visible rail.
    onExitLeftFromChannel: () -> Boolean = { false },
    dayOffset: Int = 0,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val now = System.currentTimeMillis()
    // Jumping to another day resets the horizontal scroll to that day's start; a within-day time
    // drift (dayOffset unchanged) leaves the user's scroll position untouched.
    androidx.compose.runtime.LaunchedEffect(dayOffset) { scroll.scrollTo(0) }

    Column(modifier.fillMaxSize()) {
        // No pager buttons: the programme blocks are d-pad focusable, so moving right along a row
        // scrolls the whole timeline (header and every row in lock-step) on its own.
        TimeHeader(windowStartMillis, scroll, metrics)

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(rows, key = { it.key }) { row ->
                GuideRow(
                    row = row,
                    windowStartMillis = windowStartMillis,
                    nowMillis = now,
                    scroll = scroll,
                    metrics = metrics,
                    isSelected = row.key == selectedKey,
                    onSelect = { onSelectRow(row) },
                    onFocus = { onFocusRow(row) },
                    onProgramme = { programme -> onProgramme(row, programme) },
                    onToggleFavourite = { onToggleFavourite(row) },
                    onExitLeft = onExitLeftFromChannel,
                )
            }
        }
    }
}

/**
 * The live channels as a plain vertical list instead of the time-grid — one channel per row with
 * its logo, name, and now/next line. This is the "list mode instead of tiles" some users prefer on
 * a busy line-up, and the only sane layout on a phone in portrait: at 360 dp the grid leaves 37 dp
 * for the timeline. It keeps the exact same focus/key contract as [GuideGrid] so [HomeScreen] can
 * swap between the two with nothing else changing.
 */
@Composable
fun ChannelList(
    rows: List<ChannelsViewModel.Row>,
    selectedKey: Any?,
    metrics: GuideMetrics,
    onSelectRow: (ChannelsViewModel.Row) -> Unit,
    onFocusRow: (ChannelsViewModel.Row) -> Unit,
    onToggleFavourite: (ChannelsViewModel.Row) -> Unit = {},
    onExitLeftFromChannel: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            ChannelListRow(
                row = row,
                nowMillis = now,
                minHeight = metrics.rowHeight,
                starSize = metrics.starButtonSize,
                isSelected = row.key == selectedKey,
                onSelect = { onSelectRow(row) },
                onFocus = { onFocusRow(row) },
                onToggleFavourite = { onToggleFavourite(row) },
                onExitLeft = onExitLeftFromChannel,
            )
        }
    }
}

@Composable
private fun ChannelListRow(
    row: ChannelsViewModel.Row,
    nowMillis: Long,
    minHeight: Dp,
    starSize: Dp,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFocus: () -> Unit,
    onToggleFavourite: () -> Unit,
    onExitLeft: () -> Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier,
            )
            // LEFT from a row reopens the collapsed rail, exactly like the grid's channel column.
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) onExitLeft() else false
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.primary.number?.let { num ->
            Text(
                "$num",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.width(30.dp),
            )
        }
        AsyncImage(
            model = row.primary.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.primary.shownName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.variants.size > 1) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.guide_qualities_count, row.variants.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            val nowProg = row.now
            Text(
                text = nowProg?.let { "${clockFormat.format(Date(it.startUtcMillis))}  ${it.title}" }
                    ?: stringResource(R.string.guide_no_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (nowProg != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(nowProg.progressAt(nowMillis).coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            row.next?.let { next ->
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.guide_next_prefix, clockFormat.format(Date(next.startUtcMillis)), next.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleFavourite, modifier = Modifier.size(starSize)) {
            Icon(
                imageVector = if (row.primary.favourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (row.primary.favourite) stringResource(R.string.common_remove_favourite)
                else stringResource(R.string.common_favourite),
                tint = if (row.primary.favourite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TimeHeader(windowStartMillis: Long, scroll: androidx.compose.foundation.ScrollState, metrics: GuideMetrics) {
    val halfHourWidth = halfHourWidth(metrics)
    val outline = MaterialTheme.colorScheme.outline
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        // Empty corner above the channel column.
        Box(Modifier.width(metrics.channelColumn))
        Row(
            Modifier
                .horizontalScroll(scroll)
                .width(totalWindowWidth(metrics))
                .drawBehind {
                    // Hairlines at each half hour, so a block's edge can be read against a time.
                    val step = halfHourWidth.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(outline.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), 1f)
                        x += step
                    }
                    drawLine(outline.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1f)
                },
        ) {
            // A label every half hour across the whole window.
            repeat(HOURS_IN_WINDOW * 2) { i ->
                val slotStart = windowStartMillis + i * HALF_HOUR_MS
                Box(
                    Modifier.width(halfHourWidth).height(32.dp).padding(start = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        clockFormat.format(Date(slotStart)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    row: ChannelsViewModel.Row,
    windowStartMillis: Long,
    nowMillis: Long,
    scroll: androidx.compose.foundation.ScrollState,
    metrics: GuideMetrics,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFocus: () -> Unit,
    onProgramme: (Programme) -> Unit,
    onToggleFavourite: () -> Unit = {},
    onExitLeft: () -> Boolean = { false },
) {
    // Focus (the d-pad border) just moves the highlight. Selection (the filled cell) is the
    // channel the preview is playing — it only changes when you press OK.
    var focused by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().height(metrics.rowHeight)) {

        // ---- Fixed channel cell (does not scroll) --------------------------------------
        Row(
            Modifier
                .width(metrics.channelColumn)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                )
                .then(
                    if (focused) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp),
                    ) else Modifier,
                )
                // LEFT from this leftmost column asks the host to reopen the collapsed category
                // rail; onExitLeft consumes the key only when it handled it (rail was hidden), so
                // moving left onto programme blocks and into an already-visible rail still works.
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) onExitLeft()
                    else false
                }
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                }
                .clickable(onClick = onSelect)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row.primary.number?.let { num ->
                Text(
                    "$num",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.width(26.dp),
                )
            }
            AsyncImage(
                model = row.primary.logoUrl,
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.primary.shownName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.variants.size > 1) {
                    Text(
                        stringResource(R.string.guide_qualities_count, row.variants.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
            // Favourite toggle. Phone/tablet users had no visible way to favourite from the guide
            // (only the Channel Manager) — this puts a tappable star on every row; it stays d-pad
            // focusable so it works from a TV remote too. Kept small: at 40 dp it took 18% of the
            // channel column, which is what squeezed the channel name down to ~90 dp.
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.size(metrics.starButtonSize),
            ) {
                Icon(
                    imageVector = if (row.primary.favourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (row.primary.favourite) stringResource(R.string.common_remove_favourite)
                    else stringResource(R.string.common_favourite),
                    tint = if (row.primary.favourite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(3.dp))

        // ---- Scrolling programme strip -------------------------------------------------
        // One draw pass paints the half-hour hairlines and the "now" cursor for the whole row.
        // The cursor is drawn at the window's own coordinates, so it stays glued to the content
        // while the row scrolls — that is the thing a guide is looked at for.
        val gridColor = MaterialTheme.colorScheme.outline
        val nowColor = MaterialTheme.colorScheme.primary
        // "Now" only belongs on the today window: browsing tomorrow would otherwise draw a
        // cursor pinned to the left edge as if the current programme had just started.
        val nowInWindow = nowMillis in windowStartMillis..(windowStartMillis + HOURS_IN_WINDOW * 3_600_000L)
        Box(
            Modifier
                .horizontalScroll(scroll)
                .width(totalWindowWidth(metrics))
                .fillMaxHeight()
                // drawWithContent, not drawBehind: the hairlines belong under the blocks, but the
                // "now" cursor has to paint over them or a block hides it exactly when it matters.
                .drawWithContent {
                    if (metrics.showGridLines) {
                        val step = halfHourWidth(metrics).toPx()
                        var x = step
                        while (x < size.width) {
                            drawLine(gridColor.copy(alpha = 0.22f), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                    }
                    drawContent()
                    val nowX = xFor(nowMillis, windowStartMillis, metrics).toPx()
                    if (nowInWindow && nowX >= 0f && nowX <= size.width) {
                        drawLine(nowColor, Offset(nowX, 0f), Offset(nowX, size.height), 2f)
                    }
                },
        ) {
            val programmes = row.programmes
            if (programmes.isEmpty()) {
                // Honest placeholder spanning the window, so a guide-less channel reads as
                // "no guide" rather than as a suspiciously empty gap.
                Box(
                    Modifier
                        .width(totalWindowWidth(metrics))
                        .fillMaxHeight()
                        .padding(end = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        stringResource(R.string.guide_no_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                var cursor = windowStartMillis
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    for (programme in programmes) {
                        // Clamp to the window's left edge; a programme that started earlier is
                        // drawn from `windowStart` so its block does not push everything right.
                        val start = programme.startUtcMillis.coerceAtLeast(windowStartMillis)
                        val gap = widthFor(cursor, start, metrics)
                        if (gap > 0.dp) Spacer(Modifier.width(gap))
                        val end = programme.endUtcMillis
                        val isNow = nowMillis in programme.startUtcMillis until end
                        val drawnWidth = widthFor(start, end, metrics)
                        ProgrammeBlock(
                            title = programme.title,
                            width = drawnWidth,
                            isNow = isNow,
                            progress = if (isNow) programme.progressAt(nowMillis) else 0f,
                            showText = drawnWidth >= metrics.minReadableBlock,
                            onClick = { onProgramme(programme) },
                        )
                        cursor = end
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgrammeBlock(
    title: String,
    width: Dp,
    isNow: Boolean,
    progress: Float,
    showText: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .padding(end = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    focused -> MaterialTheme.colorScheme.primary
                    isNow -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .then(
                if (focused || isNow) Modifier.border(
                    if (focused) 2.dp else 1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(6.dp),
                ) else Modifier,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        if (showText) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    focused -> MaterialTheme.colorScheme.onPrimary
                    isNow -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
            )
        }
        // How far through the current programme we are — a live fill along the bottom edge.
        if (isNow && progress > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** Minutes between two instants, as guide width. Never negative. */
private fun widthFor(fromMillis: Long, toMillis: Long, metrics: GuideMetrics): Dp {
    val minutes = ((toMillis - fromMillis) / 60_000L).coerceAtLeast(0)
    return (minutes * metrics.minuteDp).dp
}

/** Where an instant sits inside the scrolling window, in dp from the window's left edge. */
private fun xFor(millis: Long, windowStartMillis: Long, metrics: GuideMetrics): Dp {
    val minutes = ((millis - windowStartMillis) / 60_000L).coerceAtLeast(0)
    return (minutes * metrics.minuteDp).dp
}

private fun halfHourWidth(metrics: GuideMetrics): Dp = (30 * metrics.minuteDp).dp

private fun totalWindowWidth(metrics: GuideMetrics): Dp =
    (HOURS_IN_WINDOW * 60 * metrics.minuteDp).dp

private const val HOURS_IN_WINDOW = 24
private const val HALF_HOUR_MS = 30 * 60 * 1000L

private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
