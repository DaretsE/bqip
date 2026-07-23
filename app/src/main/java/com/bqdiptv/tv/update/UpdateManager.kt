package com.bqdiptv.tv.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.bqdiptv.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

sealed class UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Talks to the GitHub Releases REST API (no auth needed for public repos):
 *   GET https://api.github.com/repos/{owner}/{repo}/releases/latest
 *
 * Expects the release to have exactly one .apk asset, and a tag name that
 * looks like "v1.2.3" or "v1.2.3+45" (the trailing "+NN" — if present — is
 * read as the versionCode; otherwise versionCode falls back to the release's
 * numeric-only reading of the version, see [parseVersionCode]).
 *
 * The CI workflow in .github/workflows/release.yml produces releases in
 * exactly this shape, so nothing needs to be configured beyond setting
 * BuildConfig.GITHUB_REPO to "owner/repo" in app/build.gradle.kts.
 */
class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
            val request = Request.Builder().url(url)
                .addHeader("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("GitHub вернул ${resp.code}")
                }
                val body = resp.body?.string() ?: return@withContext UpdateCheckResult.Error("Пустой ответ")
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val notes = json.optString("body", "")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }
                if (apkUrl == null) {
                    return@withContext UpdateCheckResult.Error("В релизе нет APK-файла")
                }

                val remoteVersionCode = parseVersionCode(tag)
                val localVersionCode = currentVersionCode()

                if (remoteVersionCode > localVersionCode) {
                    UpdateCheckResult.Available(
                        UpdateInfo(
                            versionName = tag.removePrefix("v"),
                            versionCode = remoteVersionCode,
                            downloadUrl = apkUrl,
                            releaseNotes = notes
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /** Downloads the APK to cache and returns the local file, reporting 0..100 progress. */
    suspend fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(dir, "bqdiptv-${info.versionName}.apk")

        val request = Request.Builder().url(info.downloadUrl).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Ошибка загрузки: ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("Пустое тело ответа")
            val total = body.contentLength()
            var written = 0L
            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress(((written * 100) / total).toInt())
                    }
                }
            }
        }
        outFile
    }

    /** Launches the system package installer for the downloaded APK. */
    fun install(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** True on API 26+ if the user still needs to grant "install unknown apps" for this app. */
    fun needsInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !context.packageManager.canRequestPackageInstalls()
        } else {
            false
        }
    }

    fun installPermissionSettingsIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    private fun currentVersionCode(): Int = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode.toInt() else pi.versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        0
    }

    /**
     * Tag names are expected as "vMAJOR.MINOR.PATCH" (e.g. "v1.4.2") produced
     * by the CI workflow, which also stamps versionCode as
     * MAJOR*10000 + MINOR*100 + PATCH. Keep this in sync with
     * .github/workflows/release.yml if you change the scheme.
     */
    private fun parseVersionCode(tag: String): Int {
        val clean = tag.removePrefix("v").substringBefore("+")
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10000 + minor * 100 + patch
    }
}
