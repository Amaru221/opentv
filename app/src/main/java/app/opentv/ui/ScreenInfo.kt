/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.opentv.isRunningOnTelevision

/**
 * What kind of screen this is, and the sizes that follow from it.
 *
 * ## Why not just check the orientation
 *
 * Because orientation is not floor space. A phone held sideways reports `LANDSCAPE` at 640 dp of
 * width, where the guide fits; the same phone in split screen reports `LANDSCAPE` at 400 dp, where
 * it does not. A foldable changes from 400 to 800 dp without rotating at all. Everything below is
 * therefore derived from `screenWidthDp`, and [ScreenShape] is exposed only for the cases that
 * genuinely care which way the device is held.
 *
 * ## Why a TV is a separate form factor
 *
 * A 1080p television reports ~960 × 540 dp — narrower than a tablet in dp terms, so a width
 * breakpoint alone would classify it as one. What actually differs is viewing distance: text has
 * to be bigger, the channel column has to hold a full channel name plus a logo, and the guide must
 * keep a 5% overscan margin because some panels crop the outer edge. That is [FormFactor.TV], and
 * it comes from [isRunningOnTelevision] rather than from size.
 */
enum class FormFactor { TV, TABLET, HANDSET }

enum class ScreenShape { PORTRAIT, LANDSCAPE }

/**
 * Sizes the guide is built from, in one place.
 *
 * Every one of these used to be a `private val` constant inside `GuideGrid`/`GuidePreview`, which
 * is how a layout ends up with a 220 dp channel column on a 360 dp phone: nothing could change
 * them but the source. They are still fixed *values* — they just differ per form factor now.
 */
data class GuideMetrics(
    /** Width of the fixed channel column on the left of the grid. */
    val channelColumn: Dp,
    /** One channel row. Scaled by the user's font size, which is what used to clip the rows. */
    val rowHeight: Dp,
    /** Horizontal pixels per minute. 4 dp/min showed 2h40 at 960 dp; 5 gives room for text. */
    val minuteDp: Float,
    /** Height of the preview pane above the guide. Zero on a handset, where it is not drawn. */
    val previewHeight: Dp,
    /** The 16:9 card can never take more than this, whatever the pane height works out to. */
    val previewVideoMaxWidth: Dp,
    /** Below this width the preview is dropped entirely — see [GuideMetrics.previewHeight]. */
    val hidePreview: Boolean,
    /** Category rail width when open. */
    val railWidth: Dp,
    /** Horizontal margin for the content area; larger on TV for overscan. */
    val horizontalPadding: Dp,
    /** Hairlines every half hour. Costs one draw pass, makes the columns readable. */
    val showGridLines: Boolean,
    /**
     * Tap target for the favourite star. 28 dp is right under a d-pad, which aims precisely; a
     * finger needs more, and 48 dp in a 260 dp column would take a fifth of the channel name.
     */
    val starButtonSize: Dp = 28.dp,
) {
    /** A programme block narrower than this cannot show a title; it is drawn as a bare pill. */
    val minReadableBlock: Dp = 48.dp
}

/**
 * Resolves the current screen. Recomposes on rotation, resize, multi-window and font-size change.
 *
 * The activity declares `configChanges` for orientation/screenSize/screenLayout, so rotating does
 * not recreate it — Compose still receives the new configuration and this recomposes, which keeps
 * the guide's scroll position and the highlighted channel across a rotation.
 */
@Composable
fun rememberScreenInfo(): ScreenInfo {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTelevision = remember(context) { isRunningOnTelevision(context) }

    return remember(isTelevision, config) {
        val width = config.screenWidthDp
        val height = config.screenHeightDp
        // `smallestScreenWidthDp` describes the *device*, not the current window: a phone held
        // sideways is 640 dp wide but still a phone, and classifying it by width turned it into a
        // tablet and made the landscape-phone branch unreachable. Rotation must not change what
        // kind of device this is; it only changes how much room the layout has.
        val smallest = config.smallestScreenWidthDp
        val form = when {
            isTelevision -> FormFactor.TV
            smallest >= 600 -> FormFactor.TABLET
            else -> FormFactor.HANDSET
        }
        ScreenInfo(
            form = form,
            shape = if (width >= height) ScreenShape.LANDSCAPE else ScreenShape.PORTRAIT,
            widthDp = width,
            heightDp = height,
            fontScale = config.fontScale,
        )
    }
}

data class ScreenInfo(
    val form: FormFactor,
    val shape: ScreenShape,
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
) {
    /** Under 600 dp there is no room for a 220 dp channel column plus a timeline. */
    val isCompact: Boolean get() = widthDp < 600

    /**
     * The guide's sizes for this screen.
     *
     * Row height tracks the font scale: the row holds a 17 sp name and a 14 sp secondary line, so
     * a fixed 64 dp clips as soon as someone raises the system font size. Clamped, because at 2×
     * the guide would show two channels and that is worse than slightly tight text.
     */
    val guide: GuideMetrics
        get() {
            val row = (56f * fontScale.coerceIn(0.85f, 1.25f)).dp
            /*
             * The channel column is sized from the name it has to hold, not from a round number.
             * Inside it, fixed chrome on every row is: 16 dp of horizontal padding, a number cell,
             * the logo, an 8 dp gap and the favourite star. What is left is the name — and at
             * 180 dp that came to 72 dp, about eight characters, which is what "Movistar LaL…"
             * looked like on a phone. These widths give roughly 130-200 dp to the name depending
             * on the screen, while leaving the timeline most of the width it had.
             */
            return when {
                form == FormFactor.TV -> GuideMetrics(
                    channelColumn = 300.dp,
                    rowHeight = row,
                    minuteDp = 5f,
                    previewHeight = 160.dp,
                    previewVideoMaxWidth = 300.dp,
                    hidePreview = false,
                    railWidth = 240.dp,
                    // 1080p panels with overscan crop the outer ~5%; 24 dp is that margin.
                    horizontalPadding = 24.dp,
                    showGridLines = true,
                    starButtonSize = 24.dp,
                )

                form == FormFactor.TABLET -> GuideMetrics(
                    channelColumn = 260.dp,
                    rowHeight = row,
                    minuteDp = 4.5f,
                    previewHeight = 140.dp,
                    previewVideoMaxWidth = 240.dp,
                    hidePreview = false,
                    railWidth = 220.dp,
                    horizontalPadding = 16.dp,
                    showGridLines = true,
                    starButtonSize = 24.dp,
                )

                // Handset, but wide enough for the grid (landscape, or a big phone sideways).
                // 240 dp of column plus a coarser timeline: ~15 characters of name, and about an
                // hour and a half of guide still on screen, which is what a phone can show.
                widthDp >= 600 -> GuideMetrics(
                    channelColumn = 240.dp,
                    rowHeight = row,
                    minuteDp = 3f,
                    previewHeight = 132.dp,
                    previewVideoMaxWidth = 180.dp,
                    hidePreview = false,
                    railWidth = 200.dp,
                    horizontalPadding = 12.dp,
                    showGridLines = true,
                    starButtonSize = 32.dp,
                )

                // Handset in portrait: the list, and no preview pane. At 360 dp the 16:9 card alone
                // is 334 dp wide, which starved the text column down to zero — the list already
                // shows name, now, next and progress for every row, so the pane was duplication.
                else -> GuideMetrics(
                    channelColumn = 140.dp,
                    rowHeight = 72.dp,
                    minuteDp = 3f,
                    previewHeight = 0.dp,
                    previewVideoMaxWidth = 0.dp,
                    hidePreview = true,
                    railWidth = 200.dp,
                    horizontalPadding = 12.dp,
                    showGridLines = false,
                    starButtonSize = 40.dp,
                )
            }
        }
}
