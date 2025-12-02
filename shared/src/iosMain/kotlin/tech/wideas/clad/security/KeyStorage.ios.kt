package tech.wideas.clad.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecAuthFailed
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.errSecUserCanceled
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecUseAuthenticationContext
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.CoreFoundation.CFTypeRefVar
import platform.posix.memcpy
import tech.wideas.clad.crypto.Keypair

/**
 * iOS implementation of [KeyStorage] using Keychain Services and LocalAuthentication.
 *
 * Security architecture:
 * 1. Each keypair stored as a separate Keychain item with biometric protection
 * 2. Uses `kSecAccessControlBiometryCurrentSet` - keys invalidated if biometrics change
 * 3. `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` - excluded from backups
 * 4. Secure Enclave used for hardware-backed protection on all iOS 17+ devices
 * 5. LAContext provides the biometric authentication UI
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSKeyStorage : KeyStorage {

    companion object {
        private const val SERVICE_NAME = "tech.wideas.clad.keystorage"
        private const val KEYPAIR_PREFIX = "keypair_"

        // Keychain attribute short key for kSecAttrAccount.
        // When SecItemCopyMatching returns attributes via CFBridgingRelease,
        // the dictionary uses these short string keys (stable since iOS 2.0).
        // See: https://developer.apple.com/documentation/security/keychain_services/keychain_items/item_attribute_keys_and_values
        private const val KEYCHAIN_ATTR_ACCOUNT = "acct"
    }

    override suspend fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null
        )
    }

    override suspend fun isHardwareBackedAvailable(): Boolean {
        // Secure Enclave is available on all devices that support iOS 17+
        // (our minimum deployment target), including all iPhones since 5s
        return true
    }

    override suspend fun saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val serialized = KeypairSerializer.serialize(keypair)
            val data = serialized.toNSData()

            // Create access control with biometric protection
            val accessControl = SecAccessControlCreateWithFlags(
                null, // allocator
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlBiometryCurrentSet,
                null // error
            ) ?: return@withContext KeyStorageResult.StorageError(
                "Failed to create access control"
            )

            // Create LAContext for biometric prompt
            val context = LAContext()
            context.localizedReason = promptConfig.title

            val accountKey = "$KEYPAIR_PREFIX$accountId"

            // Delete existing item if present (ignore errors)
            val deleteQuery = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to accountKey
            )
            SecItemDelete(deleteQuery.toCFDictionary())

            // Build the add query
            val addQuery = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to accountKey,
                kSecValueData to data,
                kSecAttrAccessControl to accessControl,
                kSecUseAuthenticationContext to context
            )

            // Add the new item
            val status = SecItemAdd(addQuery.toCFDictionary(), null)

            statusToResult(status)
        } catch (e: Exception) {
            KeyStorageResult.StorageError(e.message ?: "Failed to save keypair")
        }
    }

    override suspend fun getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ): KeyStorageResult<Keypair> = withContext(Dispatchers.IO) {
        memScoped {
            try {
                // Create LAContext for biometric prompt
                val context = LAContext()
                context.localizedReason = promptConfig.title

                val accountKey = "$KEYPAIR_PREFIX$accountId"

                val query = mapOf<Any?, Any?>(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to SERVICE_NAME,
                    kSecAttrAccount to accountKey,
                    kSecReturnData to true,
                    kSecMatchLimit to kSecMatchLimitOne,
                    kSecUseAuthenticationContext to context
                )

                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query.toCFDictionary(), result.ptr)

                when (status) {
                    errSecSuccess -> {
                        val data = CFBridgingRelease(result.value) as? NSData
                        if (data != null) {
                            val bytes = data.toByteArray()
                            val keypair = KeypairSerializer.deserialize(bytes)
                            KeyStorageResult.Success(keypair)
                        } else {
                            KeyStorageResult.StorageError("Failed to read keychain data")
                        }
                    }
                    errSecItemNotFound -> KeyStorageResult.KeyNotFound
                    errSecUserCanceled -> KeyStorageResult.BiometricCancelled
                    errSecAuthFailed -> KeyStorageResult.BiometricError("Authentication failed")
                    else -> KeyStorageResult.StorageError("Keychain error: $status")
                }
            } catch (e: Exception) {
                KeyStorageResult.StorageError(e.message ?: "Failed to retrieve keypair")
            }
        }
    }

    override suspend fun deleteKeypair(accountId: String): KeyStorageResult<Unit> =
        withContext(Dispatchers.IO) {
            val accountKey = "$KEYPAIR_PREFIX$accountId"

            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to accountKey
            )

            val status = SecItemDelete(query.toCFDictionary())

            when (status) {
                errSecSuccess, errSecItemNotFound -> KeyStorageResult.Success(Unit)
                else -> KeyStorageResult.StorageError("Failed to delete keypair: $status")
            }
        }

    override suspend fun hasKeypair(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val accountKey = "$KEYPAIR_PREFIX$accountId"

        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to accountKey,
            kSecMatchLimit to kSecMatchLimitOne
        )

        val status = SecItemCopyMatching(query.toCFDictionary(), null)
        status == errSecSuccess
    }

    override suspend fun listAccountIds(): List<String> = withContext(Dispatchers.IO) {
        memScoped {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecReturnAttributes to true,
                kSecMatchLimit to kSecMatchLimitAll
            )

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query.toCFDictionary(), result.ptr)

            if (status == errSecSuccess) {
                @Suppress("UNCHECKED_CAST")
                val items = CFBridgingRelease(result.value) as? List<Map<Any?, Any?>>
                items?.mapNotNull { item ->
                    val account = item[KEYCHAIN_ATTR_ACCOUNT] as? String
                    account?.removePrefix(KEYPAIR_PREFIX)
                } ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    /**
     * Convert OSStatus to KeyStorageResult for save operations.
     */
    private fun statusToResult(status: OSStatus): KeyStorageResult<Unit> {
        return when (status) {
            errSecSuccess -> KeyStorageResult.Success(Unit)
            errSecUserCanceled -> KeyStorageResult.BiometricCancelled
            errSecAuthFailed -> KeyStorageResult.BiometricError("Authentication failed")
            errSecDuplicateItem -> KeyStorageResult.StorageError("Item already exists")
            else -> KeyStorageResult.StorageError("Keychain error: $status")
        }
    }
}

/**
 * Convert a Map to CFDictionaryRef for Keychain APIs.
 */
@OptIn(ExperimentalForeignApi::class)
private fun Map<Any?, Any?>.toCFDictionary(): CFDictionaryRef? {
    @Suppress("UNCHECKED_CAST")
    return CFBridgingRetain(this) as? CFDictionaryRef
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
