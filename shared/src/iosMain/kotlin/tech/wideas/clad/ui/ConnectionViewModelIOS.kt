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

/**
 * iOS-specific ConnectionViewModel
 * Mirrors the Android ConnectionViewModel but lives in the shared module
 */
data class ConnectionUiState(
    val endpoint: String = "",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConnectionViewModelIOS(
    private val substrateClient: SubstrateClient,
    private val settingsRepository: SettingsRepository
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
                    error = if (state is ConnectionState.Error) state.message else null
                )
            }
        }
    }

    fun onEndpointChanged(endpoint: String) {
        _uiState.value = _uiState.value.copy(endpoint = endpoint, error = null)
    }

    fun connect() {
        val endpoint = _uiState.value.endpoint.trim()

        // Validate endpoint format
        val validationError = validateEndpoint(endpoint)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        scope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                settingsRepository.saveRpcEndpoint(endpoint)
                substrateClient.connect(endpoint)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Connection failed"
                )
            }
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
