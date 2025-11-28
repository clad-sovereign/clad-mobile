package tech.wideas.clad.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * iOS tests for SS58 address encoding.
 *
 * These tests verify the iOS implementation using NovaCrypto's SS58AddressFactory
 * for SS58 address encoding and decoding.
 */
class Ss58IOSTest {

    private val provider = createMnemonicProvider()

    // ============================================================================
    // Encoding Tests
    // ============================================================================

    @Test
    fun `encode produces valid SS58 address`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = Ss58.encode(keypair.publicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)

        assertTrue(address.isNotBlank(), "Address should not be blank")
        assertTrue(address.length > 40, "SS58 address should be longer than 40 characters")
    }

    @Test
    fun `encode throws for invalid public key size`() {
        val tooShort = ByteArray(31) { 0 }
        val tooLong = ByteArray(33) { 0 }
        val empty = ByteArray(0)

        assertFailsWith<IllegalArgumentException>("Should reject 31-byte key") {
            Ss58.encode(tooShort)
        }
        assertFailsWith<IllegalArgumentException>("Should reject 33-byte key") {
            Ss58.encode(tooLong)
        }
        assertFailsWith<IllegalArgumentException>("Should reject empty key") {
            Ss58.encode(empty)
        }
    }

    // ============================================================================
    // Decoding Tests
    // ============================================================================

    @Test
    fun `decode extracts correct public key and prefix`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = Ss58.encode(keypair.publicKey, NetworkPrefix.GENERIC_SUBSTRATE)
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(address)

        assertTrue(
            decodedPublicKey.contentEquals(keypair.publicKey),
            "Decoded public key should match original"
        )
        assertEquals(NetworkPrefix.GENERIC_SUBSTRATE, decodedPrefix, "Decoded prefix should match original")
    }

    @Test
    fun `encode and decode roundtrip works`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = Ss58.encode(keypair.publicKey, NetworkPrefix.GENERIC_SUBSTRATE)
        val (decodedPublicKey, _) = Ss58.decode(address)

        val reEncodedAddress = Ss58.encode(decodedPublicKey, NetworkPrefix.GENERIC_SUBSTRATE)
        assertEquals(address, reEncodedAddress, "Re-encoded address should match original")
    }

    // ============================================================================
    // Validation Tests
    // ============================================================================

    @Test
    fun `isValid returns true for valid address`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val address = Ss58.encode(keypair.publicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)

        assertTrue(Ss58.isValid(address), "Encoded address should be valid")
    }

    @Test
    fun `isValid returns false for invalid address`() {
        assertFalse(Ss58.isValid("not-a-valid-address"), "Garbage should be invalid")
        assertFalse(Ss58.isValid(""), "Empty string should be invalid")
        assertFalse(Ss58.isValid("5"), "Too short should be invalid")
    }

    // ============================================================================
    // Keypair Convenience Method Tests
    // ============================================================================

    @Test
    fun `keypair toSs58Address convenience method works`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = keypair.toSs58Address()
        val directAddress = Ss58.encode(keypair.publicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(address, directAddress, "Convenience method should match direct call")
    }

    // ============================================================================
    // Known Test Vector Tests
    // ============================================================================

    @Test
    fun `known test vector - Alice well-known address`() {
        // Alice's well-known SS58 address for generic Substrate (prefix 42)
        // Public key: 0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d
        val alicePublicKey = hexToBytes("d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d")
        val expectedAliceAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        val encodedAddress = Ss58.encode(alicePublicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)
        assertEquals(expectedAliceAddress, encodedAddress, "Alice's address should match test vector")

        val (decodedKey, prefix) = Ss58.decode(expectedAliceAddress)
        assertTrue(decodedKey.contentEquals(alicePublicKey), "Decoded key should match Alice's public key")
        assertEquals(NetworkPrefix.GENERIC_SUBSTRATE, prefix, "Prefix should be GENERIC_SUBSTRATE (42)")
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x")
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
