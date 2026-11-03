package com.reyespascal.picturehelper.playback

import android.content.Context
import android.net.Uri
import java.io.File

enum class ClipMode(val fileName: String) { HDR10("hdr10.mp4"), SDR("sdr.mp4"), DV("dolby_vision.mp4") }

/**
 * Where a clip comes from. A file the owner copied to the app's own folder
 * (/sdcard/Android/data/<package>/files/) wins over the bundled one.
 */
data class ClipSource(val uri: Uri, val description: String, val bundled: Boolean)

object Clips {
    fun find(context: Context, mode: ClipMode): ClipSource? {
        context.getExternalFilesDir(null)?.let { dir ->
            val f = File(dir, mode.fileName)
            if (f.isFile && f.length() > 0) {
                return ClipSource(Uri.fromFile(f), "Your file: ${f.name}", bundled = false)
            }
        }
        val bundled = runCatching { context.assets.openFd("clips/${mode.fileName}").use { true } }
            .getOrDefault(false)
        return if (bundled) {
            ClipSource(Uri.parse("asset:///clips/${mode.fileName}"), "Built-in clip", bundled = true)
        } else null
    }
}
