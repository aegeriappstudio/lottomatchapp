package ch.lottomatchapp.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Findet die Host-Activity zu einem Context (auch wenn der Context ein Wrapper ist). */
fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
