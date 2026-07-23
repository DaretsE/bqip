package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.model.Channel
import com.bqdiptv.tv.model.EpgProgram
import com.bqdiptv.tv.ui.theme.*

/**
 * CSS: #browser.rail-shift #browserList{ left:88px; animation:railIn .36s cubic-bezier(.2,.72,.28,1) }
 * — the channel list slides out from behind the (now-collapsed) left menu
 * rail, and the preview card fades/slides up like the OSD (`osdUp .34s`).
 */
@Composable
fun BrowserOverlay(
    visible: Boolean,
    channels: List<Channel>,
    favorites: Set<String>,
    currentChannel: Channel?,
    quality: GraphicsQuality,
    currentEpg: (Channel) -> EpgProgram?,
    onSelect: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxSize()) {
        RailPanel(visible = visible) {
            GlassPanel(
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                quality = quality,
                modifier = Modifier.width(360.dp).fillMaxHeight()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Все каналы", color = BqColors.accent2, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(channels, key = { it.id }) { ch ->
                            ChannelRow(
                                channel = ch,
                                isCurrent = ch.id == currentChannel?.id,
                                isFavorite = ch.id in favorites,
                                nowTitle = currentEpg(ch)?.title,
                                quality = quality,
                                onClick = { onSelect(ch) },
                                onLongFavorite = { onToggleFavorite(ch) }
                            )
                        }
                    }
                }
            }
        }

        // #previewCard { animation: osdUp .34s cubic-bezier(.2,.7,.3,1) both }
        currentChannel?.let { ch ->
            SlidePanel(visible = visible, direction = SlideDirection.FROM_BOTTOM) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                    GlassPanel(
                        shape = RoundedCornerShape(16.dp),
                        quality = quality,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(ch.name, color = BqColors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            val ep = currentEpg(ch)
                            Text("Сейчас: ${ep?.title ?: "нет данных"}", color = BqColors.text2, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// CSS: .row.focused { background:rgba(255,255,255,.06); box-shadow:var(--halo-glow); transform:translateX(3px) }
@Composable
private fun ChannelRow(
    channel: Channel,
    isCurrent: Boolean,
    isFavorite: Boolean,
    nowTitle: String?,
    quality: GraphicsQuality,
    onClick: () -> Unit,
    onLongFavorite: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) BqColors.focusFill else BqColors.cardBg, shape)
            .haloGlow(visible = isCurrent, shape = shape, quality = quality)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BqColors.card2),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = BqColors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (nowTitle != null) {
                Text(nowTitle, color = BqColors.textDim, fontSize = 11.sp)
            }
        }
        if (isFavorite) {
            Text("★", color = BqColors.lime, fontSize = 16.sp)
        }
    }
}
