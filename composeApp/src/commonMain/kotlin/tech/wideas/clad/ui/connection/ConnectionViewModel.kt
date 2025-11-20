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

/**
 * Represents the current phase of the connection flow
 */
sealed class ConnectionFlowState {
    data object Idle : ConnectionFlowState()
    data object Authenticating : ConnectionFlowState()
    data object Connecting : ConnectionFlowState()
    data object Connected : ConnectionFlowState()
    data class Error(val message: String) : ConnectionFlowState()
}

data class ConnectionUiState(
    val endpoint: String = "",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val flowState: ConnectionFlowState = ConnectionFlowState.Idle,
    val error: String? = null,
    val authenticationRequired: Boolean = true // Require auth before connection
) {
    // Derived properties for UI convenience
    val isAuthenticating: Boolean
        get() = flowState is ConnectionFlowState.Authenticating

    val isLoading: Boolean
        get() = flowState is ConnectionFlowState.Connecting || flowState is ConnectionFlowState.Connected
}

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
                val flowState = when (state) {
                    is ConnectionState.Connecting -> ConnectionFlowState.Connecting
                    is ConnectionState.Connected -> ConnectionFlowState.Connected
                    is ConnectionState.Error -> ConnectionFlowState.Error(state.message)
                    is ConnectionState.Disconnected -> {
                        // Only return to idle if we're not authenticating
                        if (_uiState.value.flowState !is ConnectionFlowState.Authenticating) {
                            ConnectionFlowState.Idle
                        } else {
                            _uiState.value.flowState
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    flowState = flowState,
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
            _uiState.value = _uiState.value.copy(
                flowState = ConnectionFlowState.Error(validationError),
                error = validationError
            )
            return
        }

        // Trigger biometric authentication
        _uiState.value = _uiState.value.copy(
            flowState = ConnectionFlowState.Authenticating,
            error = null
        )

        val authResult = authHandler.authenticate(
            title = "Authenticate to Connect",
            subtitle = "CLAD Signer",
            description = "Verify your identity to connect to the blockchain node"
        )

        when (authResult) {
            is tech.wideas.clad.security.BiometricResult.Success -> {
                // Authentication succeeded, proceed with connection
                // flowState will be updated to Connecting by connectToNode
                connectToNode(endpoint)
            }
            is tech.wideas.clad.security.BiometricResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(
                    flowState = ConnectionFlowState.Idle,
                    error = null // Don't show error for user cancellation
                )
            }
            is tech.wideas.clad.security.BiometricResult.NotAvailable -> {
                val errorMessage = "Biometric authentication is not available on this device"
                _uiState.value = _uiState.value.copy(
                    flowState = ConnectionFlowState.Error(errorMessage),
                    error = errorMessage
                )
            }
            is tech.wideas.clad.security.BiometricResult.Error -> {
                val errorMessage = "Authentication failed: ${authResult.message}"
                _uiState.value = _uiState.value.copy(
                    flowState = ConnectionFlowState.Error(errorMessage),
                    error = errorMessage
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
            // Transition from Authenticating to Connecting
            _uiState.value = _uiState.value.copy(
                flowState = ConnectionFlowState.Connecting,
                error = null
            )
            settingsRepository.saveRpcEndpoint(endpoint)
            substrateClient.connect(endpoint)
            // Note: flowState will be updated to Connected by observeConnectionState
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Connection failed"
            _uiState.value = _uiState.value.copy(
                flowState = ConnectionFlowState.Error(errorMessage),
                error = errorMessage
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
