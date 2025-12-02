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
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * iOS implementation of [SecureStorage] using Keychain Services.
 *
 * Security architecture:
 * 1. Each key-value pair stored as a separate Keychain item
 * 2. Uses `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` - data protected when locked
 * 3. Data is excluded from device backups (not synced to iCloud)
 * 4. Hardware-encrypted at rest by iOS
 *
 * This replaces the previous NSUserDefaults implementation which was not secure.
 *
 * Note: This storage does NOT require biometric authentication.
 * For biometric-protected key storage, use [KeyStorage] instead.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSSecureStorage : SecureStorage {

    companion object {
        private const val SERVICE_NAME = "tech.wideas.clad.securestorage"

        /**
         * Short key returned by Keychain when querying with kSecReturnAttributes.
         * Maps to [kSecAttrAccount]. Useful for parsing returned attribute dictionaries
         * or debugging Keychain queries.
         */
        @Suppress("unused")
        private const val KEYCHAIN_ATTR_ACCOUNT = "acct"
    }

    override suspend fun save(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        val data = value.toNSData()

        // Try to update existing item first
        val query = baseQuery(key)
        val updateAttributes = mapOf<Any?, Any?>(
            kSecValueData to data
        )

        val updateStatus = SecItemUpdate(
            query.toCFDictionary(),
            updateAttributes.toCFDictionary()
        )

        when {
            updateStatus == errSecSuccess -> {
                // Update succeeded
            }
            updateStatus == errSecItemNotFound -> {
                // Item doesn't exist, add it
                val addQuery = mapOf<Any?, Any?>(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to SERVICE_NAME,
                    kSecAttrAccount to key,
                    kSecValueData to data,
                    kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
                )
                val addStatus = SecItemAdd(addQuery.toCFDictionary(), null)
                if (addStatus != errSecSuccess && addStatus != errSecDuplicateItem) {
                    throw IllegalStateException("Failed to save to Keychain: OSStatus $addStatus")
                }
            }
            else -> {
                throw IllegalStateException("Failed to update Keychain: OSStatus $updateStatus")
            }
        }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        memScoped {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to SERVICE_NAME,
                kSecAttrAccount to key,
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne
            )

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query.toCFDictionary(), result.ptr)

            if (status == errSecSuccess) {
                val data = CFBridgingRelease(result.value) as? NSData
                data?.toUtf8String()
            } else {
                null
            }
        }
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        val query = baseQuery(key)
        SecItemDelete(query.toCFDictionary())
        Unit
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        val query = baseQuery(key)
        val status = SecItemCopyMatching(query.toCFDictionary(), null)
        status == errSecSuccess
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        // Delete all items for this service
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME
        )
        SecItemDelete(query.toCFDictionary())
        Unit
    }

    private fun baseQuery(key: String): Map<Any?, Any?> = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE_NAME,
        kSecAttrAccount to key
    )
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
 * Convert String to NSData using UTF-8 encoding.
 */
@OptIn(BetaInteropApi::class)
private fun String.toNSData(): NSData {
    return (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
}

/**
 * Convert NSData to UTF-8 String.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toUtf8String(): String? = memScoped {
    val size = this@toUtf8String.length.toInt()
    if (size == 0) return@memScoped ""

    val bytes = ByteArray(size)
    memcpy(bytes.refTo(0).getPointer(this), this@toUtf8String.bytes, size.toULong())
    bytes.decodeToString()
}

actual fun createSecureStorage(): SecureStorage = IOSSecureStorage()
