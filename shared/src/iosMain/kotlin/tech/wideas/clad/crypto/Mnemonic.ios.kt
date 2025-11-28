package tech.wideas.clad.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import novacrypto.EDKeyFactory
import novacrypto.IREntropy128
import novacrypto.IREntropy256
import novacrypto.IRMnemonicCreator
import novacrypto.SNBIP39SeedCreator
import novacrypto.SNKeyFactory
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * iOS implementation of [MnemonicProvider] using NovaCrypto library.
 *
 * This implementation uses the `Crypto-iOS` library (novasamatech/Crypto-iOS) which provides
 * cryptographic operations for sr25519 (Schnorrkel), ed25519, and BIP39 mnemonics.
 *
 * Thread Safety: This class is NOT thread-safe. The underlying NovaCrypto
 * operations may not be safe for concurrent use. Create separate instances
 * or synchronize access externally.
 *
 * @see <a href="https://github.com/novasamatech/Crypto-iOS">NovaCrypto (iOS)</a>
 */
@OptIn(ExperimentalForeignApi::class)
class IOSMnemonicProvider : MnemonicProvider {

    private val mnemonicCreator = IRMnemonicCreator.defaultCreator()
    private val seedCreator = SNBIP39SeedCreator()
    private val sr25519KeyFactory = SNKeyFactory()
    private val ed25519KeyFactory = EDKeyFactory()

    override fun generate(wordCount: MnemonicWordCount): String {
        val strength = when (wordCount) {
            MnemonicWordCount.WORDS_12 -> IREntropy128
            MnemonicWordCount.WORDS_24 -> IREntropy256
        }

        val mnemonic = mnemonicCreator.randomMnemonic(strength, null)
            ?: throw IllegalStateException("Failed to generate mnemonic")

        return mnemonic.toString_()
    }

    override fun validate(mnemonic: String): MnemonicValidationResult {
        return try {
            val result = mnemonicCreator.mnemonicFromList(mnemonic, null)
            if (result != null) {
                MnemonicValidationResult.Valid
            } else {
                MnemonicValidationResult.Invalid("Invalid mnemonic phrase")
            }
        } catch (e: Exception) {
            MnemonicValidationResult.Invalid(e.message ?: "Invalid mnemonic")
        }
    }

    override fun toSeed(mnemonic: String, passphrase: String): ByteArray {
        // First validate and get the mnemonic object to extract entropy
        val mnemonicObj = mnemonicCreator.mnemonicFromList(mnemonic, null)
            ?: throw IllegalArgumentException("Invalid mnemonic phrase")

        val entropy = mnemonicObj.entropy()
        val effectivePassphrase = passphrase.ifEmpty { "" }

        val seedData = seedCreator.deriveSeedFrom(entropy, effectivePassphrase, null)
            ?: throw IllegalStateException("Failed to derive seed from mnemonic")

        return seedData.toByteArray()
    }

    override fun toKeypair(
        mnemonic: String,
        passphrase: String,
        keyType: KeyType,
        derivationPath: String
    ): Keypair {
        // Get the full 64-byte BIP39 seed
        val fullSeed = toSeed(mnemonic, passphrase)
        // NovaCrypto expects the first 32 bytes (mini-secret) for keypair generation
        val miniSecret = fullSeed.copyOfRange(0, 32)
        val seedData = miniSecret.toNSData()

        return when (keyType) {
            KeyType.SR25519 -> {
                val keypair = sr25519KeyFactory.createKeypairFromSeed(seedData, null)
                    ?: throw IllegalStateException("Failed to create SR25519 keypair")

                Keypair(
                    publicKey = keypair.publicKey().rawData().toByteArray(),
                    privateKey = keypair.privateKey().rawData().toByteArray(),
                    keyType = KeyType.SR25519
                )
            }
            KeyType.ED25519 -> {
                val keypair = ed25519KeyFactory.deriveFromSeed(seedData, null)
                    ?: throw IllegalStateException("Failed to create ED25519 keypair")

                Keypair(
                    publicKey = keypair.publicKey().rawData().toByteArray(),
                    privateKey = keypair.privateKey().rawData().toByteArray(),
                    keyType = KeyType.ED25519
                )
            }
        }
    }
}

/**
 * Converts NSData to Kotlin ByteArray.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)

    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

/**
 * Converts Kotlin ByteArray to NSData.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    if (this.isEmpty()) return NSData()

    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

actual fun createMnemonicProvider(): MnemonicProvider = IOSMnemonicProvider()
