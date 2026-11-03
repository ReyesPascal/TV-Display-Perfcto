package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import com.reyespascal.picturehelper.diagnostics.DeviceInfo

@Composable
fun DiagnosticsScreen() {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val display = LocalView.current.display
    val decoders = remember { DeviceInfo.hevcHdr10Decoders() }
    Page("Diagnostics") {
        Column(Modifier.focusRequester(focus).focusable()) {
            Body("Device: ${DeviceInfo.deviceSummary()}")
            Body("Display reports HDR types: ${DeviceInfo.displayHdrTypes(display)}")
            Body("Display mode (UI): ${DeviceInfo.displayMode(display)}")
            Body("HEVC decoders listing HDR10: ${decoders.ifEmpty { listOf("none") }.joinToString()}")
            Body(
                "None of this proves the TV is showing HDR. The proof is the TV's own picture menu listing HDR " +
                    "picture modes while the HDR setup video plays. Playback details are shown during the video " +
                    "(Details button).",
                size = 16.sp,
            )
        }
    }
}
