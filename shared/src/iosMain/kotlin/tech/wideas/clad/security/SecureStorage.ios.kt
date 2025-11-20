package tech.wideas.clad.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation using NSUserDefaults (simplified for PR #1)
 * TODO: Replace with proper Keychain implementation in PR #2
 */
class IOSSecureStorage : SecureStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun save(key: String, value: String) = withContext(Dispatchers.IO) {
        userDefaults.setObject(value, key)
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        userDefaults.stringForKey(key)
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        userDefaults.removeObjectForKey(key)
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        userDefaults.objectForKey(key) != null
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        val domain = userDefaults.dictionaryRepresentation().keys
        domain.forEach { key ->
            if (key is String) {
                userDefaults.removeObjectForKey(key)
            }
        }
    }
}

actual fun createSecureStorage(): SecureStorage = IOSSecureStorage()
