package tech.wideas.clad.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Security.errSecSuccess

/**
 * iOS implementation of [SecureStorage] using Swift KeychainHelper.
 *
 * Security architecture:
 * 1. Each key-value pair stored as a separate Keychain item
 * 2. Uses `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` - data protected when locked
 * 3. Data is excluded from device backups (not synced to iCloud)
 * 4. Hardware-encrypted at rest by iOS
 *
 * Note: This uses a Swift KeychainHelper because calling Keychain APIs directly
 * from Kotlin/Native has CFDictionary bridging issues (OSStatus -50).
 */
class IOSSecureStorage(
    private val keychainHelper: KeychainHelperProtocol
) : SecureStorage {

    override suspend fun save(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        val status = keychainHelper.save(key = key, value = value)
        if (status != errSecSuccess.toInt()) {
            throw IllegalStateException("Failed to save to Keychain: OSStatus $status")
        }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        keychainHelper.get(key = key)
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        keychainHelper.delete(key = key)
        Unit
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        keychainHelper.contains(key = key)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        keychainHelper.clear()
        Unit
    }
}

/**
 * Protocol for KeychainHelper to allow dependency injection.
 * Implemented by Swift KeychainHelper class.
 */
interface KeychainHelperProtocol {
    fun save(key: String, value: String): Int
    fun get(key: String): String?
    fun delete(key: String): Int
    fun contains(key: String): Boolean
    fun clear(): Int
}

/**
 * Holder for the KeychainHelper instance provided by Swift.
 * Must be set before Koin initialization.
 */
object SecureStorageFactory {
    private var keychainHelper: KeychainHelperProtocol? = null

    fun setKeychainHelper(helper: KeychainHelperProtocol) {
        keychainHelper = helper
    }

    fun create(): SecureStorage {
        val helper = keychainHelper
            ?: throw IllegalStateException("KeychainHelper not set. Call SecureStorageFactory.setKeychainHelper() before Koin init.")
        return IOSSecureStorage(helper)
    }
}

actual fun createSecureStorage(): SecureStorage = SecureStorageFactory.create()
