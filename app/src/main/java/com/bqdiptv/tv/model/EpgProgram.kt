package com.bqdiptv.tv.model

/** One <programme> entry from an XMLTV EPG feed. */
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val stopMillis: Long
) {
    fun progressFraction(nowMillis: Long): Float {
        if (nowMillis <= startMillis) return 0f
        if (nowMillis >= stopMillis) return 1f
        val total = (stopMillis - startMillis).coerceAtLeast(1)
        return (nowMillis - startMillis).toFloat() / total
    }

    fun remainingMinutes(nowMillis: Long): Long =
        ((stopMillis - nowMillis).coerceAtLeast(0)) / 60000
}
