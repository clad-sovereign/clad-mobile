package tech.wideas.clad.crypto

/**
 * Supported cryptographic key types for Substrate/Polkadot accounts.
 *
 * - SR25519: Schnorrkel/Ristretto - Substrate's default, supports key derivation
 * - ED25519: Edwards curve - widely compatible, simpler implementation
 */
enum class KeyType {
    SR25519,
    ED25519
}
