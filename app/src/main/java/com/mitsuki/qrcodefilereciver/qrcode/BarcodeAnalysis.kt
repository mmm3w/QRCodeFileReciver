package com.mitsuki.qrcodefilereciver.qrcode

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mitsuki.qrcodefilereciver.task.CodeDataHandler
import com.mitsuki.qrcodefilereciver.task.DataAnalysis

internal class BarcodeAnalysis(
    private val indicator: (CodeMark) -> Unit,
    private val handler: CodeDataHandler
) : ImageAnalysis.Analyzer {

    private var isEnable = true
    private val codeMark = CodeMark()

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!isEnable) {
            imageProxy.close()
            return
        }

        imageProxy.image?.apply {
            codeMark.rotation = imageProxy.imageInfo.rotationDegrees
            codeMark.analysisRect(width, height)
            val image = InputImage.fromMediaImage(this, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    codeMark.reset()
                    for (barcode in barcodes) {
                        barcode.rawBytes?.also { handler.postData(it) }
                        barcode.boundingBox?.apply { codeMark.path(this) }
                    }
                    indicator.invoke(codeMark)
                }
                .addOnCompleteListener { imageProxy.close() }
        } ?: let { imageProxy.close() }
    }
}