package com.bqdiptv.tv.data

import com.bqdiptv.tv.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses a standard XMLTV feed (<tv><programme channel="" start="" stop="">).
 * Times look like "20260723201400 +0300".
 *
 * IMPORTANT: this streams directly from the HTTP response / a Reader and
 * never materialises the whole file as a single String first. Real-world
 * XMLTV feeds are routinely tens or hundreds of MB — loading one into a
 * String (via ResponseBody.string()) is exactly what was causing
 * `OutOfMemoryError` on memory-constrained TV boxes (~160MB heap limit).
 * XmlPullParser is a streaming (pull) parser, so it never needs the whole
 * document in memory at once — only fetch()'s old use of `.string()` did.
 */
object XmltvParser {

    private val fmt = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    suspend fun fetch(url: String, client: OkHttpClient): List<EpgProgram> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: return@withContext emptyList()
            body.charStream().use { reader -> parse(reader) }
        }
    }

    /** Convenience overload for callers/tests that already have the XML as a String. */
    fun parse(xml: String): List<EpgProgram> = parse(StringReader(xml) as Reader)

    fun parse(reader: Reader): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)

        var channelId = ""
        var start = 0L
        var stop = 0L
        var title = ""
        var desc: String? = null
        var inProgramme = false
        var currentTag = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "programme") {
                        inProgramme = true
                        channelId = parser.getAttributeValue(null, "channel") ?: ""
                        start = parseTime(parser.getAttributeValue(null, "start"))
                        stop = parseTime(parser.getAttributeValue(null, "stop"))
                        title = ""
                        desc = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inProgramme) {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "title" -> title = text
                                "desc" -> desc = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme") {
                        inProgramme = false
                        if (channelId.isNotBlank() && start > 0 && stop > start) {
                            programs += EpgProgram(channelId, title.ifBlank { "Без названия" }, desc, start, stop)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return programs
    }

    private fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            fmt.parse(raw)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
