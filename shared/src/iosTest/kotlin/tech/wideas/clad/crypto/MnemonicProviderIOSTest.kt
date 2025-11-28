package tech.wideas.clad.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * iOS tests for MnemonicProvider.
 *
 * These tests verify the iOS implementation using NovaCrypto library
 * for BIP39 mnemonic generation and key derivation.
 *
 * Note: Derivation path tests are excluded as the current iOS implementation
 * does not yet support Substrate-style derivation paths (//hard/soft).
 * This will be addressed in a future PR.
 */
class MnemonicProviderIOSTest {

    private val provider = createMnemonicProvider()

    // ============================================================================
    // Mnemonic Generation Tests
    // ============================================================================

    @Test
    fun `generate creates 12 word mnemonic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val words = mnemonic.split(" ")
        assertEquals(12, words.size, "Should generate 12 words")
        assertTrue(words.all { it.isNotBlank() }, "All words should be non-blank")
    }

    @Test
    fun `generate creates 24 word mnemonic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_24)
        val words = mnemonic.split(" ")
        assertEquals(24, words.size, "Should generate 24 words")
        assertTrue(words.all { it.isNotBlank() }, "All words should be non-blank")
    }

    @Test
    fun `generate creates unique mnemonics`() {
        val mnemonic1 = provider.generate(MnemonicWordCount.WORDS_12)
        val mnemonic2 = provider.generate(MnemonicWordCount.WORDS_12)
        assertNotEquals(mnemonic1, mnemonic2, "Each generated mnemonic should be unique")
    }

    // ============================================================================
    // Mnemonic Validation Tests
    // ============================================================================

    @Test
    fun `validate returns valid for correct mnemonic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val result = provider.validate(mnemonic)
        assertIs<MnemonicValidationResult.Valid>(result, "Generated mnemonic should be valid")
    }

    @Test
    fun `validate returns invalid for garbage input`() {
        val result = provider.validate("this is not a valid mnemonic phrase at all")
        assertIs<MnemonicValidationResult.Invalid>(result, "Garbage input should be invalid")
    }

    @Test
    fun `validate returns invalid for wrong word count`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val truncated = mnemonic.split(" ").take(11).joinToString(" ")
        val result = provider.validate(truncated)
        assertIs<MnemonicValidationResult.Invalid>(result, "11 words should be invalid")
    }

    @Test
    fun `validate returns invalid for empty string`() {
        val result = provider.validate("")
        assertIs<MnemonicValidationResult.Invalid>(result, "Empty string should be invalid")
    }

    // ============================================================================
    // Seed Derivation Tests
    // ============================================================================

    @Test
    fun `toSeed produces valid seed`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seed = provider.toSeed(mnemonic)
        assertEquals(64, seed.size, "Seed should be 64 bytes, got ${seed.size}")
    }

    @Test
    fun `toSeed is deterministic for same mnemonic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seed1 = provider.toSeed(mnemonic)
        val seed2 = provider.toSeed(mnemonic)
        assertTrue(seed1.contentEquals(seed2), "Same mnemonic should produce same seed")
    }

    @Test
    fun `toSeed with passphrase produces different seed`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seedNoPass = provider.toSeed(mnemonic)
        val seedWithPass = provider.toSeed(mnemonic, "my-passphrase")
        assertTrue(!seedNoPass.contentEquals(seedWithPass), "Passphrase should change seed")
    }

    // ============================================================================
    // Keypair Generation Tests
    // ============================================================================

    @Test
    fun `toKeypair generates sr25519 keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        assertEquals(KeyType.SR25519, keypair.keyType)
        assertEquals(32, keypair.publicKey.size, "Public key should be 32 bytes")
        assertTrue(keypair.privateKey.isNotEmpty(), "Private key should not be empty")
    }

    @Test
    fun `toKeypair generates ed25519 keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(mnemonic, keyType = KeyType.ED25519)

        assertEquals(KeyType.ED25519, keypair.keyType)
        assertEquals(32, keypair.publicKey.size, "Public key should be 32 bytes")
        assertTrue(keypair.privateKey.isNotEmpty(), "Private key should not be empty")
    }

    @Test
    fun `toKeypair is deterministic for same mnemonic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair1 = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val keypair2 = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)

        assertTrue(
            keypair1.publicKey.contentEquals(keypair2.publicKey),
            "Same mnemonic should produce same public key"
        )
    }

    @Test
    fun `different mnemonics produce different keypairs`() {
        val mnemonic1 = provider.generate(MnemonicWordCount.WORDS_12)
        val mnemonic2 = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair1 = provider.toKeypair(mnemonic1, keyType = KeyType.SR25519)
        val keypair2 = provider.toKeypair(mnemonic2, keyType = KeyType.SR25519)

        assertTrue(
            !keypair1.publicKey.contentEquals(keypair2.publicKey),
            "Different mnemonics should produce different public keys"
        )
    }

    // ============================================================================
    // Passphrase Tests
    // ============================================================================

    @Test
    fun `toKeypair with passphrase produces valid keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(
            mnemonic,
            passphrase = "test-passphrase",
            keyType = KeyType.SR25519
        )

        assertEquals(32, keypair.publicKey.size, "Public key should be 32 bytes")
        assertTrue(keypair.privateKey.isNotEmpty(), "Private key should not be empty")
    }

    @Test
    fun `toKeypair passphrase is case sensitive`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypairLower = provider.toKeypair(mnemonic, passphrase = "password", keyType = KeyType.SR25519)
        val keypairUpper = provider.toKeypair(mnemonic, passphrase = "PASSWORD", keyType = KeyType.SR25519)

        assertTrue(
            !keypairLower.publicKey.contentEquals(keypairUpper.publicKey),
            "Passphrase should be case-sensitive"
        )
    }
}
