package com.bqdiptv.tv.model

/**
 * One entry parsed out of an M3U / M3U8 playlist (#EXTINF line + URL).
 */
data class Channel(
    val id: String,          // tvg-id, falls back to name if absent
    val name: String,
    val logoUrl: String?,
    val group: String,       // group-title, "Без категории" if absent
    val streamUrl: String,
    var isFavorite: Boolean = false
)

data class Category(
    val name: String,
    val channels: List<Channel>
)
