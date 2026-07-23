package com.bqdiptv.tv.update

import android.content.Context

/** Tiny SharedPreferences-backed flag so the background worker can hand a
 * result to the (possibly not-yet-running) UI without any IPC machinery. */
object UpdateFlags {
    private const val PREFS = "bqdiptv_update_flags"

    fun markAvailable(context: Context, info: UpdateInfo) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("available", true)
            .putString("versionName", info.versionName)
            .putInt("versionCode", info.versionCode)
            .putString("downloadUrl", info.downloadUrl)
            .putString("notes", info.releaseNotes)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun pending(context: Context): UpdateInfo? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("available", false)) return null
        return UpdateInfo(
            versionName = p.getString("versionName", "") ?: "",
            versionCode = p.getInt("versionCode", 0),
            downloadUrl = p.getString("downloadUrl", "") ?: "",
            releaseNotes = p.getString("notes", "") ?: ""
        )
    }
}
