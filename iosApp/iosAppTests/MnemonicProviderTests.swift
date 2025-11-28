import XCTest
import Shared

/// Tests for MnemonicProvider iOS implementation.
///
/// These tests verify the iOS crypto implementation using NovaCrypto library
/// for BIP39 mnemonic generation and key derivation.
final class MnemonicProviderTests: XCTestCase {

    private var provider: MnemonicProvider!

    override func setUp() {
        super.setUp()
        provider = Mnemonic_iosKt.createMnemonicProvider()
    }

    override func tearDown() {
        provider = nil
        super.tearDown()
    }

    // MARK: - Mnemonic Generation Tests

    func testGenerate12WordMnemonic() {
        let mnemonic = provider.generate(wordCount: .words12)
        let words = mnemonic.split(separator: " ")

        XCTAssertEqual(words.count, 12, "Should generate 12 words")
        XCTAssertTrue(words.allSatisfy { !$0.isEmpty }, "All words should be non-blank")
    }

    func testGenerate24WordMnemonic() {
        let mnemonic = provider.generate(wordCount: .words24)
        let words = mnemonic.split(separator: " ")

        XCTAssertEqual(words.count, 24, "Should generate 24 words")
        XCTAssertTrue(words.allSatisfy { !$0.isEmpty }, "All words should be non-blank")
    }

    func testGenerateUniqueMnemonics() {
        let mnemonic1 = provider.generate(wordCount: .words12)
        let mnemonic2 = provider.generate(wordCount: .words12)

        XCTAssertNotEqual(mnemonic1, mnemonic2, "Each generated mnemonic should be unique")
    }

    // MARK: - Mnemonic Validation Tests

    func testValidateCorrectMnemonic() {
        let mnemonic = provider.generate(wordCount: .words12)
        let result = provider.validate(mnemonic: mnemonic)

        XCTAssertTrue(result is MnemonicValidationResult.Valid, "Generated mnemonic should be valid")
    }

    func testValidateGarbageInput() {
        let result = provider.validate(mnemonic: "this is not a valid mnemonic phrase at all")

        XCTAssertTrue(result is MnemonicValidationResult.Invalid, "Garbage input should be invalid")
    }

    func testValidateWrongWordCount() {
        let mnemonic = provider.generate(wordCount: .words12)
        let words = mnemonic.split(separator: " ")
        let truncated = words.prefix(11).joined(separator: " ")
        let result = provider.validate(mnemonic: truncated)

        XCTAssertTrue(result is MnemonicValidationResult.Invalid, "11 words should be invalid")
    }

    func testValidateEmptyString() {
        let result = provider.validate(mnemonic: "")

        XCTAssertTrue(result is MnemonicValidationResult.Invalid, "Empty string should be invalid")
    }

    // MARK: - Seed Derivation Tests

    func testToSeedProducesValidSeed() {
        let mnemonic = provider.generate(wordCount: .words12)
        let seed = provider.toSeed(mnemonic: mnemonic, passphrase: "")

        XCTAssertEqual(Int(seed.size), 64, "Seed should be 64 bytes")
    }

    func testToSeedIsDeterministic() {
        let mnemonic = provider.generate(wordCount: .words12)
        let seed1 = provider.toSeed(mnemonic: mnemonic, passphrase: "")
        let seed2 = provider.toSeed(mnemonic: mnemonic, passphrase: "")

        XCTAssertTrue(byteArraysEqual(seed1, seed2), "Same mnemonic should produce same seed")
    }

    func testToSeedWithPassphraseProducesDifferentSeed() {
        let mnemonic = provider.generate(wordCount: .words12)
        let seedNoPass = provider.toSeed(mnemonic: mnemonic, passphrase: "")
        let seedWithPass = provider.toSeed(mnemonic: mnemonic, passphrase: "my-passphrase")

        XCTAssertFalse(byteArraysEqual(seedNoPass, seedWithPass), "Passphrase should change seed")
    }

    // MARK: - Keypair Generation Tests

    func testToKeypairGeneratesSr25519Keypair() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        XCTAssertEqual(keypair.keyType, .sr25519)
        XCTAssertEqual(Int(keypair.publicKey.size), 32, "Public key should be 32 bytes")
        XCTAssertGreaterThan(Int(keypair.privateKey.size), 0, "Private key should not be empty")
    }

    func testToKeypairGeneratesEd25519Keypair() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            keyType: .ed25519,
            derivationPath: ""
        )

        XCTAssertEqual(keypair.keyType, .ed25519)
        XCTAssertEqual(Int(keypair.publicKey.size), 32, "Public key should be 32 bytes")
        XCTAssertGreaterThan(Int(keypair.privateKey.size), 0, "Private key should not be empty")
    }

    func testToKeypairIsDeterministic() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair1 = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )
        let keypair2 = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        XCTAssertTrue(byteArraysEqual(keypair1.publicKey, keypair2.publicKey), "Same mnemonic should produce same public key")
    }

    func testDifferentMnemonicsProduceDifferentKeypairs() {
        let mnemonic1 = provider.generate(wordCount: .words12)
        let mnemonic2 = provider.generate(wordCount: .words12)
        let keypair1 = provider.toKeypair(
            mnemonic: mnemonic1,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )
        let keypair2 = provider.toKeypair(
            mnemonic: mnemonic2,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        XCTAssertFalse(byteArraysEqual(keypair1.publicKey, keypair2.publicKey), "Different mnemonics should produce different public keys")
    }

    // MARK: - Passphrase Tests

    func testToKeypairWithPassphraseProducesValidKeypair() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "test-passphrase",
            keyType: .sr25519,
            derivationPath: ""
        )

        XCTAssertEqual(Int(keypair.publicKey.size), 32, "Public key should be 32 bytes")
        XCTAssertGreaterThan(Int(keypair.privateKey.size), 0, "Private key should not be empty")
    }

    func testToKeypairPassphraseIsCaseSensitive() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypairLower = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "password",
            keyType: .sr25519,
            derivationPath: ""
        )
        let keypairUpper = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "PASSWORD",
            keyType: .sr25519,
            derivationPath: ""
        )

        XCTAssertFalse(byteArraysEqual(keypairLower.publicKey, keypairUpper.publicKey), "Passphrase should be case-sensitive")
    }

    // MARK: - Helper Methods

    private func byteArraysEqual(_ a: KotlinByteArray, _ b: KotlinByteArray) -> Bool {
        guard a.size == b.size else { return false }
        for i in 0..<a.size {
            if a.get(index: i) != b.get(index: i) {
                return false
            }
        }
        return true
    }
}
