package tech.wideas.clad.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import novacrypto.SS58AddressFactory
import tech.wideas.clad.util.toByteArray
import tech.wideas.clad.util.toNSData

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
