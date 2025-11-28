package tech.wideas.clad.crypto

/** Expected size of a Substrate/Polkadot public key in bytes. */
const val PUBLIC_KEY_SIZE = 32

/**
 * SS58 address encoding/decoding for Clad Sovereign.
 *
 * SS58 is a simple address format designed for Substrate-based chains.
 * Format: base58(concat(prefix, pubkey, checksum))
 *
 * Clad Sovereign uses [NetworkPrefix.CLAD] (42) as its network prefix,
 * which is the generic Substrate prefix providing compatibility with
 * standard tooling.
 *
 * @see NetworkPrefix for available prefix constants
 * @see <a href="https://docs.substrate.io/v3/advanced/ss58/">SS58 Address Format</a>
 */
expect object Ss58 {
    /**
     * Encode a public key to SS58 address format.
     *
     * @param publicKey 32-byte public key. Must be exactly [PUBLIC_KEY_SIZE] bytes.
     * @param networkPrefix Network identifier. Defaults to [NetworkPrefix.GENERIC_SUBSTRATE] (42).
     * @return SS58 encoded address string.
     * @throws IllegalArgumentException if publicKey is not exactly 32 bytes.
     */
    fun encode(publicKey: ByteArray, networkPrefix: Short = NetworkPrefix.GENERIC_SUBSTRATE): String

    /**
     * Decode an SS58 address to its public key.
     *
     * @param address SS58 encoded address.
     * @return Pair of (publicKey, networkPrefix).
     * @throws IllegalArgumentException if address is invalid.
     */
    fun decode(address: String): Pair<ByteArray, Short>

    /**
     * Validate an SS58 address format.
     *
     * @param address SS58 encoded address.
     * @return true if valid, false otherwise.
     */
    fun isValid(address: String): Boolean
}
