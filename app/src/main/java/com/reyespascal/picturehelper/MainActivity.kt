package com.reyespascal.picturehelper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Surface
import com.reyespascal.picturehelper.checklist.Progress
import com.reyespascal.picturehelper.checklist.ProgressStore
import com.reyespascal.picturehelper.checklist.RtingsChecklist
import com.reyespascal.picturehelper.checklist.SetupMode
import com.reyespascal.picturehelper.playback.ClipMode
import com.reyespascal.picturehelper.playback.Clips
import com.reyespascal.picturehelper.ui.AboutScreen
import com.reyespascal.picturehelper.ui.DiagnosticsScreen
import com.reyespascal.picturehelper.ui.GuideFlow
import com.reyespascal.picturehelper.ui.HomeChoice
import com.reyespascal.picturehelper.ui.HomeScreen
import com.reyespascal.picturehelper.ui.PictureHelperTheme
import com.reyespascal.picturehelper.ui.PrePlayScreen
import com.reyespascal.picturehelper.ui.ProgressScreen
import com.reyespascal.picturehelper.ui.summary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.progressData by preferencesDataStore(name = "progress")

class MainActivity : ComponentActivity() {

    /** Recent lifecycle and window-focus events, shown in Details and logged. */
    private val events = mutableStateListOf<String>()
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)

    /** False while another window, such as the TV's picture menu, has focus over the app. */
    private var windowFocused by mutableStateOf(true)

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
        val store = ProgressStore(applicationContext.progressData)
        val clips = mapOf(
            SetupMode.HDR to Clips.find(this, ClipMode.HDR10),
            SetupMode.SDR to Clips.find(this, ClipMode.SDR),
            SetupMode.DV to Clips.find(this, ClipMode.DV),
        )
        PhLog.i("clips: ${clips.mapValues { it.value?.uri }}")

        setContent {
            PictureHelperTheme {
                Surface(Modifier.fillMaxSize()) {
                    // Simple back stack of routes: "home", "pre/HDR", "play/HDR", "progress", "about", "diag".
                    var stack by rememberSaveable { mutableStateOf(listOf("home")) }
                    fun open(route: String) { stack = stack + route }
                    fun back() { if (stack.size > 1) stack = stack.dropLast(1) else finish() }
                    // Last focused button per screen, so returning puts focus where it was.
                    val focusMemory = remember { mutableStateMapOf<String, Int>() }
                    val scope = rememberCoroutineScope()

                    val progress = SetupMode.entries.associateWith { mode ->
                        remember(mode) { store.progress(mode) }.collectAsState(initial = Progress(emptyMap(), 0)).value
                    }
                    fun progressOf(m: SetupMode) = progress.getValue(m)

                    val route = stack.last()
                    if (route != "home") BackHandler(enabled = !route.startsWith("play/")) { back() }
                    when {
                        route == "home" -> HomeScreen(
                            remembered = focusMemory["home"] ?: 0, onFocus = { focusMemory["home"] = it },
                            hdrSummary = progressOf(SetupMode.HDR).summary(RtingsChecklist.forMode(SetupMode.HDR).size),
                            dvSummary = progressOf(SetupMode.DV).summary(RtingsChecklist.forMode(SetupMode.DV).size),
                            sdrSummary = progressOf(SetupMode.SDR).summary(RtingsChecklist.forMode(SetupMode.SDR).size),
                        ) { choice ->
                            when (choice) {
                                HomeChoice.HDR -> open("pre/HDR")
                                HomeChoice.DV -> open("pre/DV")
                                HomeChoice.SDR -> open("pre/SDR")
                                HomeChoice.PROGRESS -> open("progress")
                                HomeChoice.DIAGNOSTICS -> open("diag")
                                HomeChoice.ABOUT -> open("about")
                            }
                        }
                        route.startsWith("pre/") -> {
                            val mode = SetupMode.valueOf(route.removePrefix("pre/"))
                            PrePlayScreen(
                                mode, clips[mode], remembered = focusMemory[route] ?: 0,
                                onFocus = { focusMemory[route] = it },
                                onStart = { open("play/${mode.name}") }, onBack = { back() },
                            )
                        }
                        route.startsWith("play/") -> {
                            val mode = SetupMode.valueOf(route.removePrefix("play/"))
                            val clip = clips[mode]
                            if (clip == null) back() else GuideFlow(
                                mode, clip, progressOf(mode), store, events, tvMenuOpen = !windowFocused,
                                onExit = { back() },
                            )
                        }
                        route == "progress" -> ProgressScreen(::progressOf) { m -> scope.launch { store.reset(m) } }
                        route == "about" -> AboutScreen()
                        route == "diag" -> DiagnosticsScreen()
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        windowFocused = hasFocus
        record(if (hasFocus) "window focus gained" else "window focus lost")
    }
}
