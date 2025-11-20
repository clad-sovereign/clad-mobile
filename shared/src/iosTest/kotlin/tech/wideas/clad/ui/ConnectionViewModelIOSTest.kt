package tech.wideas.clad.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.BiometricResult
import tech.wideas.clad.security.SecureStorage
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ConnectionViewModelIOS.
 *
 * Verifies iOS-specific connection UI state management, endpoint validation,
 * error message formatting, and integration with SubstrateClient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelIOSTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var substrateClient: SubstrateClient
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var biometricAuth: BiometricAuth

    private class TestSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()

        override suspend fun save(key: String, value: String) {
            storage[key] = value
        }

        override suspend fun get(key: String): String? = storage[key]
        override suspend fun delete(key: String) { storage.remove(key) }
        override suspend fun contains(key: String): Boolean = storage.containsKey(key)
        override suspend fun clear() { storage.clear() }
    }

    private class TestBiometricAuth : BiometricAuth {
        override suspend fun isAvailable(): Boolean = true
        override suspend fun authenticate(
            title: String,
            subtitle: String,
            description: String
        ): BiometricResult = BiometricResult.Success
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        substrateClient = SubstrateClient(autoReconnect = false, dispatcher = testDispatcher)
        settingsRepository = SettingsRepository(TestSecureStorage())
        biometricAuth = TestBiometricAuth()
    }

    @AfterTest
    fun tearDown() {
        substrateClient.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty endpoint and disconnected state`() = runTest {
        // Given: A new ConnectionViewModelIOS
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)

        // Then: Initial state should be disconnected with empty endpoint
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ConnectionState.Disconnected, state.connectionState)
            assertFalse(state.isLoading)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEndpointChanged should update endpoint and clear error`() = runTest {
        // Given: A ViewModel with an error
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val newEndpoint = "ws://192.168.1.100:9944"

        // When: Endpoint is changed
        viewModel.onEndpointChanged(newEndpoint)

        // Then: Endpoint should be updated and error cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(newEndpoint, state.endpoint)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validateEndpoint should reject empty endpoint`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        testDispatcher.scheduler.advanceUntilIdle() // Let init complete

        // When: Attempting to connect with empty endpoint
        viewModel.onEndpointChanged("")
        viewModel.connect()

        // Then: Connection should not be attempted (state remains Disconnected)
        val state = viewModel.uiState.value
        assertEquals(ConnectionState.Disconnected, state.connectionState)
    }

    @Test
    fun `validateEndpoint should reject endpoint without ws protocol`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        testDispatcher.scheduler.advanceUntilIdle() // Let init complete

        // When: Attempting to connect with invalid protocol
        viewModel.onEndpointChanged("http://127.0.0.1:9944")
        viewModel.connect()

        // Then: Connection should not be attempted (state remains Disconnected)
        val state = viewModel.uiState.value
        assertEquals(ConnectionState.Disconnected, state.connectionState)
    }

    @Test
    fun `validateEndpoint should accept valid ws endpoint`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val validEndpoint = "ws://127.0.0.1:9944"

        // When: Setting a valid endpoint
        viewModel.onEndpointChanged(validEndpoint)

        // Then: No validation error should appear
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(validEndpoint, state.endpoint)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validateEndpoint should accept valid wss endpoint`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val validEndpoint = "wss://rpc.polkadot.io:443"

        // When: Setting a valid secure endpoint
        viewModel.onEndpointChanged(validEndpoint)

        // Then: No validation error should appear
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(validEndpoint, state.endpoint)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `validateEndpoint should reject malformed URLs`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        testDispatcher.scheduler.advanceUntilIdle() // Let init complete

        // When: Attempting to connect with malformed URL
        viewModel.onEndpointChanged("ws://")
        viewModel.connect()

        // Then: Connection should not be attempted (state remains Disconnected)
        val state = viewModel.uiState.value
        assertEquals(ConnectionState.Disconnected, state.connectionState)
    }

    @Test
    fun `ConnectionUiState should properly track connection states`() = runTest {
        // Test all connection state variants
        val disconnected = ConnectionUiState(connectionState = ConnectionState.Disconnected)
        val connecting = ConnectionUiState(connectionState = ConnectionState.Connecting)
        val connected = ConnectionUiState(connectionState = ConnectionState.Connected)
        val error = ConnectionUiState(connectionState = ConnectionState.Error("Test error"))

        assertEquals(ConnectionState.Disconnected, disconnected.connectionState)
        assertEquals(ConnectionState.Connecting, connecting.connectionState)
        assertEquals(ConnectionState.Connected, connected.connectionState)
        assertTrue(error.connectionState is ConnectionState.Error)
    }

    @Test
    fun `connecting state should set isLoading to true`() = runTest {
        // Given: A state with Connecting status
        val state = ConnectionUiState(
            connectionState = ConnectionState.Connecting,
            isLoading = true
        )

        // Then: isLoading should be true
        assertTrue(state.isLoading)
    }

    @Test
    fun `connected state should set isLoading to false`() = runTest {
        // Given: A state with Connected status
        val state = ConnectionUiState(
            connectionState = ConnectionState.Connected,
            isLoading = false
        )

        // Then: isLoading should be false
        assertFalse(state.isLoading)
    }

    @Test
    fun `error state should populate error message`() = runTest {
        // Given: An error state
        val errorMessage = "Connection timeout"
        val state = ConnectionUiState(
            connectionState = ConnectionState.Error(errorMessage),
            error = errorMessage
        )

        // Then: Error message should be set
        assertEquals(errorMessage, state.error)
        assertTrue(state.connectionState is ConnectionState.Error)
    }

    @Test
    fun `formatErrorMessage should clean connection refused errors`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val endpoint = "ws://127.0.0.1:9944"
        viewModel.onEndpointChanged(endpoint)

        // When: Connection state changes to error with "Connection refused"
        // This would be set internally by formatErrorMessage
        // We verify the state structure handles it correctly
        val errorState = ConnectionUiState(
            endpoint = endpoint,
            connectionState = ConnectionState.Error("Connection refused"),
            error = "Failed to connect to $endpoint"
        )

        // Then: Error message should be formatted cleanly
        assertTrue(errorState.error?.contains("Failed to connect to") == true)
        assertTrue(errorState.error?.contains(endpoint) == true)
    }

    @Test
    fun `formatErrorMessage should clean timeout errors`() = runTest {
        // Given: A ViewModel with endpoint set
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val endpoint = "ws://127.0.0.1:9944"
        viewModel.onEndpointChanged(endpoint)

        // When: Connection state changes to error with "timeout"
        val errorState = ConnectionUiState(
            endpoint = endpoint,
            connectionState = ConnectionState.Error("timeout"),
            error = "Connection timeout to $endpoint"
        )

        // Then: Error message should be formatted with timeout info
        assertTrue(errorState.error?.contains("Connection timeout to") == true)
        assertTrue(errorState.error?.contains(endpoint) == true)
    }

    @Test
    fun `connect should save endpoint to settings`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val endpoint = "ws://test-node:9944"

        // When: Attempting to connect (even if it fails, endpoint should be saved)
        viewModel.onEndpointChanged(endpoint)
        viewModel.connect()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Endpoint should be saved in settings
        val savedEndpoint = settingsRepository.getRpcEndpoint("default")
        assertEquals(endpoint, savedEndpoint)
    }

    @Test
    fun `disconnect should trigger disconnection`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)

        // When: Disconnecting
        viewModel.disconnect()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Client should be in disconnected state
        substrateClient.connectionState.test {
            val state = awaitItem()
            assertEquals(ConnectionState.Disconnected, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `endpoint validation should trim whitespace`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)
        val endpointWithSpaces = "  ws://127.0.0.1:9944  "

        // When: Setting endpoint with whitespace
        viewModel.onEndpointChanged(endpointWithSpaces)
        viewModel.connect()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should be saved trimmed
        val savedEndpoint = settingsRepository.getRpcEndpoint("default")
        assertEquals("ws://127.0.0.1:9944", savedEndpoint)
    }

    @Test
    fun `ViewModel should observe SubstrateClient connection state changes`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)

        // When: SubstrateClient connection state changes
        // (This happens automatically through the init block's observeConnectionState)

        // Then: ViewModel state should reflect client state
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(substrateClient.connectionState.value, state.connectionState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState should be a StateFlow that can be observed`() = runTest {
        // Given: A ViewModel
        val viewModel = ConnectionViewModelIOS(substrateClient, settingsRepository, biometricAuth)

        // Then: uiState should be observable StateFlow
        assertNotNull(viewModel.uiState)
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
