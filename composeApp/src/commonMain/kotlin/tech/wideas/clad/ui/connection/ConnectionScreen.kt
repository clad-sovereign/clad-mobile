package tech.wideas.clad.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.wideas.clad.substrate.ConnectionState

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val authHandler = rememberBiometricAuthHandler()
    val scope = rememberCoroutineScope()

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
                text = "Sovereign Real-World Asset Issuance",
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

            // Single button that shows loading state via text and disabled state
            val buttonText = when {
                uiState.isAuthenticating -> "Authenticating..."
                uiState.connectionState is ConnectionState.Connecting -> "Connecting..."
                uiState.connectionState is ConnectionState.Connected -> "Connecting..."
                uiState.connectionState is ConnectionState.Error -> "Retry Connection"
                else -> "Connect to Node"
            }

            Button(
                onClick = {
                    scope.launch {
                        viewModel.authenticateAndConnect(authHandler)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.connectionState !is ConnectionState.Connecting &&
                         uiState.connectionState !is ConnectionState.Connected &&
                         !uiState.isAuthenticating
            ) {
                Text(buttonText)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "For finance ministries, debt offices and state issuers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
