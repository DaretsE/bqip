package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.setup.SetupUtils
import com.bqdiptv.tv.ui.theme.*

// CSS: #setupOverlay{ ... animation:fadeScale .3s cubic-bezier(.2,.7,.3,1) both }
@Composable
fun FirstRunSetupOverlay(
    visible: Boolean,
    provisioningAddress: String?,
    modifier: Modifier = Modifier
) {
    FadeScalePanel(visible = visible, durationMs = BqMotion.FadeScaleSlowMs, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(BqColors.bg0),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("BQDiptv", color = BqColors.accent2, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Отсканируйте QR-код телефоном или откройте адрес в браузере\n(телефон и ТВ должны быть в одной Wi-Fi сети)",
                color = BqColors.text2, fontSize = 15.sp
            )
            Spacer(Modifier.height(24.dp))

            if (provisioningAddress != null) {
                val bmp = remember(provisioningAddress) { SetupUtils.qrBitmap("http://$provisioningAddress") }
                Box(Modifier.background(BqColors.text, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR")
                }
                Spacer(Modifier.height(16.dp))
                Text(provisioningAddress, color = BqColors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                CircularProgressIndicator(color = BqColors.accent2)
                Spacer(Modifier.height(12.dp))
                Text("Запуск сервера настройки…", color = BqColors.textDim, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "На странице настройки добавьте плейлист M3U — эфир запустится сам.",
                color = BqColors.textDim3, fontSize = 13.sp
            )
        }
    }
}
