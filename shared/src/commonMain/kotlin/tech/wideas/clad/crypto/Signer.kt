package tech.wideas.clad.crypto

/**
 * Cryptographic signing operations.
 * Platform implementations:
 * - Android: Nova Substrate SDK
 * - iOS: SubstrateSdk
 */
interface Signer {
    /**
     * Sign a message with the given keypair.
     *
     * @param message The message bytes to sign
     * @param keypair The keypair to sign with
     * @return The signature bytes (64 bytes)
     */
    fun sign(message: ByteArray, keypair: Keypair): ByteArray

    /**
     * Verify a signature against a message and public key.
     *
     * @param message The original message bytes
     * @param signature The signature to verify (64 bytes)
     * @param publicKey The public key to verify against
     * @param keyType The key type used for signing
     * @return true if signature is valid, false otherwise
     */
    fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean
}

/**
 * Factory function to create platform-specific Signer.
 */
expect fun createSigner(): Signer
