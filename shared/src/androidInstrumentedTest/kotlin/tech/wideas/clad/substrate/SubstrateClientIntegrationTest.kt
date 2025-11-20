package tech.wideas.clad.substrate

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for SubstrateClient.
 *
 * These tests require a local Substrate node running on ws://localhost:9944
 *
 * To run these tests:
 * 1. Start a local Substrate node (e.g., ./target/release/clad-node --dev --chain local --alice --port 30334 --rpc-port 9944)
 * 2. Run: ./gradlew :shared:connectedAndroidTest
 *
 * Note: These tests will be skipped if no node is running on the expected port.
 */
@RunWith(AndroidJUnit4::class)
class SubstrateClientIntegrationTest {

    private lateinit var client: SubstrateClient

    // Test endpoints - use 10.0.2.2 to reach host machine from Android emulator
    // For physical devices, use adb reverse tcp:9944 tcp:9944 or your machine's IP
    private val primaryEndpoint = "ws://localhost:9944"   // alice
    private val secondaryEndpoint = "ws://localhost:9945" // bob

    @Before
    fun setup() {
        // Use Dispatchers.IO for real WebSocket I/O operations
        client = SubstrateClient(
            autoReconnect = false,
            dispatcher = Dispatchers.IO
        )
    }

    @After
    fun teardown(): Unit = runBlocking {
        client.disconnect()
        client.close()
    }

    // ============================================
    // Connection Tests
    // ============================================

    @Test
    fun testConnectToLocalNode() = runBlocking {
        // Given: A disconnected client
        client.connectionState.test {
            assertIs<ConnectionState.Disconnected>(awaitItem())

            // When: Connecting to local node
            client.connect(primaryEndpoint)

            // Then: State transitions through Connecting to Connected
            val connectingState = awaitItem()
            assertIs<ConnectionState.Connecting>(connectingState)

            val connectedState = awaitItem()
            assertIs<ConnectionState.Connected>(connectedState)
        }
    }

    @Test
    fun testConnectionStateTransitions() = runBlocking {
        val states = mutableListOf<ConnectionState>()

        client.connectionState.test {
            // Collect initial state
            states.add(awaitItem())

            // Connect
            client.connect(primaryEndpoint)

            // Collect state transitions
            states.add(awaitItem()) // Connecting
            states.add(awaitItem()) // Connected

            // Verify correct sequence
            assertIs<ConnectionState.Disconnected>(states[0])
            assertIs<ConnectionState.Connecting>(states[1])
            assertIs<ConnectionState.Connected>(states[2])
        }
    }

    @Test
    fun testConnectionTimeout() = runBlocking {
        // Given: An invalid endpoint
        val invalidEndpoint = "ws://localhost:9999" // Non-existent port

        client.connectionState.test {
            skipItems(1) // Skip initial Disconnected state

            // When: Attempting to connect to invalid endpoint
            client.connect(invalidEndpoint)

            // Then: State transitions to Connecting
            assertIs<ConnectionState.Connecting>(awaitItem())

            // And eventually to Error state
            val errorState = awaitItem()
            assertIs<ConnectionState.Error>(errorState)
            assertTrue(errorState.message.isNotEmpty())
        }
    }

    @Test
    fun testDisconnectFromConnectedNode() = runBlocking {
        // Given: A connected client
        client.connect(primaryEndpoint)
        delay(2000) // Wait for connection

        client.connectionState.test {
            skipItems(1) // Skip current state

            // When: Disconnecting
            client.disconnect()

            // Then: State becomes Disconnected
            val state = awaitItem()
            assertIs<ConnectionState.Disconnected>(state)
        }
    }

    @Test
    fun testAlreadyConnectedDoesNotReconnect() = runBlocking {
        // Given: A connected client
        client.connect(primaryEndpoint)
        delay(2000) // Wait for connection

        client.connectionState.test {
            val initialState = awaitItem()
            assertIs<ConnectionState.Connected>(initialState)

            // When: Calling connect again with same endpoint
            client.connect(primaryEndpoint)

            // Then: No state change occurs (times out waiting for new state)
            // This demonstrates idempotent connection behavior
            expectNoEvents()
        }
    }

    @Test
    fun testConnectToSecondaryNode() = runBlocking {
        // Test connecting to the second node (bob on port 9945)
        // Prerequisites:
        // - Alice node running on ws://localhost:9944
        // - Bob node running on ws://localhost:9945
        // - Port forwarding: adb reverse tcp:9944 tcp:9944 && adb reverse tcp:9945 tcp:9945
        client.connectionState.test {
            skipItems(1) // Skip Disconnected

            client.connect(secondaryEndpoint)

            assertIs<ConnectionState.Connecting>(awaitItem())
            assertIs<ConnectionState.Connected>(awaitItem())
        }
    }

    @Test
    fun testMetadataFetchedAfterConnection(): Unit = runBlocking {
        // Given: A disconnected client
        assertEquals(null, client.metadata.value) // Initially null

        // When: Connecting to node
        client.connect(primaryEndpoint)

        // Wait for connection and metadata fetch
        delay(3000)

        // Then: Metadata should be populated
        val metadata = client.metadata.value
        assertNotNull(metadata)
        assertTrue(metadata!!.isNotEmpty())
        assertTrue(metadata.startsWith("0x")) // Metadata is hex-encoded
    }

    @Test
    fun testMetadataClearedOnDisconnect(): Unit = runBlocking {
        // Given: A connected client with metadata
        client.connect(primaryEndpoint)
        delay(3000) // Wait for connection and metadata

        // Verify metadata is present
        assertNotNull(client.metadata.value)

        // When: Disconnecting
        client.disconnect()

        // Then: Metadata is cleared
        val metadata = client.metadata.value
        assertEquals(null, metadata)
    }
}
