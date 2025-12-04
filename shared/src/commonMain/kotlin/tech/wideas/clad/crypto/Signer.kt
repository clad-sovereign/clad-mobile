package tech.wideas.clad.crypto

/**
 * SR25519 cryptographic signing operations.
 *
 * Platform implementations:
 * - Android: Nova Substrate SDK
 * - iOS: NovaCrypto (Crypto-iOS)
 */
interface Signer {
    /**
     * Sign a message with the given SR25519 keypair.
     *
     * @param message The message bytes to sign
     * @param keypair The SR25519 keypair to sign with
     * @return The signature bytes (64 bytes)
     */
    fun sign(message: ByteArray, keypair: Keypair): ByteArray

    /**
     * Verify an SR25519 signature against a message and public key.
     *
     * @param message The original message bytes
     * @param signature The signature to verify (64 bytes)
     * @param publicKey The SR25519 public key to verify against
     * @return true if signature is valid, false otherwise
     */
    fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean
}

/**
 * Factory function to create platform-specific Signer.
 */
expect fun createSigner(): Signer
