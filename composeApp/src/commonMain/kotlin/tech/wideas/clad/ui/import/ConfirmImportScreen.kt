package tech.wideas.clad.ui.import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.wideas.clad.crypto.KeyType

/**
 * Screen to confirm import and set account label.
 */
@Composable
fun ConfirmImportScreen(
    address: String,
    isWatchOnly: Boolean,
    label: String,
    keyType: KeyType,
    onLabelChanged: (String) -> Unit,
    onKeyTypeChanged: (KeyType) -> Unit,
    onConfirm: () -> Unit,
    canConfirm: Boolean,
    error: String?
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Confirm Import",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Account preview card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isWatchOnly) "Watch-only Account" else "Full Access Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isWatchOnly) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Address",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show address with ellipsis in middle for long addresses
                Text(
                    text = formatAddressForDisplay(address),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isWatchOnly) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Key Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = keyType.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account label input
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account Label") },
            placeholder = { Text("e.g., Treasury Main") },
            supportingText = { Text("Give this account a name for easy identification") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
        )

        // Key type selector (only for seed phrase imports)
        if (!isWatchOnly) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Key Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = keyType == KeyType.SR25519,
                    onClick = { onKeyTypeChanged(KeyType.SR25519) },
                    label = { Text("SR25519") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = keyType == KeyType.ED25519,
                    onClick = { onKeyTypeChanged(KeyType.ED25519) },
                    label = { Text("ED25519") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (keyType == KeyType.SR25519) {
                    "SR25519 is recommended for Substrate chains (supports derivation)"
                } else {
                    "ED25519 is widely compatible but doesn't support soft derivation"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error message
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Security notice for full access accounts
        if (!isWatchOnly) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your private key will be secured with biometric authentication",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = canConfirm
        ) {
            Text(if (isWatchOnly) "Import Account" else "Secure & Import")
        }
    }
}

/**
 * Format address for display with ellipsis in the middle.
 */
private fun formatAddressForDisplay(address: String): String {
    return if (address.length > 20) {
        "${address.take(12)}...${address.takeLast(12)}"
    } else {
        address
    }
}
