package com.reyespascal.picturehelper

import android.util.Log

/** All app logging goes through one tag so `adb logcat -s PictureHelper` shows everything. */
object PhLog {
    const val TAG = "PictureHelper"
    fun i(msg: String) { Log.i(TAG, msg) }
    fun w(msg: String, t: Throwable? = null) { Log.w(TAG, msg, t) }
}
