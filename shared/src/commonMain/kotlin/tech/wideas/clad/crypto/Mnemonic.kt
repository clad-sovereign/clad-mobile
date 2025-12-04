package tech.wideas.clad.crypto

/**
 * Result of mnemonic validation.
 */
sealed class MnemonicValidationResult {
    /** The mnemonic is valid according to BIP39 specification. */
    data object Valid : MnemonicValidationResult()
    /** The mnemonic is invalid. [reason] describes the validation failure. */
    data class Invalid(val reason: String) : MnemonicValidationResult()
}

/**
 * Supported mnemonic word counts for BIP39.
 *
 * @property count The number of words in the mnemonic phrase.
 */
enum class MnemonicWordCount(val count: Int) {
    /** 12-word mnemonic (128 bits of entropy). */
    WORDS_12(12),
    /** 24-word mnemonic (256 bits of entropy). */
    WORDS_24(24)
}

/**
 * BIP39 mnemonic operations for Substrate/Polkadot key derivation.
 *
 * Platform implementations:
 * - Android: Nova Substrate SDK (`substrate-sdk-android`)
 * - iOS: SubstrateSdk (CocoaPods)
 *
 * Thread Safety: Implementations are NOT thread-safe. If you need to use
 * [MnemonicProvider] from multiple threads, you must provide external synchronization
 * or create separate instances per thread.
 *
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki">BIP39 Specification</a>
 */
interface MnemonicProvider {
    /**
     * Generate a new random mnemonic phrase.
     *
     * @param wordCount Number of words (12 or 24). Defaults to [MnemonicWordCount.WORDS_12].
     * @return Space-separated mnemonic words from the BIP39 English wordlist.
     */
    fun generate(wordCount: MnemonicWordCount = MnemonicWordCount.WORDS_12): String

    /**
     * Validate a mnemonic phrase against BIP39 specification.
     *
     * Checks:
     * - Word count is valid (12 or 24)
     * - All words are in the BIP39 English wordlist
     * - Checksum is correct
     *
     * @param mnemonic Space-separated mnemonic words.
     * @return [MnemonicValidationResult.Valid] if valid, [MnemonicValidationResult.Invalid] otherwise.
     */
    fun validate(mnemonic: String): MnemonicValidationResult

    /**
     * Derive a seed from a mnemonic phrase.
     *
     * Uses Substrate's seed derivation (not standard BIP39 PBKDF2).
     *
     * @param mnemonic Space-separated mnemonic words.
     * @param passphrase Optional passphrase for additional security. Empty string means no passphrase.
     * @return 64-byte seed (Nova SDK's FULL_SEED_LENGTH).
     */
    fun toSeed(mnemonic: String, passphrase: String = ""): ByteArray

    /**
     * Derive an SR25519 keypair from a mnemonic phrase.
     *
     * @param mnemonic Space-separated mnemonic words.
     * @param passphrase Optional passphrase for additional security.
     * @param derivationPath Optional Substrate derivation path (e.g., "//hard/soft").
     *   Use "//" for hard derivation and "/" for soft derivation.
     *   Empty string means no derivation (master key).
     * @return The derived [Keypair]. Caller is responsible for calling [Keypair.clear] when done.
     */
    fun toKeypair(
        mnemonic: String,
        passphrase: String = "",
        derivationPath: String = ""
    ): Keypair
}

/**
 * Factory function to create platform-specific [MnemonicProvider].
 *
 * @return A new [MnemonicProvider] instance for the current platform.
 */
expect fun createMnemonicProvider(): MnemonicProvider
