package tech.wideas.clad.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.getPlatform
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.BiometricResult

/**
 * iOS-specific ConnectionViewModel
 * Mirrors the Android ConnectionViewModel but lives in the shared module
 */
data class ConnectionUiState(
    val endpoint: String = "",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val authenticationRequired: Boolean = true // Require auth before connection
)

class ConnectionViewModelIOS(
    private val substrateClient: SubstrateClient,
    private val settingsRepository: SettingsRepository,
    private val biometricAuth: BiometricAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        loadSavedEndpoint()
        observeConnectionState()
    }

    private fun loadSavedEndpoint() {
        scope.launch {
            val defaultEndpoint = getPlatform().defaultRpcEndpoint
            val savedEndpoint = settingsRepository.getRpcEndpoint(defaultEndpoint)
            _uiState.value = _uiState.value.copy(endpoint = savedEndpoint)
        }
    }

    private fun observeConnectionState() {
        scope.launch {
            substrateClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isLoading = state is ConnectionState.Connecting,
                    error = if (state is ConnectionState.Error) formatErrorMessage(state.message) else null
                )
            }
        }
    }

    private fun formatErrorMessage(rawMessage: String): String {
        // Extract clean error message from raw exception text
        // Example: "Failed to connect to /127.0.0.1:9944" from full stack trace

        // Check if it contains "Could not connect to the server"
        if (rawMessage.contains("Could not connect to the server", ignoreCase = true)) {
            // Extract the endpoint from the error
            val endpoint = _uiState.value.endpoint
            return "Failed to connect to $endpoint"
        }

        // Check for connection refused
        if (rawMessage.contains("Connection refused", ignoreCase = true)) {
            val endpoint = _uiState.value.endpoint
            return "Failed to connect to $endpoint"
        }

        // Check for timeout
        if (rawMessage.contains("timeout", ignoreCase = true)) {
            val endpoint = _uiState.value.endpoint
            return "Connection timeout to $endpoint"
        }

        // Default: try to extract first line or first meaningful part
        val firstLine = rawMessage.lines().firstOrNull { it.isNotBlank() } ?: rawMessage
        return firstLine.take(100) // Limit length to 100 chars
    }

    fun onEndpointChanged(endpoint: String) {
        _uiState.value = _uiState.value.copy(endpoint = endpoint, error = null)
    }

    /**
     * Authenticate user and connect to node
     */
    fun authenticateAndConnect() {
        val endpoint = _uiState.value.endpoint.trim()

        // Validate endpoint format
        val validationError = validateEndpoint(endpoint)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        scope.launch {
            // Trigger biometric authentication
            _uiState.value = _uiState.value.copy(isAuthenticating = true, error = null)

            val authResult = biometricAuth.authenticate(
                title = "Authenticate to Connect",
                subtitle = "CLAD Signer",
                description = "Verify your identity to connect to the blockchain node"
            )

            when (authResult) {
                is BiometricResult.Success -> {
                    // Authentication succeeded, proceed with connection
                    connectToNode(endpoint)
                }
                is BiometricResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        error = null // Don't show error for user cancellation
                    )
                }
                is BiometricResult.NotAvailable -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        error = "Biometric authentication is not available on this device"
                    )
                }
                is BiometricResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticating = false,
                        error = "Authentication failed: ${authResult.message}"
                    )
                }
            }
        }
    }

    /**
     * Legacy connect method (kept for backwards compatibility)
     * Connects without biometric authentication
     */
    fun connect() {
        val endpoint = _uiState.value.endpoint.trim()

        // Validate endpoint format
        val validationError = validateEndpoint(endpoint)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        scope.launch {
            connectToNode(endpoint)
        }
    }

    private suspend fun connectToNode(endpoint: String) {
        try {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isAuthenticating = false,
                error = null
            )
            settingsRepository.saveRpcEndpoint(endpoint)
            substrateClient.connect(endpoint)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticating = false,
                error = e.message ?: "Connection failed"
            )
        }
    }

    private fun validateEndpoint(endpoint: String): String? {
        if (endpoint.isEmpty()) {
            return "Please enter an endpoint"
        }

        if (!endpoint.startsWith("ws://") && !endpoint.startsWith("wss://")) {
            return "Endpoint must start with ws:// or wss://"
        }

        // Basic URL validation
        val urlPattern = Regex("^wss?://[\\w\\-.]+(:\\d+)?(/.*)?$")
        if (!urlPattern.matches(endpoint)) {
            return "Invalid endpoint format"
        }

        return null
    }

    fun disconnect() {
        scope.launch {
            substrateClient.disconnect()
        }
    }
}
