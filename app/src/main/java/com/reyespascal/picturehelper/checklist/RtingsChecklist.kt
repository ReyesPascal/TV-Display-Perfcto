package com.reyespascal.picturehelper.checklist

/** Which setup workflow an item belongs to. BOTH items appear in each list, with progress saved per list. */
enum class Applies { SDR, HDR, BOTH }

/** How RTINGS presents the value. Shown to the owner so preferences aren't mistaken for rules. */
enum class Kind(val label: String) {
    REC("RTINGS recommends"),
    SHOWN("Shown in RTINGS' settings photos"),
    DEFAULT("Leave as it is"),
    AUTO("The TV changes this itself"),
    PREF("Your preference"),
}

data class ChecklistItem(
    val id: String,
    val setting: String,
    val value: String,
    val applies: Applies,
    val kind: Kind,
    /** One or two short sentences, our own wording. */
    val note: String,
    /** Where to find it in the Hisense menu, as far as we know. */
    val where: String,
    /** False when the menu path hasn't been seen on the owner's TV yet. */
    val whereConfirmed: Boolean,
    /** Section of the RTINGS article this comes from. */
    val section: String,
)

enum class SetupMode { SDR, HDR }

/**
 * Summary of RTINGS' "Hisense U6H Calibration Settings" (page last updated 2023-10-24, checked 2026-11-03).
 * Values are RTINGS'; wording is ours. Deliberately excluded: RTINGS' white-balance and colorspace
 * calibration numbers (specific to their own TV), HDMI, gaming, eARC and other non-picture settings.
 */
object RtingsChecklist {
    const val SOURCE_TITLE = "Hisense U6H Calibration Settings — RTINGS.com"
    const val SOURCE_URL = "https://www.rtings.com/tv/reviews/hisense/u6-u6h/settings"
    const val SOURCE_UPDATED = "2023-10-24"
    const val CHECKED = "2026-11-03"

    private const val MENU = "Square two-line button › Picture"

    val all: List<ChecklistItem> = listOf(
        // ---- HDR ----
        ChecklistItem(
            "H1", "HDR is active", "HDR picture modes are listed", Applies.HDR, Kind.REC,
            "HDR settings only appear while HDR is playing. If Picture Mode lists modes starting with \"HDR\", you're in the right place.",
            "$MENU › Picture Mode", true, "HDR",
        ),
        ChecklistItem(
            "H3", "Picture Mode", "HDR Theater", Applies.HDR, Kind.REC,
            "RTINGS used this mode for HDR.",
            "$MENU › Picture Mode", true, "HDR",
        ),
        ChecklistItem(
            "H2", "Backlight Level", "Max (check only)", Applies.HDR, Kind.AUTO,
            "In HDR the TV raises the backlight to Max by itself. Just check it.",
            "$MENU › Backlight › Backlight Level", true, "HDR",
        ),
        ChecklistItem(
            "H4", "Color Temperature", "Low", Applies.HDR, Kind.REC,
            "Low is the closest to the standard white point.",
            "$MENU › Advanced Settings › Color Temperature", false, "HDR",
        ),
        ChecklistItem(
            "H5", "Everything else in HDR", "Leave as it is", Applies.HDR, Kind.DEFAULT,
            "RTINGS gives no other HDR values. Don't copy the SDR numbers (like Contrast 39) into HDR.",
            MENU, true, "HDR",
        ),
        // ---- SDR ----
        ChecklistItem(
            "S1", "Picture Mode", "Theater Night", Applies.SDR, Kind.REC,
            "The most accurate preset with the most adjustments. Theater Day is a bit brighter but less accurate.",
            "$MENU › Picture Mode", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S2", "Backlight Level", "55, then to taste", Applies.SDR, Kind.PREF,
            "55 matched RTINGS' dim test room. Raise or lower it for your room; it doesn't affect accuracy.",
            "$MENU › Backlight › Backlight Level", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S3", "Local Dimming", "High", Applies.SDR, Kind.SHOWN,
            "The value on RTINGS' Backlight screen.",
            "$MENU › Backlight › Local Dimming", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S4", "Automatic Light Sensor", "Off", Applies.SDR, Kind.SHOWN,
            "Off keeps brightness steady instead of following room light.",
            "$MENU › Backlight › Automatic Light Sensor", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S5", "Contrast", "39", Applies.SDR, Kind.REC, "",
            "$MENU › Contrast", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S6", "Brightness", "50", Applies.SDR, Kind.REC,
            "Not the same as Backlight. RTINGS suggests leaving it at 50.",
            "$MENU › Brightness", true, "Basic SDR Picture Settings",
        ),
        ChecklistItem("S7", "Color", "50", Applies.SDR, Kind.REC, "", "$MENU › Color", true, "Basic SDR Picture Settings"),
        ChecklistItem("S8", "Tint", "0", Applies.SDR, Kind.REC, "", "$MENU › Tint", true, "Basic SDR Picture Settings"),
        ChecklistItem("S9", "Sharpness", "0", Applies.SDR, Kind.SHOWN, "", "$MENU › Sharpness", true, "Basic SDR Picture Settings"),
        ChecklistItem(
            "S10", "Picture Size", "Wide", Applies.SDR, Kind.SHOWN, "",
            "$MENU › Picture Size", false, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "S11", "Color Temperature", "Low", Applies.SDR, Kind.REC,
            "Low is the closest to the standard white point.",
            "$MENU › Advanced Settings › Color Temperature", false, "Advanced SDR Picture Settings",
        ),
        ChecklistItem(
            "S12", "Overscan", "Off", Applies.SDR, Kind.SHOWN, "",
            "$MENU › Advanced Settings › Overscan", false, "Advanced SDR Picture Settings",
        ),
        ChecklistItem(
            "S13", "Noise Reduction", "Off", Applies.SDR, Kind.REC,
            "Can smear fine detail in good-quality video.",
            "$MENU › Advanced Settings › Noise Reduction", false, "Advanced SDR Picture Settings",
        ),
        ChecklistItem(
            "S14", "Digital Noise Reduction", "Off", Applies.SDR, Kind.REC,
            "Same reason as Noise Reduction.",
            "$MENU › Advanced Settings › Digital Noise Reduction", false, "Advanced SDR Picture Settings",
        ),
        ChecklistItem(
            "S15", "Active Contrast", "Off", Applies.SDR, Kind.REC,
            "Same reason: it can cost fine detail.",
            "$MENU › Advanced Settings › Active Contrast", false, "Advanced SDR Picture Settings",
        ),
        ChecklistItem(
            "S16", "Color Space", "Auto", Applies.SDR, Kind.REC,
            "Stops SDR colours looking oversaturated.",
            "$MENU › Advanced Settings › Color Space", false, "Advanced SDR Picture Settings",
        ),
        // ---- Both ----
        ChecklistItem(
            "M1", "Motion Enhancement", "Off (or Custom to taste)", Applies.BOTH, Kind.PREF,
            "RTINGS tested with it off. For smoother motion pick Custom and set Judder and Blur Reduction to taste.",
            "$MENU › Advanced Settings › Motion Enhancement", false, "Motion Settings",
        ),
        ChecklistItem(
            "M2", "Motion Clearness", "Off", Applies.BOTH, Kind.SHOWN,
            "Off as RTINGS tested. Your choice.",
            "$MENU › Advanced Settings › Motion Clearness", false, "Motion Settings",
        ),
        ChecklistItem(
            "G2", "Apply Picture Settings", "Your choice", Applies.BOTH, Kind.SHOWN,
            "RTINGS' photo shows \"All Sources\". This decides whether your changes also reach the HDMI inputs.",
            "$MENU › Apply Picture Settings", false, "Basic SDR Picture Settings",
        ),
        ChecklistItem(
            "G1", "Energy Saver", "Your choice", Applies.BOTH, Kind.PREF,
            "RTINGS turned it off only so the TV stayed on during testing.",
            "Settings › System › Power & Energy (if present)", false, "Power and Energy Saving",
        ),
    )

    fun forMode(mode: SetupMode): List<ChecklistItem> = all.filter {
        it.applies == Applies.BOTH || it.applies.name == mode.name
    }

    /** RTINGS' informational note that isn't a setting to change. Shown in About. */
    const val JUDDER_NOTE =
        "RTINGS says the TV removes judder from native 24p signals on its own. Nothing to set."
}
