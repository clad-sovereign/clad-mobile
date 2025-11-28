package tech.wideas.clad.crypto

/**
 * Android implementation of [Signer].
 *
 * This is a stub implementation. Full signing will be completed in issue #23 (Transaction Signing Pipeline).
 *
 * **Implementation Notes:**
 * - Use Nova SDK's `MultiChainEncryption.Substrate` for signing
 * - Use Nova SDK's `SignatureVerifier` for verification
 * - Requires converting [Keypair] to Nova's internal keypair format
 *
 * Thread Safety: This class is NOT thread-safe.
 *
 * @see <a href="https://github.com/novasamatech/substrate-sdk-android">Nova Substrate SDK</a>
 */
class AndroidSigner : Signer {

    override fun sign(message: ByteArray, keypair: Keypair): ByteArray {
        // TODO(#23): Implement using Nova SDK's Signer with MultiChainEncryption.Substrate
        throw NotImplementedError("Signing will be implemented in issue #23")
    }

    override fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean {
        // TODO(#23): Implement using Nova SDK's SignatureVerifier
        throw NotImplementedError("Signature verification will be implemented in issue #23")
    }
}

actual fun createSigner(): Signer = AndroidSigner()
