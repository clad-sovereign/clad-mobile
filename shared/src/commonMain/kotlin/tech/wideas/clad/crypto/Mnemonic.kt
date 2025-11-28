package tech.wideas.clad.crypto

/**
 * Result of mnemonic validation.
 */
sealed class MnemonicValidationResult {
    data object Valid : MnemonicValidationResult()
    data class Invalid(val reason: String) : MnemonicValidationResult()
}

/**
 * Supported mnemonic word counts for BIP39.
 */
enum class MnemonicWordCount(val count: Int) {
    WORDS_12(12),
    WORDS_24(24)
}

/**
 * BIP39 mnemonic operations.
 * Platform implementations:
 * - Android: Nova Substrate SDK
 * - iOS: SubstrateSdk
 */
interface MnemonicProvider {
    /**
     * Generate a new random mnemonic phrase.
     *
     * @param wordCount Number of words (12 or 24)
     * @return Space-separated mnemonic words
     */
    fun generate(wordCount: MnemonicWordCount = MnemonicWordCount.WORDS_12): String

    /**
     * Validate a mnemonic phrase.
     *
     * @param mnemonic Space-separated mnemonic words
     * @return Validation result
     */
    fun validate(mnemonic: String): MnemonicValidationResult

    /**
     * Derive a seed from a mnemonic phrase.
     *
     * @param mnemonic Space-separated mnemonic words
     * @param passphrase Optional passphrase for additional security
     * @return 32-byte seed
     */
    fun toSeed(mnemonic: String, passphrase: String = ""): ByteArray

    /**
     * Derive a keypair from a mnemonic phrase.
     *
     * @param mnemonic Space-separated mnemonic words
     * @param passphrase Optional passphrase
     * @param keyType The key type to generate
     * @param derivationPath Optional derivation path (e.g., "//hard/soft")
     * @return The derived keypair
     */
    fun toKeypair(
        mnemonic: String,
        passphrase: String = "",
        keyType: KeyType = KeyType.SR25519,
        derivationPath: String = ""
    ): Keypair
}

/**
 * Factory function to create platform-specific MnemonicProvider.
 */
expect fun createMnemonicProvider(): MnemonicProvider
