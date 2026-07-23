package com.bqdiptv.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bqdiptv_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PLAYLIST_URL = stringPreferencesKey("playlist_url")
        val PLAYLIST_NAME = stringPreferencesKey("playlist_name")
        val EPG_URL = stringPreferencesKey("epg_url")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality") // "AUTO" | "HIGH" | "LOW"
    }

    val graphicsQualityOverride: Flow<String> = context.dataStore.data.map { it[Keys.GRAPHICS_QUALITY] ?: "AUTO" }

    suspend fun setGraphicsQualityOverride(value: String) {
        context.dataStore.edit { it[Keys.GRAPHICS_QUALITY] = value }
    }

    val playlistUrl: Flow<String?> = context.dataStore.data.map { it[Keys.PLAYLIST_URL] }
    val epgUrl: Flow<String?> = context.dataStore.data.map { it[Keys.EPG_URL] }
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }

    suspend fun currentPlaylistUrl(): String? = playlistUrl.first()
    suspend fun currentEpgUrl(): String? = epgUrl.first()

    suspend fun setPlaylist(url: String, name: String = "Мой плейлист") {
        context.dataStore.edit {
            it[Keys.PLAYLIST_URL] = url
            it[Keys.PLAYLIST_NAME] = name
        }
    }

    suspend fun setEpgUrl(url: String) {
        context.dataStore.edit { it[Keys.EPG_URL] = url }
    }

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (channelId in current) current - channelId else current + channelId
        }
    }

    suspend fun clearPlaylist() {
        context.dataStore.edit {
            it.remove(Keys.PLAYLIST_URL)
            it.remove(Keys.PLAYLIST_NAME)
        }
    }
}
