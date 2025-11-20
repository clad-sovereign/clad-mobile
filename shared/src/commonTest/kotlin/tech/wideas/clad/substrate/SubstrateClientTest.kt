package tech.wideas.clad.substrate

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for SubstrateClient basic behavior.
 *
 * These are lightweight tests that don't require network connectivity.
 * They test the client's state management and basic API without making real network calls.
 *
 * For full integration tests against a real Substrate node, see:
 * - androidInstrumentedTest/SubstrateClientIntegrationTest.kt
 * - androidInstrumentedTest/SubstrateClientRpcTest.kt
 * - androidInstrumentedTest/SubstrateClientReconnectionTest.kt
 * - androidInstrumentedTest/SubstrateClientConcurrencyTest.kt
 */
class SubstrateClientTest {

    @Test
    fun testInitialState() = runTest {
        // Given: A new SubstrateClient
        val client = SubstrateClient(autoReconnect = false)

        // Then: Initial state is Disconnected
        client.connectionState.test {
            val initialState = awaitItem()
            assertIs<ConnectionState.Disconnected>(initialState)
            cancelAndIgnoreRemainingEvents()
        }

        client.close()
    }

    @Test
    fun testInitialMetadataIsNull() = runTest {
        // Given: A new SubstrateClient
        val client = SubstrateClient(autoReconnect = false)

        // Then: Initial metadata is null
        client.metadata.test {
            val initialMetadata = awaitItem()
            assertEquals(null, initialMetadata)
            cancelAndIgnoreRemainingEvents()
        }

        client.close()
    }

    @Test
    fun testAutoReconnectConfiguration() = runTest {
        // Given: Clients with different auto-reconnect settings
        val clientWithAutoReconnect = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 5)
        val clientWithoutAutoReconnect = SubstrateClient(autoReconnect = false)

        // Then: Both clients initialize successfully
        assertNotNull(clientWithAutoReconnect)
        assertNotNull(clientWithoutAutoReconnect)

        clientWithAutoReconnect.close()
        clientWithoutAutoReconnect.close()
    }

    @Test
    fun testDisconnectWhenNotConnected() = runTest(timeout = 10.seconds) {
        // Given: A disconnected client
        val client = SubstrateClient(autoReconnect = false)

        client.connectionState.test {
            assertIs<ConnectionState.Disconnected>(awaitItem())

            // When: Calling disconnect on already disconnected client
            client.disconnect()

            // Then: Should remain in Disconnected state (idempotent)
            expectNoEvents()
        }

        client.close()
    }

    @Test
    fun testCloseClient() = runTest {
        // Given: A client
        val client = SubstrateClient(autoReconnect = false)

        // When: Closing the client
        client.close()

        // Then: No exception is thrown
        // (Verifies cleanup happens gracefully)
    }

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testGetChainPropertiesThrowsWhenNotConnected() = runTest {
        // Given: A disconnected client
        val client = SubstrateClient(autoReconnect = false)

        try {
            // When: Attempting to get chain properties while disconnected
            // Then: Should throw SubstrateException
            val exception = kotlin.runCatching {
                client.getChainProperties()
            }

            // Verify it failed (we expect failure when not connected)
            assert(exception.isFailure) {
                "getChainProperties should fail when not connected"
            }
        } finally {
            client.close()
        }
    }

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testCallThrowsWhenNotConnected() = runTest {
        // Given: A disconnected client
        val client = SubstrateClient(autoReconnect = false)

        try {
            // When: Attempting to make RPC call while disconnected
            // Then: Should throw SubstrateException
            val exception = kotlin.runCatching {
                client.call("system_chain")
            }

            // Verify it failed (we expect failure when not connected)
            assert(exception.isFailure) {
                "RPC call should fail when not connected"
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun testMultipleCloseCallsAreSafe() = runTest {
        // Given: A client
        val client = SubstrateClient(autoReconnect = false)

        // When: Calling close multiple times
        client.close()
        client.close()
        client.close()

        // Then: No exception is thrown (close is idempotent)
    }

    @Test
    fun testClientCreationWithDifferentMaxReconnectAttempts() = runTest {
        // Test various max reconnect attempt values
        val clients = listOf(
            SubstrateClient(autoReconnect = true, maxReconnectAttempts = 0),
            SubstrateClient(autoReconnect = true, maxReconnectAttempts = 1),
            SubstrateClient(autoReconnect = true, maxReconnectAttempts = 10),
            SubstrateClient(autoReconnect = true, maxReconnectAttempts = 100)
        )

        // All should initialize successfully
        clients.forEach { client ->
            assertNotNull(client)
            client.close()
        }
    }
}
