package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.model.Channel
import com.bqdiptv.tv.model.EpgProgram
import com.bqdiptv.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// CSS: #rightPanel{ ... border-radius:24px 0 0 24px; box-shadow:-24px 0 60px rgba(0,0,0,.4);
//                    animation:panelInRight .3s cubic-bezier(.2,.7,.3,1) both }
@Composable
fun EpgOverlay(
    visible: Boolean,
    channel: Channel?,
    programs: List<EpgProgram>,
    quality: GraphicsQuality,
    nowMillis: Long,
    modifier: Modifier = Modifier
) {
    SlidePanel(visible = visible, direction = SlideDirection.FROM_RIGHT, modifier = modifier) {
        GlassPanel(
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            quality = quality,
            modifier = Modifier.width(380.dp).fillMaxHeight()
        ) {
            val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            Column(Modifier.padding(20.dp)) {
                Text("${channel?.name ?: ""} — программа", color = BqColors.accent2, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                val current = programs.firstOrNull { nowMillis in it.startMillis until it.stopMillis }
                current?.let {
                    Text(it.title, color = BqColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${fmt.format(Date(it.startMillis))} – ${fmt.format(Date(it.stopMillis))}",
                        color = BqColors.textDim, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { it.progressFraction(nowMillis) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = BqColors.accent2,
                        trackColor = BqColors.card2
                    )
                    it.description?.let { d ->
                        Spacer(Modifier.height(8.dp))
                        Text(d, color = BqColors.text2, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("ПРОГРАММА ПЕРЕДАЧ", color = BqColors.textDim2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(programs.filter { it.startMillis > nowMillis }) { p ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BqColors.cardBg)
                                .padding(10.dp)
                        ) {
                            Text(fmt.format(Date(p.startMillis)), color = BqColors.accent2, fontSize = 12.sp)
                            Text(p.title, color = BqColors.text, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
