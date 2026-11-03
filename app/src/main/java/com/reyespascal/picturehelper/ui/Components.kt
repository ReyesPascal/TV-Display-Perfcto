package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

val Muted = Color(0xFFC8CCD4)

/** A focusable button with an obvious yellow focus state. Reports focus so screens can restore it. */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 20.sp,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() },
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        colors = ButtonDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, fontSize = fontSize, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

/**
 * A vertical list of buttons where focus starts on, and returns to, the last-used item.
 * [remembered] is the index to focus first; [onFocus] is told whenever another index gains focus.
 */
@Composable
fun ButtonColumn(
    labels: List<String>,
    remembered: Int,
    onFocus: (Int) -> Unit,
    onClick: (Int) -> Unit,
    enabled: (Int) -> Boolean = { true },
) {
    val requesters = remember(labels.size) { List(labels.size) { FocusRequester() } }
    LaunchedEffect(labels.size) {
        val start = remembered.coerceIn(0, labels.lastIndex).let { if (enabled(it)) it else labels.indices.first(enabled) }
        runCatching { requesters[start].requestFocus() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed { i, label ->
            TvButton(
                label, onClick = { onClick(i) }, enabled = enabled(i),
                focusRequester = requesters[i], onFocused = { onFocus(i) },
            )
        }
    }
}

/** Standard page: title at the top, safe margins all round. */
@Composable
fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, fontSize = 34.sp, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp, color: Color = Muted) {
    Text(text, fontSize = size, color = color, modifier = modifier.widthIn(max = 780.dp))
}

/** Yes/No confirmation. Focus starts on the safe choice (Cancel). */
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val cancelFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { cancelFocus.requestFocus() }
        Box(Modifier.background(Color(0xFF1B1E26), RoundedCornerShape(12.dp)).padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                Text(message, fontSize = 18.sp, color = Color(0xFFF2F2F2), modifier = Modifier.widthIn(max = 520.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvButton("Cancel", onDismiss, focusRequester = cancelFocus)
                    TvButton(confirm, onConfirm)
                }
            }
        }
    }
}
