package tech.wideas.clad.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform determinism tests for SS58 address encoding.
 *
 * These tests verify that SS58 encoding/decoding works identically across platforms.
 * They use well-known test vectors that don't require native crypto libraries.
 *
 * NOTE: Keypair generation tests are in platform-specific test directories
 * (androidTest, iosTest) because they require native crypto libraries that
 * aren't available in JVM unit tests.
 *
 * Test vectors sourced from:
 * - Substrate subkey documentation
 * - https://github.com/polkadot-developers/substrate-developer-hub.github.io
 */
class CrossPlatformDeterminismTest {

    /**
     * Test Vector: Alice's well-known public key and SS58 address
     * This is used to verify SS58 encoding/decoding without native crypto.
     */
    object AliceTestVector {
        const val PUBLIC_KEY_HEX = "d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d"
        const val SS58_ADDRESS = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
    }

    /**
     * Test Vector 1: Expected output for mnemonic without derivation path
     * Used by platform-specific tests to verify keypair generation.
     */
    object TestVector1 {
        const val MNEMONIC = "caution juice atom organ advance problem want pledge someone senior holiday very"
        const val EXPECTED_PUBLIC_KEY_HEX = "d6a3105d6768e956e9e5d41050ac29843f98561410d3a47f9dd5b3b227ab8746"
        const val EXPECTED_SS58_ADDRESS = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"
    }

    // ============================================================================
    // SS58 Address Encoding Determinism (No native crypto required)
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

    @Test
    fun `SS58 encoding for TestVector1 expected public key produces expected address`() {
        val publicKey = hexToBytes(TestVector1.EXPECTED_PUBLIC_KEY_HEX)
        val address = Ss58.encode(publicKey, NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            TestVector1.EXPECTED_SS58_ADDRESS,
            address,
            "SS58 encoding of TestVector1 public key should produce expected address"
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
