package tech.wideas.clad.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import novacrypto.IREntropy128
import novacrypto.IREntropy256
import novacrypto.IRMnemonicCreator
import novacrypto.SNBIP39SeedCreator
import novacrypto.SNKeyFactory
import tech.wideas.clad.util.toByteArray
import tech.wideas.clad.util.toNSData

/**
 * iOS implementation of [MnemonicProvider] using NovaCrypto library.
 *
 * This implementation uses the `Crypto-iOS` library (novasamatech/Crypto-iOS) which provides
 * cryptographic operations for SR25519 (Schnorrkel) and BIP39 mnemonics.
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
    private val junctionDecoder = JunctionDecoder()

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
        derivationPath: String
    ): Keypair {
        // Get the full 64-byte BIP39 seed
        val fullSeed = toSeed(mnemonic, passphrase)
        // NovaCrypto expects the first 32 bytes (mini-secret) for keypair generation
        val miniSecret = fullSeed.copyOfRange(0, 32)
        val seedData = miniSecret.toNSData()

        return createSr25519Keypair(seedData, derivationPath)
    }

    /**
     * Create an SR25519 keypair with optional derivation path.
     *
     * Note: Inherits the thread-safety constraints of the parent class.
     * The underlying NovaCrypto derivation operations are not thread-safe.
     *
     * @param seedData The 32-byte mini-secret as NSData
     * @param derivationPath Substrate derivation path (e.g., "//Alice", "//hard/soft")
     * @return The derived keypair
     */
    private fun createSr25519Keypair(seedData: platform.Foundation.NSData, derivationPath: String): Keypair {
        // Create base keypair from seed
        var currentKeypair = sr25519KeyFactory.createKeypairFromSeed(seedData, null)
            ?: throw IllegalStateException("Failed to create SR25519 keypair from seed")

        // Apply derivation junctions if path is provided
        if (derivationPath.isNotEmpty()) {
            for (junction in junctionDecoder.decode(derivationPath)) {
                val chaincodeData = junction.chaincode.toNSData()

                currentKeypair = when (junction.type) {
                    JunctionType.HARD -> {
                        sr25519KeyFactory.createKeypairHard(currentKeypair, chaincodeData, null)
                            ?: throw IllegalStateException(
                                "Failed to apply hard derivation for junction in path: $derivationPath"
                            )
                    }
                    JunctionType.SOFT -> {
                        sr25519KeyFactory.createKeypairSoft(currentKeypair, chaincodeData, null)
                            ?: throw IllegalStateException(
                                "Failed to apply soft derivation for junction in path: $derivationPath"
                            )
                    }
                }
            }
        }

        return Keypair(
            publicKey = currentKeypair.publicKey().rawData().toByteArray(),
            privateKey = currentKeypair.privateKey().rawData().toByteArray()
        )
    }
}

actual fun createMnemonicProvider(): MnemonicProvider = IOSMnemonicProvider()
