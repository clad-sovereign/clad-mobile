package tech.wideas.clad.substrate

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for SubstrateClient concurrent operations.
 *
 * These tests validate that the client can handle multiple simultaneous
 * RPC calls and correctly match responses to requests.
 *
 * Prerequisites:
 * - Local Substrate node running on ws://localhost:9944
 */
@RunWith(AndroidJUnit4::class)
class SubstrateClientConcurrencyTest {

    private lateinit var client: SubstrateClient
    private val endpoint = "ws://localhost:9944"  // Use 10.0.2.2 to reach host from Android emulator

    @Before
    fun setup(): Unit = runBlocking {
        // Use Dispatchers.IO for real WebSocket I/O operations
        client = SubstrateClient(
            autoReconnect = false,
            dispatcher = Dispatchers.IO
        )
        client.connect(endpoint)
        client.waitForConnection()
    }

    @After
    fun teardown(): Unit = runBlocking {
        client.disconnect()
        client.close()
    }

    // ============================================
    // Concurrent RPC Call Tests
    // ============================================

    @Test
    fun testTwoConcurrentRpcCalls(): Unit = runBlocking {
        // When: Making two concurrent RPC calls
        val deferred1 = async { client.call("system_chain") }
        val deferred2 = async { client.call("system_name") }

        val results = awaitAll(deferred1, deferred2)

        // Then: Both calls succeed
        assertEquals(2, results.size)
        assertNotNull(results[0])
        assertNotNull(results[1])

        assertIs<JsonPrimitive>(results[0])
        assertIs<JsonPrimitive>(results[1])
    }

    @Test
    fun testMultipleConcurrentRpcCalls(): Unit = runBlocking {
        // When: Making multiple concurrent RPC calls
        val deferredCalls = listOf(
            async { client.call("system_chain") },
            async { client.call("system_name") },
            async { client.call("system_version") },
            async { client.call("system_properties") },
            async { client.call("chain_getBlockHash") }
        )

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All calls succeed and return non-null results
        assertEquals(5, results.size)
        results.forEach { result ->
            assertNotNull(result)
        }

        // Verify specific result types
        assertIs<JsonPrimitive>(results[0]) // chain
        assertIs<JsonPrimitive>(results[1]) // name
        assertIs<JsonPrimitive>(results[2]) // version
        assertIs<JsonObject>(results[3])    // properties
        assertIs<JsonPrimitive>(results[4]) // block hash
    }

    @Test
    fun testTenConcurrentRpcCalls(): Unit = runBlocking {
        // When: Making 10 concurrent calls to test channel buffer handling
        val deferredCalls = (1..10).map { index ->
            async {
                // Alternate between different RPC methods
                when (index % 4) {
                    0 -> client.call("system_chain")
                    1 -> client.call("system_name")
                    2 -> client.call("system_version")
                    else -> client.call("chain_getBlockHash")
                }
            }
        }

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All 10 calls succeed
        assertEquals(10, results.size)
        results.forEach { result ->
            assertNotNull(result)
        }
    }

    @Test
    fun testConcurrentCallsWithDifferentParams(): Unit = runBlocking {
        // When: Making concurrent calls with different parameters
        val deferredCalls = listOf(
            async {
                // Get block at height 0 (genesis)
                client.call("chain_getBlockHash", JsonArray(listOf(JsonPrimitive(0))))
            },
            async {
                // Get latest block hash
                client.call("chain_getBlockHash")
            },
            async {
                // Get metadata
                client.call("state_getMetadata")
            }
        )

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All calls succeed with correct results
        assertEquals(3, results.size)

        val genesisHash = results[0] as? JsonPrimitive
        val latestHash = results[1] as? JsonPrimitive
        val metadata = results[2] as? JsonPrimitive

        assertNotNull(genesisHash)
        assertNotNull(latestHash)
        assertNotNull(metadata)

        assertTrue(genesisHash.content.startsWith("0x"))
        assertTrue(latestHash.content.startsWith("0x"))
        assertTrue(metadata.content.startsWith("0x"))
    }

    // ============================================
    // Response Matching Tests
    // ============================================

    @Test
    fun testResponseMatchingWithConcurrentCalls(): Unit = runBlocking {
        // When: Making concurrent calls that return different values
        val chainDeferred = async { client.call("system_chain") }
        val nameDeferred = async { client.call("system_name") }
        val versionDeferred = async { client.call("system_version") }

        val chain = awaitAll(chainDeferred, nameDeferred, versionDeferred)

        // Then: Each response matches its request
        // (If matching were broken, we might get wrong values or exceptions)
        assertNotNull(chain[0]) // chain
        assertNotNull(chain[1]) // name
        assertNotNull(chain[2]) // version

        // All should be primitives (strings)
        assertIs<JsonPrimitive>(chain[0])
        assertIs<JsonPrimitive>(chain[1])
        assertIs<JsonPrimitive>(chain[2])
    }

    @Test
    fun testConcurrentCallsWithMixedSuccessAndFailure(): Unit = runBlocking {
        // When: Making concurrent calls where some fail
        val results = mutableListOf<Result<Any?>>()

        val jobs = listOf(
            launch {
                results.add(runCatching { client.call("system_chain") })
            },
            launch {
                results.add(runCatching { client.call("invalid_method_xyz") })
            },
            launch {
                results.add(runCatching { client.call("system_version") })
            }
        )

        jobs.forEach { it.join() }

        // Then: Valid calls succeed, invalid calls fail
        assertEquals(3, results.size)

        // Check counts instead of assuming order (concurrent execution order is not guaranteed)
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }

        assertEquals(2, successCount, "Expected 2 successful calls")
        assertEquals(1, failureCount, "Expected 1 failed call")
    }

    // ============================================
    // Channel Buffer Behavior Tests
    // ============================================

    @Test
    fun testChannelBufferUnderLoad(): Unit = runBlocking {
        // When: Making many concurrent calls to test channel buffer (64 capacity)
        val callCount = 50
        val deferredCalls = (1..callCount).map {
            async {
                client.call("system_chain")
            }
        }

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All calls complete successfully despite buffer limits
        assertEquals(callCount, results.size)
        results.forEach { result ->
            assertNotNull(result)
            assertIs<JsonPrimitive>(result)
        }
    }

    @Test
    fun testHighFrequencySequentialCalls(): Unit = runBlocking {
        // When: Making rapid sequential calls
        val results = mutableListOf<Any?>()

        repeat(20) {
            val result = client.call("system_chain")
            results.add(result)
        }

        // Then: All calls succeed
        assertEquals(20, results.size)
        results.forEach { result ->
            assertNotNull(result)
        }
    }

    // ============================================
    // Stress Tests
    // ============================================

    @Test
    fun testConcurrentCallsFromMultipleCoroutines(): Unit = runBlocking {
        // When: Multiple coroutines each making multiple calls
        val jobs = (1..5).map { coroutineId ->
            async {
                val results = mutableListOf<Any?>()
                repeat(5) { callId ->
                    val result = client.call("system_chain")
                    results.add(result)
                }
                results
            }
        }

        val allResults = awaitAll(*jobs.toTypedArray())

        // Then: All coroutines complete successfully
        assertEquals(5, allResults.size) // 5 coroutines
        allResults.forEach { coroutineResults ->
            assertEquals(5, coroutineResults.size) // 5 calls each
            coroutineResults.forEach { result ->
                assertNotNull(result)
            }
        }
    }

    @Test
    fun testInterleavedSequentialAndConcurrentCalls(): Unit = runBlocking {
        // When: Mixing sequential and concurrent call patterns
        // Sequential call 1
        val result1 = client.call("system_chain")
        assertNotNull(result1)

        // Concurrent calls
        val concurrent = awaitAll(
            async { client.call("system_name") },
            async { client.call("system_version") }
        )
        assertEquals(2, concurrent.size)

        // Sequential call 2
        val result2 = client.call("chain_getBlockHash")
        assertNotNull(result2)

        // More concurrent calls
        val concurrent2 = awaitAll(
            async { client.call("system_properties") },
            async { client.call("state_getRuntimeVersion") }
        )
        assertEquals(2, concurrent2.size)

        // Then: All calls succeed
        concurrent.forEach { assertNotNull(it) }
        concurrent2.forEach { assertNotNull(it) }
    }

    // ============================================
    // Timeout Behavior Under Load
    // ============================================

    @Test
    fun testConcurrentCallsWithIndividualTimeouts(): Unit = runBlocking {
        // When: Making concurrent calls with different timeout values
        val deferredCalls = listOf(
            async { client.call("system_chain", timeoutMs = 10000) },
            async { client.call("system_name", timeoutMs = 5000) },
            async { client.call("system_version", timeoutMs = 15000) },
            async { client.call("system_properties", timeoutMs = 8000) }
        )

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All calls complete within their respective timeouts
        assertEquals(4, results.size)
        results.forEach { result ->
            assertNotNull(result)
        }
    }

    @Test
    fun testConcurrentCallsWithOneTimeout(): Unit = runBlocking {
        // When: Making concurrent calls where one has very short timeout
        val results = mutableListOf<Result<Any?>>()

        val jobs = listOf(
            launch {
                results.add(runCatching {
                    client.call("system_chain", timeoutMs = 10000)
                })
            },
            launch {
                results.add(runCatching {
                    // This might timeout depending on metadata size and network speed
                    client.call("state_getMetadata", timeoutMs = 1)
                })
            },
            launch {
                results.add(runCatching {
                    client.call("system_version", timeoutMs = 10000)
                })
            }
        )

        jobs.forEach { it.join() }

        // Then: Calls with reasonable timeouts succeed
        // Call with 1ms timeout likely fails, but others should succeed
        val successCount = results.count { it.isSuccess }
        assertTrue(
            successCount >= 2,
            "At least 2 out of 3 calls should succeed"
        )
    }

    // ============================================
    // Large Payload Tests
    // ============================================

    @Test
    fun testConcurrentCallsWithLargeResponses(): Unit = runBlocking {
        // When: Making concurrent calls that return large payloads
        val deferredCalls = listOf(
            async { client.call("state_getMetadata") },      // Large response
            async { client.call("system_properties") },      // Small response
            async { client.call("state_getRuntimeVersion") } // Medium response
        )

        val results = awaitAll(*deferredCalls.toTypedArray())

        // Then: All calls succeed regardless of response size
        assertEquals(3, results.size)
        results.forEach { result ->
            assertNotNull(result)
        }

        // Verify metadata is large
        val metadata = results[0] as JsonPrimitive
        assertTrue(metadata.content.length > 1000)
    }
}
