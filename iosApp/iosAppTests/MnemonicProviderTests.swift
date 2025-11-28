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

    // MARK: - Cross-Platform Determinism Tests (Known Test Vectors)

    /// CRITICAL TEST: Verifies iOS produces the same SR25519 keypair as Substrate subkey.
    /// If this test fails, iOS wallets will NOT be recoverable on other platforms!
    ///
    /// Test vector from Substrate subkey documentation:
    /// `subkey inspect "caution juice atom organ advance problem want pledge someone senior holiday very"`
    ///
    /// NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector1 (shared/commonTest).
    /// Duplication required because Swift cannot import Kotlin test sources.
    /// See also: shared/src/androidInstrumentedTest/.../MnemonicProviderTest.kt (Android equivalent)
    func testKnownMnemonicProducesExpectedSr25519PublicKey() {
        // Values from CrossPlatformDeterminismTest.TestVector1
        let testMnemonic = "caution juice atom organ advance problem want pledge someone senior holiday very"
        let expectedPublicKeyHex = "d6a3105d6768e956e9e5d41050ac29843f98561410d3a47f9dd5b3b227ab8746"

        let keypair = provider.toKeypair(
            mnemonic: testMnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        let actualPublicKeyHex = kotlinByteArrayToHex(keypair.publicKey)

        XCTAssertEqual(
            actualPublicKeyHex,
            expectedPublicKeyHex,
            """
            CROSS-PLATFORM DETERMINISM FAILURE!

            The SR25519 public key derived from the test mnemonic does not match
            the expected value from Substrate subkey.

            Mnemonic: "\(testMnemonic)"
            Expected: \(expectedPublicKeyHex)
            Actual:   \(actualPublicKeyHex)

            This means iOS will generate DIFFERENT addresses than Android/web wallets
            from the same recovery phrase, breaking wallet portability!
            """
        )
    }

    /// Verifies iOS produces the correct SS58 address from a known mnemonic using SR25519.
    /// This complements the SR25519 public key test above and validates the full
    /// mnemonic → keypair → address pipeline for SR25519.
    ///
    /// NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector1 (shared/commonTest).
    /// Duplication required because Swift cannot import Kotlin test sources.
    /// See also: shared/src/androidInstrumentedTest/.../MnemonicProviderTest.kt (Android equivalent)
    func testKnownMnemonicProducesExpectedSr25519Ss58Address() {
        // Values from CrossPlatformDeterminismTest.TestVector1
        let testMnemonic = "caution juice atom organ advance problem want pledge someone senior holiday very"
        let expectedAddress = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"

        let keypair = provider.toKeypair(
            mnemonic: testMnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        let actualAddress = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertEqual(
            actualAddress,
            expectedAddress,
            """
            SS58 address mismatch!

            Expected: \(expectedAddress)
            Actual:   \(actualAddress)
            """
        )
    }

    /// CRITICAL TEST: Verifies iOS produces the same ED25519 keypair as Substrate subkey.
    /// If this test fails, iOS ED25519 wallets will NOT be recoverable on other platforms!
    ///
    /// Test vector from Substrate subkey documentation:
    /// `subkey inspect --scheme ed25519 "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"`
    ///
    /// NOTE: These values are duplicated from CrossPlatformDeterminismTest.TestVector2Ed25519 in Kotlin
    /// because Swift cannot import Kotlin test sources. The single source of truth is the Kotlin file.
    /// See also: shared/src/androidInstrumentedTest/.../MnemonicProviderTest.kt for Android equivalent.
    func testKnownMnemonicProducesExpectedEd25519PublicKey() {
        // Values from CrossPlatformDeterminismTest.TestVector2Ed25519
        let testMnemonic = "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"
        let expectedPublicKeyHex = "1a0e2bf1e0195a1f5396c5fd209a620a48fe90f6f336d89c89405a0183a857a3"

        let keypair = provider.toKeypair(
            mnemonic: testMnemonic,
            passphrase: "",
            keyType: .ed25519,
            derivationPath: ""
        )

        let actualPublicKeyHex = kotlinByteArrayToHex(keypair.publicKey)

        XCTAssertEqual(
            actualPublicKeyHex,
            expectedPublicKeyHex,
            """
            CROSS-PLATFORM DETERMINISM FAILURE!

            The ED25519 public key derived from the test mnemonic does not match
            the expected value from Substrate subkey.

            Mnemonic: "\(testMnemonic)"
            Expected: \(expectedPublicKeyHex)
            Actual:   \(actualPublicKeyHex)

            This means iOS will generate DIFFERENT ED25519 addresses than Android/web wallets
            from the same recovery phrase, breaking wallet portability!
            """
        )
    }

    /// Verifies iOS produces the correct SS58 address from a known mnemonic using ED25519.
    ///
    /// NOTE: Values duplicated from CrossPlatformDeterminismTest.TestVector2Ed25519 (Kotlin).
    /// See also: shared/src/androidInstrumentedTest/.../MnemonicProviderTest.kt for Android equivalent.
    func testKnownMnemonicProducesExpectedEd25519Ss58Address() {
        // Values from CrossPlatformDeterminismTest.TestVector2Ed25519
        let testMnemonic = "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"
        let expectedAddress = "5CesK3uTmn4NGfD3oyGBd1jrp4EfRyYdtqL3ERe9SXv8jUHb"

        let keypair = provider.toKeypair(
            mnemonic: testMnemonic,
            passphrase: "",
            keyType: .ed25519,
            derivationPath: ""
        )

        let actualAddress = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertEqual(
            actualAddress,
            expectedAddress,
            """
            ED25519 SS58 address mismatch!

            Expected: \(expectedAddress)
            Actual:   \(actualAddress)
            """
        )
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

    private func kotlinByteArrayToHex(_ byteArray: KotlinByteArray) -> String {
        var hex = ""
        for i in 0..<byteArray.size {
            let byte = byteArray.get(index: i)
            hex += String(format: "%02x", UInt8(bitPattern: byte))
        }
        return hex
    }
}
