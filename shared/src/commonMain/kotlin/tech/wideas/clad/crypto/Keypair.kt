package tech.wideas.clad.crypto

/**
 * Represents a cryptographic keypair for Substrate/Polkadot accounts.
 *
 * @property publicKey The public key bytes (32 bytes for both sr25519 and ed25519)
 * @property privateKey The private key/seed bytes (32 bytes for seed, 64 bytes for full keypair)
 * @property keyType The cryptographic algorithm used
 */
data class Keypair(
    val publicKey: ByteArray,
    val privateKey: ByteArray,
    val keyType: KeyType
) {
    /**
     * Get the SS58 encoded address for this keypair.
     * Uses network prefix 42 (generic Substrate) by default.
     */
    fun toSs58Address(networkPrefix: Short = 42): String {
        return Ss58.encode(publicKey, networkPrefix)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Keypair

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (keyType != other.keyType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + keyType.hashCode()
        return result
    }

    /**
     * Securely clear the private key from memory.
     * Call this when the keypair is no longer needed.
     */
    fun clear() {
        privateKey.fill(0)
    }
}
