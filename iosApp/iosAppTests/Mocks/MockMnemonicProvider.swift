import Foundation
@testable import CladSigner
import Shared

/// Mock implementation of MnemonicProviderProtocol for testing.
final class MockMnemonicProvider: MnemonicProviderProtocol {

    // MARK: - Mock State

    /// Mnemonic to return from generate()
    var generatedMnemonic = "abandon ability able about above absent absorb abstract absurd abuse access accident"

    /// Validation result to return
    var validationResult: MnemonicValidationResult = MnemonicValidationResult.Valid()

    /// Seed to return from toSeed()
    var seedResult: KotlinByteArray = KotlinByteArray(size: 64)

    /// Keypair to return from toKeypair()
    var keypairResult: Keypair?

    /// Track method calls for verification
    var generateCallCount = 0
    var validateCallCount = 0
    var toSeedCallCount = 0
    var toKeypairCallCount = 0

    /// Captured parameters from method calls
    var lastGenerateWordCount: MnemonicWordCount?
    var lastValidatedMnemonic: String?
    var lastToSeedMnemonic: String?
    var lastToSeedPassphrase: String?
    var lastToKeypairMnemonic: String?
    var lastToKeypairPassphrase: String?
    var lastToKeypairDerivationPath: String?

    // MARK: - MnemonicProviderProtocol

    func generate(wordCount: MnemonicWordCount) -> String {
        generateCallCount += 1
        lastGenerateWordCount = wordCount

        if wordCount == .words24 {
            return generatedMnemonic + " " + generatedMnemonic  // 24 words
        }
        return generatedMnemonic
    }

    func validate(mnemonic: String) -> MnemonicValidationResult {
        validateCallCount += 1
        lastValidatedMnemonic = mnemonic
        return validationResult
    }

    func toSeed(mnemonic: String, passphrase: String) -> KotlinByteArray {
        toSeedCallCount += 1
        lastToSeedMnemonic = mnemonic
        lastToSeedPassphrase = passphrase
        return seedResult
    }

    func toKeypair(mnemonic: String, passphrase: String, derivationPath: String) -> Keypair {
        toKeypairCallCount += 1
        lastToKeypairMnemonic = mnemonic
        lastToKeypairPassphrase = passphrase
        lastToKeypairDerivationPath = derivationPath

        if let keypair = keypairResult {
            return keypair
        }

        // Return a deterministic test keypair
        return createTestKeypair()
    }

    // MARK: - Test Helpers

    /// Reset all mock state
    func reset() {
        generatedMnemonic = "abandon ability able about above absent absorb abstract absurd abuse access accident"
        validationResult = MnemonicValidationResult.Valid()
        seedResult = KotlinByteArray(size: 64)
        keypairResult = nil
        generateCallCount = 0
        validateCallCount = 0
        toSeedCallCount = 0
        toKeypairCallCount = 0
        lastGenerateWordCount = nil
        lastValidatedMnemonic = nil
        lastToSeedMnemonic = nil
        lastToSeedPassphrase = nil
        lastToKeypairMnemonic = nil
        lastToKeypairPassphrase = nil
        lastToKeypairDerivationPath = nil
    }

    /// Configure to return invalid mnemonic
    func simulateInvalidMnemonic(reason: String) {
        validationResult = MnemonicValidationResult.Invalid(reason: reason)
    }

    /// Create a test keypair with known values
    private func createTestKeypair() -> Keypair {
        // Create a deterministic 32-byte public key (represents Alice's well-known address)
        let publicKeyBytes: [UInt8] = [
            0xd4, 0x35, 0x93, 0xc7, 0x15, 0xfd, 0xd3, 0x1c,
            0x61, 0x14, 0x1a, 0xbd, 0x04, 0xa9, 0x9f, 0xd6,
            0x82, 0x2c, 0x85, 0x58, 0x85, 0x4c, 0xcd, 0xe3,
            0x9a, 0x56, 0x84, 0xe7, 0xa5, 0x6d, 0xa2, 0x7d
        ]

        // Create a 64-byte private key (dummy for testing)
        let privateKeyBytes = [UInt8](repeating: 0x00, count: 64)

        let publicKey = KotlinByteArray(size: 32)
        for (i, byte) in publicKeyBytes.enumerated() {
            publicKey.set(index: Int32(i), value: Int8(bitPattern: byte))
        }

        let privateKey = KotlinByteArray(size: 64)
        for (i, byte) in privateKeyBytes.enumerated() {
            privateKey.set(index: Int32(i), value: Int8(bitPattern: byte))
        }

        return Keypair(publicKey: publicKey, privateKey: privateKey)
    }
}
