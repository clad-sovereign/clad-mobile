package tech.wideas.clad.ui.import

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific QR code scanner composable.
 *
 * Platform implementations:
 * - Android: CameraX with ML Kit barcode scanning
 * - iOS: AVFoundation camera with Vision framework
 *
 * @param onQrCodeScanned Callback when a QR code is successfully scanned
 * @param modifier Modifier for the scanner view
 */
@Composable
expect fun QrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
)
