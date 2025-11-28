package tech.wideas.clad.crypto

import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.seed.substrate.SubstrateSeedFactory

/**
 * Android implementation of [MnemonicProvider] using Nova Substrate SDK.
 *
 * This implementation uses the `substrate-sdk-android` library which provides
 * native Rust-based cryptographic operations via JNI for sr25519 (Schnorrkel).
 *
 * Thread Safety: This class is NOT thread-safe. The underlying Nova SDK
 * operations may not be safe for concurrent use. Create separate instances
 * or synchronize access externally.
 *
 * @see <a href="https://github.com/novasamatech/substrate-sdk-android">Nova Substrate SDK</a>
 */
class AndroidMnemonicProvider : MnemonicProvider {

    private val seedFactory = SubstrateSeedFactory

    override fun generate(wordCount: MnemonicWordCount): String {
        val length = when (wordCount) {
            MnemonicWordCount.WORDS_12 -> Mnemonic.Length.TWELVE
            MnemonicWordCount.WORDS_24 -> Mnemonic.Length.TWENTY_FOUR
        }
        val mnemonic = MnemonicCreator.randomMnemonic(length)
        return mnemonic.words
    }

    override fun validate(mnemonic: String): MnemonicValidationResult {
        return try {
            MnemonicCreator.fromWords(mnemonic)
            MnemonicValidationResult.Valid
        } catch (e: Exception) {
            MnemonicValidationResult.Invalid(e.message ?: "Invalid mnemonic")
        }
    }

    override fun toSeed(mnemonic: String, passphrase: String): ByteArray {
        val result = seedFactory.deriveSeed(mnemonic, passphrase.ifEmpty { null })
        return result.seed
    }

    override fun toKeypair(
        mnemonic: String,
        passphrase: String,
        keyType: KeyType,
        derivationPath: String
    ): Keypair {
        val result = seedFactory.deriveSeed(mnemonic, passphrase.ifEmpty { null })
        val encryptionType = when (keyType) {
            KeyType.SR25519 -> EncryptionType.SR25519
            KeyType.ED25519 -> EncryptionType.ED25519
        }

        // Nova SDK requires derivation path to be empty list (not empty string) when no derivation
        val novaKeypair = if (derivationPath.isEmpty()) {
            SubstrateKeypairFactory.generate(
                encryptionType = encryptionType,
                seed = result.seed,
                junctions = emptyList()
            )
        } else {
            SubstrateKeypairFactory.generate(
                encryptionType = encryptionType,
                seed = result.seed,
                derivationPath = derivationPath
            )
        }

        return Keypair(
            publicKey = novaKeypair.publicKey,
            privateKey = novaKeypair.privateKey,
            keyType = keyType
        )
    }
}

actual fun createMnemonicProvider(): MnemonicProvider = AndroidMnemonicProvider()
