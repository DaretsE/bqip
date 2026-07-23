package com.bqdiptv.tv.data

import com.bqdiptv.tv.model.Category
import com.bqdiptv.tv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader

/**
 * Minimal but reasonably robust #EXTM3U parser: handles tvg-id, tvg-logo,
 * group-title attributes and tvg-name/plain fallback for the channel name.
 *
 * Streams line-by-line from the HTTP response instead of buffering the
 * whole playlist into one String first (large IPTV playlists can be tens
 * of MB — the same class of bug that caused an OutOfMemoryError in
 * XmltvParser was latent here too, just less likely to be hit in practice).
 */
object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9\-]+)="([^"]*)"""")

    suspend fun fetch(url: String, client: OkHttpClient): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("Пустой ответ")
            body.charStream().use { reader -> parse(BufferedReader(reader)) }
        }
    }

    /** Convenience overload for callers/tests that already have the M3U as a String. */
    fun parse(content: String): List<Channel> = parse(BufferedReader(StringReader(content)))

    fun parse(reader: BufferedReader): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var pendingId: String? = null

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    val attrs = attrRegex.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                    pendingId = attrs["tvg-id"]
                    pendingLogo = attrs["tvg-logo"]
                    pendingGroup = attrs["group-title"]?.ifBlank { null }
                    // Channel display name is whatever follows the last comma.
                    pendingName = (attrs["tvg-name"]
                        ?: line.substringAfterLast(',').trim())
                        .ifBlank { "Канал" }
                }
                line.isBlank() || line.startsWith("#") -> { /* ignore other tags */ }
                else -> {
                    // This line is the stream URL for the preceding #EXTINF.
                    val name = pendingName ?: "Канал ${channels.size + 1}"
                    channels += Channel(
                        id = pendingId?.ifBlank { null } ?: name,
                        name = name,
                        logoUrl = pendingLogo,
                        group = pendingGroup ?: "Без категории",
                        streamUrl = line
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                    pendingId = null
                }
            }
        }
        return channels
    }

    fun groupByCategory(channels: List<Channel>): List<Category> =
        channels.groupBy { it.group }
            .map { (name, list) -> Category(name, list) }
            .sortedBy { it.name }
}
