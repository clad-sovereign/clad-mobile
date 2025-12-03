package tech.wideas.clad.security

import kotlin.concurrent.Volatile
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.errSecAuthFailed
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.errSecUserCanceled
import platform.darwin.OSStatus
import platform.posix.memcpy
import tech.wideas.clad.crypto.Keypair

/**
 * iOS implementation of [KeyStorage] using Swift BiometricKeychainHelper.
 *
 * Security architecture:
 * 1. Each keypair stored as a separate Keychain item with biometric protection
 * 2. Uses `kSecAccessControlBiometryCurrentSet` - keys invalidated if biometrics change
 * 3. `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` - excluded from backups
 * 4. Secure Enclave used for hardware-backed protection on all iOS 17+ devices
 * 5. LAContext provides the biometric authentication UI
 *
 * Note: This uses a Swift BiometricKeychainHelper because calling Keychain APIs directly
 * from Kotlin/Native has CFDictionary bridging issues (OSStatus -50).
 */
class IOSKeyStorage(
    private val keychainHelper: BiometricKeychainHelperProtocol
) : KeyStorage {

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        keychainHelper.isAvailable()
    }

    override suspend fun isHardwareBackedAvailable(): Boolean {
        // Secure Enclave is available on all devices that support iOS 17+
        // (our minimum deployment target), including all iPhones since 5s
        return true
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val serialized = KeypairSerializer.serialize(keypair)
            val data = serialized.toNSData()

            val status = keychainHelper.saveKeypair(
                accountId = accountId,
                data = data,
                promptTitle = promptConfig.title
            )

            statusToResult(status)
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to save keypair")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Keypair> = withContext(Dispatchers.IO) {
        try {
            val result = keychainHelper.getKeypair(
                accountId = accountId,
                promptTitle = promptConfig.title
            )

            when (result.status) {
                errSecSuccess.toInt() -> {
                    val data = result.data
                    if (data != null) {
                        val bytes = data.toByteArray()
                        val keypair = KeypairSerializer.deserialize(bytes)
                        KeyStorageResult.Success(keypair)
                    } else {
                        KeyStorageResult.StorageError("Failed to read keychain data")
                    }
                }
                errSecItemNotFound.toInt() -> KeyStorageResult.KeyNotFound
                errSecUserCanceled.toInt() -> KeyStorageResult.BiometricCancelled
                errSecAuthFailed.toInt() -> KeyStorageResult.BiometricError("Authentication failed")
                else -> KeyStorageResult.StorageError("Keychain error: ${result.status}")
            }
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to retrieve keypair")
        }
    }

    override suspend fun deleteKeypair(accountId: String): KeyStorageResult<Unit> =
        withContext(Dispatchers.IO) {
            val status = keychainHelper.deleteKeypair(accountId = accountId)

            when (status) {
                errSecSuccess.toInt(), errSecItemNotFound.toInt() -> KeyStorageResult.Success(Unit)
                else -> KeyStorageResult.StorageError("Failed to delete keypair: $status")
            }
        }

    override suspend fun hasKeypair(accountId: String): Boolean = withContext(Dispatchers.IO) {
        keychainHelper.hasKeypair(accountId = accountId)
    }

    override suspend fun listAccountIds(): List<String> = withContext(Dispatchers.IO) {
        keychainHelper.listAccountIds()
    }

    /**
     * Convert OSStatus to KeyStorageResult for save operations.
     */
    private fun statusToResult(status: Int): KeyStorageResult<Unit> {
        return when (status) {
            errSecSuccess.toInt() -> KeyStorageResult.Success(Unit)
            errSecUserCanceled.toInt() -> KeyStorageResult.BiometricCancelled
            errSecAuthFailed.toInt() -> KeyStorageResult.BiometricError("Authentication failed")
            errSecDuplicateItem.toInt() -> KeyStorageResult.StorageError("Item already exists")
            else -> KeyStorageResult.StorageError("Keychain error: $status")
        }
    }
}

/**
 * Protocol for BiometricKeychainHelper to allow dependency injection.
 * Implemented by Swift BiometricKeychainHelper class.
 */
interface BiometricKeychainHelperProtocol {
    fun isAvailable(): Boolean
    fun saveKeypair(accountId: String, data: NSData, promptTitle: String): Int
    fun getKeypair(accountId: String, promptTitle: String): BiometricKeychainResultProtocol
    fun deleteKeypair(accountId: String): Int
    fun hasKeypair(accountId: String): Boolean
    fun listAccountIds(): List<String>
}

/**
 * Result wrapper protocol for biometric Keychain operations.
 */
interface BiometricKeychainResultProtocol {
    val status: Int
    val data: NSData?
}

/**
 * Holder for the BiometricKeychainHelper instance provided by Swift.
 * Must be set before Koin initialization.
 */
object KeyStorageFactory {
    @Volatile
    private var keychainHelper: BiometricKeychainHelperProtocol? = null

    fun setKeychainHelper(helper: BiometricKeychainHelperProtocol) {
        keychainHelper = helper
    }

    fun create(): KeyStorage {
        val helper = keychainHelper
            ?: throw IllegalStateException("BiometricKeychainHelper not set. Call KeyStorageFactory.setKeychainHelper() before Koin init.")
        return IOSKeyStorage(helper)
    }
}

/**
 * Convert ByteArray to NSData.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = this@toNSData.refTo(0).getPointer(this),
        length = this@toNSData.size.toULong()
    )
}

/**
 * Convert NSData to ByteArray.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = memScoped {
    val size = this@toByteArray.length.toInt()
    if (size == 0) return@memScoped ByteArray(0)

    val bytes = ByteArray(size)
    memcpy(bytes.refTo(0).getPointer(this), this@toByteArray.bytes, size.toULong())
    bytes
}
