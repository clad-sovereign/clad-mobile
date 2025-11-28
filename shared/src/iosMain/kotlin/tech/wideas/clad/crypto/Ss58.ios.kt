package tech.wideas.clad.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import novacrypto.SS58AddressFactory
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * iOS implementation of SS58 address encoding using NovaCrypto library.
 *
 * @see <a href="https://github.com/novasamatech/Crypto-iOS">NovaCrypto (iOS)</a>
 */
@OptIn(ExperimentalForeignApi::class)
actual object Ss58 {

    private val addressFactory = SS58AddressFactory()

    /**
     * @throws IllegalArgumentException if publicKey is not exactly [PUBLIC_KEY_SIZE] bytes.
     */
    actual fun encode(publicKey: ByteArray, networkPrefix: Short): String {
        require(publicKey.size == PUBLIC_KEY_SIZE) {
            "Public key must be exactly $PUBLIC_KEY_SIZE bytes, got ${publicKey.size}"
        }

        val accountIdData = publicKey.toNSData()
        val address = addressFactory.addressFromAccountId(
            accountIdData,
            networkPrefix.toUShort(),
            null
        ) ?: throw IllegalStateException("Failed to encode SS58 address")

        return address
    }

    actual fun decode(address: String): Pair<ByteArray, Short> {
        // First extract the type/prefix from the address
        val typeNumber = addressFactory.typeFromAddress(address, null)
            ?: throw IllegalArgumentException("Invalid SS58 address: cannot determine type")

        val networkPrefix = typeNumber.shortValue()

        // Then extract the account ID using the detected type
        val accountIdData = addressFactory.accountIdFromAddress(
            address,
            networkPrefix.toUShort(),
            null
        ) ?: throw IllegalArgumentException("Invalid SS58 address: cannot decode")

        return Pair(accountIdData.toByteArray(), networkPrefix)
    }

    actual fun isValid(address: String): Boolean {
        return try {
            val typeNumber = addressFactory.typeFromAddress(address, null)
            if (typeNumber == null) {
                false
            } else {
                val accountId = addressFactory.accountIdFromAddress(
                    address,
                    typeNumber.shortValue().toUShort(),
                    null
                )
                accountId != null
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Converts NSData to Kotlin ByteArray.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)

    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

/**
 * Converts Kotlin ByteArray to NSData.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    if (this.isEmpty()) return NSData()

    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}
