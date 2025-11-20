package tech.wideas.clad.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.getPlatform
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient

data class ConnectionUiState(
    val endpoint: String = "",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val authenticationRequired: Boolean = true // Require auth before connection
)

class ConnectionViewModel(
    private val substrateClient: SubstrateClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        loadSavedEndpoint()
        observeConnectionState()
    }

    private fun loadSavedEndpoint() {
        viewModelScope.launch {
            val defaultEndpoint = getPlatform().defaultRpcEndpoint
            val savedEndpoint = settingsRepository.getRpcEndpoint(defaultEndpoint)
            _uiState.value = _uiState.value.copy(endpoint = savedEndpoint)
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            substrateClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isLoading = state is ConnectionState.Connecting,
                    error = if (state is ConnectionState.Error) state.message else null
                )
            }
        }
    }

    fun onEndpointChanged(endpoint: String) {
        _uiState.value = _uiState.value.copy(endpoint = endpoint, error = null)
    }

    /**
     * Authenticate user and connect to node
     * This method should be called with a BiometricAuthHandler
     */
    suspend fun authenticateAndConnect(authHandler: BiometricAuthHandler) {
        val endpoint = _uiState.value.endpoint.trim()

        // Validate endpoint format
        val validationError = validateEndpoint(endpoint)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        // Trigger biometric authentication
        _uiState.value = _uiState.value.copy(isAuthenticating = true, error = null)

        val authResult = authHandler.authenticate(
            title = "Authenticate to Connect",
            subtitle = "CLAD Signer",
            description = "Verify your identity to connect to the blockchain node"
        )

        when (authResult) {
            is tech.wideas.clad.security.BiometricResult.Success -> {
                // Authentication succeeded, proceed with connection
                connectToNode(endpoint)
            }
            is tech.wideas.clad.security.BiometricResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    error = null // Don't show error for user cancellation
                )
            }
            is tech.wideas.clad.security.BiometricResult.NotAvailable -> {
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    error = "Biometric authentication is not available on this device"
                )
            }
            is tech.wideas.clad.security.BiometricResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isAuthenticating = false,
                    error = "Authentication failed: ${authResult.message}"
                )
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

        viewModelScope.launch {
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
        viewModelScope.launch {
            substrateClient.disconnect()
        }
    }
}
