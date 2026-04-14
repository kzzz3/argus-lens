package com.kzzz3.argus.lens.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun QrScannerPreview(
    isActive: Boolean,
    onPayloadDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }
    var lastPayload by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, cameraController, barcodeScanner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            barcodeScanner.close()
        }
    }

    LaunchedEffect(isActive, barcodeScanner, cameraController, mainExecutor) {
        if (!isActive) {
            lastPayload = ""
            cameraController.clearImageAnalysisAnalyzer()
            return@LaunchedEffect
        }

        cameraController.setImageAnalysisAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOf(barcodeScanner),
                ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                mainExecutor,
            ) { result ->
                val detectedPayload = result
                    ?.getValue(barcodeScanner)
                    ?.firstOrNull { !it.rawValue.isNullOrBlank() }
                    ?.rawValue
                    ?.trim()
                    .orEmpty()
                if (detectedPayload.isNotEmpty() && detectedPayload != lastPayload) {
                    lastPayload = detectedPayload
                    onPayloadDetected(detectedPayload)
                }
            }
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController
            }
        },
        update = { previewView ->
            previewView.controller = cameraController
            previewView.alpha = if (isActive) 1f else 0.45f
        }
    )
}

fun hasCameraPermission(permissionState: Int): Boolean {
    return permissionState == PackageManager.PERMISSION_GRANTED
}

fun cameraPermissionName(): String = Manifest.permission.CAMERA
