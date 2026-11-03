package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.sp
import com.reyespascal.picturehelper.checklist.RtingsChecklist

@Composable
fun AboutScreen() {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Page("About and sources") {
        // Focusable so D-pad Up/Down scrolls the text.
        Column(Modifier.focusRequester(focus).focusable().verticalScroll(rememberScrollState())) {
            Body("Settings source", size = 20.sp, color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
            Body(
                "${RtingsChecklist.SOURCE_TITLE}\n${RtingsChecklist.SOURCE_URL}\n" +
                    "Page last updated ${RtingsChecklist.SOURCE_UPDATED}; checked ${RtingsChecklist.CHECKED}. " +
                    "Values are RTINGS'; descriptions are Picture Helper's own summary. RTINGS calls these a " +
                    "starting point, not exact values to copy. Their white-balance and colour calibration numbers " +
                    "are left out because they're specific to the TV RTINGS tested.",
            )
            Body("Motion", size = 20.sp, color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
            Body(RtingsChecklist.JUDDER_NOTE)
            Body("Setup videos", size = 20.sp, color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
            Body(
                "Short excerpts of \"Sol Levante\" © Netflix, Inc., from Netflix Open Content, used under the " +
                    "Creative Commons Attribution 4.0 license (creativecommons.org/licenses/by/4.0). Trimmed, audio " +
                    "removed and re-encoded for TV playback; grading unchanged. They are viewing content, not " +
                    "calibration test patterns.",
            )
            Body("What this app does", size = 20.sp, color = androidx.tv.material3.MaterialTheme.colorScheme.primary)
            Body(
                "Guided setup only. It doesn't measure the picture and can't read or change TV settings. " +
                    "Applied / Skipped / Not found are your own notes.",
            )
        }
    }
}
