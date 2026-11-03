package com.reyespascal.picturehelper.checklist

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the owner said about one setting. These are confirmations, not values read from the TV. */
enum class Mark(val label: String) { APPLIED("Applied"), SKIPPED("Skipped"), NOT_FOUND("Not found on my TV") }

data class Progress(val marks: Map<String, Mark>, val position: Int) {
    fun count(mark: Mark) = marks.values.count { it == mark }
}

/** Checklist progress, kept separately for the SDR and HDR workflows. */
class ProgressStore(private val store: DataStore<Preferences>) {

    private fun markKey(mode: SetupMode, id: String) = stringPreferencesKey("${mode.name}:mark:$id")
    private fun posKey(mode: SetupMode) = intPreferencesKey("${mode.name}:position")

    fun progress(mode: SetupMode): Flow<Progress> = store.data.map { prefs ->
        val marks = RtingsChecklist.forMode(mode).mapNotNull { item ->
            prefs[markKey(mode, item.id)]?.let { raw -> Mark.entries.firstOrNull { it.name == raw } }
                ?.let { item.id to it }
        }.toMap()
        Progress(marks, prefs[posKey(mode)] ?: 0)
    }

    suspend fun mark(mode: SetupMode, id: String, mark: Mark?) {
        store.edit { if (mark == null) it.remove(markKey(mode, id)) else it[markKey(mode, id)] = mark.name }
    }

    suspend fun setPosition(mode: SetupMode, position: Int) {
        store.edit { it[posKey(mode)] = position }
    }

    /** Clears this app's checklist for one mode. It does not touch any TV setting. */
    suspend fun reset(mode: SetupMode) {
        store.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith("${mode.name}:") }.forEach { prefs.remove(it) }
        }
    }
}
