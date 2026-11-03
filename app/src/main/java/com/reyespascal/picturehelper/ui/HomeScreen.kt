package com.reyespascal.picturehelper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

enum class HomeChoice(val label: String) {
    HDR("HDR10 setup"), SDR("SDR setup"), PROGRESS("Saved progress"), DIAGNOSTICS("Diagnostics"), ABOUT("About and sources"),
}

@Composable
fun HomeScreen(remembered: Int, onFocus: (Int) -> Unit, hdrSummary: String, sdrSummary: String, onChoose: (HomeChoice) -> Unit) {
    Page("Picture Helper") {
        Body(
            "Plays a setup video so your TV shows its own picture settings, then walks you through RTINGS' " +
                "recommended values one at a time. You change each setting yourself in the TV's menu.",
            size = 20.sp,
        )
        ButtonColumn(
            labels = HomeChoice.entries.map {
                when (it) {
                    HomeChoice.HDR -> "${it.label}  ·  $hdrSummary"
                    HomeChoice.SDR -> "${it.label}  ·  $sdrSummary"
                    else -> it.label
                }
            },
            remembered = remembered, onFocus = onFocus,
            onClick = { onChoose(HomeChoice.entries[it]) },
        )
    }
}
