package tech.wideas.clad.crypto

/**
 * iOS implementation of [Signer].
 *
 * This is a stub implementation. Full signing will be completed in issue #23 (Transaction Signing Pipeline).
 *
 * **Implementation Notes:**
 * - Use NovaCrypto's `SNSigner` for SR25519 signing
 * - Use NovaCrypto's `EDSigner` for ED25519 signing
 * - Use NovaCrypto's `SNSignatureVerifier` / `EDSignatureVerifier` for verification
 * - Requires converting [Keypair] to NovaCrypto's internal keypair format
 *
 * Thread Safety: This class is NOT thread-safe.
 *
 * @see <a href="https://github.com/novasamatech/Crypto-iOS">NovaCrypto (iOS)</a>
 */
class IOSSigner : Signer {

    override fun sign(message: ByteArray, keypair: Keypair): ByteArray {
        // TODO(#23): Implement using NovaCrypto's SNSigner/EDSigner
        throw NotImplementedError("Signing will be implemented in issue #23")
    }

    override fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean {
        // TODO(#23): Implement using NovaCrypto's SNSignatureVerifier/EDSignatureVerifier
        throw NotImplementedError("Signature verification will be implemented in issue #23")
    }
}

actual fun createSigner(): Signer = IOSSigner()
