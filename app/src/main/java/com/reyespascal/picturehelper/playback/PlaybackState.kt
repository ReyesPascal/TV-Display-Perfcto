@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.reyespascal.picturehelper.playback

import androidx.media3.common.C
import androidx.media3.common.Format

/** What the player itself reports. Shown in diagnostics, never as proof of HDR on the TV. */
data class PlaybackInfo(
    val state: String = "starting",
    val decoder: String? = null,
    val format: Format? = null,
    val droppedFrames: Int = 0,
    val loops: Int = 0,
    val error: String? = null,
)

fun Format.sourceSummary(): String {
    val c = colorInfo
    val prim = when (c?.colorSpace) {
        C.COLOR_SPACE_BT2020 -> "BT.2020"; C.COLOR_SPACE_BT709 -> "BT.709"; C.COLOR_SPACE_BT601 -> "BT.601"
        null -> "not tagged"; else -> "space ${c.colorSpace}"
    }
    val transfer = when (c?.colorTransfer) {
        C.COLOR_TRANSFER_ST2084 -> "PQ (ST 2084)"; C.COLOR_TRANSFER_HLG -> "HLG"
        C.COLOR_TRANSFER_SDR -> "SDR"; null -> "not tagged"; else -> "transfer ${c.colorTransfer}"
    }
    val range = when (c?.colorRange) { C.COLOR_RANGE_LIMITED -> "limited"; C.COLOR_RANGE_FULL -> "full"; else -> "?" }
    val meta = if (c?.hdrStaticInfo != null) "HDR static metadata present" else "no HDR static metadata"
    val fps = if (frameRate > 0) " · ${"%.3f".format(frameRate)} fps" else ""
    return "${sampleMimeType ?: "?"} ${codecs ?: ""} · ${width}x$height$fps\n$prim · $transfer · $range range · $meta"
}
