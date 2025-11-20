package tech.wideas.clad.security

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for SecureStorage implementations.
 *
 * These tests verify the contract that all platform-specific SecureStorage
 * implementations must follow.
 */
class SecureStorageTest {

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
    fun `save and get should store and retrieve values correctly`() = runTest {
        val storage = TestSecureStorage()
        val key = "test_key"
        val value = "test_value"

        storage.save(key, value)
        val retrieved = storage.get(key)

        assertEquals(value, retrieved)
    }

    @Test
    fun `get should return null for non-existent key`() = runTest {
        val storage = TestSecureStorage()

        val retrieved = storage.get("non_existent_key")

        assertNull(retrieved)
    }

    @Test
    fun `contains should return true for existing key`() = runTest {
        val storage = TestSecureStorage()
        val key = "test_key"

        storage.save(key, "value")

        assertTrue(storage.contains(key))
    }

    @Test
    fun `contains should return false for non-existent key`() = runTest {
        val storage = TestSecureStorage()

        assertFalse(storage.contains("non_existent_key"))
    }

    @Test
    fun `delete should remove stored value`() = runTest {
        val storage = TestSecureStorage()
        val key = "test_key"

        storage.save(key, "value")
        assertTrue(storage.contains(key))

        storage.delete(key)

        assertFalse(storage.contains(key))
        assertNull(storage.get(key))
    }

    @Test
    fun `clear should remove all stored values`() = runTest {
        val storage = TestSecureStorage()

        storage.save("key1", "value1")
        storage.save("key2", "value2")
        storage.save("key3", "value3")

        storage.clear()

        assertFalse(storage.contains("key1"))
        assertFalse(storage.contains("key2"))
        assertFalse(storage.contains("key3"))
    }

    @Test
    fun `save should overwrite existing values`() = runTest {
        val storage = TestSecureStorage()
        val key = "test_key"

        storage.save(key, "old_value")
        storage.save(key, "new_value")

        assertEquals("new_value", storage.get(key))
    }

    @Test
    fun `storage should handle multiple keys independently`() = runTest {
        val storage = TestSecureStorage()

        storage.save("key1", "value1")
        storage.save("key2", "value2")

        assertEquals("value1", storage.get("key1"))
        assertEquals("value2", storage.get("key2"))

        storage.delete("key1")

        assertFalse(storage.contains("key1"))
        assertTrue(storage.contains("key2"))
    }

    @Test
    fun `storage should handle empty string values`() = runTest {
        val storage = TestSecureStorage()
        val key = "empty_key"

        storage.save(key, "")

        assertTrue(storage.contains(key))
        assertEquals("", storage.get(key))
    }

    @Test
    fun `storage should handle special characters in values`() = runTest {
        val storage = TestSecureStorage()
        val specialValue = "test@#$%^&*(){}[]|\\:;\"'<>,.?/~`"

        storage.save("special", specialValue)

        assertEquals(specialValue, storage.get("special"))
    }
}
