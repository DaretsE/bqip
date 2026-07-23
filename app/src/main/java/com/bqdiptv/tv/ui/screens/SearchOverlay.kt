package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.model.Channel
import com.bqdiptv.tv.model.EpgProgram
import com.bqdiptv.tv.ui.theme.*

data class SearchHit(val channel: Channel, val program: EpgProgram)

// CSS: #search{ background:rgba(4,14,20,.86); backdrop-filter:blur(16px);
//               animation:fadeScale .28s cubic-bezier(.2,.7,.3,1) both }
@Composable
fun SearchOverlay(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchHit>,
    quality: GraphicsQuality,
    onSelectHit: (SearchHit) -> Unit,
    modifier: Modifier = Modifier
) {
    FadeScalePanel(visible = visible, modifier = modifier.fillMaxSize()) {
        GlassPanel(
            shape = RoundedCornerShape(0.dp),
            tint = BqColors.bg1.copy(alpha = 0.92f),
            quality = quality,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.padding(40.dp)) {
                Text("🔎  Поиск передачи", color = BqColors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BqColors.cardBg)
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(color = BqColors.text, fontSize = 16.sp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Введите название. Поиск идёт по идущим сейчас и будущим передачам.",
                    color = BqColors.textDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(20.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { hit ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BqColors.cardBg)
                                .clickable { onSelectHit(hit) }
                                .padding(14.dp)
                        ) {
                            Text(hit.program.title, color = BqColors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(hit.channel.name, color = BqColors.textDim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
