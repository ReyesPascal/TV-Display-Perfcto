package com.reyespascal.picturehelper.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.reyespascal.picturehelper.checklist.ChecklistItem
import com.reyespascal.picturehelper.checklist.Mark
import com.reyespascal.picturehelper.checklist.Progress
import com.reyespascal.picturehelper.checklist.ProgressStore
import com.reyespascal.picturehelper.checklist.RtingsChecklist
import com.reyespascal.picturehelper.checklist.Scene
import com.reyespascal.picturehelper.checklist.SceneFor
import com.reyespascal.picturehelper.checklist.SetupMode
import com.reyespascal.picturehelper.playback.ClipSource
import com.reyespascal.picturehelper.playback.OverlayActions
import com.reyespascal.picturehelper.playback.PlaybackScreen
import kotlinx.coroutines.launch

/** Video + guide for one mode: one setting at a time, with a held scene to compare before/after. */
@Composable
fun GuideFlow(
    mode: SetupMode,
    clip: ClipSource,
    progress: Progress,
    store: ProgressStore,
    lifecycleLog: List<String>,
    tvMenuOpen: Boolean,
    onExit: () -> Unit,
) {
    val items = remember(mode) { RtingsChecklist.forMode(mode) }
    val scope = rememberCoroutineScope()
    val position = progress.position.coerceIn(0, items.size)
    val item = items.getOrNull(position)

    // Each setting starts on its suggested scene, held still; the owner can change scene or play.
    var scene by remember { mutableStateOf(Scene.MOVING) }
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(item?.id) {
        scene = item?.let { SceneFor.item(it.id) } ?: Scene.MOVING
        holding = scene.positionMs != null
    }

    fun go(to: Int) = scope.launch { store.setPosition(mode, to.coerceIn(0, items.size)) }
    fun mark(m: Mark) = scope.launch {
        item?.let { store.mark(mode, it.id, m) }
        store.setPosition(mode, (position + 1).coerceAtMost(items.size))
    }

    val focus = remember { FocusRequester() }
    PlaybackScreen(
        clip = clip,
        lifecycleLog = lifecycleLog,
        scene = scene,
        holding = holding,
        tvMenuOpen = tvMenuOpen,
        menuCard = { MenuCard(mode, items, progress, item) },
        guideFocus = focus,
        guide = { actions ->
            if (item == null) {
                Summary(mode, progress, items.size, focus, onRestart = { go(0) }, onFinish = onExit)
            } else {
                GuideItem(
                    mode, position, items.size, item, progress.marks[item.id], scene, holding, focus, actions,
                    onMark = ::mark, onPrev = { go(position - 1) }, onNext = { go(position + 1) },
                    onNextScene = {
                        scene = Scene.entries[(scene.ordinal + 1) % Scene.entries.size]
                        holding = scene.positionMs != null
                    },
                    onToggleHold = { holding = !holding && scene.positionMs != null },
                )
            }
        },
        onExit = onExit,
    )
}

@Composable
private fun ColumnScope.GuideItem(
    mode: SetupMode, position: Int, total: Int,
    item: com.reyespascal.picturehelper.checklist.ChecklistItem, current: Mark?,
    scene: Scene, holding: Boolean, focus: FocusRequester, actions: OverlayActions,
    onMark: (Mark) -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onNextScene: () -> Unit, onToggleHold: () -> Unit,
) {
    LaunchedEffect(item.id) { runCatching { focus.requestFocus() } }

    // Header: where you are, with a thin progress bar.
    Text("${mode.title}  ·  ${position + 1} of $total", fontSize = 13.sp, color = Muted)
    ProgressBar((position + 1f) / total, Modifier.padding(top = 6.dp, bottom = 10.dp))

    // The setting itself fades in each time you move on, so the change is felt, not just seen.
    AnimatedContent(
        targetState = item,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        label = "item",
    ) { it ->
        Column {
            Text(it.setting, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(it.value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (it.note.isNotBlank()) {
                Text(it.note, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
            }
            Text(
                it.source.label + (if (it.credit.isBlank()) "" else " (${it.credit})") + "  ·  Where: ${it.where}",
                fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    // Scene for before/after: one button cycles scenes, one holds or plays.
    Text(
        when {
            item.check -> "Quick check: usually already right. If the TV shows this, press Applied."
            scene.positionMs != null && holding ->
                "The picture is frozen on ${scene.shows}. To compare: open the TV menu, go to this setting, " +
                    "pick one choice with OK and look at the picture, then pick the other and look again. " +
                    "The picture changes behind the menu straight away."
            else -> "Playing. Choose a scene to freeze the picture while you compare."
        },
        fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButton("Scene: ${scene.label} ›", onNextScene, fontSize = 14.sp)
        if (scene.positionMs != null) TvButton(if (holding) "Play" else "Hold", onToggleHold, fontSize = 14.sp)
    }

    // Primary choice, focused first. Your earlier answer is ticked.
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fun label(m: Mark, text: String) = if (current == m) "✓ $text" else text
        TvButton(label(Mark.APPLIED, "Applied"), { onMark(Mark.APPLIED) }, fontSize = 16.sp, focusRequester = focus)
        TvButton(label(Mark.SKIPPED, "Skip"), { onMark(Mark.SKIPPED) }, fontSize = 16.sp)
        TvButton(label(Mark.NOT_FOUND, "Not found"), { onMark(Mark.NOT_FOUND) }, fontSize = 16.sp)
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButton("‹ Previous", onPrev, enabled = position > 0, fontSize = 13.sp)
        TvButton("Next ›", onNext, fontSize = 13.sp)
        TvButton("Hide", actions.hide, fontSize = 13.sp)
        TvButton("Details", actions.showDetails, fontSize = 13.sp)
    }
}

/**
 * Shown on the right while the TV menu is open. The guide can't take remote presses then, so instead of
 * one step it lists everything still to do in this mode: settings to change first, check-only and
 * "leave it" items after, dimmer. Marked items drop off; the current step is highlighted.
 */
@Composable
private fun ColumnScope.MenuCard(
    mode: SetupMode, items: List<ChecklistItem>, progress: Progress, current: ChecklistItem?,
) {
    val open = items.filter { progress.marks[it.id] == null }
    val (check, change) = open.partition { it.check }
    val rows = (change + check).take(MenuCardRows)

    Text("Change these in the TV menu  ·  ${mode.title}", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    if (mode == SetupMode.HDR) {
        Text("HDR is on if Picture Mode starts with \u201cHDR\u201d.", fontSize = 13.sp, color = Muted)
    }
    if (rows.isEmpty()) {
        Text("Everything is marked. Close the menu to see the summary.", fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
    }
    rows.forEach { row ->
        val dim = row.check
        val here = row.id == current?.id
        Column(
            Modifier.padding(top = 4.dp).fillMaxWidth()
                .background(if (here) Color(0x26FFD860) else Color.Transparent, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.setting, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 1, modifier = Modifier.weight(1f),
                    color = if (dim) Muted else MaterialTheme.colorScheme.onSurface)
                Text(row.value, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 1, fontWeight = FontWeight.Bold,
                    color = if (dim) Muted else MaterialTheme.colorScheme.primary)
            }
            // One line per path; "Picture ›" is implied because the card sits beside the Picture menu.
            Text(
                row.where.substringAfter("Picture › ", row.where),
                fontSize = 12.sp, lineHeight = 14.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (open.size > rows.size) {
        Text("+ ${open.size - rows.size} more after you mark these", fontSize = 12.sp, color = Muted,
            modifier = Modifier.padding(top = 6.dp))
    }
    Text("Up/Down move · OK open · Left/Right change a number · Back closes", fontSize = 12.sp, lineHeight = 14.sp,
        color = Muted, modifier = Modifier.padding(top = 8.dp))
    Text("Close the menu when done, then mark each one.", fontSize = 12.sp, lineHeight = 14.sp, color = Muted)
}

/** Rows that fit the 540 dp TV canvas without crowding. */
private const val MenuCardRows = 8

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(300), label = "progress")
    Box(modifier.fillMaxWidth().height(3.dp).background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(f).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun ColumnScope.Summary(
    mode: SetupMode, progress: Progress, total: Int, focus: FocusRequester,
    onRestart: () -> Unit, onFinish: () -> Unit,
) {
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val notFound = RtingsChecklist.forMode(mode).filter { progress.marks[it.id] == Mark.NOT_FOUND }
    Text("${mode.title} setup finished", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
    Text(
        "${progress.count(Mark.APPLIED)} applied · ${progress.count(Mark.SKIPPED)} skipped · " +
            "${progress.count(Mark.NOT_FOUND)} not found · ${total - progress.marks.size} not marked",
        fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
    )
    if (notFound.isNotEmpty()) {
        Text("Not found: ${notFound.joinToString { it.setting }}", fontSize = 14.sp, color = Muted)
    }
    Text("Your marks are saved. They're your own notes; the app didn't check the TV.", fontSize = 14.sp, color = Muted)
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButton("Finish", onFinish, focusRequester = focus)
        TvButton("Go through again", onRestart)
    }
}

val SetupMode.title: String get() = if (this == SetupMode.HDR) "HDR10" else "SDR"
