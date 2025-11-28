package tech.wideas.clad.crypto

/**
 * SS58 network prefix for Clad Sovereign addresses.
 *
 * Clad Sovereign is a standalone Substrate chain that uses the generic Substrate
 * prefix (42) for address encoding. This provides compatibility with standard
 * Substrate tooling while maintaining sovereignty as an independent chain.
 *
 * Note: If Clad registers a unique prefix in the SS58 registry in the future,
 * a dedicated `CLAD` constant should be added here.
 *
 * @see <a href="https://github.com/paritytech/ss58-registry">SS58 Registry</a>
 */
object NetworkPrefix {
    /**
     * Clad Sovereign network prefix.
     *
     * Currently uses the generic Substrate prefix (42) as defined in
     * clad-studio's runtime configuration (`runtime/src/lib.rs`).
     */
    const val CLAD: Short = 42

    /** Generic Substrate prefix - alias for [CLAD] during development. */
    const val GENERIC_SUBSTRATE: Short = CLAD
}

/**
 * Represents a cryptographic keypair for Substrate/Polkadot accounts.
 *
 * This class contains sensitive cryptographic material. Implementations should:
 * - Call [clear] when the keypair is no longer needed
 * - Avoid logging or serializing instances (see [toString])
 * - Store encrypted at rest when persistence is required
 *
 * Thread Safety: This class is NOT thread-safe. Concurrent access to the same
 * instance (especially [clear]) must be externally synchronized.
 *
 * @property publicKey The public key bytes (32 bytes for both sr25519 and ed25519).
 * @property privateKey The private key bytes. SENSITIVE: Contains secret key material.
 *   Size varies by key type and SDK (typically 32-64 bytes).
 * @property keyType The cryptographic algorithm used.
 */
data class Keypair(
    val publicKey: ByteArray,
    /**
     * SENSITIVE: This field contains secret cryptographic material.
     * - Do not log, serialize, or expose this value
     * - Call [clear] when no longer needed
     * - Handle with care in memory dumps and crash reports
     */
    val privateKey: ByteArray,
    val keyType: KeyType
) {
    /**
     * Get the SS58 encoded address for this keypair.
     *
     * @param networkPrefix Network identifier. Defaults to [NetworkPrefix.GENERIC_SUBSTRATE] (42).
     * @return SS58 encoded address string.
     * @see NetworkPrefix for common prefix values
     */
    fun toSs58Address(networkPrefix: Short = NetworkPrefix.GENERIC_SUBSTRATE): String {
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
     * Returns a string representation that excludes sensitive private key data.
     * Safe to use in logs and debug output.
     */
    override fun toString(): String {
        return "Keypair(keyType=$keyType, publicKey=[${publicKey.size} bytes], privateKey=[REDACTED])"
    }

    /**
     * Attempts to clear the private key from memory by overwriting with zeros.
     * Call this when the keypair is no longer needed.
     *
     * IMPORTANT: This is a best-effort operation, not cryptographic-grade secure erasure.
     * Limitations:
     * - The JVM may optimize away the fill operation
     * - Garbage collector may have copied the original bytes elsewhere in memory
     * - JIT compilation may cache values in registers
     * - Memory may be swapped to disk before clearing
     *
     * For high-security applications, consider:
     * - Using platform-specific secure memory APIs
     * - Keeping sensitive data in native memory (off-heap)
     * - Minimizing the lifetime of keypair objects
     */
    fun clear() {
        privateKey.fill(0)
    }
}
