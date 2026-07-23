package com.bqdiptv.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.bqdiptv.tv.model.Channel
import com.bqdiptv.tv.model.EpgProgram
import com.bqdiptv.tv.player.PlayerHolder
import com.bqdiptv.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerSurface(playerHolder: PlayerHolder, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = playerHolder.player
            }
        }
    )
}

// CSS: #osd{ animation:osdUp .34s cubic-bezier(.2,.7,.3,1) both }
//      .live .dot{ animation:livePulse 1.6s ease-in-out infinite } @keyframes livePulse{50%{opacity:.4}}
@Composable
fun OsdOverlay(
    channel: Channel?,
    current: EpgProgram?,
    next: EpgProgram?,
    visible: Boolean,
    quality: GraphicsQuality,
    modifier: Modifier = Modifier
) {
    val now = remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = Date()
            kotlinx.coroutines.delay(1000)
        }
    }

    SlidePanel(visible = visible, direction = SlideDirection.FROM_BOTTOM, modifier = modifier) {
        GlassPanel(shape = RoundedCornerShape(16.dp), quality = quality, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(BqColors.card2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(channel?.name?.take(1) ?: "?", color = BqColors.text, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(channel?.name ?: "", color = BqColors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(current?.title ?: "Нет данных программы", color = BqColors.text2, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        val fraction = current?.progressFraction(now.value.time) ?: 0f
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = BqColors.accent2,
                            trackColor = BqColors.card2
                        )
                        current?.let {
                            Text("осталось ${it.remainingMinutes(now.value.time)} мин", color = BqColors.textDim, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        LivePulseBadge()
                        Text(
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.value),
                            color = BqColors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                next?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Далее: ${it.title} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.startMillis))}",
                        color = BqColors.textDim2, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/** `@keyframes livePulse{50%{opacity:.4}}` — 1.6s ease-in-out infinite. */
@Composable
private fun LivePulseBadge() {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(BqMotion.LivePulseDurationMs / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulseAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(50))
                .background(Color(0xFFFF5A5A).copy(alpha = alpha))
        )
        Spacer(Modifier.width(6.dp))
        Text("прямой эфир", color = BqColors.lime, fontSize = 12.sp)
    }
}

// CSS: #toast{ animation:toastUp .28s cubic-bezier(.2,.7,.3,1) both }
@Composable
fun ToastBanner(message: String?, modifier: Modifier = Modifier) {
    SlidePanel(visible = message != null, direction = SlideDirection.FROM_BOTTOM, modifier = modifier) {
        Box(
            Modifier.background(BqColors.red.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(message ?: "", color = Color.White, fontSize = 14.sp)
        }
    }
}
