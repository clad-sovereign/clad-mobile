package tech.wideas.clad.security

/**
 * Secure storage interface for sensitive data (account keys, settings, etc.)
 * Platform implementations:
 * - Android: EncryptedSharedPreferences
 * - iOS: Keychain Services
 */
interface SecureStorage {
    /**
     * Save a key-value pair securely
     * @param key The key to store under
     * @param value The value to store
     */
    suspend fun save(key: String, value: String)

    /**
     * Retrieve a value by key
     * @param key The key to retrieve
     * @return The value, or null if not found
     */
    suspend fun get(key: String): String?

    /**
     * Delete a value by key
     * @param key The key to delete
     */
    suspend fun delete(key: String)

    /**
     * Check if a key exists
     * @param key The key to check
     * @return true if the key exists, false otherwise
     */
    suspend fun contains(key: String): Boolean

    /**
     * Clear all stored data
     */
    suspend fun clear()
}

/**
 * Factory function to create platform-specific SecureStorage
 */
expect fun createSecureStorage(): SecureStorage
