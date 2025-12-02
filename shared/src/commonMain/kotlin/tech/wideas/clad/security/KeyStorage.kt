package tech.wideas.clad.security

import tech.wideas.clad.crypto.Keypair

/**
 * Result of key storage operations requiring biometric authentication.
 */
sealed class KeyStorageResult<out T> {
    data class Success<T>(val data: T) : KeyStorageResult<T>()
    data class BiometricError(val message: String) : KeyStorageResult<Nothing>()
    data object BiometricCancelled : KeyStorageResult<Nothing>()
    data object BiometricNotAvailable : KeyStorageResult<Nothing>()
    data class StorageError(val message: String) : KeyStorageResult<Nothing>()
    data object KeyNotFound : KeyStorageResult<Nothing>()
}

/**
 * Configuration for biometric prompt during key access.
 */
data class BiometricPromptConfig(
    val title: String,
    val subtitle: String = "",
    val promptDescription: String = "",
    val negativeButtonText: String = "Cancel"
)

/**
 * Secure key storage interface with biometric protection.
 *
 * This interface handles storage of sensitive cryptographic keys.
 * All read/write operations require biometric authentication.
 *
 * Platform implementations:
 * - Android: Android Keystore with BiometricPrompt CryptoObject binding
 * - iOS: Keychain with kSecAttrAccessControl and Secure Enclave
 *
 * IMPORTANT: This is separate from [SecureStorage] which handles
 * general app preferences without biometric requirements.
 *
 * Security guarantees:
 * - Keys are encrypted at rest using platform-native mechanisms
 * - Hardware-backed storage when available (StrongBox/Secure Enclave)
 * - Biometric authentication bound to cryptographic operations
 * - Keys excluded from device backups
 */
interface KeyStorage {
    /**
     * Check if biometric-protected storage is available.
     * Returns false if:
     * - Device doesn't support biometrics
     * - No biometrics enrolled
     * - Hardware security module not available
     */
    suspend fun isAvailable(): Boolean

    /**
     * Check if hardware-backed key storage is available.
     * - Android: StrongBox Keymaster (Pixel 3+, Samsung S10+, etc.)
     * - iOS: Secure Enclave (all devices supporting iOS 17+)
     */
    suspend fun isHardwareBackedAvailable(): Boolean

    /**
     * Store a keypair with biometric protection.
     *
     * The keypair is encrypted using platform-native mechanisms before storage.
     * Biometric authentication is required to complete the operation.
     *
     * @param accountId Unique identifier for the account (e.g., UUID)
     * @param keypair The keypair to store. Caller should call keypair.clear() after.
     * @param promptConfig Configuration for the biometric prompt.
     * @return KeyStorageResult indicating success or failure type.
     */
    suspend fun saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Unit>

    /**
     * Retrieve a keypair with biometric authentication.
     *
     * @param accountId The account identifier used when saving.
     * @param promptConfig Configuration for the biometric prompt.
     * @return KeyStorageResult containing the Keypair or error.
     *         Caller is responsible for calling keypair.clear() when done.
     */
    suspend fun getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Keypair>

    /**
     * Delete a keypair. Does not require biometric authentication.
     *
     * @param accountId The account identifier.
     * @return KeyStorageResult indicating success or failure.
     */
    suspend fun deleteKeypair(accountId: String): KeyStorageResult<Unit>

    /**
     * Check if a keypair exists for the given account.
     * Does not require biometric authentication.
     */
    suspend fun hasKeypair(accountId: String): Boolean

    /**
     * List all stored account IDs. Does not require biometric authentication.
     */
    suspend fun listAccountIds(): List<String>
}
