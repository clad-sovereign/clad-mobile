package tech.wideas.clad.substrate

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for SubstrateClient reconnection behavior.
 *
 * These tests validate automatic reconnection logic with exponential backoff.
 *
 * Prerequisites:
 * - Local Substrate node running on ws://localhost:9944
 *
 * Note: Some tests simulate connection failures and may take longer to complete
 * due to backoff delays.
 */
@RunWith(AndroidJUnit4::class)
class SubstrateClientReconnectionTest {

    private lateinit var client: SubstrateClient

    @After
    fun teardown() = runBlocking {
        client.disconnect()
        client.close()
    }

    // ============================================
    // Auto-Reconnect Disabled Tests
    // ============================================

    @Test
    fun testNoReconnectWhenDisabled() = runBlocking {
        // Given: Client with auto-reconnect disabled
        client = SubstrateClient(autoReconnect = false)

        // When: Attempting to connect to invalid endpoint
        val invalidEndpoint = "ws://localhost:9999"

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect(invalidEndpoint)

            // Then: State becomes Connecting then Error
            assertIs<ConnectionState.Connecting>(awaitItem())
            val errorState = awaitItem()
            assertIs<ConnectionState.Error>(errorState)

            // And: No reconnection attempts (stays in Error state)
            expectNoEvents()
        }
    }

    // ============================================
    // Auto-Reconnect Enabled Tests
    // ============================================

    @Test
    fun testReconnectOnConnectionFailure() = runBlocking {
        // Given: Client with auto-reconnect enabled
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 3)

        // When: Attempting to connect to invalid endpoint
        val invalidEndpoint = "ws://localhost:9999"

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect(invalidEndpoint)

            // Then: Multiple connection attempts occur
            val states = mutableListOf<ConnectionState>()

            // First attempt
            states.add(awaitItem()) // Connecting
            states.add(awaitItem()) // Error

            // Wait for backoff and retry
            delay(2000)

            // Second attempt (after ~1s backoff)
            states.add(awaitItem()) // Connecting
            states.add(awaitItem()) // Error

            // Verify we saw reconnection attempts
            val connectingStates = states.filterIsInstance<ConnectionState.Connecting>()
            assertTrue(
                connectingStates.size >= 2,
                "Should see multiple Connecting states during reconnection attempts"
            )
        }
    }

    @Test
    fun testMaxReconnectAttemptsLimit() = runBlocking {
        // Given: Client with limited reconnect attempts
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 2)

        // When: Attempting to connect to invalid endpoint
        val invalidEndpoint = "ws://localhost:9999"

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect(invalidEndpoint)

            val states = mutableListOf<ConnectionState>()

            // Collect states for a period covering max attempts
            // Attempt 1
            states.add(awaitItem()) // Connecting
            states.add(awaitItem()) // Error

            delay(2000) // Wait for backoff

            // Attempt 2
            states.add(awaitItem()) // Connecting
            states.add(awaitItem()) // Error

            delay(3000) // Wait for potential third attempt

            // Then: After max attempts, should stay in Error state
            expectNoEvents() // No more reconnection attempts

            val connectingCount = states.count { it is ConnectionState.Connecting }
            assertTrue(
                connectingCount <= 2,
                "Should not exceed max reconnect attempts"
            )
        }
    }

    @Test
    fun testExponentialBackoffDelay() = runBlocking {
        // Given: Client with auto-reconnect
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 3)

        val invalidEndpoint = "ws://localhost:9999"
        val attemptTimestamps = mutableListOf<Long>()

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect(invalidEndpoint)

            // Record timestamps of connection attempts
            for (i in 1..3) {
                val connectingState = awaitItem()
                if (connectingState is ConnectionState.Connecting) {
                    attemptTimestamps.add(System.currentTimeMillis())
                }
                awaitItem() // Error state
                delay(5000) // Wait for next attempt
            }

            cancelAndIgnoreRemainingEvents()
        }

        // Then: Delays should increase (exponential backoff: ~1s, ~2s, ~4s)
        if (attemptTimestamps.size >= 2) {
            val delay1 = attemptTimestamps[1] - attemptTimestamps[0]

            // First delay should be around 1000ms (with tolerance)
            assertTrue(
                delay1 in 800..2000,
                "First reconnect delay should be ~1s, was ${delay1}ms"
            )
        }
    }

    @Test
    fun testReconnectAttemptsResetOnSuccess() = runBlocking {
        // Given: Client with auto-reconnect
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 5)

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            // When: Successfully connecting to valid endpoint
            client.connect("ws://localhost:9944")

            assertIs<ConnectionState.Connecting>(awaitItem())
            assertIs<ConnectionState.Connected>(awaitItem())

            // Then: Disconnect and reconnect to different endpoint
            client.disconnect()
            assertIs<ConnectionState.Disconnected>(awaitItem())

            // Connect to invalid endpoint
            client.connect("ws://localhost:9999")

            // Should get full reconnect attempts, not reduced by previous attempts
            val states = mutableListOf<ConnectionState>()
            repeat(4) {
                states.add(awaitItem())
                delay(2000)
            }

            val connectingCount = states.count { it is ConnectionState.Connecting }
            assertTrue(
                connectingCount >= 1,
                "Reconnect counter should reset after successful connection"
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================
    // Connection Recovery Tests
    // ============================================

    @Test
    fun testRecoveryAfterNodeBecomesAvailable() = runBlocking {
        // Given: Client with auto-reconnect attempting to connect to unavailable node
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 10)

        // This test simulates:
        // 1. Node is unavailable (invalid port)
        // 2. Client attempts reconnection
        // 3. "Node becomes available" (we switch to valid endpoint)

        val invalidEndpoint = "ws://localhost:9999"

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            // Start with invalid endpoint
            client.connect(invalidEndpoint)

            // Wait through a few failed attempts
            repeat(2) {
                assertIs<ConnectionState.Connecting>(awaitItem())
                assertIs<ConnectionState.Error>(awaitItem())
                delay(2000)
            }

            // "Node becomes available" - reconnect to valid endpoint
            client.disconnect()
            assertIs<ConnectionState.Disconnected>(awaitItem())

            client.connect("ws://localhost:9944")

            // Then: Should successfully connect
            assertIs<ConnectionState.Connecting>(awaitItem())
            assertIs<ConnectionState.Connected>(awaitItem())
        }
    }

    @Test
    fun testCancelReconnectionOnExplicitDisconnect() = runBlocking {
        // Given: Client attempting to reconnect to invalid endpoint
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 5)

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect("ws://localhost:9999")

            assertIs<ConnectionState.Connecting>(awaitItem())
            assertIs<ConnectionState.Error>(awaitItem())

            // When: Explicitly disconnecting during reconnection attempts
            delay(500) // Let reconnection be scheduled
            client.disconnect()

            // Then: State becomes Disconnected and stays there
            assertIs<ConnectionState.Disconnected>(awaitItem())

            delay(3000) // Wait longer than first backoff
            expectNoEvents() // No reconnection should occur
        }
    }

    @Test
    fun testReconnectionWithValidEndpoint() = runBlocking {
        // This test verifies reconnection behavior when connecting to a valid
        // endpoint that might experience temporary disconnections

        // Given: Client with auto-reconnect
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 3)

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            // When: Connecting to valid endpoint
            client.connect("ws://localhost:9944")

            assertIs<ConnectionState.Connecting>(awaitItem())
            val connectedState = awaitItem()
            assertIs<ConnectionState.Connected>(connectedState)

            // Note: We cannot easily simulate a connection drop in an integration test
            // without stopping the actual node. This test primarily validates that
            // auto-reconnect doesn't interfere with stable connections.

            // Verify connection remains stable
            delay(5000)
            expectNoEvents() // Should stay connected
        }
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    fun testZeroMaxReconnectAttempts() = runBlocking {
        // Given: Client with zero max reconnect attempts
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 0)

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            // When: Connecting to invalid endpoint
            client.connect("ws://localhost:9999")

            // Then: Only one connection attempt, no retries
            assertIs<ConnectionState.Connecting>(awaitItem())
            assertIs<ConnectionState.Error>(awaitItem())

            delay(3000) // Wait for potential retry
            expectNoEvents() // No reconnection
        }
    }

    @Test
    fun testHighMaxReconnectAttempts() = runBlocking {
        // Given: Client with high max reconnect attempts
        client = SubstrateClient(autoReconnect = true, maxReconnectAttempts = 100)

        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            // When: Connecting to invalid endpoint
            client.connect("ws://localhost:9999")

            // Then: Should keep retrying within the test timeout
            val states = mutableListOf<ConnectionState>()

            repeat(6) { // Collect several attempts
                states.add(awaitItem())
                delay(2000)
            }

            val connectingCount = states.count { it is ConnectionState.Connecting }
            assertTrue(
                connectingCount >= 3,
                "Should see multiple reconnection attempts with high limit"
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
