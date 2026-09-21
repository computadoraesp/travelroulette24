package com.example.travelroulette24.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.EncodeHintType
import java.util.EnumMap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

object QRCodeUtil {
    /**
     * Generates a QR code bitmap for the given [text].
     * Returns a 512x512 Bitmap.
     */
    fun generate(text: String, size: Int = 512): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 1
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            createBitmap(size, size, Bitmap.Config.RGB_565).apply {
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        val color = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                        this[x, y] = color
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
