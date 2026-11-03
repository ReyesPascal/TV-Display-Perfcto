package com.reyespascal.picturehelper.checklist

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun newStore(name: String = "progress") = ProgressStore(
        PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("$name.preferences_pb") },
    )

    @After fun tearDown() = scope.cancel()

    @Test fun marksAreSavedPerMode() = runTest {
        val store = newStore()
        store.mark(SetupMode.HDR, "M1", Mark.APPLIED)
        store.mark(SetupMode.SDR, "M1", Mark.SKIPPED)
        store.mark(SetupMode.DV, "M1", Mark.NOT_FOUND)
        assertEquals(Mark.APPLIED, store.progress(SetupMode.HDR).first().marks["M1"])
        assertEquals(Mark.SKIPPED, store.progress(SetupMode.SDR).first().marks["M1"])
        assertEquals(Mark.NOT_FOUND, store.progress(SetupMode.DV).first().marks["M1"])
    }

    @Test fun resetClearsOnlyThatMode() = runTest {
        val store = newStore()
        store.mark(SetupMode.HDR, "H3", Mark.APPLIED)
        store.setPosition(SetupMode.HDR, 2)
        store.mark(SetupMode.SDR, "S5", Mark.NOT_FOUND)
        store.mark(SetupMode.DV, "D1", Mark.APPLIED)
        store.setPosition(SetupMode.DV, 3)
        store.reset(SetupMode.HDR)
        val hdr = store.progress(SetupMode.HDR).first()
        assertNull(hdr.marks["H3"])
        assertEquals(0, hdr.position)
        assertEquals(Mark.NOT_FOUND, store.progress(SetupMode.SDR).first().marks["S5"])
        val dv = store.progress(SetupMode.DV).first()
        assertEquals(Mark.APPLIED, dv.marks["D1"])
        assertEquals(3, dv.position)
    }

    @Test fun clearingAMarkAndCounting() = runTest {
        val store = newStore()
        store.mark(SetupMode.SDR, "S5", Mark.APPLIED)
        store.mark(SetupMode.SDR, "Q4", Mark.APPLIED)
        store.mark(SetupMode.SDR, "Q5", Mark.SKIPPED)
        store.mark(SetupMode.SDR, "Q4", null)
        val p = store.progress(SetupMode.SDR).first()
        assertEquals(1, p.count(Mark.APPLIED))
        assertEquals(1, p.count(Mark.SKIPPED))
    }

    @Test fun positionFromAnOlderListResumesAtFirstUnmarked() = runTest {
        val store = newStore()
        // Marks saved without a list version (the old list): position must not be trusted.
        store.mark(SetupMode.HDR, "H3", Mark.APPLIED)
        assertEquals(1, store.progress(SetupMode.HDR).first().position)
        store.setPosition(SetupMode.HDR, 5)
        assertEquals(5, store.progress(SetupMode.HDR).first().position)
    }
}
