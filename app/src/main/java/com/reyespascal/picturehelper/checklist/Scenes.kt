package com.reyespascal.picturehelper.checklist

/**
 * Still frames from the 30.3 s setup clip (all three modes use the same frames), each chosen to
 * make a group of settings easy to judge. Times are the middle of a shot, so holding there shows one clean image.
 */
enum class Scene(val label: String, val positionMs: Long?, val shows: String) {
    FACE("Face", 25_875, "skin, whites and greys, fine hair"),
    SKY("Sky", 20_417, "bright whites and a smooth blue gradient"),
    SHADOWS("Shadows", 7_292, "deep blacks next to bright spots"),
    DETAIL("Detail", 22_958, "tiny houses right out to the edges"),
    COLOUR("Colour", 28_708, "very saturated cyan and magenta"),
    MOVING("Moving", null, "normal playback, for motion"),
}

object SceneFor {
    private val map = mapOf(
        "H3" to Scene.FACE, "S1" to Scene.FACE, "H2" to Scene.SKY, "S2" to Scene.SKY,
        "H9" to Scene.SHADOWS, "S5" to Scene.SHADOWS, "H7" to Scene.DETAIL, "S9" to Scene.DETAIL,
        "H6" to Scene.FACE, "S11" to Scene.FACE, "M1" to Scene.MOVING, "M2" to Scene.MOVING,
        "M3" to Scene.SKY, "M4" to Scene.DETAIL, "M5" to Scene.MOVING, "H8" to Scene.SHADOWS,
        "S17" to Scene.SHADOWS, "Q1" to Scene.SHADOWS, "Q2" to Scene.SKY, "Q4" to Scene.SHADOWS,
        "Q5" to Scene.COLOUR, "Q6" to Scene.FACE, "G2" to Scene.MOVING, "Q3" to Scene.MOVING,
        "Q7" to Scene.SHADOWS, "Q8" to Scene.COLOUR, "Q10" to Scene.MOVING, "Q9" to Scene.FACE,
        "Q12" to Scene.FACE,
        "D1" to Scene.FACE, "D2" to Scene.SKY, "D3" to Scene.SHADOWS, "D4" to Scene.DETAIL,
        "D5" to Scene.FACE,
    )

    fun item(id: String): Scene = map[id] ?: Scene.MOVING
    fun isMapped(id: String): Boolean = id in map
}
