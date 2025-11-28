package tech.wideas.clad.crypto

import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

/**
 * Android implementation of SS58 address encoding using Nova Substrate SDK.
 */
actual object Ss58 {
    /**
     * @throws IllegalArgumentException if publicKey is not exactly [PUBLIC_KEY_SIZE] bytes.
     */
    actual fun encode(publicKey: ByteArray, networkPrefix: Short): String {
        require(publicKey.size == PUBLIC_KEY_SIZE) {
            "Public key must be exactly $PUBLIC_KEY_SIZE bytes, got ${publicKey.size}"
        }
        return publicKey.toAddress(networkPrefix)
    }

    actual fun decode(address: String): Pair<ByteArray, Short> {
        val publicKey = address.toAccountId()
        val prefix = SS58Encoder.extractAddressPrefix(address)
        return Pair(publicKey, prefix)
    }

    actual fun isValid(address: String): Boolean {
        return try {
            address.toAccountId()
            true
        } catch (e: Exception) {
            false
        }
    }
}
