package com.reyespascal.picturehelper.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtingsChecklistTest {

    private val all = RtingsChecklist.all

    @Test fun idsAreUnique() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun everyItemHasNameValueWhereAndSection() {
        all.forEach {
            assertTrue(it.id, it.setting.isNotBlank() && it.value.isNotBlank())
            assertTrue(it.id, it.where.isNotBlank() && it.section.isNotBlank())
        }
    }

    @Test fun keyRtingsValuesArePresent() {
        fun value(id: String) = all.single { it.id == id }.value
        assertEquals("Theater Night", value("S1"))
        assertEquals("39", value("S5"))
        assertEquals("50", value("S6"))
        assertEquals("50", value("S7"))
        assertEquals("0", value("S8"))
        assertEquals("Low", value("S11"))
        assertEquals("Auto", value("S16"))
        assertEquals("HDR Theater", value("H3"))
        assertEquals("Warm1 or Warm2", value("H4"))
    }

    @Test fun hdrListDoesNotCarrySdrNumbers() {
        val hdr = RtingsChecklist.forMode(SetupMode.HDR)
        assertFalse(hdr.any { it.applies == Applies.SDR })
        assertFalse(hdr.any { it.setting == "Contrast" })
        assertTrue(hdr.any { it.id == "H2" && it.kind == Kind.AUTO })
    }

    @Test fun sharedItemsAppearInBothLists() {
        val sdr = RtingsChecklist.forMode(SetupMode.SDR).map { it.id }
        val hdr = RtingsChecklist.forMode(SetupMode.HDR).map { it.id }
        all.filter { it.applies == Applies.BOTH }.forEach {
            assertTrue(it.id in sdr && it.id in hdr)
        }
    }

    @Test fun unitSpecificCalibrationValuesAreExcluded() {
        val banned = listOf("white balance", "2 point", "20 point", "gain", "offset", "hue", "saturation", "luminance")
        all.forEach { item ->
            val text = "${item.setting} ${item.value} ${item.note}".lowercase()
            banned.forEach { assertFalse("${item.id} mentions '$it'", text.contains(it)) }
        }
    }
}

class ScenesTest {
    @Test fun everyItemHasAScene() {
        // Unknown ids would silently fall back to MOVING; every item must be mapped on purpose.
        RtingsChecklist.all.forEach { assertTrue(it.id, SceneFor.isMapped(it.id)) }
        val mapped = RtingsChecklist.all.count { SceneFor.item(it.id) != Scene.MOVING }
        org.junit.Assert.assertTrue("most settings should have a still scene", mapped >= 15)
    }

    @Test fun sceneTimesAreInsideTheClip() {
        val clipMs = 30_333L // 728 frames at 24 fps
        Scene.entries.mapNotNull { it.positionMs }.forEach {
            org.junit.Assert.assertTrue("$it ms", it in 0 until clipMs)
        }
    }
}
