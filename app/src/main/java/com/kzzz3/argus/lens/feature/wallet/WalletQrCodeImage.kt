package com.kzzz3.argus.lens.feature.wallet

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun WalletQrCodeImage(
    payload: String,
    modifier: Modifier = Modifier,
) {
    val qrBitmap = remember(payload) {
        if (payload.isBlank()) null else createQrBitmap(payload)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Wallet collection QR",
                modifier = Modifier.fillMaxSize(),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

private fun createQrBitmap(payload: String, sizePx: Int = 768): Bitmap {
    val bitMatrix = QRCodeWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        ),
    )
    val pixels = IntArray(bitMatrix.width * bitMatrix.height)
    for (y in 0 until bitMatrix.height) {
        val row = y * bitMatrix.width
        for (x in 0 until bitMatrix.width) {
            pixels[row + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, bitMatrix.width, 0, 0, bitMatrix.width, bitMatrix.height)
    }
}
