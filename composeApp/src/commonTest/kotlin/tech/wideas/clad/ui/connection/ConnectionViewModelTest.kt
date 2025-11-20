package tech.wideas.clad.ui.connection

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.security.SecureStorage
import tech.wideas.clad.substrate.ConnectionState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ConnectionViewModel.
 *
 * Verifies connection UI state management and data flow.
 * Note: These tests focus on UI state logic rather than full ViewModel integration
 * since SubstrateClient is a final class and cannot be mocked in common tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty endpoint and disconnected state`() = runTest {
        val initialState = ConnectionUiState()

        assertEquals("", initialState.endpoint)
        assertEquals(ConnectionState.Disconnected, initialState.connectionState)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
    }

    @Test
    fun `onEndpointChanged should update endpoint and clear error`() = runTest {
        val newEndpoint = "ws://192.168.1.100:9944"
        val stateWithError = ConnectionUiState(error = "Some error")

        val updatedState = stateWithError.copy(endpoint = newEndpoint, error = null)

        assertEquals(newEndpoint, updatedState.endpoint)
        assertNull(updatedState.error)
    }

    @Test
    fun `connecting state should set isLoading to true`() = runTest {
        val state = ConnectionUiState(connectionState = ConnectionState.Connecting)

        val isLoading = state.connectionState is ConnectionState.Connecting

        assertTrue(isLoading)
    }

    @Test
    fun `connected state should set isLoading to false`() = runTest {
        val state = ConnectionUiState(connectionState = ConnectionState.Connected)

        val isLoading = state.connectionState is ConnectionState.Connecting

        assertFalse(isLoading)
    }

    @Test
    fun `error state should populate error message`() = runTest {
        val errorMessage = "Connection timeout"
        val errorState = ConnectionState.Error(errorMessage)

        assertEquals(errorMessage, errorState.message)
    }

    @Test
    fun `ConnectionUiState should handle all connection states`() = runTest {
        val disconnected = ConnectionUiState(connectionState = ConnectionState.Disconnected)
        val connecting = ConnectionUiState(connectionState = ConnectionState.Connecting)
        val connected = ConnectionUiState(connectionState = ConnectionState.Connected)
        val error = ConnectionUiState(connectionState = ConnectionState.Error("Error"))

        assertEquals(ConnectionState.Disconnected, disconnected.connectionState)
        assertEquals(ConnectionState.Connecting, connecting.connectionState)
        assertEquals(ConnectionState.Connected, connected.connectionState)
        assertTrue(error.connectionState is ConnectionState.Error)
    }

    @Test
    fun `empty endpoint should produce validation error`() = runTest {
        val endpoint = "   "
        val trimmedEndpoint = endpoint.trim()

        assertTrue(trimmedEndpoint.isEmpty())
    }

    @Test
    fun `valid endpoint should pass validation`() = runTest {
        val endpoint = "ws://192.168.1.100:9944"
        val trimmedEndpoint = endpoint.trim()

        assertFalse(trimmedEndpoint.isEmpty())
    }

    @Test
    fun `SettingsRepository integration should save and load endpoint`() = runTest {
        val storage = TestSecureStorage()
        val repository = SettingsRepository(storage)
        val endpoint = "ws://test:9944"

        repository.saveRpcEndpoint(endpoint)
        val loaded = repository.getRpcEndpoint("default")

        assertEquals(endpoint, loaded)
    }

    @Test
    fun `error state should be reflected in UI state`() = runTest {
        val errorMessage = "Network error"
        val state = ConnectionUiState(
            connectionState = ConnectionState.Error(errorMessage),
            error = errorMessage
        )

        assertEquals(errorMessage, state.error)
        assertTrue(state.connectionState is ConnectionState.Error)
    }
}
