import XCTest
import Shared

/// Tests for DebugAccountSeeder functionality on iOS.
///
/// These tests verify that:
/// 1. Debug constants (mnemonic, addresses) are correct
/// 2. Alice's derived address matches the expected well-known address
/// 3. The seeding logic works correctly in debug mode
///
/// Note: Full integration tests with actual Keychain require running on a device.
/// These tests focus on the derivation correctness which is the critical path.
final class DebugAccountSeederTests: XCTestCase {

    // MARK: - Test Constants Verification

    /// Verifies that DEV_MNEMONIC matches the well-known Substrate dev mnemonic.
    func testDevMnemonicMatchesWellKnownSubstrateMnemonic() {
        let expectedMnemonic = "bottom drive obey lake curtain smoke basket hold race lonely fit walk"
        let actualMnemonic = DebugAccountSeeder.companion.DEV_MNEMONIC

        XCTAssertEqual(
            actualMnemonic,
            expectedMnemonic,
            "DEV_MNEMONIC should match the well-known Substrate dev mnemonic"
        )
    }

    /// Verifies that ALICE_DERIVATION_PATH is the correct hard derivation path.
    func testAliceDerivationPathIsCorrect() {
        let expectedPath = "//Alice"
        let actualPath = DebugAccountSeeder.companion.ALICE_DERIVATION_PATH

        XCTAssertEqual(
            actualPath,
            expectedPath,
            "ALICE_DERIVATION_PATH should be //Alice"
        )
    }

    /// Verifies that ALICE_ADDRESS matches the well-known SS58 address.
    func testAliceAddressIsWellKnownAddress() {
        let expectedAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        let actualAddress = DebugAccountSeeder.companion.ALICE_ADDRESS

        XCTAssertEqual(
            actualAddress,
            expectedAddress,
            "ALICE_ADDRESS should be the well-known Substrate Alice address"
        )
    }

    /// Verifies that BOB_ADDRESS matches the well-known SS58 address.
    func testBobAddressIsWellKnownAddress() {
        let expectedAddress = "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty"
        let actualAddress = DebugAccountSeeder.companion.BOB_ADDRESS

        XCTAssertEqual(
            actualAddress,
            expectedAddress,
            "BOB_ADDRESS should be the well-known Substrate Bob address"
        )
    }

    /// Verifies the dev mnemonic has the correct word count.
    func testDevMnemonicHas12Words() {
        let mnemonic = DebugAccountSeeder.companion.DEV_MNEMONIC
        let words = mnemonic.split(separator: " ")

        XCTAssertEqual(words.count, 12, "Dev mnemonic should have 12 words")
    }

    /// Verifies Alice address has correct length for SS58.
    func testAliceAddressHasCorrectLength() {
        let address = DebugAccountSeeder.companion.ALICE_ADDRESS
        // SS58 addresses are typically 47-48 characters for Substrate
        XCTAssertTrue(
            address.count >= 46 && address.count <= 50,
            "Alice address should be 46-50 characters, got \(address.count)"
        )
    }

    /// Verifies Bob address has correct length for SS58.
    func testBobAddressHasCorrectLength() {
        let address = DebugAccountSeeder.companion.BOB_ADDRESS
        XCTAssertTrue(
            address.count >= 46 && address.count <= 50,
            "Bob address should be 46-50 characters, got \(address.count)"
        )
    }

    /// Verifies Alice address starts with "5" (generic Substrate prefix 42).
    func testAliceAddressHasCorrectPrefix() {
        let address = DebugAccountSeeder.companion.ALICE_ADDRESS
        XCTAssertTrue(
            address.hasPrefix("5"),
            "Alice address should start with 5 (generic Substrate prefix 42)"
        )
    }

    /// Verifies Bob address starts with "5" (generic Substrate prefix 42).
    func testBobAddressHasCorrectPrefix() {
        let address = DebugAccountSeeder.companion.BOB_ADDRESS
        XCTAssertTrue(
            address.hasPrefix("5"),
            "Bob address should start with 5 (generic Substrate prefix 42)"
        )
    }

    // MARK: - Derivation Verification Tests

    /// CRITICAL TEST: Verifies that deriving a keypair from DEV_MNEMONIC + //Alice
    /// produces the expected ALICE_ADDRESS.
    ///
    /// This ensures that the DebugAccountSeeder will create the correct Alice account
    /// when seeding test accounts.
    func testDerivedAliceAddressMatchesExpectedAddress() {
        let provider = Mnemonic_iosKt.createMnemonicProvider()

        // Use the same constants as DebugAccountSeeder
        let mnemonic = DebugAccountSeeder.companion.DEV_MNEMONIC
        let derivationPath = DebugAccountSeeder.companion.ALICE_DERIVATION_PATH
        let expectedAddress = DebugAccountSeeder.companion.ALICE_ADDRESS

        // Derive keypair
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: derivationPath
        )

        // Encode as SS58 address
        let actualAddress = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertEqual(
            actualAddress,
            expectedAddress,
            """
            ALICE DERIVATION MISMATCH!

            Deriving from DEV_MNEMONIC + //Alice should produce ALICE_ADDRESS.

            Expected: \(expectedAddress)
            Actual:   \(actualAddress)

            This means DebugAccountSeeder will create accounts with wrong addresses!
            """
        )
    }

    /// Verifies that the Alice keypair's public key matches the expected hex value.
    func testDerivedAlicePublicKeyMatchesExpected() {
        let provider = Mnemonic_iosKt.createMnemonicProvider()

        let mnemonic = DebugAccountSeeder.companion.DEV_MNEMONIC
        let derivationPath = DebugAccountSeeder.companion.ALICE_DERIVATION_PATH
        // Expected public key from Substrate subkey
        let expectedPublicKeyHex = "d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d"

        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: derivationPath
        )

        let actualPublicKeyHex = kotlinByteArrayToHex(keypair.publicKey)

        XCTAssertEqual(
            actualPublicKeyHex,
            expectedPublicKeyHex,
            """
            ALICE PUBLIC KEY MISMATCH!

            Expected: \(expectedPublicKeyHex)
            Actual:   \(actualPublicKeyHex)
            """
        )
    }

    // MARK: - DebugConfig Tests

    /// Verifies DebugConfig default state.
    func testDebugConfigDefaultState() {
        // Note: DebugConfig.isDebug may have been set by app initialization
        // This test verifies the API is accessible
        _ = DebugConfig.shared.isDebug
    }

    /// Verifies DebugConfigFactory can set debug mode.
    func testDebugConfigFactoryCanSetDebugMode() {
        // Set to true
        DebugConfigFactory.shared.setDebugMode(isDebug: true)
        XCTAssertTrue(DebugConfig.shared.isDebug, "isDebug should be true after setDebugMode(true)")

        // Set to false
        DebugConfigFactory.shared.setDebugMode(isDebug: false)
        XCTAssertFalse(DebugConfig.shared.isDebug, "isDebug should be false after setDebugMode(false)")
    }

    // MARK: - Helper Methods

    private func kotlinByteArrayToHex(_ byteArray: KotlinByteArray) -> String {
        var hex = ""
        for i in 0..<byteArray.size {
            let byte = byteArray.get(index: i)
            hex += String(format: "%02x", UInt8(bitPattern: byte))
        }
        return hex
    }
}
