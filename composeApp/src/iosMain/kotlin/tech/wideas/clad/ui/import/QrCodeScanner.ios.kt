package tech.wideas.clad.ui.import

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.AVFoundation.*
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS QR code scanner using AVFoundation.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier
) {
    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }
    var scannerController by remember { mutableStateOf<QrScannerController?>(null) }

    // Check camera permission on launch
    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> {
                hasCameraPermission = true
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        hasCameraPermission = granted
                    }
                }
            }
            else -> {
                hasCameraPermission = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scannerController?.stopScanning()
        }
    }

    when (hasCameraPermission) {
        true -> {
            UIKitView(
                factory = {
                    val controller = QrScannerController(onQrCodeScanned)
                    scannerController = controller
                    controller.view
                },
                modifier = modifier.fillMaxSize(),
                update = { _ ->
                    scannerController?.startScanning()
                },
                onRelease = { _ ->
                    scannerController?.stopScanning()
                }
            )
        }
        false -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission required",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Requesting camera access...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class QrScannerController(
    private val onQrCodeScanned: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    val view = UIView()
    private var captureSession: AVCaptureSession? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var isScanning = false

    init {
        setupCamera()
    }

    private fun setupCamera() {
        val session = AVCaptureSession()
        session.sessionPreset = AVCaptureSessionPresetHigh

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) return

        val input = try {
            AVCaptureDeviceInput.deviceInputWithDevice(device, null)
        } catch (e: Exception) {
            null
        }

        if (input == null || !session.canAddInput(input)) return
        session.addInput(input)

        val metadataOutput = AVCaptureMetadataOutput()
        if (!session.canAddOutput(metadataOutput)) return
        session.addOutput(metadataOutput)

        metadataOutput.setMetadataObjectsDelegate(this, dispatch_get_main_queue())
        metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

        val preview = AVCaptureVideoPreviewLayer(session = session)
        preview.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(preview)

        captureSession = session
        previewLayer = preview

        // Observe bounds changes to update preview layer
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString("updatePreviewLayerFrame"),
            name = "UIViewBoundsDidChange",
            `object` = view
        )
    }

    @ObjCAction
    fun updatePreviewLayerFrame() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer?.frame = view.bounds
        CATransaction.commit()
    }

    fun startScanning() {
        if (isScanning) return
        isScanning = true

        previewLayer?.frame = view.bounds
        captureSession?.startRunning()
    }

    fun stopScanning() {
        isScanning = false
        captureSession?.stopRunning()
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        val metadataObjects = didOutputMetadataObjects.filterIsInstance<AVMetadataMachineReadableCodeObject>()

        for (metadata in metadataObjects) {
            if (metadata.type == AVMetadataObjectTypeQRCode) {
                metadata.stringValue?.let { value ->
                    stopScanning()
                    onQrCodeScanned(value)
                    return
                }
            }
        }
    }
}
