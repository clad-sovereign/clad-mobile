package tech.wideas.clad.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform determinism tests for cryptographic operations.
 *
 * These tests verify that both Android and iOS implementations produce
 * identical results for the same inputs. This is critical for ensuring
 * that a wallet created on one platform can be recovered on another.
 *
 * The tests use well-known test vectors from the Substrate ecosystem to validate that:
 * 1. The same mnemonic produces the expected keypair on both platforms
 * 2. The same public key produces the expected SS58 address on both platforms
 *
 * Test vectors sourced from:
 * - Substrate subkey documentation
 * - https://github.com/polkadot-developers/substrate-developer-hub.github.io
 *
 * IMPORTANT: These test mnemonics are PUBLIC and should NEVER be used in production.
 */
class CrossPlatformDeterminismTest {

    private val provider = createMnemonicProvider()

    // ============================================================================
    // Known Test Vectors from Substrate Subkey
    // ============================================================================

    /**
     * Test Vector 1: 12-word mnemonic without derivation path
     * Source: Substrate subkey documentation
     *
     * Command: `subkey inspect "caution juice atom organ advance problem want pledge someone senior holiday very"`
     */
    object TestVector1 {
        const val MNEMONIC = "caution juice atom organ advance problem want pledge someone senior holiday very"
        const val EXPECTED_PUBLIC_KEY_HEX = "d6a3105d6768e956e9e5d41050ac29843f98561410d3a47f9dd5b3b227ab8746"
        const val EXPECTED_SS58_ADDRESS = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"
    }

    /**
     * Test Vector 2: Alice's well-known public key and SS58 address
     * Note: Alice's key is derived using "//Alice" derivation path from the dev seed,
     * but we use this to test SS58 encoding/decoding with a known public key.
     */
    object AliceTestVector {
        const val PUBLIC_KEY_HEX = "d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d"
        const val SS58_ADDRESS = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
    }

    // ============================================================================
    // CRITICAL: Cross-Platform Keypair Generation Test
    // ============================================================================

    @Test
    fun `SR25519 keypair from known mnemonic matches expected public key`() {
        val keypair = provider.toKeypair(
            mnemonic = TestVector1.MNEMONIC,
            passphrase = "",
            keyType = KeyType.SR25519,
            derivationPath = ""
        )

        val actualPublicKeyHex = keypair.publicKey.toHexString()
        assertEquals(
            TestVector1.EXPECTED_PUBLIC_KEY_HEX,
            actualPublicKeyHex,
            """
            CROSS-PLATFORM DETERMINISM FAILURE!

            The SR25519 public key derived from the test mnemonic does not match
            the expected value from Substrate subkey.

            Mnemonic: "${TestVector1.MNEMONIC}"
            Expected: ${TestVector1.EXPECTED_PUBLIC_KEY_HEX}
            Actual:   $actualPublicKeyHex

            This means Android and iOS will generate DIFFERENT addresses from the
            same recovery phrase, breaking wallet portability!
            """.trimIndent()
        )
    }

    @Test
    fun `SR25519 keypair produces expected SS58 address`() {
        val keypair = provider.toKeypair(
            mnemonic = TestVector1.MNEMONIC,
            passphrase = "",
            keyType = KeyType.SR25519,
            derivationPath = ""
        )

        val address = Ss58.encode(keypair.publicKey, NetworkPrefix.GENERIC_SUBSTRATE)
        assertEquals(
            TestVector1.EXPECTED_SS58_ADDRESS,
            address,
            """
            SS58 address mismatch!

            Expected: ${TestVector1.EXPECTED_SS58_ADDRESS}
            Actual:   $address
            """.trimIndent()
        )
    }

    // ============================================================================
    // SS58 Address Encoding Determinism
    // ============================================================================

    @Test
    fun `SS58 encoding produces correct address for Alice test vector`() {
        val alicePublicKey = hexToBytes(AliceTestVector.PUBLIC_KEY_HEX)
        val encodedAddress = Ss58.encode(alicePublicKey, NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            AliceTestVector.SS58_ADDRESS,
            encodedAddress,
            "SS58 encoding should produce Alice's well-known address"
        )
    }

    @Test
    fun `SS58 decode extracts correct public key from Alice address`() {
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(AliceTestVector.SS58_ADDRESS)

        assertEquals(
            AliceTestVector.PUBLIC_KEY_HEX,
            decodedPublicKey.toHexString(),
            "Decoded public key should match Alice's known public key"
        )
        assertEquals(
            NetworkPrefix.GENERIC_SUBSTRATE,
            decodedPrefix,
            "Decoded prefix should be GENERIC_SUBSTRATE (42)"
        )
    }

    @Test
    fun `SS58 decode and re-encode produces identical address`() {
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(AliceTestVector.SS58_ADDRESS)
        val reEncodedAddress = Ss58.encode(decodedPublicKey, decodedPrefix)

        assertEquals(
            AliceTestVector.SS58_ADDRESS,
            reEncodedAddress,
            "Decode and re-encode should produce identical address"
        )
    }

    // ============================================================================
    // Mnemonic Validation
    // ============================================================================

    @Test
    fun `test vector mnemonic is recognized as valid`() {
        val result = provider.validate(TestVector1.MNEMONIC)
        assertTrue(
            result is MnemonicValidationResult.Valid,
            "Test vector mnemonic should be valid on all platforms"
        )
    }

    // ============================================================================
    // Internal Consistency Tests
    // ============================================================================

    @Test
    fun `SR25519 keypair generation is internally deterministic`() {
        val keypair1 = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.SR25519, "")
        val keypair2 = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.SR25519, "")

        assertTrue(
            keypair1.publicKey.contentEquals(keypair2.publicKey),
            "Same mnemonic should produce identical SR25519 public key on repeated calls"
        )
    }

    @Test
    fun `ED25519 keypair generation is internally deterministic`() {
        val keypair1 = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.ED25519, "")
        val keypair2 = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.ED25519, "")

        assertTrue(
            keypair1.publicKey.contentEquals(keypair2.publicKey),
            "Same mnemonic should produce identical ED25519 public key on repeated calls"
        )
    }

    @Test
    fun `SR25519 and ED25519 produce different keypairs from same mnemonic`() {
        val sr25519Keypair = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.SR25519, "")
        val ed25519Keypair = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.ED25519, "")

        assertTrue(
            !sr25519Keypair.publicKey.contentEquals(ed25519Keypair.publicKey),
            "Different key types should produce different public keys"
        )
    }

    @Test
    fun `passphrase changes derived keypair`() {
        val keypairNoPass = provider.toKeypair(TestVector1.MNEMONIC, "", KeyType.SR25519, "")
        val keypairWithPass = provider.toKeypair(TestVector1.MNEMONIC, "test-passphrase", KeyType.SR25519, "")

        assertTrue(
            !keypairNoPass.publicKey.contentEquals(keypairWithPass.publicKey),
            "Passphrase should produce different keypair"
        )
    }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x")
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { byte -> "%02x".format(byte) }
    }
}
