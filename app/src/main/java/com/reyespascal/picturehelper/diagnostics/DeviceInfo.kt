package com.reyespascal.picturehelper.diagnostics

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.os.Build
import android.view.Display

/** Facts the system reports. None of these prove the TV is showing HDR right now. */
object DeviceInfo {

    fun displayHdrTypes(display: Display?): String {
        @Suppress("DEPRECATION")
        val types = display?.hdrCapabilities?.supportedHdrTypes ?: return "unknown"
        if (types.isEmpty()) return "none reported"
        return types.joinToString { hdrTypeName(it) }
    }

    fun displayMode(display: Display?): String {
        val m = display?.mode ?: return "unknown"
        return "${m.physicalWidth}x${m.physicalHeight} @ ${"%.2f".format(m.refreshRate)} Hz"
    }

    /** HEVC decoders that list the Main10 HDR10 profile. */
    fun hevcHdr10Decoders(): List<String> =
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { !it.isEncoder && it.supportedTypes.any { t -> t.equals("video/hevc", true) } }
            .filter { info ->
                runCatching {
                    info.getCapabilitiesForType("video/hevc").profileLevels
                        .any { it.profile == CodecProfileLevel.HEVCProfileMain10HDR10 }
                }.getOrDefault(false)
            }
            .map { if (it.isHardwareAccelerated) "${it.name} (hardware)" else it.name }

    fun deviceSummary(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) · " +
            Build.SUPPORTED_ABIS.joinToString()

    private fun hdrTypeName(t: Int): String = when (t) {
        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
        else -> "type $t"
    }
}
