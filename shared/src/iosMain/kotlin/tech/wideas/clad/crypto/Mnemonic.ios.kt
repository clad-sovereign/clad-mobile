package tech.wideas.clad.crypto

/**
 * iOS implementation of [MnemonicProvider].
 *
 * This is a stub implementation. Full integration requires Nova SubstrateSdk via CocoaPods/cinterop.
 *
 * **Required Setup:**
 * 1. Add to Podfile: `pod 'SubstrateSdk', :git => 'https://github.com/nova-wallet/substrate-sdk-ios.git'`
 * 2. Create cinterop `.def` file for SubstrateSdk
 * 3. Configure cinterop in `shared/build.gradle.kts`
 *
 * **Classes needed from SubstrateSdk:**
 * - `IRMnemonicCreator` - Mnemonic generation and validation
 * - `IRCryptoType` - Key type selection (sr25519, ed25519)
 * - `IRKeypairFactory` - Keypair derivation
 *
 * Thread Safety: This class is NOT thread-safe. Create separate instances
 * or synchronize access externally.
 *
 * @see <a href="https://github.com/nova-wallet/substrate-sdk-ios">Nova SubstrateSdk (iOS)</a>
 */
class IOSMnemonicProvider : MnemonicProvider {

    override fun generate(wordCount: MnemonicWordCount): String {
        // TODO: Use SubstrateSdk's IRMnemonicCreator.randomMnemonic()
        // val length = when (wordCount) {
        //     MnemonicWordCount.WORDS_12 -> IRMnemonicLength.twelve
        //     MnemonicWordCount.WORDS_24 -> IRMnemonicLength.twentyFour
        // }
        // return IRMnemonicCreator.mnemonic(fromLength: length)
        throw NotImplementedError("iOS mnemonic generation requires SubstrateSdk integration")
    }

    override fun validate(mnemonic: String): MnemonicValidationResult {
        // TODO: Use SubstrateSdk's IRMnemonicCreator validation
        throw NotImplementedError("iOS mnemonic validation requires SubstrateSdk integration")
    }

    override fun toSeed(mnemonic: String, passphrase: String): ByteArray {
        // TODO: Use SubstrateSdk's seed derivation
        throw NotImplementedError("iOS seed derivation requires SubstrateSdk integration")
    }

    override fun toKeypair(
        mnemonic: String,
        passphrase: String,
        keyType: KeyType,
        derivationPath: String
    ): Keypair {
        // TODO: Use SubstrateSdk's IRKeypairFactory with IRCryptoType.sr25519/ed25519
        throw NotImplementedError("iOS keypair generation requires SubstrateSdk integration")
    }
}

actual fun createMnemonicProvider(): MnemonicProvider = IOSMnemonicProvider()
