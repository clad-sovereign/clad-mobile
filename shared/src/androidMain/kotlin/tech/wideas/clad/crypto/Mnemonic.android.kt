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
 * native Rust-based cryptographic operations via JNI for SR25519 (Schnorrkel).
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
        derivationPath: String
    ): Keypair {
        val result = seedFactory.deriveSeed(mnemonic, passphrase.ifEmpty { null })

        // Nova SDK's SubstrateKeypairFactory.generate() expects a 32-byte mini-secret for sr25519,
        // not the full 64-byte BIP39 seed. Extract the first 32 bytes (mini-secret) from the
        // full seed. This matches the iOS implementation and Substrate's mini_secret_from_entropy.
        // See: https://wiki.polkadot.network/docs/learn-cryptography#keypairs-and-signing
        val miniSecret = result.seed.copyOfRange(0, 32)

        // Nova SDK requires derivation path to be empty list (not empty string) when no derivation
        val novaKeypair = if (derivationPath.isEmpty()) {
            SubstrateKeypairFactory.generate(
                encryptionType = EncryptionType.SR25519,
                seed = miniSecret,
                junctions = emptyList()
            )
        } else {
            SubstrateKeypairFactory.generate(
                encryptionType = EncryptionType.SR25519,
                seed = miniSecret,
                derivationPath = derivationPath
            )
        }

        return Keypair(
            publicKey = novaKeypair.publicKey,
            privateKey = novaKeypair.privateKey
        )
    }
}

actual fun createMnemonicProvider(): MnemonicProvider = AndroidMnemonicProvider()
