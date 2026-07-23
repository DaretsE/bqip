package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.BuildConfig
import com.bqdiptv.tv.ui.theme.*

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class Available(val versionName: String, val notes: String) : UpdateUiState()
    object UpToDate : UpdateUiState()
    data class Downloading(val percent: Int) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

// CSS: #setPanel #setList{ ... animation:panelInLeft .3s ... }
//      #setDetail{ ... animation:panelInRight .32s ... }
@Composable
fun SettingsOverlay(
    visible: Boolean,
    playlistUrl: String?,
    epgUrl: String?,
    updateState: UpdateUiState,
    graphicsQuality: GraphicsQuality,
    graphicsAuto: Boolean,
    onChangePlaylist: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onToggleEconomyMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    SlidePanel(visible = visible, direction = SlideDirection.FROM_LEFT, modifier = modifier) {
        Row(Modifier.fillMaxSize()) {
            GlassPanel(
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                quality = graphicsQuality,
                modifier = Modifier.width(360.dp).fillMaxHeight()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("BQDiptv · v${BuildConfig.VERSION_NAME}", color = BqColors.textDim, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Настройки", color = BqColors.accent2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))

                    SettingsRow("Плейлист", playlistUrl ?: "не задан", graphicsQuality, onChangePlaylist)
                    SettingsRow("EPG", epgUrl ?: "не задан", graphicsQuality, onChangePlaylist)

                    Spacer(Modifier.height(18.dp))
                    Text("ПРОИЗВОДИТЕЛЬНОСТЬ", color = BqColors.textDim2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    EconomyToggleRow(
                        auto = graphicsAuto,
                        quality = graphicsQuality,
                        onClick = onToggleEconomyMode
                    )

                    Spacer(Modifier.height(18.dp))
                    Text("ОБНОВЛЕНИЯ", color = BqColors.textDim2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    when (updateState) {
                        is UpdateUiState.Idle -> SettingsRow("Проверить обновления", "", graphicsQuality, onCheckUpdate)
                        is UpdateUiState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BqColors.accent2)
                            Spacer(Modifier.width(8.dp))
                            Text("Проверка…", color = BqColors.textDim, fontSize = 13.sp)
                        }
                        is UpdateUiState.UpToDate -> Text("Установлена последняя версия ✓", color = BqColors.green, fontSize = 13.sp)
                        is UpdateUiState.Available -> Column {
                            Text("Доступна версия ${updateState.versionName}", color = BqColors.lime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (updateState.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(updateState.notes.take(200), color = BqColors.textDim, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            SettingsRow("Скачать и установить", "", graphicsQuality, onInstallUpdate, highlightLime = true)
                        }
                        is UpdateUiState.Downloading -> Text("Загрузка… ${updateState.percent}%", color = BqColors.accent2, fontSize = 13.sp)
                        is UpdateUiState.Error -> Text(updateState.message, color = BqColors.red, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// CSS: .setrow.focused{ background:rgba(255,255,255,.06); box-shadow:var(--halo-glow); transform:translateX(3px) }
@Composable
private fun SettingsRow(
    title: String,
    value: String,
    quality: GraphicsQuality,
    onClick: () -> Unit,
    highlightLime: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlightLime) BqColors.focusFill else BqColors.cardBg, shape)
            .haloGlow(visible = highlightLime, shape = shape, quality = quality, lime = highlightLime)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Text(title, color = BqColors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (value.isNotBlank()) {
            Text(value, color = BqColors.textDim, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
}

// CSS: .sd-toggle .track{ ... } .track > i{ transition:left .22s cubic-bezier(.3,.7,.3,1) }
@Composable
private fun EconomyToggleRow(auto: Boolean, quality: GraphicsQuality, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BqColors.cardBg, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Экономичный режим", color = BqColors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (auto) "Авто (сейчас: ${if (quality == GraphicsQuality.HIGH) "полный визуал" else "облегчённый"})"
                else if (quality == GraphicsQuality.LOW) "Включён вручную — для слабых ТВ-приставок"
                else "Выключен вручную — полные эффекты",
                color = BqColors.textDim, fontSize = 11.sp
            )
        }
        ToggleTrack(on = quality == GraphicsQuality.LOW)
    }
}

@Composable
private fun ToggleTrack(on: Boolean) {
    Box(
        Modifier
            .size(width = 56.dp, height = 30.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(BqColors.cardBg)
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(22.dp)
                .align(if (on) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .background(if (on) BqColors.accent2 else BqColors.textDim2)
        )
    }
}
