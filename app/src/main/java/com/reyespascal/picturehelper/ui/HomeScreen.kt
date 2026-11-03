package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.reyespascal.picturehelper.diagnostics.DeviceInfo
import com.reyespascal.picturehelper.playback.ClipSource

/** M1 prototype home: one button. Focus returns to it whenever the screen is shown again. */
@Composable
fun HomeScreen(hdrClip: ClipSource?, onPlayHdr: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = SafeH, vertical = SafeV),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Picture Helper", fontSize = 40.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Plays an HDR10 video so the TV's own HDR picture settings appear. " +
                "Open the TV's picture menu while it plays.",
            fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 760.dp),
        )
        Button(
            onClick = onPlayHdr,
            enabled = hdrClip != null,
            modifier = Modifier.padding(top = 12.dp).focusRequester(focus),
            scale = ButtonDefaults.scale(focusedScale = 1.08f),
            colors = ButtonDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Play HDR setup video", fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text(
            hdrClip?.description?.let { "Clip: $it" }
                ?: "No HDR clip found. Copy one named hdr10.mp4 into this app's folder (see SPEC.md).",
            fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(DeviceInfo.deviceSummary(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp))
        Text("Settings guide based on RTINGS.com's Hisense U6H settings (coming after this test).",
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
