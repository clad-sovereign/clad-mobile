package tech.wideas.clad.substrate

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for SubstrateClient RPC calls.
 *
 * These tests validate actual RPC method calls against a running Substrate node.
 *
 * Prerequisites:
 * - Local Substrate node running on ws://localhost:9944
 */
@RunWith(AndroidJUnit4::class)
class SubstrateClientRpcTest {

    private lateinit var client: SubstrateClient
    private val endpoint = "ws://localhost:9944"  // Use 10.0.2.2 to reach host from Android emulator

    @Before
    fun setup() = runBlocking {
        // Use Dispatchers.IO for real WebSocket I/O operations
        client = SubstrateClient(
            autoReconnect = false,
            dispatcher = Dispatchers.IO
        )
        client.connect(endpoint)
        delay(2000) // Wait for connection
    }

    @After
    fun teardown() = runBlocking {
        client.disconnect()
        client.close()
    }

    // ============================================
    // Basic RPC Call Tests
    // ============================================

    @Test
    fun testSystemProperties() = runBlocking {
        // When: Fetching system properties
        val result = client.call("system_properties")

        // Then: Result is a JsonObject with expected properties
        assertNotNull(result)
        assertIs<JsonObject>(result)

        val properties = result as JsonObject
        assertTrue(properties.isNotEmpty())

        // Common Substrate properties
        assertTrue(
            properties.containsKey("ss58Format") ||
            properties.containsKey("tokenDecimals") ||
            properties.containsKey("tokenSymbol"),
            "Should contain at least one standard Substrate property"
        )
    }

    @Test
    fun testGetChainProperties() = runBlocking {
        // When: Fetching chain properties using helper method
        val properties = client.getChainProperties()

        // Then: Properties map is not empty
        assertTrue(properties.isNotEmpty())
    }

    @Test
    fun testGetMetadata() = runBlocking {
        // When: Fetching runtime metadata
        val result = client.call("state_getMetadata")

        // Then: Metadata is returned as hex string
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val metadataHex = result.content
        assertTrue(metadataHex.startsWith("0x"))
        assertTrue(metadataHex.length > 100) // Metadata is large
    }

    @Test
    fun testSystemChain() = runBlocking {
        // When: Fetching chain name
        val result = client.call("system_chain")

        // Then: Chain name is returned
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val chainName = result.content
        assertTrue(chainName.isNotEmpty())
    }

    @Test
    fun testSystemName() = runBlocking {
        // When: Fetching system name
        val result = client.call("system_name")

        // Then: System name is returned
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val systemName = result.content
        assertTrue(systemName.isNotEmpty())
    }

    @Test
    fun testSystemVersion() = runBlocking {
        // When: Fetching system version
        val result = client.call("system_version")

        // Then: Version string is returned
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val version = result.content
        assertTrue(version.isNotEmpty())
    }

    @Test
    fun testChainGetBlockHash() = runBlocking {
        // When: Fetching latest block hash (no params = latest block)
        val result = client.call("chain_getBlockHash")

        // Then: Block hash is returned as hex string
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val blockHash = result.content
        assertTrue(blockHash.startsWith("0x"))
        assertEquals(66, blockHash.length) // 0x + 64 hex chars
    }

    @Test
    fun testChainGetHeader() = runBlocking {
        // When: Fetching latest chain header
        val result = client.call("chain_getHeader")

        // Then: Header object is returned
        assertNotNull(result)
        assertIs<JsonObject>(result)

        val header = result as JsonObject
        assertTrue(header.containsKey("parentHash"))
        assertTrue(header.containsKey("number"))
        assertTrue(header.containsKey("stateRoot"))
        assertTrue(header.containsKey("extrinsicsRoot"))
    }

    @Test
    fun testRuntimeVersion() = runBlocking {
        // When: Fetching runtime version
        val result = client.call("state_getRuntimeVersion")

        // Then: Runtime version object is returned
        assertNotNull(result)
        assertIs<JsonObject>(result)

        val runtimeVersion = result as JsonObject
        assertTrue(runtimeVersion.containsKey("specVersion"))
        assertTrue(runtimeVersion.containsKey("transactionVersion"))
    }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    fun testInvalidRpcMethod() = runBlocking {
        // When: Calling non-existent RPC method
        val exception = assertFailsWith<SubstrateException> {
            client.call("invalid_method_that_does_not_exist")
        }

        // Then: Exception contains error information
        assertTrue(exception.message!!.contains("RPC error"))
    }

    @Test
    fun testInvalidMethodParams() = runBlocking {
        // When: Calling method with invalid parameter format
        val invalidParams = JsonArray(listOf(JsonPrimitive("not_a_valid_hash")))

        val exception = assertFailsWith<SubstrateException> {
            client.call("chain_getBlock", invalidParams)
        }

        // Then: Exception is thrown
        assertTrue(exception.message!!.contains("RPC error"))
    }

    // ============================================
    // Timeout Tests
    // ============================================

    @Test
    fun testRpcCallWithCustomTimeout() = runBlocking {
        // When: Calling with short timeout (but should complete in time)
        val result = client.call(
            method = "system_chain",
            timeoutMs = 5000 // 5 second timeout
        )

        // Then: Call succeeds
        assertNotNull(result)
    }

    @Test
    fun testRpcCallTimeoutExceeded() = runBlocking {
        // When: Calling with extremely short timeout on slow operation
        val exception = assertFailsWith<SubstrateException> {
            client.call(
                method = "state_getMetadata",
                timeoutMs = 1 // 1ms timeout - should fail
            )
        }

        // Then: Timeout exception is thrown
        assertTrue(exception.message!!.contains("timeout"))
    }

    // ============================================
    // RPC with Parameters Tests
    // ============================================

    @Test
    fun testChainGetBlockHashWithNumber() = runBlocking {
        // When: Fetching block hash at specific height (genesis block)
        val params = JsonArray(listOf(JsonPrimitive(0)))
        val result = client.call("chain_getBlockHash", params)

        // Then: Block hash is returned
        assertNotNull(result)
        assertIs<JsonPrimitive>(result)

        val blockHash = result.content
        assertTrue(blockHash.startsWith("0x"))
        assertEquals(66, blockHash.length)
    }

    @Test
    fun testChainGetBlock() = runBlocking {
        // Given: Latest block hash
        val hashResult = client.call("chain_getBlockHash")
        assertNotNull(hashResult)
        val blockHash = (hashResult as JsonPrimitive).content

        // When: Fetching block by hash
        val params = JsonArray(listOf(JsonPrimitive(blockHash)))
        val result = client.call("chain_getBlock", params)

        // Then: Block object is returned with expected structure
        assertNotNull(result)
        assertIs<JsonObject>(result)

        val block = result as JsonObject
        assertTrue(block.containsKey("block"))
    }

    @Test
    fun testStateGetStorage() = runBlocking {
        // When: Querying storage (system number - always exists)
        // The storage key for System.Number is well-known
        val storageKey = "0x26aa394eea5630e07c48ae0c9558cef702a5c1b19ab7a04f536c519aca4983ac"
        val params = JsonArray(listOf(JsonPrimitive(storageKey)))

        val result = client.call("state_getStorage", params)

        // Then: Storage value is returned (may be null for some keys)
        // We don't assert on the value since it depends on chain state
        assertNotNull(result)
    }

    // ============================================
    // Multiple Sequential Calls
    // ============================================

    @Test
    fun testMultipleSequentialRpcCalls() = runBlocking {
        // When: Making multiple sequential calls
        val chain = client.call("system_chain")
        val name = client.call("system_name")
        val version = client.call("system_version")
        val properties = client.call("system_properties")

        // Then: All calls succeed and return valid data
        assertNotNull(chain)
        assertNotNull(name)
        assertNotNull(version)
        assertNotNull(properties)

        assertIs<JsonPrimitive>(chain)
        assertIs<JsonPrimitive>(name)
        assertIs<JsonPrimitive>(version)
        assertIs<JsonObject>(properties)
    }

    @Test
    fun testRpcCallAfterMetadataFetch() = runBlocking {
        // Given: Metadata has been fetched
        client.fetchMetadata()
        delay(1000)

        // When: Making another RPC call
        val result = client.call("system_chain")

        // Then: Call succeeds
        assertNotNull(result)
    }
}
