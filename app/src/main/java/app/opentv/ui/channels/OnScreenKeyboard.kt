/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.opentv.R

/**
 * A d-pad-navigable on-screen keyboard, shared by every screen that needs text entry on a TV.
 * Android TV's system text field punts to "type on your phone"; this keeps entry on the remote.
 *
 * ## Why it is laid out this way
 *
 * The first version drew rows of 48 dp keys with no relationship to the space available: ten keys
 * plus gaps is 552 dp, so on anything narrower than a small tablet the last keys were cut off and
 * the whole thing read as a list that ran out of screen. Keys are now sized from the width actually
 * on offer, and the rows are staggered the way a physical keyboard is (10 / 10 / 9 / 7), with shift,
 * space and backspace where a keyboard has them.
 */
@Composable
internal fun OnScreenKeyboard(
    onKey: (String) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shifted by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }
    val context = LocalContext.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 6.dp
        // "1234567890" and "QWERTYUIOP" are the widest rows, so they set the key width.
        val keyWidth = ((maxWidth - gap * 9) / 10).coerceIn(24.dp, 54.dp)
        val keyHeight = (keyWidth * 1.05f).coerceIn(30.dp, 52.dp)

        fun wide(units: Float): Dp = keyWidth * units + gap * (units - 1f)

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KeyboardRow(gap) {
                "1234567890".forEach { ch ->
                    KeyboardKey(ch.toString(), keyWidth, keyHeight) { onKey(ch.toString()) }
                }
            }

            KeyboardRow(gap) {
                "QWERTYUIOP".forEachIndexed { index, ch ->
                    val letter = (if (shifted) ch else ch.lowercaseChar()).toString()
                    KeyboardKey(
                        label = letter,
                        w = keyWidth,
                        h = keyHeight,
                        focusRequester = if (index == 0) firstKey else null,
                    ) { onKey(letter) }
                }
            }

            KeyboardRow(gap) {
                "ASDFGHJKL".forEach { ch ->
                    val letter = (if (shifted) ch else ch.lowercaseChar()).toString()
                    KeyboardKey(letter, keyWidth, keyHeight) { onKey(letter) }
                }
            }

            KeyboardRow(gap) {
                KeyboardKey(
                    label = stringResource(R.string.kbd_shift),
                    w = wide(1.5f),
                    h = keyHeight,
                    icon = Icons.Filled.KeyboardArrowUp,
                    selected = shifted,
                ) { shifted = !shifted }
                "ZXCVBNM".forEach { ch ->
                    val letter = (if (shifted) ch else ch.lowercaseChar()).toString()
                    KeyboardKey(letter, keyWidth, keyHeight) { onKey(letter) }
                }
                KeyboardKey(
                    label = stringResource(R.string.kbd_del),
                    w = wide(1.5f),
                    h = keyHeight,
                    icon = Icons.Filled.Backspace,
                ) { onBackspace() }
            }

            KeyboardRow(gap) {
                KeyboardKey(
                    label = stringResource(R.string.kbd_paste),
                    w = wide(2f),
                    h = keyHeight,
                ) {
                    val pasted = readClipboardText(context)
                    if (pasted != null) {
                        onKey(pasted)
                    } else {
                        Toast.makeText(context, context.getString(R.string.kbd_clipboard_empty), Toast.LENGTH_SHORT).show()
                    }
                }
                KeyboardKey(
                    label = stringResource(R.string.kbd_space),
                    w = wide(5f),
                    h = keyHeight,
                ) { onSpace() }
                KeyboardKey(
                    label = stringResource(R.string.kbd_clear),
                    w = wide(2f),
                    h = keyHeight,
                ) { onClear() }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Focus starts on the first letter, not on "1": a keyboard that opens on the digit row makes
        // the user press DOWN before they can type anything.
        runCatching { firstKey.requestFocus() }
    }
}

@Composable
private fun KeyboardRow(gap: Dp, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    w: Dp,
    h: Dp,
    icon: ImageVector? = null,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        Modifier
            .width(w)
            .height(h)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                else Modifier,
            )
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(h * 0.5f))
        } else {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/**
 * Plain text currently on the system clipboard, trimmed — or null when there's nothing to paste.
 * Shared by this keyboard's Paste key and the provider form's paste buttons so both read the
 * clipboard the same way.
 */
internal fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0)?.coerceToText(context)?.toString()
    return text?.trim()?.takeIf { it.isNotEmpty() }
}
