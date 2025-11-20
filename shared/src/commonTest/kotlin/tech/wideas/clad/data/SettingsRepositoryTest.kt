package tech.wideas.clad.data

import kotlinx.coroutines.test.runTest
import tech.wideas.clad.security.SecureStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SettingsRepository.
 *
 * Verifies that settings are correctly saved and retrieved using SecureStorage.
 */
class SettingsRepositoryTest {

    private class TestSecureStorage : SecureStorage {
        private val storage = mutableMapOf<String, String>()

        override suspend fun save(key: String, value: String) {
            storage[key] = value
        }

        override suspend fun get(key: String): String? {
            return storage[key]
        }

        override suspend fun delete(key: String) {
            storage.remove(key)
        }

        override suspend fun contains(key: String): Boolean {
            return storage.containsKey(key)
        }

        override suspend fun clear() {
            storage.clear()
        }
    }

    @Test
    fun `saveRpcEndpoint should store endpoint in secure storage`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)
        val endpoint = "ws://192.168.1.100:9944"

        repository.saveRpcEndpoint(endpoint)

        val saved = secureStorage.get("rpc_endpoint")
        assertEquals(endpoint, saved)
    }

    @Test
    fun `getRpcEndpoint should return saved endpoint`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)
        val endpoint = "ws://192.168.1.100:9944"

        repository.saveRpcEndpoint(endpoint)
        val retrieved = repository.getRpcEndpoint("default")

        assertEquals(endpoint, retrieved)
    }

    @Test
    fun `getRpcEndpoint should return default when no endpoint saved`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)
        val defaultEndpoint = "ws://default:9944"

        val retrieved = repository.getRpcEndpoint(defaultEndpoint)

        assertEquals(defaultEndpoint, retrieved)
    }

    @Test
    fun `hasRpcEndpoint should return true when endpoint is saved`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)

        repository.saveRpcEndpoint("ws://test:9944")

        assertTrue(repository.hasRpcEndpoint())
    }

    @Test
    fun `hasRpcEndpoint should return false when no endpoint saved`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)

        assertFalse(repository.hasRpcEndpoint())
    }

    @Test
    fun `saveRpcEndpoint should overwrite existing endpoint`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)

        repository.saveRpcEndpoint("ws://old:9944")
        repository.saveRpcEndpoint("ws://new:9944")

        val retrieved = repository.getRpcEndpoint("default")
        assertEquals("ws://new:9944", retrieved)
    }

    @Test
    fun `saveRpcEndpoint should handle empty string`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)

        repository.saveRpcEndpoint("")
        val retrieved = repository.getRpcEndpoint("default")

        assertEquals("", retrieved)
    }

    @Test
    fun `saveRpcEndpoint should handle whitespace`() = runTest {
        val secureStorage = TestSecureStorage()
        val repository = SettingsRepository(secureStorage)
        val endpoint = "  ws://test:9944  "

        repository.saveRpcEndpoint(endpoint)
        val retrieved = repository.getRpcEndpoint("default")

        // Repository should save exactly what's provided
        assertEquals(endpoint, retrieved)
    }

    @Test
    fun `default RPC endpoints should be defined`() {
        // Verify that platform-specific defaults exist
        assertEquals("ws://10.0.2.2:9944", SettingsRepository.DEFAULT_RPC_ENDPOINT_ANDROID)
        assertEquals("ws://127.0.0.1:9944", SettingsRepository.DEFAULT_RPC_ENDPOINT_IOS)
    }
}
