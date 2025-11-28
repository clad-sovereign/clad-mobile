package tech.wideas.clad.crypto

/**
 * SS58 address encoding/decoding for Substrate/Polkadot.
 *
 * SS58 is a simple address format designed for Substrate-based chains.
 * Format: base58(concat(prefix, pubkey, checksum))
 *
 * Common network prefixes:
 * - 0: Polkadot
 * - 2: Kusama
 * - 42: Generic Substrate (used for development)
 */
expect object Ss58 {
    /**
     * Encode a public key to SS58 address format.
     *
     * @param publicKey 32-byte public key
     * @param networkPrefix Network identifier (default: 42 for generic Substrate)
     * @return SS58 encoded address string
     */
    fun encode(publicKey: ByteArray, networkPrefix: Short = 42): String

    /**
     * Decode an SS58 address to its public key.
     *
     * @param address SS58 encoded address
     * @return Pair of (publicKey, networkPrefix)
     * @throws IllegalArgumentException if address is invalid
     */
    fun decode(address: String): Pair<ByteArray, Short>

    /**
     * Validate an SS58 address format.
     *
     * @param address SS58 encoded address
     * @return true if valid, false otherwise
     */
    fun isValid(address: String): Boolean
}
