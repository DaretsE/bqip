package com.bqdiptv.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bqdiptv.tv.data.M3uParser
import com.bqdiptv.tv.data.SettingsRepository
import com.bqdiptv.tv.data.XmltvParser
import com.bqdiptv.tv.model.Category
import com.bqdiptv.tv.model.Channel
import com.bqdiptv.tv.model.EpgProgram
import com.bqdiptv.tv.player.PlayerHolder
import com.bqdiptv.tv.setup.ProvisioningServer
import com.bqdiptv.tv.ui.theme.GraphicsQuality
import com.bqdiptv.tv.ui.theme.GraphicsQualityDetector
import com.bqdiptv.tv.update.UpdateCheckResult
import com.bqdiptv.tv.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

enum class Overlay { NONE, LEFT_MENU, BROWSER, EPG, SEARCH, SETTINGS, FIRST_RUN, SCREENSAVER }

data class AppUiState(
    val loading: Boolean = true,
    val overlay: Overlay = Overlay.FIRST_RUN,
    val categories: List<Category> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val currentChannel: Channel? = null,
    val epg: Map<String, List<EpgProgram>> = emptyMap(),
    val provisioningActive: Boolean = false,
    val provisioningAddress: String? = null,
    val errorMessage: String? = null,
    val toast: String? = null,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.LOW,
    val graphicsAuto: Boolean = true,
    val lastCrashLog: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val httpClient = OkHttpClient()
    val player = PlayerHolder(application)

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var provisioningServer: ProvisioningServer? = null

    init {
        player.setOnError { onPlaybackError() }

        // If the previous run crashed, BqdiptvApp's uncaught-exception
        // handler will have written the stack trace here — surface it once.
        val crashFile = java.io.File(application.filesDir, "last_crash.txt")
        if (crashFile.exists()) {
            val log = try { crashFile.readText() } catch (e: Exception) { null }
            crashFile.delete()
            if (!log.isNullOrBlank()) {
                _state.value = _state.value.copy(lastCrashLog = log)
            }
        }

        val detected = GraphicsQualityDetector.detect(application)
        viewModelScope.launch {
            settings.graphicsQualityOverride.collect { override ->
                val effective = when (override) {
                    "HIGH" -> GraphicsQuality.HIGH
                    "LOW" -> GraphicsQuality.LOW
                    else -> detected // "AUTO"
                }
                _state.value = _state.value.copy(
                    graphicsQuality = effective,
                    graphicsAuto = override == "AUTO"
                )
            }
        }

        viewModelScope.launch {
            val savedPlaylist = settings.currentPlaylistUrl()
            if (savedPlaylist.isNullOrBlank()) {
                _state.value = _state.value.copy(loading = false, overlay = Overlay.FIRST_RUN)
            } else {
                loadPlaylist(savedPlaylist, showAsOverlay = false)
            }
        }
    }

    fun dismissCrashLog() {
        _state.value = _state.value.copy(lastCrashLog = null)
    }

    fun toggleEconomyMode() {
        viewModelScope.launch {
            val current = settings.graphicsQualityOverride.first()
            settings.setGraphicsQualityOverride(if (current == "LOW") "AUTO" else "LOW")
        }
    }

    fun loadPlaylist(url: String, epgUrl: String? = null, showAsOverlay: Boolean = true) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            try {
                settings.setPlaylist(url)
                epgUrl?.let { settings.setEpgUrl(it) }

                val channels = M3uParser.fetch(url, httpClient)
                val categories = M3uParser.groupByCategory(channels)
                val favs = settings.favorites.first()

                val effectiveEpgUrl = epgUrl ?: settings.currentEpgUrl()
                val epgByChannel: Map<String, List<EpgProgram>> = try {
                    if (!effectiveEpgUrl.isNullOrBlank()) {
                        XmltvParser.fetch(effectiveEpgUrl, httpClient).groupBy { it.channelId }
                    } else emptyMap()
                } catch (e: Throwable) {
                    // Deliberately catches Throwable, not just Exception:
                    // OutOfMemoryError is an Error, and an oversized EPG feed
                    // should degrade to "no programme guide" rather than
                    // crash the whole app.
                    emptyMap()
                }

                val first = channels.firstOrNull()
                _state.value = _state.value.copy(
                    loading = false,
                    categories = categories,
                    allChannels = channels,
                    favorites = favs,
                    epg = epgByChannel,
                    currentChannel = first,
                    overlay = Overlay.NONE
                )
                first?.let { player.play(it.streamUrl) }
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    errorMessage = "Не удалось загрузить плейлист: ${e.message}",
                    toast = "Не удалось загрузить плейлист: ${e.message}"
                )
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _state.value = _state.value.copy(currentChannel = channel, overlay = Overlay.NONE)
        player.play(channel.streamUrl)
    }

    fun nextChannel(direction: Int) {
        val all = _state.value.allChannels
        val current = _state.value.currentChannel ?: return
        val idx = all.indexOf(current)
        if (idx < 0 || all.isEmpty()) return
        val next = all[(idx + direction + all.size) % all.size]
        selectChannel(next)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            settings.toggleFavorite(channel.id)
            _state.value = _state.value.copy(favorites = settings.favorites.first())
        }
    }

    fun setOverlay(overlay: Overlay) {
        _state.value = _state.value.copy(overlay = overlay)
    }

    fun currentEpgFor(channel: Channel?, nowMillis: Long): EpgProgram? {
        if (channel == null) return null
        return _state.value.epg[channel.id]?.firstOrNull { nowMillis in it.startMillis until it.stopMillis }
    }

    fun nextEpgFor(channel: Channel?, nowMillis: Long): EpgProgram? {
        if (channel == null) return null
        return _state.value.epg[channel.id]
            ?.filter { it.startMillis > nowMillis }
            ?.minByOrNull { it.startMillis }
    }

    // ---- First-run phone provisioning ----

    fun startProvisioning() {
        if (provisioningServer != null) return
        val server = ProvisioningServer { playlistUrl, epgUrl ->
            viewModelScope.launch { loadPlaylist(playlistUrl, epgUrl) }
            // Stop the server a moment later, not immediately: stopping it
            // synchronously here would race the HTTP response NanoHTTPD is
            // still writing back to the phone's browser for this very
            // request, and NanoHTTPD.stop() force-closes open connections —
            // that's what was causing "site can't be reached" on the phone
            // right after tapping submit.
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                stopProvisioning()
            }
        }
        try {
            server.start()
            provisioningServer = server
            val ip = com.bqdiptv.tv.setup.SetupUtils.localIpAddress(getApplication())
            _state.value = _state.value.copy(
                provisioningActive = true,
                provisioningAddress = if (ip != null) "$ip:8080" else null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(errorMessage = "Не удалось запустить сервер настройки: ${e.message}")
        }
    }

    fun stopProvisioning() {
        try {
            provisioningServer?.stop()
        } catch (e: Exception) {
            // Best-effort shutdown; a failure here must never crash the app.
        }
        provisioningServer = null
        _state.value = _state.value.copy(provisioningActive = false)
    }

    // ---- Playback error -> try next channel like the prototype's OSD error banner ----

    private var errorRetryCount = 0
    private fun onPlaybackError() {
        _state.value = _state.value.copy(toast = "Не удалось воспроизвести канал. Пробуем следующий источник…")
        if (errorRetryCount < 3) {
            errorRetryCount++
            nextChannel(1)
        }
    }

    fun clearToast() {
        _state.value = _state.value.copy(toast = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopProvisioning()
        player.release()
    }
}
