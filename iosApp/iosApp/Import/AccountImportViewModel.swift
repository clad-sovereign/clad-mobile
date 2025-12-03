import SwiftUI
import Observation
import Shared

/// Import method options available to users
enum ImportMethod: Identifiable {
    case seedPhrase
    case qrCode
    case manualAddress

    var id: Self { self }

    var title: String {
        switch self {
        case .seedPhrase: return "Recovery Phrase"
        case .qrCode: return "Scan QR Code"
        case .manualAddress: return "Enter Address"
        }
    }

    var icon: String {
        switch self {
        case .seedPhrase: return "key.fill"
        case .qrCode: return "qrcode.viewfinder"
        case .manualAddress: return "keyboard"
        }
    }

    var description: String {
        switch self {
        case .seedPhrase: return "Import using 12 or 24 word recovery phrase"
        case .qrCode: return "Scan a QR code containing an address"
        case .manualAddress: return "Enter an SS58 address manually (watch-only)"
        }
    }
}

/// State for the import flow
enum ImportFlowState: Equatable {
    case selectMethod
    case seedPhraseInput
    case qrCodeScan
    case manualAddressInput
    case confirmImport
    case importing
    case success
    case error(String)
}

/// Data collected during the import flow
struct ImportData {
    var mnemonic: String = ""
    var address: String = ""
    var label: String = ""
    var isWatchOnly: Bool = false
    var keyType: KeyType = .sr25519
}

/// ViewModel for account import flows using the modern @Observable macro (iOS 17+)
@MainActor
@Observable
final class AccountImportViewModel {

    // MARK: - Dependencies
    private let accountRepository: AccountRepository
    private let mnemonicProvider: MnemonicProvider
    private let keyStorage: KeyStorage

    // MARK: - Observable State
    var flowState: ImportFlowState = .selectMethod
    var importData = ImportData()
    var seedWords: [String] = Array(repeating: "", count: 12)
    var wordCount: Int = 12
    var validationError: String?
    var isValidating: Bool = false
    var scannedAddress: String = ""
    var manualAddress: String = ""
    var accountLabel: String = ""

    // MARK: - Computed Properties
    var seedPhrase: String {
        seedWords.joined(separator: " ").trimmingCharacters(in: .whitespaces)
    }

    var canProceedFromSeedPhrase: Bool {
        let filledWords = seedWords.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        return filledWords.count == wordCount && validationError == nil
    }

    var canProceedFromManualAddress: Bool {
        !manualAddress.trimmingCharacters(in: .whitespaces).isEmpty && validationError == nil
    }

    var derivedAddress: String {
        importData.address
    }

    // MARK: - Initialization
    init() {
        let helper = ViewModelHelper()
        self.accountRepository = helper.getAccountRepository()
        self.mnemonicProvider = helper.getMnemonicProvider()
        self.keyStorage = helper.getKeyStorage()
    }

    /// Test initializer for dependency injection
    init(accountRepository: AccountRepository, mnemonicProvider: MnemonicProvider, keyStorage: KeyStorage) {
        self.accountRepository = accountRepository
        self.mnemonicProvider = mnemonicProvider
        self.keyStorage = keyStorage
    }

    // MARK: - Navigation Actions

    func selectImportMethod(_ method: ImportMethod) {
        switch method {
        case .seedPhrase:
            resetSeedPhraseState()
            flowState = .seedPhraseInput
        case .qrCode:
            scannedAddress = ""
            flowState = .qrCodeScan
        case .manualAddress:
            manualAddress = ""
            validationError = nil
            flowState = .manualAddressInput
        }
    }

    func goBack() {
        switch flowState {
        case .seedPhraseInput, .qrCodeScan, .manualAddressInput:
            flowState = .selectMethod
        case .confirmImport:
            if importData.isWatchOnly {
                flowState = importData.mnemonic.isEmpty ? .manualAddressInput : .qrCodeScan
            } else {
                flowState = .seedPhraseInput
            }
        default:
            flowState = .selectMethod
        }
    }

    func reset() {
        flowState = .selectMethod
        importData = ImportData()
        resetSeedPhraseState()
        scannedAddress = ""
        manualAddress = ""
        accountLabel = ""
        validationError = nil
    }

    // MARK: - Seed Phrase Methods

    func setWordCount(_ count: Int) {
        guard count == 12 || count == 24 else { return }
        wordCount = count
        seedWords = Array(repeating: "", count: count)
        validationError = nil
    }

    func updateWord(at index: Int, with word: String) {
        guard index >= 0 && index < seedWords.count else { return }
        seedWords[index] = word.lowercased().trimmingCharacters(in: .whitespaces)
        validationError = nil
    }

    /// Handle pasting a full recovery phrase (12 or 24 words)
    func pasteFullPhrase(_ words: [String]) {
        let cleanedWords = words.map { $0.lowercased().trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        // Auto-detect word count if it matches 12 or 24
        if cleanedWords.count == 12 || cleanedWords.count == 24 {
            setWordCount(cleanedWords.count)
            for (index, word) in cleanedWords.enumerated() {
                seedWords[index] = word
            }
            validationError = nil
        }
    }

    func validateAndProceedFromSeedPhrase() {
        isValidating = true
        validationError = nil

        let phrase = seedPhrase
        let result = mnemonicProvider.validate(mnemonic: phrase)

        if result is MnemonicValidationResult.Valid {
            // Derive keypair to get the address
            do {
                let keypair = mnemonicProvider.toKeypair(
                    mnemonic: phrase,
                    passphrase: "",
                    keyType: importData.keyType,
                    derivationPath: ""
                )

                let address = Ss58.shared.encode(
                    publicKey: keypair.publicKey,
                    networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
                )

                importData.mnemonic = phrase
                importData.address = address
                importData.isWatchOnly = false
                accountLabel = ""
                flowState = .confirmImport
            } catch {
                validationError = "Failed to derive keypair: \(error.localizedDescription)"
            }
        } else if let invalid = result as? MnemonicValidationResult.Invalid {
            validationError = invalid.reason
        } else {
            validationError = "Invalid recovery phrase"
        }

        isValidating = false
    }

    private func resetSeedPhraseState() {
        wordCount = 12
        seedWords = Array(repeating: "", count: 12)
        validationError = nil
    }

    // MARK: - QR Code Methods

    func handleScannedCode(_ code: String) {
        // Try to extract SS58 address from the scanned code
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)

        if Ss58.shared.isValid(address: trimmed) {
            scannedAddress = trimmed
            importData.address = trimmed
            importData.mnemonic = ""
            importData.isWatchOnly = true
            accountLabel = ""
            flowState = .confirmImport
        } else {
            validationError = "Invalid address format in QR code"
        }
    }

    // MARK: - Manual Address Methods

    func validateManualAddress() {
        let address = manualAddress.trimmingCharacters(in: .whitespacesAndNewlines)

        if address.isEmpty {
            validationError = "Please enter an address"
            return
        }

        if Ss58.shared.isValid(address: address) {
            validationError = nil
        } else {
            validationError = "Invalid SS58 address format"
        }
    }

    func proceedFromManualAddress() {
        let address = manualAddress.trimmingCharacters(in: .whitespacesAndNewlines)

        guard Ss58.shared.isValid(address: address) else {
            validationError = "Invalid SS58 address format"
            return
        }

        importData.address = address
        importData.mnemonic = ""
        importData.isWatchOnly = true
        accountLabel = ""
        flowState = .confirmImport
    }

    // MARK: - Import Confirmation

    func confirmImport() async {
        flowState = .importing

        let label = accountLabel.trimmingCharacters(in: .whitespaces)
        let finalLabel = label.isEmpty ? generateDefaultLabel() : label

        do {
            // Check for duplicate address
            if let existing = try await accountRepository.getByAddress(address: importData.address) {
                flowState = .error("An account with this address already exists: \(existing.label)")
                return
            }

            if !importData.isWatchOnly {
                // For keypair imports, save the keypair with biometric protection
                let keypair = mnemonicProvider.toKeypair(
                    mnemonic: importData.mnemonic,
                    passphrase: "",
                    keyType: importData.keyType,
                    derivationPath: ""
                )

                // Create account first to get the ID
                let account = try await accountRepository.create(
                    label: finalLabel,
                    address: importData.address,
                    keyType: importData.keyType
                )

                // Save keypair with biometric protection
                let promptConfig = BiometricPromptConfig(
                    title: "Save Recovery Key",
                    subtitle: "Authenticate to securely store your key",
                    promptDescription: "",
                    negativeButtonText: "Cancel"
                )

                let saveResult = try await keyStorage.saveKeypair(
                    accountId: account.id,
                    keypair: keypair,
                    promptConfig: promptConfig
                )

                if saveResult is KeyStorageResultBiometricCancelled {
                    // User cancelled, delete the account we just created
                    try await accountRepository.delete(id: account.id)
                    flowState = .selectMethod
                    return
                } else if let error = saveResult as? KeyStorageResultBiometricError {
                    try await accountRepository.delete(id: account.id)
                    flowState = .error("Biometric error: \(error.message)")
                    return
                } else if let error = saveResult as? KeyStorageResultStorageError {
                    try await accountRepository.delete(id: account.id)
                    flowState = .error("Storage error: \(error.message)")
                    return
                }

                clearSensitiveData()
                flowState = .success
            } else {
                // Watch-only account - just save metadata
                _ = try await accountRepository.create(
                    label: finalLabel,
                    address: importData.address,
                    keyType: .sr25519 // Default for watch-only
                )

                flowState = .success
            }
        } catch {
            flowState = .error(error.localizedDescription)
        }
    }

    /// Clear sensitive data from memory after successful import
    private func clearSensitiveData() {
        importData.mnemonic = ""
        seedWords = Array(repeating: "", count: seedWords.count)
    }

    private func generateDefaultLabel() -> String {
        let prefix = importData.isWatchOnly ? "Watch" : "Account"
        let shortAddress = String(importData.address.prefix(8))
        return "\(prefix) \(shortAddress)..."
    }
}
