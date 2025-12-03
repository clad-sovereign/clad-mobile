package tech.wideas.clad.ui.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen for scanning QR codes containing SS58 addresses.
 */
@Composable
fun QrCodeScannerScreen(
    error: String?,
    onQrCodeScanned: (String) -> Unit,
    onManualEntry: () -> Unit
) {
    var hasScanned by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Point your camera at a QR code",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // QR Scanner view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            QrCodeScanner(
                onQrCodeScanned = { content ->
                    if (!hasScanned) {
                        hasScanned = true
                        onQrCodeScanned(content)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning overlay with viewfinder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Semi-transparent overlay with cutout effect
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        // Error message
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Reset scan state when error shows so user can try again
            LaunchedEffect(error) {
                hasScanned = false
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Supported formats: SS58 address, substrate: URI",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Manual entry fallback
        OutlinedButton(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter Address Manually")
        }
    }
}
