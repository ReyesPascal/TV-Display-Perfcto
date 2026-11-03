@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.reyespascal.picturehelper.playback

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Format
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
import com.reyespascal.picturehelper.checklist.Scene
import com.reyespascal.picturehelper.diagnostics.DeviceInfo
import com.reyespascal.picturehelper.ui.SafeH
import com.reyespascal.picturehelper.ui.SafeV

/** GUIDE = full panel; MENU_CARD = instructions card while the TV menu is open; DETAILS = diagnostics; HIDDEN = nothing. */
enum class OverlayPanel { GUIDE, MENU_CARD, DETAILS, HIDDEN }

/** Actions the guide panel can trigger on the playback screen. */
class OverlayActions(val showDetails: () -> Unit, val hide: () -> Unit)

/**
 * Full-screen looping playback with an overlay on the right (the Hisense menu covers the left).
 * - The player exists only between onStart and onStop, so it keeps playing through onPause (the TV menu
 *   only takes window focus) and is released when the screen is really left.
 * - [scene] with a still time and [holding] = true seeks there and pauses, for before/after comparisons.
 * - While the TV menu is open, the guide becomes a [menuCard] on the right (clear of the menu) that says
 *   what to change and where. When the menu closes the full guide comes straight back.
 * - Only D-pad/OK (inside the panel) and Back are used; all other keys go to the TV.
 */
@Composable
fun PlaybackScreen(
    clip: ClipSource,
    lifecycleLog: List<String>,
    scene: Scene,
    holding: Boolean,
    /** True while another window (the TV's picture menu) has focus over this screen. */
    tvMenuOpen: Boolean,
    /** Instructions shown on the right while the TV menu is open; null = nothing to show. */
    menuCard: (@Composable ColumnScope.() -> Unit)?,
    guideFocus: FocusRequester,
    guide: @Composable ColumnScope.(OverlayActions) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var info by remember { mutableStateOf(PlaybackInfo()) }
    var panel by remember { mutableStateOf(OverlayPanel.GUIDE) }

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
                        Player.STATE_READY -> "ready"; Player.STATE_ENDED -> "ended"; else -> "$state"
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
                // Start decoding only once the real SurfaceView surface exists. If the decoder starts on
                // ExoPlayer's placeholder surface and is moved later, the Hisense decoder switches to a
                // GPU-composited buffer format and the TV leaves HDR (seen on the TV 2026-11-03).
                override fun onSurfaceSizeChanged(width: Int, height: Int) {
                    if (width > 0 && height > 0 && playbackState == Player.STATE_IDLE && playerError == null) {
                        PhLog.i("player: surface ready ${width}x$height, prepare")
                        prepare()
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
                    eventTime: AnalyticsListener.EventTime, format: Format,
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
            playWhenReady = true
            // prepare() waits for the surface: see onSurfaceSizeChanged above.
        }
        player = p
        onStopOrDispose {
            PhLog.i("player: release (screen stopped or left)")
            p.release()
            player = null
        }
    }

    // Hold a still scene for comparing, or keep playing.
    LaunchedEffect(player, scene, holding) {
        val p = player ?: return@LaunchedEffect
        val at = scene.positionMs
        if (holding && at != null) {
            PhLog.i("player: hold scene ${scene.name} at $at ms")
            p.seekTo(at)
            p.playWhenReady = false
        } else {
            PhLog.i("player: play (${scene.name})")
            p.playWhenReady = true
        }
    }

    // Keep the TV awake while the setup video is on screen, playing or held.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler {
        when (panel) {
            OverlayPanel.HIDDEN, OverlayPanel.DETAILS, OverlayPanel.MENU_CARD -> panel = OverlayPanel.GUIDE
            OverlayPanel.GUIDE -> onExit()
        }
    }

    // While the guide isn't shown, the screen itself holds focus; any D-pad or OK press brings it back.
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(panel, tvMenuOpen) {
        if (tvMenuOpen) return@LaunchedEffect
        runCatching { if (panel == OverlayPanel.GUIDE) guideFocus.requestFocus() else rootFocus.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onKeyEvent { e ->
                val code = e.key.nativeKeyCode
                // When hidden, any D-pad key or OK shows the guide.
                if (code !in NAV_KEYS || panel == OverlayPanel.GUIDE) return@onKeyEvent false // other keys go to the TV
                if (e.type == KeyEventType.KeyUp) panel = OverlayPanel.GUIDE
                true
            },
    ) {
        PlayerSurface(player = player, surfaceType = SURFACE_TYPE_SURFACE_VIEW, modifier = Modifier.fillMaxSize())

        info.error?.let { err ->
            Panel(Modifier.align(Alignment.Center)) {
                Text("Playback problem", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                Text(err, fontSize = 18.sp)
                Text("Press Back to return.", fontSize = 18.sp)
            }
        }

        // What the overlay shows. The TV menu being open means "instructions card" (unless hidden).
        val shown = when {
            info.error != null -> OverlayPanel.HIDDEN
            tvMenuOpen && panel != OverlayPanel.HIDDEN -> OverlayPanel.MENU_CARD
            else -> panel
        }

        // Full guide / details: fade and slide in from the right edge, like a tvOS side panel.
        AnimatedVisibility(
            visible = shown == OverlayPanel.GUIDE || shown == OverlayPanel.DETAILS,
            enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 6 },
            modifier = Modifier.align(Alignment.TopEnd).padding(end = SafeH, top = SafeV).widthIn(max = RightPanelMax),
        ) {
            Panel(Modifier) {
                if (shown == OverlayPanel.DETAILS) {
                    val display = view.display
                    Text("Details  ·  Back returns to the guide", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Line("Clip", "${clip.description} · ${info.state} · loops ${info.loops} · dropped ${info.droppedFrames}")
                    Line("Source (file)", info.format?.sourceSummary() ?: "waiting…")
                    Line("Decoder used", info.decoder ?: "waiting…")
                    Line("Display reports", "${DeviceInfo.displayHdrTypes(display)} · UI ${DeviceInfo.displayMode(display)}")
                    Line("HDR on the TV", "Only the TV's menu can show this: HDR picture modes listed = HDR is on.")
                    Line("Recent events", lifecycleLog.takeLast(4).joinToString("\n"))
                } else {
                    guide(OverlayActions(showDetails = { panel = OverlayPanel.DETAILS }, hide = { panel = OverlayPanel.HIDDEN }))
                }
            }
        }

        // While the TV menu is open: what to change and where, on the right, clear of the menu.
        AnimatedVisibility(
            visible = shown == OverlayPanel.MENU_CARD && menuCard != null,
            enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 },
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = SafeH, top = SafeV).widthIn(max = RightPanelMax),
        ) {
            Panel(Modifier) { menuCard?.invoke(this) }
        }
    }
}

private val OK_KEYS = setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER)
private val NAV_KEYS = OK_KEYS + setOf(
    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN,
    AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
)

/**
 * The Hisense picture menu covers the left ~44% of the screen (418 of 960 dp, measured on the TV).
 * App overlays sit on the right, inside what's left after the safe margin, so the menu never hides them.
 */
val RightPanelMax = 440.dp

@Composable
fun Panel(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .widthIn(max = 560.dp)
            .background(Color(0xD90E1014), RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun Line(label: String, value: String) {
    Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    Text(value, fontSize = 15.sp, color = Color(0xFFF2F2F2))
}
