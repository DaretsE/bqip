package com.bqdiptv.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bqdiptv.tv.ui.theme.BqColors

/**
 * Shown once, right after a crash, with the actual stack trace so it can be
 * read/photographed straight off the TV screen without needing ADB.
 */
@Composable
fun CrashLogOverlay(log: String?, onDismiss: () -> Unit) {
    if (log == null) return
    Box(
        Modifier
            .fillMaxSize()
            .background(BqColors.bg0.copy(alpha = 0.97f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .background(BqColors.panelSolid, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                "Приложение упало при прошлом запуске",
                color = BqColors.red, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Сфотографируйте текст ниже и пришлите — по нему можно найти точную причину. Нажмите в любом месте, чтобы закрыть.",
                color = BqColors.textDim, fontSize = 12.sp
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BqColors.cardBg)
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    log,
                    color = BqColors.text2,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
