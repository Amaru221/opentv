/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * The heading of a settings screen, inside the same [Row] as its action buttons.
 *
 * ## Why this exists
 *
 * The recurring pattern was `Row { Text(title); Spacer(Modifier.weight(1f)); Button("Done") }`.
 * That reads as "title on the left, button on the right", but it measures the opposite way round:
 * `weight` is applied to the *spacer*, so the title is measured first with the full width and can
 * take all of it. The button is then laid out in what is left — often zero — and its own label
 * wraps one letter per line. On a phone that is what "Done" rendered vertically was.
 *
 * Here the weight is on the *title*, so it is the one that yields: it keeps one line and an
 * ellipsis, and the buttons keep their intrinsic width, at any screen size.
 */
@Composable
fun RowScope.HeaderTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.weight(1f),
    )
}
