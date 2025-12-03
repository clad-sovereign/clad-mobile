package tech.wideas.clad.ui.import

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen for selecting the import method.
 */
@Composable
fun ImportMethodSelection(
    onMethodSelected: (ImportMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "How would you like to import?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Seed Phrase option
        ImportMethodCard(
            title = "Seed Phrase",
            description = "Import using your 12 or 24 word recovery phrase",
            onClick = { onMethodSelected(ImportMethod.SEED_PHRASE) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // QR Code option
        ImportMethodCard(
            title = "QR Code",
            description = "Scan a QR code containing an address",
            onClick = { onMethodSelected(ImportMethod.QR_CODE) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Address option
        ImportMethodCard(
            title = "Manual Entry",
            description = "Enter an SS58 address for watch-only tracking",
            onClick = { onMethodSelected(ImportMethod.MANUAL_ADDRESS) }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Seed phrase import gives full signing capability.\nQR and manual imports are watch-only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ImportMethodCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
