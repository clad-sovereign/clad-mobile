package tech.wideas.clad.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.wideas.clad.substrate.ConnectionState

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onConnected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to accounts screen when connected
    // LaunchedEffect with isConnected key ensures this only runs when connection state changes to Connected
    androidx.compose.runtime.LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState is ConnectionState.Connected) {
            onConnected()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CLAD Signer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sovereign Bond Tokenization",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = uiState.endpoint,
                onValueChange = viewModel::onEndpointChanged,
                label = { Text("Node Endpoint") },
                placeholder = { Text("ws://127.0.0.1:9944") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = uiState.error != null,
                singleLine = true
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState.connectionState) {
                is ConnectionState.Disconnected -> {
                    Button(
                        onClick = viewModel::connect,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Connect to Node")
                    }
                }
                is ConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting...", style = MaterialTheme.typography.bodyMedium)
                }
                is ConnectionState.Connected -> {
                    // Will navigate away
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
                is ConnectionState.Error -> {
                    Button(
                        onClick = viewModel::connect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry Connection")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "For Ministry and Debt Office Officials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
