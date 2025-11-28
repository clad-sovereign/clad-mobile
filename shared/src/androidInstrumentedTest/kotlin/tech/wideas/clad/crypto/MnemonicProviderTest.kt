package tech.wideas.clad.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for MnemonicProvider on Android.
 * These tests require running on a device/emulator because the Nova SDK
 * uses native (JNI/Rust) code for sr25519 cryptographic operations.
 */
@RunWith(AndroidJUnit4::class)
class MnemonicProviderTest {

    private val provider = createMnemonicProvider()

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
        // Generate valid mnemonic then remove words
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

    @Test
    fun `toSeed produces valid seed`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seed = provider.toSeed(mnemonic)
        // Nova SDK's SubstrateSeedFactory.deriveSeed returns 64 bytes (FULL_SEED_LENGTH)
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
    // Derivation Path Tests
    // ============================================================================

    @Test
    fun `toKeypair with hard derivation path produces different keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val masterKeypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val derivedKeypair = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//polkadot"
        )

        assertTrue(
            !masterKeypair.publicKey.contentEquals(derivedKeypair.publicKey),
            "Hard derivation path should produce different public key"
        )
    }

    @Test
    fun `toKeypair with soft derivation path produces different keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val masterKeypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val derivedKeypair = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "/soft"
        )

        assertTrue(
            !masterKeypair.publicKey.contentEquals(derivedKeypair.publicKey),
            "Soft derivation path should produce different public key"
        )
    }

    @Test
    fun `toKeypair with mixed derivation path produces different keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val masterKeypair = provider.toKeypair(mnemonic, keyType = KeyType.SR25519)
        val derivedKeypair = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//hard/soft"
        )

        assertTrue(
            !masterKeypair.publicKey.contentEquals(derivedKeypair.publicKey),
            "Mixed derivation path should produce different public key"
        )
    }

    @Test
    fun `toKeypair derivation path is deterministic`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val derivedKeypair1 = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//polkadot//staking"
        )
        val derivedKeypair2 = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//polkadot//staking"
        )

        assertTrue(
            derivedKeypair1.publicKey.contentEquals(derivedKeypair2.publicKey),
            "Same derivation path should produce same public key"
        )
    }

    @Test
    fun `different derivation paths produce different keypairs`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair1 = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//polkadot"
        )
        val keypair2 = provider.toKeypair(
            mnemonic,
            keyType = KeyType.SR25519,
            derivationPath = "//kusama"
        )

        assertTrue(
            !keypair1.publicKey.contentEquals(keypair2.publicKey),
            "Different derivation paths should produce different public keys"
        )
    }

    // ============================================================================
    // Passphrase Edge Case Tests
    // ============================================================================

    @Test
    fun `toSeed with unicode passphrase works`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seedWithUnicode = provider.toSeed(mnemonic, "密码パスワード🔐")
        val seedWithAscii = provider.toSeed(mnemonic, "password")

        assertEquals(64, seedWithUnicode.size, "Unicode passphrase seed should be 64 bytes")
        assertTrue(
            !seedWithUnicode.contentEquals(seedWithAscii),
            "Unicode passphrase should produce different seed than ASCII"
        )
    }

    @Test
    fun `toSeed with very long passphrase works`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val longPassphrase = "a".repeat(1000)
        val seedWithLongPass = provider.toSeed(mnemonic, longPassphrase)

        assertEquals(64, seedWithLongPass.size, "Long passphrase seed should be 64 bytes")
    }

    @Test
    fun `toSeed with whitespace passphrase is distinct from empty`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val seedEmpty = provider.toSeed(mnemonic, "")
        val seedWhitespace = provider.toSeed(mnemonic, "   ")

        assertTrue(
            !seedEmpty.contentEquals(seedWhitespace),
            "Whitespace passphrase should produce different seed than empty"
        )
    }

    @Test
    fun `toKeypair with unicode passphrase produces valid keypair`() {
        val mnemonic = provider.generate(MnemonicWordCount.WORDS_12)
        val keypair = provider.toKeypair(
            mnemonic,
            passphrase = "日本語パスフレーズ",
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

    // ============================================================================
    // Cross-Platform Determinism Tests (Known Test Vectors)
    // ============================================================================

    /**
     * CRITICAL TEST: Verifies Android produces the same SR25519 keypair as Substrate subkey.
     * If this test fails, Android wallets will NOT be recoverable on other platforms!
     *
     * Test vector from Substrate subkey documentation:
     * `subkey inspect "caution juice atom organ advance problem want pledge someone senior holiday very"`
     *
     * NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector1 (shared/commonTest).
     * Duplication required because androidInstrumentedTest cannot access commonTest sources.
     * See also: iosApp/iosAppTests/MnemonicProviderTests.swift (iOS equivalent)
     *
     * This test ensures cross-platform compatibility with:
     * - iOS (NovaCrypto) - tested in iosAppTests/MnemonicProviderTests.swift
     * - Web wallets (polkadot.js)
     * - Substrate CLI (subkey)
     */
    @Test
    fun `known mnemonic produces expected sr25519 public key`() {
        // Values from CrossPlatformDeterminismTest.TestVector1
        val testMnemonic = "caution juice atom organ advance problem want pledge someone senior holiday very"
        val expectedPublicKeyHex = "d6a3105d6768e956e9e5d41050ac29843f98561410d3a47f9dd5b3b227ab8746"

        val keypair = provider.toKeypair(
            mnemonic = testMnemonic,
            passphrase = "",
            keyType = KeyType.SR25519,
            derivationPath = ""
        )

        val actualPublicKeyHex = keypair.publicKey.toHexString()

        assertEquals(
            expectedPublicKeyHex,
            actualPublicKeyHex,
            """
            CROSS-PLATFORM DETERMINISM FAILURE!

            The SR25519 public key derived from the test mnemonic does not match
            the expected value from Substrate subkey.

            Mnemonic: "$testMnemonic"
            Expected: $expectedPublicKeyHex
            Actual:   $actualPublicKeyHex

            This means Android will generate DIFFERENT addresses than iOS/web wallets
            from the same recovery phrase, breaking wallet portability!
            """.trimIndent()
        )
    }

    /**
     * Verifies Android produces the correct SS58 address from a known mnemonic using SR25519.
     * This complements the SR25519 public key test above and validates the full
     * mnemonic → keypair → address pipeline for SR25519.
     *
     * NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector1 (shared/commonTest).
     * Duplication required because androidInstrumentedTest cannot access commonTest sources.
     * See also: iosApp/iosAppTests/MnemonicProviderTests.swift (iOS equivalent)
     */
    @Test
    fun `known mnemonic produces expected sr25519 ss58 address`() {
        // Values from CrossPlatformDeterminismTest.TestVector1
        val testMnemonic = "caution juice atom organ advance problem want pledge someone senior holiday very"
        val expectedAddress = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"

        val keypair = provider.toKeypair(
            mnemonic = testMnemonic,
            passphrase = "",
            keyType = KeyType.SR25519,
            derivationPath = ""
        )

        val actualAddress = Ss58.encode(keypair.publicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            expectedAddress,
            actualAddress,
            """
            SS58 address mismatch!

            Expected: $expectedAddress
            Actual:   $actualAddress
            """.trimIndent()
        )
    }

    /**
     * CRITICAL TEST: Verifies Android produces the same ED25519 keypair as Substrate subkey.
     * If this test fails, Android ED25519 wallets will NOT be recoverable on other platforms!
     *
     * Test vector from Substrate subkey documentation:
     * `subkey inspect --scheme ed25519 "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"`
     *
     * NOTE: These values are duplicated from CrossPlatformDeterminismTest.TestVector2Ed25519 (commonTest)
     * because androidInstrumentedTest cannot access commonTest sources. The single source of truth
     * is CrossPlatformDeterminismTest.TestVector2Ed25519.
     * See also: iosAppTests/MnemonicProviderTests.swift for iOS equivalent.
     *
     * This test ensures cross-platform compatibility with:
     * - iOS (NovaCrypto) - tested in iosAppTests/MnemonicProviderTests.swift
     * - Web wallets (polkadot.js)
     * - Substrate CLI (subkey)
     */
    @Test
    fun `known mnemonic produces expected ed25519 public key`() {
        // Values from CrossPlatformDeterminismTest.TestVector2Ed25519
        val testMnemonic = "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"
        val expectedPublicKeyHex = "1a0e2bf1e0195a1f5396c5fd209a620a48fe90f6f336d89c89405a0183a857a3"

        val keypair = provider.toKeypair(
            mnemonic = testMnemonic,
            passphrase = "",
            keyType = KeyType.ED25519,
            derivationPath = ""
        )

        val actualPublicKeyHex = keypair.publicKey.toHexString()

        assertEquals(
            expectedPublicKeyHex,
            actualPublicKeyHex,
            """
            CROSS-PLATFORM DETERMINISM FAILURE!

            The ED25519 public key derived from the test mnemonic does not match
            the expected value from Substrate subkey.

            Mnemonic: "$testMnemonic"
            Expected: $expectedPublicKeyHex
            Actual:   $actualPublicKeyHex

            This means Android will generate DIFFERENT ED25519 addresses than iOS/web wallets
            from the same recovery phrase, breaking wallet portability!
            """.trimIndent()
        )
    }

    /**
     * Verifies Android produces the correct SS58 address from a known mnemonic using ED25519.
     * This complements the ED25519 public key test above and validates the full
     * mnemonic → keypair → address pipeline for ED25519.
     *
     * NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector2Ed25519 (commonTest).
     * See also: iosAppTests/MnemonicProviderTests.swift for iOS equivalent.
     */
    @Test
    fun `known mnemonic produces expected ed25519 ss58 address`() {
        // Values from CrossPlatformDeterminismTest.TestVector2Ed25519
        val testMnemonic = "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"
        val expectedAddress = "5CesK3uTmn4NGfD3oyGBd1jrp4EfRyYdtqL3ERe9SXv8jUHb"

        val keypair = provider.toKeypair(
            mnemonic = testMnemonic,
            passphrase = "",
            keyType = KeyType.ED25519,
            derivationPath = ""
        )

        val actualAddress = Ss58.encode(keypair.publicKey, networkPrefix = NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            expectedAddress,
            actualAddress,
            """
            ED25519 SS58 address mismatch!

            Expected: $expectedAddress
            Actual:   $actualAddress
            """.trimIndent()
        )
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun ByteArray.toHexString(): String =
        joinToString("") { byte -> "%02x".format(byte) }
}
