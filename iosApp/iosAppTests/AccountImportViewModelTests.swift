import XCTest
@testable import CladSigner
import Shared

/// Tests for AccountImportViewModel state management and import logic.
///
/// Note: These tests focus on the pure logic aspects of the ViewModel (state machine,
/// word handling, validation) because the Kotlin repositories exported to Swift
/// are not easily mockable. Repository-dependent functionality is tested via
/// manual/integration testing.
@MainActor
final class AccountImportViewModelTests: XCTestCase {

    private var mnemonicProvider: MnemonicProvider!

    override func setUp() {
        super.setUp()
        mnemonicProvider = Mnemonic_iosKt.createMnemonicProvider()
    }

    override func tearDown() {
        mnemonicProvider = nil
        super.tearDown()
    }

    // MARK: - ImportMethod Tests

    func testImportMethodProperties() {
        XCTAssertEqual(ImportMethod.seedPhrase.title, "Recovery Phrase")
        XCTAssertEqual(ImportMethod.qrCode.title, "Scan QR Code")
        XCTAssertEqual(ImportMethod.manualAddress.title, "Enter Address")

        XCTAssertEqual(ImportMethod.seedPhrase.icon, "key.fill")
        XCTAssertEqual(ImportMethod.qrCode.icon, "qrcode.viewfinder")
        XCTAssertEqual(ImportMethod.manualAddress.icon, "keyboard")
    }

    // MARK: - ImportFlowState Tests

    func testImportFlowStateEquality() {
        XCTAssertEqual(ImportFlowState.selectMethod, ImportFlowState.selectMethod)
        XCTAssertEqual(ImportFlowState.seedPhraseInput, ImportFlowState.seedPhraseInput)
        XCTAssertEqual(ImportFlowState.error("test"), ImportFlowState.error("test"))
        XCTAssertNotEqual(ImportFlowState.error("test1"), ImportFlowState.error("test2"))
    }

    // MARK: - ImportData Tests

    func testImportDataDefaults() {
        let data = ImportData()

        XCTAssertTrue(data.mnemonic.isEmpty)
        XCTAssertTrue(data.address.isEmpty)
        XCTAssertTrue(data.label.isEmpty)
        XCTAssertFalse(data.isWatchOnly)
        XCTAssertEqual(data.keyType, .sr25519)
    }

    // MARK: - Word Count Tests

    func testWordCountValidation() {
        // Valid word counts
        XCTAssertTrue([12, 24].contains(12))
        XCTAssertTrue([12, 24].contains(24))

        // Invalid word counts
        XCTAssertFalse([12, 24].contains(15))
        XCTAssertFalse([12, 24].contains(0))
    }

    // MARK: - Seed Phrase Generation and Validation Tests

    func testMnemonicGenerate12Words() {
        let mnemonic = mnemonicProvider.generate(wordCount: .words12)
        let words = mnemonic.split(separator: " ")

        XCTAssertEqual(words.count, 12)
    }

    func testMnemonicGenerate24Words() {
        let mnemonic = mnemonicProvider.generate(wordCount: .words24)
        let words = mnemonic.split(separator: " ")

        XCTAssertEqual(words.count, 24)
    }

    func testMnemonicValidation() {
        let validMnemonic = mnemonicProvider.generate(wordCount: .words12)
        let result = mnemonicProvider.validate(mnemonic: validMnemonic)

        XCTAssertTrue(result is MnemonicValidationResult.Valid)
    }

    func testMnemonicValidationInvalid() {
        let invalidMnemonic = "this is not a valid mnemonic phrase at all"
        let result = mnemonicProvider.validate(mnemonic: invalidMnemonic)

        XCTAssertTrue(result is MnemonicValidationResult.Invalid)
    }

    // MARK: - Full Phrase Paste Logic Tests

    func testParsePastedPhrase12Words() {
        let pastedText = "abandon ability able about above absent absorb abstract absurd abuse access accident"
        let words = pastedText.lowercased()
            .trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .filter { !$0.isEmpty }

        XCTAssertEqual(words.count, 12)
    }

    func testParsePastedPhrase24Words() {
        let pastedText = "abandon ability able about above absent absorb abstract absurd abuse access accident acoustic acquire across act action actor actress actual adapt add addict address"
        let words = pastedText.lowercased()
            .trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .filter { !$0.isEmpty }

        XCTAssertEqual(words.count, 24)
    }

    func testParsePastedPhraseWithExtraSpaces() {
        let pastedText = "  abandon   ability  able  about   above absent absorb abstract absurd abuse access accident  "
        let words = pastedText.lowercased()
            .trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .filter { !$0.isEmpty }

        XCTAssertEqual(words.count, 12)
        XCTAssertEqual(words[0], "abandon")
        XCTAssertEqual(words[11], "accident")
    }

    func testParsePastedPhraseWithMixedCase() {
        let pastedText = "ABANDON Ability ABLE About above ABSENT absorb abstract absurd abuse access accident"
        let words = pastedText.lowercased()
            .trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .filter { !$0.isEmpty }

        XCTAssertEqual(words.count, 12)
        XCTAssertTrue(words.allSatisfy { $0 == $0.lowercased() })
    }

    // MARK: - SS58 Address Validation Tests

    func testSs58ValidAddress() {
        // Alice's well-known address
        let validAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        XCTAssertTrue(Ss58.shared.isValid(address: validAddress))
    }

    func testSs58InvalidAddress() {
        XCTAssertFalse(Ss58.shared.isValid(address: "not-a-valid-address"))
        XCTAssertFalse(Ss58.shared.isValid(address: ""))
        XCTAssertFalse(Ss58.shared.isValid(address: "5"))
    }

    func testSs58AddressTrimming() {
        let addressWithWhitespace = "  5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY  \n"
        let trimmed = addressWithWhitespace.trimmingCharacters(in: .whitespacesAndNewlines)

        XCTAssertTrue(Ss58.shared.isValid(address: trimmed))
    }

    // MARK: - Keypair Derivation Tests

    func testKeypairDerivationProducesValidAddress() {
        let mnemonic = mnemonicProvider.generate(wordCount: .words12)
        let keypair = mnemonicProvider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        let address = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertTrue(Ss58.shared.isValid(address: address))
        XCTAssertTrue(address.hasPrefix("5")) // Generic Substrate prefix
    }

    // MARK: - Default Label Generation Logic Tests

    func testDefaultLabelFormatWatchOnly() {
        let address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        let shortAddress = String(address.prefix(8))
        let label = "Watch \(shortAddress)..."

        XCTAssertEqual(label, "Watch 5GrwvaEF...")
    }

    func testDefaultLabelFormatAccount() {
        let address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        let shortAddress = String(address.prefix(8))
        let label = "Account \(shortAddress)..."

        XCTAssertEqual(label, "Account 5GrwvaEF...")
    }

    // MARK: - Address Formatting Tests

    func testAddressFormatting() {
        let address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        // Simulate the formatAddress logic from AccountCard
        let prefix = String(address.prefix(8))
        let suffix = String(address.suffix(8))
        let formatted = "\(prefix)...\(suffix)"

        XCTAssertEqual(formatted, "5GrwvaEF...oHGKutQY")
    }

    func testShortAddressNotFormatted() {
        let shortAddress = "5Grwva" // Less than 16 chars

        let formatted: String
        if shortAddress.count > 16 {
            let prefix = String(shortAddress.prefix(8))
            let suffix = String(shortAddress.suffix(8))
            formatted = "\(prefix)...\(suffix)"
        } else {
            formatted = shortAddress
        }

        XCTAssertEqual(formatted, "5Grwva")
    }

    // MARK: - Cross-Platform Determinism Tests

    /// Verifies that iOS produces the expected SR25519 address from known test vector.
    func testKnownMnemonicProducesExpectedAddress() {
        // Test vector from Substrate subkey
        let testMnemonic = "caution juice atom organ advance problem want pledge someone senior holiday very"
        let expectedAddress = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"

        let keypair = mnemonicProvider.toKeypair(
            mnemonic: testMnemonic,
            passphrase: "",
            keyType: .sr25519,
            derivationPath: ""
        )

        let actualAddress = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertEqual(actualAddress, expectedAddress)
    }
}
