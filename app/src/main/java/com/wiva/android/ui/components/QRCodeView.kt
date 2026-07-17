package com.wiva.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import qrcode.QRCode
import qrcode.raw.ErrorCorrectionLevel
import qrcode.render.graphics.DrawScopeGraphics

/**
 * Квадратный QR во [modifier]: библиотека даёт [qrcode.QRCode.fitIntoArea] с floor(module), паттерн
 * оказывается в углу — сдвигаем на половину «хвоста», чтобы поля до края Canvas были равными.
 *
 * По умолчанию [errorCorrectionLevel] = [ErrorCorrectionLevel.HIGH]: при логотипе поверх центра
 * (как у СБП) уровень LOW ломает считывание — перекрытые модули должны восстанавливаться за счёт ECC.
 */
@Composable
fun QRCodeView(
    data: String,
    modifier: Modifier = Modifier,
    errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.HIGH,
) {
    val qrCode =
        remember(data, errorCorrectionLevel) {
            QRCode.ofSquares()
                .withErrorCorrectionLevel(errorCorrectionLevel)
                .build(data)
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = minOf(maxWidth, maxHeight)
            Canvas(Modifier.size(side)) {
                drawQrCodeCenteredInCanvas(qrCode)
            }
        }
    }
}

private fun DrawScope.drawQrCodeCenteredInCanvas(qrCode: QRCode) {
    val w = size.width.toInt().coerceAtLeast(1)
    val h = size.height.toInt().coerceAtLeast(1)
    qrCode.fitIntoArea(w, h)
    val moduleCount = qrCode.rawData.size
    val usedPx = moduleCount * qrCode.squareSize
    val slack = (qrCode.canvasSize - usedPx).coerceAtLeast(0)
    val inset = slack / 2f

    val qrCodeGraphics = qrCode.graphics
    val previousDrawingInterface = qrCodeGraphics.drawingInterface
    val drawScopeGraphics = DrawScopeGraphics(this, qrCodeGraphics.width, qrCodeGraphics.height)
    qrCodeGraphics.drawingInterface = drawScopeGraphics

    translate(inset, inset) {
        qrCode.render()
    }

    qrCodeGraphics.drawingInterface = previousDrawingInterface
}
