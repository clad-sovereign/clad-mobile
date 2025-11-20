package tech.wideas.clad.substrate

import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for SubstrateClient.
 *
 * These tests verify the core functionality of the Substrate RPC client
 * including connection state management and RPC request/response handling.
 */
class SubstrateClientTest {

    @Test
    fun `initial connection state should be Disconnected`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = SubstrateClient(testScope)

        client.connectionState.test {
            val state = awaitItem()
            assertIs<ConnectionState.Disconnected>(state)
        }

        client.close()
    }

    @Test
    fun `initial metadata should be null`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = SubstrateClient(testScope)

        client.metadata.test {
            val metadata = awaitItem()
            assertNull(metadata)
        }

        client.close()
    }

    @Test
    fun `disconnect should reset connection state to Disconnected`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = SubstrateClient(testScope)

        client.disconnect()

        client.connectionState.test {
            val state = awaitItem()
            assertIs<ConnectionState.Disconnected>(state)
        }

        client.close()
    }

    @Test
    fun `disconnect should clear metadata`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = SubstrateClient(testScope)

        client.disconnect()

        client.metadata.test {
            val metadata = awaitItem()
            assertNull(metadata)
        }

        client.close()
    }

    @Test
    fun `RpcRequest should serialize with correct jsonrpc version`() {
        val request = RpcRequest(id = 1, method = "test_method")

        assertEquals("2.0", request.jsonrpc)
        assertEquals(1, request.id)
        assertEquals("test_method", request.method)
    }

    @Test
    fun `RpcRequest should handle empty params array`() {
        val request = RpcRequest(id = 1, method = "test_method")

        assertEquals(JsonArray(emptyList()), request.params)
    }

    @Test
    fun `RpcRequest should handle params with values`() {
        val params = JsonArray(listOf(JsonPrimitive("param1"), JsonPrimitive(123)))
        val request = RpcRequest(id = 1, method = "test_method", params = params)

        assertEquals(params, request.params)
    }

    @Test
    fun `RpcResponse should represent successful response`() {
        val response = RpcResponse(
            jsonrpc = "2.0",
            id = 1,
            result = JsonPrimitive("success")
        )

        assertEquals("2.0", response.jsonrpc)
        assertEquals(1, response.id)
        assertIs<JsonPrimitive>(response.result)
        assertNull(response.error)
    }

    @Test
    fun `RpcResponse should represent error response`() {
        val errorJson = JsonPrimitive("Error message")
        val response = RpcResponse(
            jsonrpc = "2.0",
            id = 1,
            error = errorJson
        )

        assertEquals("2.0", response.jsonrpc)
        assertEquals(1, response.id)
        assertNull(response.result)
        assertIs<JsonPrimitive>(response.error)
    }

    @Test
    fun `ConnectionState sealed class should cover all states`() {
        val states = listOf(
            ConnectionState.Disconnected,
            ConnectionState.Connecting,
            ConnectionState.Connected,
            ConnectionState.Error("Test error")
        )

        // Verify we can handle all cases in a when expression
        states.forEach { state ->
            val handled = when (state) {
                is ConnectionState.Disconnected -> true
                is ConnectionState.Connecting -> true
                is ConnectionState.Connected -> true
                is ConnectionState.Error -> true
            }
            assertEquals(true, handled, "All ConnectionState cases should be handled")
        }
    }

    @Test
    fun `ConnectionState Error should contain error message`() {
        val errorMessage = "Connection timeout"
        val errorState = ConnectionState.Error(errorMessage)

        assertEquals(errorMessage, errorState.message)
    }

    @Test
    fun `SubstrateException should be throwable`() {
        val exception = SubstrateException("Test error")

        assertEquals("Test error", exception.message)
        assertIs<Exception>(exception)
    }

    @Test
    fun `close should clean up resources`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = SubstrateClient(testScope)

        // Should not throw
        client.close()
    }
}
