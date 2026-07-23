package com.bqdiptv.tv.setup

import fi.iki.elonen.NanoHTTPD

/**
 * Tiny local web server, mirroring the prototype's "phone setup" flow: the
 * TV shows a QR code pointing at http://<tv-ip>:8080, the phone opens it in
 * a browser, submits an M3U (and optional EPG) URL, and this server hands
 * that straight to [onPlaylistSubmitted].
 */
class ProvisioningServer(
    port: Int = 8080,
    private val onPlaylistSubmitted: (playlistUrl: String, epgUrl: String?) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.POST && session.uri == "/submit" -> {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val params = session.parms
                val playlist = params["playlist"]?.trim().orEmpty()
                val epg = params["epg"]?.trim()
                if (playlist.isNotEmpty()) {
                    onPlaylistSubmitted(playlist, epg?.ifBlank { null })
                    htmlResponse(SUCCESS_PAGE)
                } else {
                    htmlResponse(FORM_PAGE.replace("{{error}}", "Укажите ссылку на M3U-плейлист"))
                }
            }
            else -> htmlResponse(FORM_PAGE.replace("{{error}}", ""))
        }
    }

    private fun htmlResponse(html: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)

    companion object {
        private val FORM_PAGE = """
            <!doctype html><html lang="ru"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>BQDiptv — настройка</title>
            <style>
              body{font-family:sans-serif;background:#04121A;color:#EEF6F8;padding:24px;max-width:480px;margin:auto}
              h1{color:#63D4E2;font-size:22px}
              input{width:100%;padding:12px;margin:8px 0;border-radius:8px;border:1px solid #1a3a45;background:#0F2A33;color:#EEF6F8}
              button{width:100%;padding:14px;border-radius:8px;border:none;background:#45B8C7;color:#04121A;font-weight:700;font-size:16px}
              .err{color:#F0663F;min-height:20px}
            </style></head><body>
              <h1>BQDiptv — настройка плейлиста</h1>
              <p>Введите ссылку на ваш M3U-плейлист. EPG (телепрограмма) — по желанию.</p>
              <div class="err">{{error}}</div>
              <form method="post" action="/submit">
                <input name="playlist" placeholder="https://example.com/playlist.m3u" required>
                <input name="epg" placeholder="https://example.com/epg.xml (необязательно)">
                <button type="submit">Сохранить и запустить эфир</button>
              </form>
            </body></html>
        """.trimIndent()

        private val SUCCESS_PAGE = """
            <!doctype html><html lang="ru"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Готово</title>
            <style>body{font-family:sans-serif;background:#04121A;color:#EEF6F8;padding:24px;text-align:center}
            h1{color:#A8E05F}</style></head><body>
              <h1>Готово ✓</h1>
              <p>Плейлист сохранён. Возвращайтесь к телевизору — эфир запускается.</p>
            </body></html>
        """.trimIndent()
    }
}
