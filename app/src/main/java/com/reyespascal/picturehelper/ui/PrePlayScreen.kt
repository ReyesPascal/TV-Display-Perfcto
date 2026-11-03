package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.reyespascal.picturehelper.checklist.SetupMode
import com.reyespascal.picturehelper.playback.ClipSource

@Composable
fun PrePlayScreen(mode: SetupMode, clip: ClipSource?, remembered: Int, onFocus: (Int) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    val title = if (mode == SetupMode.HDR) "HDR10 setup" else "SDR setup"
    Page(title) {
        Body(
            if (mode == SetupMode.HDR)
                "This plays an HDR10 video on a loop. While it plays, the TV shows its HDR picture settings."
            else
                "This plays a normal (SDR) video on a loop, so you can set the TV's everyday picture settings.",
            size = 20.sp, color = MaterialTheme.colorScheme.onBackground,
        )
        Body("How to change a setting while the video plays:", size = 20.sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp))
        Body(
            "1. Read the setting on the right of the screen.\n" +
                "2. Press the remote's square button with two lines, then choose Picture.\n" +
                "3. Up/Down moves, OK opens, Left/Right changes a slider, Back goes up a level.\n" +
                "4. Close the TV menu with Back, then mark the setting Applied, Skipped or Not found.",
            size = 18.sp,
        )
        Body(
            "Picture Helper can't read or change TV settings; you do that in the TV's menu. Settings you make here " +
                "apply to the TV's built-in apps. HDMI inputs may keep their own settings.",
            size = 16.sp,
        )
        ButtonColumn(
            labels = listOf(if (clip != null) "Start video" else "No video found", "Back"),
            remembered = remembered, onFocus = onFocus,
            enabled = { it != 0 || clip != null },
            onClick = { if (it == 0) onStart() else onBack() },
        )
    }
}
