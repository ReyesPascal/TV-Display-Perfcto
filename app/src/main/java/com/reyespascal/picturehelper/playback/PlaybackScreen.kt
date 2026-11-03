package com.reyespascal.picturehelper.playback

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.reyespascal.picturehelper.PhLog
import com.reyespascal.picturehelper.diagnostics.DeviceInfo
import com.reyespascal.picturehelper.ui.SafeH
import com.reyespascal.picturehelper.ui.SafeV

/**
 * Full-screen looping playback.
 * - The player exists only between onStart and onStop, so it keeps playing through onPause
 *   (for example when a TV menu opens on top) and is released when the screen is really left.
 * - Only OK (show/hide diagnostics) and Back (leave) are used. Every other key is passed on,
 *   so the TV's own Settings/Menu keys keep working.
 */
@Composable
fun PlaybackScreen(
    clip: ClipSource,
    lifecycleLog: List<String>,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var info by remember { mutableStateOf(PlaybackInfo()) }
    var showDiagnostics by remember { mutableStateOf(true) }

    LifecycleStartEffect(clip.uri) {
        PhLog.i("player: create for ${clip.uri}")
        val p = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(clip.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    val name = when (state) {
                        Player.STATE_IDLE -> "idle"; Player.STATE_BUFFERING -> "buffering"
                        Player.STATE_READY -> "playing"; Player.STATE_ENDED -> "ended"; else -> "$state"
                    }
                    PhLog.i("player: state $name")
                    info = info.copy(state = name)
                }
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                        PhLog.i("player: loop ${info.loops + 1}")
                        info = info.copy(loops = info.loops + 1)
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    PhLog.w("player: error ${error.errorCodeName}", error)
                    info = info.copy(state = "error", error = "${error.errorCodeName}: ${error.message ?: ""}")
                }
            })
            addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime, decoderName: String,
                    initializedTimestampMs: Long, initializationDurationMs: Long,
                ) {
                    PhLog.i("player: video decoder $decoderName")
                    info = info.copy(decoder = decoderName)
                }
                override fun onVideoInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime, format: androidx.media3.common.Format,
                    decoderReuseEvaluation: DecoderReuseEvaluation?,
                ) {
                    PhLog.i("player: video format ${format.sourceSummary().replace('\n', ' ')}")
                    info = info.copy(format = format)
                }
                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long,
                ) {
                    info = info.copy(droppedFrames = info.droppedFrames + droppedFrames)
                }
            })
            prepare()
            playWhenReady = true
        }
        player = p
        onStopOrDispose {
            PhLog.i("player: release (screen stopped or left)")
            p.release()
            player = null
        }
    }

    // Keep the TV awake while the setup video plays.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler { onExit() }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                val code = e.key.nativeKeyCode
                val isOk = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER || code == AndroidKeyEvent.KEYCODE_ENTER
                if (isOk && e.type == KeyEventType.KeyUp) { showDiagnostics = !showDiagnostics; true }
                else isOk // swallow OK key-down too; everything else goes to the system
            },
    ) {
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.fillMaxSize(),
        )

        info.error?.let { err ->
            Panel(Modifier.align(Alignment.Center)) {
                Text("Playback problem", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                Text(err, fontSize = 18.sp)
                Text("Press Back to return.", fontSize = 18.sp)
            }
        }

        if (showDiagnostics && info.error == null) {
            Panel(Modifier.align(Alignment.TopEnd).padding(end = SafeH, top = SafeV).widthIn(max = RightPanelMax)) {
                val display = view.display
                Text("Diagnostics  ·  OK hides this  ·  Back exits", fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary)
                Line("Clip", "${clip.description} · ${info.state} · loops ${info.loops} · dropped ${info.droppedFrames}")
                Line("Source (file)", info.format?.sourceSummary() ?: "waiting…")
                Line("Decoder used", info.decoder ?: "waiting…")
                Line("Display reports", "${DeviceInfo.displayHdrTypes(display)} · UI ${DeviceInfo.displayMode(display)}")
                Line("HDR on the TV", "Needs confirmation: open the TV's picture menu and look for the HDR icon.")
                Line("Recent events", lifecycleLog.takeLast(4).joinToString("\n"))
            }
        }
    }
}

/**
 * The Hisense picture menu covers the left ~44% of the screen (418 of 960 dp, measured on the TV).
 * App overlays sit on the right, inside what's left after the safe margin, so the menu never hides them.
 */
private val RightPanelMax = 440.dp

@Composable
private fun Panel(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .widthIn(max = 560.dp)
            .background(Color(0xCC0E1014), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Line(label: String, value: String) {
    Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp))
    Text(value, fontSize = 15.sp, color = Color(0xFFF2F2F2))
}
