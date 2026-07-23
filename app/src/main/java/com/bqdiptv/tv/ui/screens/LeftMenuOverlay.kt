package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.model.Category
import com.bqdiptv.tv.ui.theme.*

@Composable
fun LeftMenuOverlay(
    visible: Boolean,
    categories: List<Category>,
    quality: GraphicsQuality,
    onCategorySelected: (Category) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // CSS: #leftMenu{ ... box-shadow:24px 0 60px rgba(0,0,0,.4); animation:panelInLeft .3s ... }
    SlidePanel(visible = visible, direction = SlideDirection.FROM_LEFT, modifier = modifier) {
        GlassPanel(
            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            quality = quality,
            modifier = Modifier.width(320.dp).fillMaxHeight()
        ) {
            var focusedRow by remember { mutableStateOf(-1) }
            Column(Modifier.padding(20.dp)) {
                Text("BQDiptv", color = BqColors.accent2, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Мой плейлист", color = BqColors.textDim, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))

                MenuRow("🔎", "Поиск передачи", focused = focusedRow == -2, quality = quality, onClick = onOpenSearch)
                MenuRow("⚙", "Настройки", focused = focusedRow == -3, quality = quality, onClick = onOpenSettings)

                Spacer(Modifier.height(16.dp))
                Text("КАТЕГОРИИ", color = BqColors.textDim2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categories.size) { i ->
                        val cat = categories[i]
                        CategoryRow(cat, focused = focusedRow == i, quality = quality, onClick = { onCategorySelected(cat) })
                    }
                }
            }
        }
    }
}

// CSS: .catrow / .row focused -> background rgba(255,255,255,.06) + halo-glow + translateX(3px)
@Composable
private fun MenuRow(icon: String, label: String, focused: Boolean, quality: GraphicsQuality, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) BqColors.focusFill else BqColors.cardBg, RoundedCornerShape(10.dp))
            .haloGlow(visible = focused, shape = RoundedCornerShape(10.dp), quality = quality)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        Text(icon, fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, color = BqColors.text2, fontSize = 15.sp)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CategoryRow(category: Category, focused: Boolean, quality: GraphicsQuality, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) BqColors.focusFill else BqColors.card2, RoundedCornerShape(10.dp))
            .haloGlow(visible = focused, shape = RoundedCornerShape(10.dp), quality = quality)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(category.name, color = BqColors.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("${category.channels.size}", color = BqColors.textDim, fontSize = 12.sp)
    }
}
