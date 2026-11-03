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
            assertTrue(it.id, it.where.startsWith("Picture"))
            assertTrue(it.id, it.source == Source.OURS || it.credit.isNotBlank())
        }
    }

    @Test fun keyRtingsValuesArePresent() {
        fun value(id: String) = all.single { it.id == id }.value
        assertEquals("Theater Night", value("S1"))
        assertEquals("39", value("S5"))
        assertEquals("50", value("Q4"))
        assertEquals("50", value("Q5"))
        assertEquals("0", value("Q6"))
        assertEquals("Warm1", value("S11"))
        assertEquals("Auto", value("Q8"))
        assertEquals("Dolby Vision Dark", value("D1"))
        assertEquals("HDR Theater", value("H3"))
        assertEquals("Warm1", value("H6"))
        assertEquals("50", value("H9"))
        assertEquals("0", value("H7"))
    }

    @Test fun hdrListDoesNotCarrySdrNumbers() {
        val hdr = RtingsChecklist.forMode(SetupMode.HDR)
        assertFalse(hdr.any { it.applies == Applies.SDR })
        assertFalse(hdr.any { it.setting == "Contrast" && it.value == "39" })
        assertTrue(hdr.any { it.id == "H2" && it.check })
    }

    @Test fun dvListDoesNotCarrySdrOrHdrItems() {
        val dv = RtingsChecklist.forMode(SetupMode.DV)
        assertFalse(dv.any { it.value == "39" })
        assertFalse(dv.any { it.applies == Applies.SDR || it.applies == Applies.HDR })
    }

    @Test fun pictureModeComesFirstAndChecksComeLast() {
        SetupMode.entries.forEach { mode ->
            val list = RtingsChecklist.forMode(mode)
            assertEquals("Picture Mode", list.first().setting)
            val firstCheck = list.indexOfFirst { it.check }
            assertTrue(list.drop(firstCheck).all { it.check })
        }
    }

    @Test fun everyMenuSettingIsCoveredOncePerMode() {
        val menu = listOf(
            "Picture Mode", "Backlight Level", "Local Dimming", "Automatic Light Sensor", "Contrast", "Brightness",
            "Color", "Tint", "Sharpness", "Apply Picture Settings", "Smart Scene", "Color Temperature",
            "Motion Enhancement", "Motion Clearness", "Noise Reduction", "Digital Noise Reduction",
            "Active Contrast", "FILMMAKER MODE Auto Detection", "Color Space", "Instant Game Response",
            "Low Blue Light", "Gamma", "Calibration Settings",
        )
        SetupMode.entries.forEach { mode ->
            val names = RtingsChecklist.forMode(mode).map { it.setting }
            val expected = if (mode == SetupMode.DV)
                menu - setOf("Motion Clearness", "Low Blue Light", "Gamma") else menu
            assertEquals(mode.name, expected.sorted(), names.sorted())
        }
        assertFalse(all.any { it.setting == "Picture Size" || it.setting == "Overscan" })
    }

    @Test fun sharedItemsAppearInBothLists() {
        val sdr = RtingsChecklist.forMode(SetupMode.SDR).map { it.id }
        val hdr = RtingsChecklist.forMode(SetupMode.HDR).map { it.id }
        val dv = RtingsChecklist.forMode(SetupMode.DV).map { it.id }
        all.filter { it.applies == Applies.BOTH }.forEach {
            assertTrue(it.id in sdr && it.id in hdr)
            assertFalse(it.id in dv)
        }
        all.filter { it.applies == Applies.ALL }.forEach {
            assertTrue(it.id in sdr && it.id in hdr && it.id in dv)
        }
    }

    @Test fun unitSpecificCalibrationValuesAreExcluded() {
        val banned = listOf("white balance", "2 point", "20 point", "gain", "offset", "hue", "saturation", "luminance")
        all.forEach { item ->
            val text = "${item.setting} ${item.value} ${item.note}".lowercase()
            banned.forEach {
                assertFalse("${item.id} mentions '$it'", Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(text))
            }
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
