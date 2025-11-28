package tech.wideas.clad.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Instrumented tests for SS58 address encoding on Android.
 * These tests require running on a device/emulator because the Nova SDK
 * uses native (JNI/Rust) code for cryptographic operations.
 */
@RunWith(AndroidJUnit4::class)
class Ss58Test {

    private val provider = createMnemonicProvider()

    @Test
    fun `encode produces valid SS58 address`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = Ss58.encode(keypair.publicKey, networkPrefix = 42)

        assertTrue(address.isNotBlank(), "Address should not be blank")
        assertTrue(address.length > 40, "SS58 address should be longer than 40 characters")
    }

    @Test
    fun `encode with different network prefixes produces different addresses`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val genericAddress = Ss58.encode(keypair.publicKey, networkPrefix = 42) // Generic Substrate
        val polkadotAddress = Ss58.encode(keypair.publicKey, networkPrefix = 0)  // Polkadot
        val kusamaAddress = Ss58.encode(keypair.publicKey, networkPrefix = 2)    // Kusama

        assertTrue(
            genericAddress != polkadotAddress,
            "Different network prefixes should produce different addresses"
        )
        assertTrue(
            polkadotAddress != kusamaAddress,
            "Different network prefixes should produce different addresses"
        )
    }

    @Test
    fun `decode extracts correct public key and prefix`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val networkPrefix: Short = 42

        val address = Ss58.encode(keypair.publicKey, networkPrefix)
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(address)

        assertTrue(
            decodedPublicKey.contentEquals(keypair.publicKey),
            "Decoded public key should match original"
        )
        assertEquals(networkPrefix, decodedPrefix, "Decoded prefix should match original")
    }

    @Test
    fun `isValid returns true for valid address`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val address = Ss58.encode(keypair.publicKey, networkPrefix = 42)

        assertTrue(Ss58.isValid(address), "Encoded address should be valid")
    }

    @Test
    fun `isValid returns false for invalid address`() {
        assertFalse(Ss58.isValid("not-a-valid-address"), "Garbage should be invalid")
        assertFalse(Ss58.isValid(""), "Empty string should be invalid")
        assertFalse(Ss58.isValid("5"), "Too short should be invalid")
    }

    @Test
    fun `keypair toSs58Address convenience method works`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        val address = keypair.toSs58Address()
        val directAddress = Ss58.encode(keypair.publicKey, networkPrefix = 42)

        assertEquals(address, directAddress, "Convenience method should match direct call")
    }

    @Test
    fun `known test vector - Alice well-known address`() {
        // Alice's well-known SS58 address for generic Substrate (prefix 42)
        // Public key: 0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d
        val alicePublicKey = hexToBytes("d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d")
        val expectedAliceAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        val encodedAddress = Ss58.encode(alicePublicKey, networkPrefix = 42)
        assertEquals(expectedAliceAddress, encodedAddress, "Alice's address should match test vector")

        val (decodedKey, prefix) = Ss58.decode(expectedAliceAddress)
        assertTrue(decodedKey.contentEquals(alicePublicKey), "Decoded key should match Alice's public key")
        assertEquals(42.toShort(), prefix, "Prefix should be 42")
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x")
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
