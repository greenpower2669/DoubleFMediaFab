package com.doublefmediafab

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val MAX_BYTES = 1024 * 1024
    private const val TAG = "DoubleFMediaFab"
    @Synchronized
    fun write(context: Context, event: String, detail: String = "") {
        val row = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} [$event] $detail\n"
        Log.i(TAG, row.trim())
        try {
            val file = File(context.filesDir, "diagnostic.log")
            if (file.exists() && file.length() > MAX_BYTES) {
                file.writeText(file.readText().takeLast(MAX_BYTES / 2))
            }
            file.appendText(row)
        } catch (e: Exception) { Log.e(TAG, "Log write failure", e) }
    }

    fun contents(context: Context): String =
        try { File(context.filesDir, "diagnostic.log").takeIf { it.exists() }?.readText() ?: "Aucun journal" }
        catch (e: Exception) { "Lecture impossible : ${e.message}" }
}
