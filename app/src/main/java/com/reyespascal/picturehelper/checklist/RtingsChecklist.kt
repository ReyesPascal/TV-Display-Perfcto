package com.reyespascal.picturehelper.checklist

enum class SetupMode { SDR, HDR, DV }

/** Which setup workflows an item belongs to; progress is saved per mode. */
enum class Applies(val modes: Set<SetupMode>) {
    SDR(setOf(SetupMode.SDR)),
    HDR(setOf(SetupMode.HDR)),
    DV(setOf(SetupMode.DV)),
    /** SDR and HDR10 only: greyed out or different in Dolby Vision. */
    BOTH(setOf(SetupMode.SDR, SetupMode.HDR)),
    ALL(SetupMode.entries.toSet()),
}

/** Who the value comes from. Shown on every step so suggestions aren't mistaken for measurements. */
enum class Source(val label: String) {
    RTINGS("RTINGS"),
    REVIEWERS("Other reviewers"),
    OURS("Our suggestion"),
    YOURS("Your choice"),
}

data class ChecklistItem(
    val id: String,
    val setting: String,
    val value: String,
    val applies: Applies,
    val source: Source,
    /** Which review(s) back the value, in a few words; empty for our own suggestions. */
    val credit: String,
    /** True when the TV is usually already right: a quick look, listed after the real changes. */
    val check: Boolean,
    /** One or two short sentences, our own wording. */
    val note: String,
    /** Path in this TV's Picture menu (read from the owner's TV 2026-11-03). */
    val where: String,
)

/**
 * Every setting in the Hisense 65U6H Picture menu (firmware read 2026-11-03), for SDR, HDR10 and Dolby Vision.
 * Motion Clearness, Low Blue Light and Gamma are greyed out in Dolby Vision, so they aren't in that list.
 * Values come from RTINGS' U6H settings page, backed or filled in by other reviews; wording is ours.
 * Deliberately excluded: white-balance / colour calibration numbers (specific to one measured unit).
 * Order: Picture Mode first (the TV keeps settings per mode), then menu order; quick checks last.
 */
object RtingsChecklist {
    const val SOURCE_TITLE = "Hisense U6H Calibration Settings — RTINGS.com"
    const val SOURCE_URL = "https://www.rtings.com/tv/reviews/hisense/u6-u6h/settings"
    const val SOURCE_UPDATED = "2023-10-24"
    const val CHECKED = "2026-11-03"

    /** Other reviews used to confirm or fill in values. Shown in About. */
    val OTHER_SOURCES = listOf(
        "RTINGS — Hisense U8H and U6K settings pages",
        "Reviewed — Hisense U6H review",
        "Digital Trends — Hisense U8K settings",
        "PCMag and T3 — Hisense U-series reviews",
        "Sound & Vision — Hisense 65H8G review (gamma)",
        "Dolby Vision: RTINGS U6N/U7N (Dolby Vision Dark most accurate), AVForums U7K/U8K, Dolby professional support, Spears & Munsil setup guide",
    )

    /** Bump when ids or order change, so a saved position from an older list isn't reused. */
    const val LIST_VERSION = 3

    private const val P = "Picture"
    private const val ADV = "$P › Advanced Settings"
    private const val BL = "$P › Backlight"
    private const val CAL = "$P › Calibration Settings"

    val all: List<ChecklistItem> = listOf(
        // ---- Changes, menu order ----
        ChecklistItem(
            "H3", "Picture Mode", "HDR Theater", Applies.HDR, Source.RTINGS, "RTINGS U6H", false,
            "Set this first: the TV remembers the other settings separately for each mode.",
            "$P › Picture Mode",
        ),
        ChecklistItem(
            "S1", "Picture Mode", "Theater Night", Applies.SDR, Source.RTINGS, "RTINGS U6H", false,
            "The most accurate preset. Set it first: the TV remembers the other settings per mode.",
            "$P › Picture Mode",
        ),
        ChecklistItem(
            "D1", "Picture Mode", "Dolby Vision Dark", Applies.DV, Source.REVIEWERS, "RTINGS U6N/U7N · Dolby", false,
            "The most accurate Dolby Vision mode on Hisense's newer sets. In a bright room use Dolby Vision IQ instead: it brightens the picture to match room light. If Dark looks too dim to you, IQ is fine too.",
            "$P › Picture Mode",
        ),
        ChecklistItem(
            "D2", "Backlight Level", "100", Applies.DV, Source.REVIEWERS, "Spears & Munsil", true,
            "Dolby Vision expects full backlight. Just check it.",
            "$BL › Backlight Level",
        ),
        ChecklistItem(
            "D3", "Contrast", "50", Applies.DV, Source.REVIEWERS, "Spears & Munsil", true,
            "Dolby Vision's tone mapping assumes the default. Don't copy the SDR value (39).",
            "$P › Contrast",
        ),
        ChecklistItem(
            "D4", "Sharpness", "0", Applies.DV, Source.RTINGS, "RTINGS U6H (HDR, SDR)", false,
            "0 adds no artificial edges.",
            "$P › Sharpness",
        ),
        ChecklistItem(
            "D5", "Color Temperature", "Leave the mode's own", Applies.DV, Source.REVIEWERS, "Dolby", true,
            "Dolby Vision modes are set up for standard white already. Just check it shows a Warm choice; nobody has measured Warm1 against Warm2 on this TV in Dolby Vision.",
            "$ADV › Color Temperature",
        ),
        ChecklistItem(
            "H2", "Backlight Level", "Max", Applies.HDR, Source.RTINGS, "RTINGS U6H", true,
            "In HDR the TV sets this to Max itself. Just check it.",
            "$BL › Backlight Level",
        ),
        ChecklistItem(
            "S2", "Backlight Level", "55 dark room · 80–100 bright", Applies.SDR, Source.RTINGS,
            "RTINGS U6H (55 in a dim room)", false,
            "Pick for your room; it changes brightness, not accuracy.",
            "$BL › Backlight Level",
        ),
        ChecklistItem(
            "H9", "Contrast", "50", Applies.HDR, Source.RTINGS, "RTINGS U6H", false,
            "Don't copy the SDR value (39) into HDR.",
            "$P › Contrast",
        ),
        ChecklistItem(
            "S5", "Contrast", "39", Applies.SDR, Source.RTINGS, "RTINGS U6H", false, "",
            "$P › Contrast",
        ),
        ChecklistItem(
            "H7", "Sharpness", "0", Applies.HDR, Source.RTINGS, "RTINGS U6H settings photo", false,
            "0 adds no artificial edges. If the picture looks soft to you, 7 is the factory value.",
            "$P › Sharpness",
        ),
        ChecklistItem(
            "S9", "Sharpness", "0", Applies.SDR, Source.RTINGS, "RTINGS U6H settings photo", false,
            "0 adds no artificial edges.",
            "$P › Sharpness",
        ),
        ChecklistItem(
            "H6", "Color Temperature", "Warm1", Applies.HDR, Source.RTINGS,
            "RTINGS U8H · Digital Trends U8K", false,
            "RTINGS' U6H page says \"Low\"; this TV's closest match is Warm1, the one RTINGS measured nearest standard white.",
            "$ADV › Color Temperature",
        ),
        ChecklistItem(
            "S11", "Color Temperature", "Warm1", Applies.SDR, Source.RTINGS,
            "RTINGS U8H · Digital Trends U8K", false,
            "RTINGS' U6H page says \"Low\"; this TV's closest match is Warm1, the one RTINGS measured nearest standard white.",
            "$ADV › Color Temperature",
        ),
        ChecklistItem(
            "M1", "Motion Enhancement", "Off", Applies.ALL, Source.RTINGS, "RTINGS · Reviewed", false,
            "Keeps films looking like films. If camera pans stutter, try Film.",
            "$ADV › Motion Enhancement",
        ),
        ChecklistItem(
            "M2", "Motion Clearness", "Off", Applies.BOTH, Source.REVIEWERS, "Reviewed", false,
            "It flickers the backlight and dims the picture.",
            "$ADV › Motion Clearness",
        ),
        ChecklistItem(
            "M3", "Noise Reduction", "Off", Applies.ALL, Source.RTINGS, "RTINGS · Reviewed", false,
            "Smears fine detail in good video. Low only for a bad, noisy stream.",
            "$ADV › Noise Reduction",
        ),
        ChecklistItem(
            "M4", "Digital Noise Reduction", "Off", Applies.ALL, Source.RTINGS, "RTINGS · Reviewed", false,
            "Same reason as Noise Reduction. Low only for a blocky stream.",
            "$ADV › Digital Noise Reduction",
        ),
        ChecklistItem(
            "M5", "FILMMAKER MODE Auto Detection", "Off", Applies.ALL, Source.OURS, "", false,
            "When On, the TV can switch to FILMMAKER MODE by itself and replace the mode you just set up.",
            "$ADV › FILMMAKER MODE Auto Detection",
        ),
        ChecklistItem(
            "H8", "Gamma", "ST2084", Applies.HDR, Source.RTINGS, "RTINGS U6H", true,
            "HDR uses its own curve; the TV usually locks it. Just check it.",
            "$CAL › Gamma",
        ),
        ChecklistItem(
            "S17", "Gamma", "2.2 lit room · BT1886 dark", Applies.SDR, Source.RTINGS,
            "RTINGS U6H · Sound & Vision", false,
            "BT1886 shows more shadow detail in a dark room; 2.2 holds up better with lights on.",
            "$CAL › Gamma",
        ),
        // ---- Quick checks: usually already right ----
        ChecklistItem(
            "Q1", "Local Dimming", "High", Applies.ALL, Source.RTINGS, "RTINGS U6H settings photo", true,
            "Deepest blacks.", "$BL › Local Dimming",
        ),
        ChecklistItem(
            "Q2", "Automatic Light Sensor", "Off", Applies.ALL, Source.RTINGS, "RTINGS U6H settings photo", true,
            "Keeps brightness steady instead of following room light. (Dolby Vision IQ uses the sensor on purpose.)", "$BL › Automatic Light Sensor",
        ),
        ChecklistItem(
            "Q4", "Brightness", "50", Applies.ALL, Source.RTINGS, "RTINGS U6H", true,
            "Not the same as Backlight: this sets the black level.", "$P › Brightness",
        ),
        ChecklistItem(
            "Q5", "Color", "50", Applies.ALL, Source.RTINGS, "RTINGS U6H", true, "", "$P › Color",
        ),
        ChecklistItem(
            "Q6", "Tint", "0", Applies.ALL, Source.RTINGS, "RTINGS U6H", true, "", "$P › Tint",
        ),
        ChecklistItem(
            "G2", "Apply Picture Settings", "Your choice", Applies.ALL, Source.YOURS, "RTINGS photo: All Sources", true,
            "Decides whether your changes also reach the HDMI inputs.", "$P › Apply Picture Settings",
        ),
        ChecklistItem(
            "Q3", "Smart Scene", "Off", Applies.ALL, Source.OURS, "", true,
            "When On, the TV adjusts the picture for each scene on its own.", "$P › Smart Scene",
        ),
        ChecklistItem(
            "Q7", "Active Contrast", "Off", Applies.ALL, Source.RTINGS, "RTINGS U6H", true,
            "Can crush dark detail.", "$ADV › Active Contrast",
        ),
        ChecklistItem(
            "Q8", "Color Space", "Auto", Applies.ALL, Source.RTINGS, "RTINGS U6H", true,
            "Stops colours looking oversaturated.", "$ADV › Color Space",
        ),
        ChecklistItem(
            "Q10", "Instant Game Response", "On", Applies.ALL, Source.OURS, "", true,
            "Only acts when a game console asks for it; films are unaffected.", "$ADV › Instant Game Response",
        ),
        ChecklistItem(
            "Q9", "Low Blue Light", "Off", Applies.BOTH, Source.OURS, "", true,
            "On tints the whole picture yellow.", "$ADV › Low Blue Light",
        ),
        ChecklistItem(
            "Q12", "Calibration Settings", "Leave at 0", Applies.ALL, Source.OURS, "", true,
            "Color Tuner and the other adjustments here fine-tune one measured TV. Leave them at 0.",
            "$CAL",
        ),
    )

    /** Changes first, then quick checks; each part keeps menu order. */
    fun forMode(mode: SetupMode): List<ChecklistItem> = all.filter { mode in it.applies.modes }.sortedBy { it.check }

    /** RTINGS' informational note that isn't a setting to change. Shown in About. */
    const val JUDDER_NOTE =
        "RTINGS says the TV removes judder from native 24p signals on its own. Nothing to set."
}
