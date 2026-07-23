package com.bqdiptv.tv.setup

import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object SetupUtils {

    /** Best-effort local IPv4 address of the TV on its Wi-Fi/Ethernet network. */
    fun localIpAddress(context: Context): String? {
        return try {
            val wifi = context.getSystemService<WifiManager>()
            val ipInt = wifi?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff
                )
            } else {
                // Fall back to scanning network interfaces (covers Ethernet-connected TVs).
                java.net.NetworkInterface.getNetworkInterfaces().asSequence()
                    .flatMap { it.inetAddresses.asSequence() }
                    .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                    ?.hostAddress
            }
        } catch (e: Exception) {
            null
        }
    }

    fun qrBitmap(text: String, sizePx: Int = 480): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }
}
