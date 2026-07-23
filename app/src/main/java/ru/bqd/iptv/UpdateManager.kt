package ru.bqd.iptv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

object UpdateManager {

    data class Info(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String,
        val prevCode: Int,
        val prevName: String,
        val prevUrl: String
    )

    private val main = Handler(Looper.getMainLooper())

    fun currentCode(ctx: Context): Int = try {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode
    } catch (_: Exception) { 0 }

    fun currentName(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }

    fun fetchInfo(repo: String): Info? {
        return try {
            val url = repo.trim().trimEnd('/') + "/releases/latest/download/version.json"
            val txt = Net.downloadText(url)
            val o = JSONObject(txt)
            val prev = o.optJSONObject("previous")
            Info(
                versionCode = o.optInt("versionCode", 0),
                versionName = o.optString("versionName", "?"),
                apkUrl = o.optString("apkUrl", ""),
                notes = o.optString("notes", ""),
                prevCode = prev?.optInt("versionCode", 0) ?: 0,
                prevName = prev?.optString("versionName", "") ?: "",
                prevUrl = prev?.optString("apkUrl", "") ?: ""
            )
        } catch (_: Exception) { null }
    }

    fun checkAsync(ctx: Context, repo: String, onResult: (Info?, Int) -> Unit) {
        Thread {
            val info = fetchInfo(repo)
            val cur = currentCode(ctx)
            main.post { onResult(info, cur) }
        }.start()
    }

    fun canInstall(act: Activity): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || act.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(act: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return try {
            act.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + act.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            try {
                act.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) { false }
        }
    }

    private fun updateFile(act: Context): File = File(File(act.filesDir, "update"), "BQDiptv.apk")
    private fun stampFile(act: Context): File = File(File(act.filesDir, "update"), "version.txt")

    private fun writeStamp(act: Context, versionCode: Int) {
        try { stampFile(act).writeText(versionCode.toString()) } catch (_: Exception) { }
    }

    private fun readStamp(act: Context): Int = try {
        stampFile(act).readText().trim().toInt()
    } catch (_: Exception) { 0 }

    fun hasDownloaded(act: Context, versionCode: Int): Boolean {
        val f = updateFile(act)
        if (!f.exists() || f.length() == 0L || !isApk(f)) return false
        return versionCode > 0 && readStamp(act) == versionCode
    }

    fun clearDownloaded(act: Context) {
        try { updateFile(act).delete(); stampFile(act).delete() } catch (_: Exception) { }
    }

    private fun isApk(f: File): Boolean = try {
        f.inputStream().use { ins ->
            val b = ByteArray(2)
            ins.read(b) == 2 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte()
        }
    } catch (_: Exception) { false }

    fun downloadAndInstall(
        act: Activity,
        apkUrl: String,
        versionCode: Int,
        onError: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onNeedPermission: () -> Unit,
        onDone: () -> Unit
    ) {
        if (apkUrl.isEmpty()) { onError("Нет ссылки на файл обновления"); onDone(); return }

        if (!canInstall(act)) {
            onDone()
            onNeedPermission()
            return
        }

        Thread {
            try {
                val dir = File(act.filesDir, "update").apply { mkdirs() }
                val apk = File(dir, "BQDiptv.apk")
                if (apk.exists()) apk.delete()

                var url = apkUrl
                var conn: java.net.HttpURLConnection? = null
                var redirects = 0
                while (true) {
                    conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection)
                    conn.connectTimeout = 20000
                    conn.readTimeout = 60000
                    conn.instanceFollowRedirects = false
                    conn.setRequestProperty("User-Agent", "BQDiptv")
                    conn.setRequestProperty("Accept", "*/*")
                    val code = conn.responseCode
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        val loc = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (loc.isNullOrEmpty() || ++redirects > 5) throw Exception("Слишком много переадресаций")
                        url = java.net.URL(java.net.URL(url), loc).toString()
                        continue
                    }
                    if (code !in 200..299) throw Exception("Сервер ответил $code")
                    break
                }

                val c = conn ?: throw Exception("Нет соединения")
                val total = c.contentLength
                c.inputStream.use { ins ->
                    apk.outputStream().use { out ->
                        val buf = ByteArray(1 shl 16); var read: Int; var done = 0L; var lastPct = -1
                        while (ins.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read); done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != lastPct) { lastPct = pct; main.post { onProgress(pct) } }
                            }
                        }
                    }
                }
                c.disconnect()

                if (!apk.exists() || apk.length() == 0L) throw Exception("Файл обновления пуст")
                if (!isApk(apk)) throw Exception("Скачан не APK — проверьте ссылку в релизе")
                writeStamp(act, versionCode)

                main.post {
                    onDone()
                    install(act, apk, onError, onNeedPermission)
                }
            } catch (e: Exception) {
                try { stampFile(act).delete() } catch (_: Exception) { }
                main.post { onDone(); onError(e.message ?: "Ошибка скачивания") }
            }
        }.start()
    }

    fun installDownloaded(act: Activity, onError: (String) -> Unit) {
        val apk = updateFile(act)
        if (!apk.exists() || apk.length() == 0L) { onError("Файл обновления не найден — скачайте заново"); return }
        if (!isApk(apk)) { apk.delete(); onError("Файл обновления повреждён — скачайте заново"); return }
        install(act, apk, onError) { onError("Разрешите установку из этого приложения и повторите") }
    }

    private fun install(act: Activity, apk: File, onError: (String) -> Unit, onNeedPermission: () -> Unit) {
        if (!canInstall(act)) { onNeedPermission(); return }
        try {
            val uri = FileProvider.getUriForFile(act, act.packageName + ".fileprovider", apk)
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, "application/vnd.android.package-archive")
            i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            try {
                val resolved = act.packageManager.queryIntentActivities(i, 0)
                for (r in resolved) {
                    act.grantUriPermission(r.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (_: Exception) { }

            if (act.packageManager.resolveActivity(i, 0) == null) {
                onError("На устройстве нет установщика пакетов")
                return
            }
            act.startActivity(i)
        } catch (e: Exception) {
            onError("Не удалось запустить установку: ${e.message ?: "неизвестная ошибка"}")
        }
    }
}
