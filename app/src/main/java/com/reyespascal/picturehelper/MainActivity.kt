package com.reyespascal.picturehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Surface
import com.reyespascal.picturehelper.playback.ClipMode
import com.reyespascal.picturehelper.playback.Clips
import com.reyespascal.picturehelper.playback.PlaybackScreen
import com.reyespascal.picturehelper.ui.HomeScreen
import com.reyespascal.picturehelper.ui.PictureHelperTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Recent lifecycle and window-focus events, shown in diagnostics and logged. */
    private val events = mutableStateListOf<String>()
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)

    private fun record(event: String) {
        PhLog.i("activity: $event")
        events.add("${clock.format(Date())} $event")
        if (events.size > 20) events.removeAt(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifecycleEventObserver { _, e ->
            if (e != Lifecycle.Event.ON_ANY) record(e.name.removePrefix("ON_").lowercase())
        })
        val hdrClip = Clips.find(this, ClipMode.HDR10)
        PhLog.i("hdr clip: ${hdrClip?.uri ?: "none"}")

        setContent {
            PictureHelperTheme {
                Surface(Modifier.fillMaxSize()) {
                    var playing by rememberSaveable { mutableStateOf(false) }
                    if (playing && hdrClip != null) {
                        PlaybackScreen(clip = hdrClip, lifecycleLog = events, onExit = { playing = false })
                    } else {
                        HomeScreen(hdrClip = hdrClip, onPlayHdr = { playing = true })
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        record(if (hasFocus) "window focus gained" else "window focus lost")
    }
}
