package com.reyespascal.picturehelper.checklist

/**
 * Still frames from the 30.3 s setup clip (the HDR10 and SDR clips are the same frames), each chosen to
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
        "H3" to Scene.FACE, "H2" to Scene.SKY, "H4" to Scene.FACE, "H5" to Scene.MOVING,
        "S1" to Scene.FACE, "S2" to Scene.SKY, "S3" to Scene.SHADOWS, "S4" to Scene.SKY,
        "S5" to Scene.SHADOWS, "S6" to Scene.SHADOWS, "S7" to Scene.COLOUR, "S8" to Scene.FACE,
        "S9" to Scene.DETAIL, "S10" to Scene.DETAIL, "S11" to Scene.FACE, "S12" to Scene.DETAIL,
        "S13" to Scene.SKY, "S14" to Scene.DETAIL, "S15" to Scene.SHADOWS, "S16" to Scene.COLOUR,
        "M1" to Scene.MOVING, "M2" to Scene.MOVING, "G1" to Scene.MOVING, "G2" to Scene.MOVING,
    )

    fun item(id: String): Scene = map[id] ?: Scene.MOVING
    fun isMapped(id: String): Boolean = id in map
}
