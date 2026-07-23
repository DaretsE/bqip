package com.bqdiptv.tv

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Installs a global crash handler that writes the full stack trace to a
 * file before letting the app die as normal. There's no reliable ADB/logcat
 * access assumed on a TV box, so this is the practical way to actually see
 * what crashed: AppViewModel reads this file on the next launch and shows
 * it on-screen (see CrashLogOverlay).
 */
class BqdiptvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "last_crash.txt").writeText(
                    "Thread: ${thread.name}\n\n${sw}"
                )
            } catch (_: Throwable) {
                // Never let crash-logging itself throw and mask the real crash.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
