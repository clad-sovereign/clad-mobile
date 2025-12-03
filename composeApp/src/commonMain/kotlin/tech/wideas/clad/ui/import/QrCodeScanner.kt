package tech.wideas.clad.ui.import

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private val log = Logger.withTag("QrCodeScanner")

/**
 * QR code scanner composable using CameraX and ML Kit.
 *
 * Note: This is Android-only since composeApp targets Android.
 * iOS uses native SwiftUI with AVFoundation for QR scanning.
 *
 * @param onQrCodeScanned Callback when a QR code is successfully scanned
 * @param onError Callback when camera initialization fails
 * @param modifier Modifier for the scanner view
 */
@Composable
fun QrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDenied = true
            log.w { "Camera permission denied by user" }
            onError?.invoke("Camera permission is required to scan QR codes")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            log.d { "Requesting camera permission" }
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when {
        hasCameraPermission -> {
            CameraPreview(
                onQrCodeScanned = onQrCodeScanned,
                onError = onError,
                modifier = modifier
            )
        }
        permissionDenied -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission denied. Please enable it in Settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        else -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Requesting camera permission...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onQrCodeScanned: (String) -> Unit,
    onError: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            log.d { "Disposing camera resources" }
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    if (cameraError != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cameraError ?: "Camera error",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                if (barcode.valueType == Barcode.TYPE_TEXT ||
                                                    barcode.valueType == Barcode.TYPE_URL
                                                ) {
                                                    barcode.rawValue?.let { value ->
                                                        log.i { "QR code scanned successfully" }
                                                        onQrCodeScanned(value)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            log.w(e) { "Barcode processing failed" }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    log.i { "Camera bound to lifecycle successfully" }
                } catch (e: Exception) {
                    val errorMessage = "Failed to initialize camera: ${e.message}"
                    log.e(e) { errorMessage }
                    cameraError = errorMessage
                    onError?.invoke(errorMessage)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}
