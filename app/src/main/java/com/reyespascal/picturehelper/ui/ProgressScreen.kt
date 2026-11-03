package com.reyespascal.picturehelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.reyespascal.picturehelper.checklist.ChecklistItem
import com.reyespascal.picturehelper.checklist.Mark
import com.reyespascal.picturehelper.checklist.Progress
import com.reyespascal.picturehelper.checklist.RtingsChecklist
import com.reyespascal.picturehelper.checklist.SetupMode

fun Progress.summary(total: Int): String =
    "${marks.size} of $total done"

@Composable
fun ProgressScreen(progress: (SetupMode) -> Progress, onReset: (SetupMode) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(SetupMode.HDR) }
    var confirmReset by remember { mutableStateOf(false) }
    val items = RtingsChecklist.forMode(mode)
    val p = progress(mode)
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Page("Saved progress") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupMode.entries.forEachIndexed { index, choice ->
                TvButton(choice.title, { mode = choice }, focusRequester = if (index == 0) firstFocus else null)
            }
            TvButton("Reset ${mode.title} checklist", { confirmReset = true })
        }
        Body(
            "${mode.title}: ${p.count(Mark.APPLIED)} applied · " +
                "${p.count(Mark.SKIPPED)} skipped · ${p.count(Mark.NOT_FOUND)} not found · " +
                "${items.size - p.marks.size} not done",
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(items, key = { it.id }) { item -> ProgressRow(item, p.marks[item.id]) }
        }
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset checklist?",
            message = "This clears Picture Helper's ${mode.title} checklist only. " +
                "Your TV's picture settings stay exactly as they are.",
            confirm = "Reset",
            onConfirm = { onReset(mode); confirmReset = false },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun ProgressRow(item: ChecklistItem, mark: Mark?) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (focused) Color(0xFF2A2F3A) else Color.Transparent, RoundedCornerShape(8.dp))
            .focusable(interactionSource = source)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(item.setting, fontSize = 18.sp, color = Color(0xFFF2F2F2), modifier = Modifier.width(300.dp))
        Text(item.value, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(240.dp))
        Text(mark?.label ?: "—", fontSize = 18.sp, color = Muted)
    }
}
