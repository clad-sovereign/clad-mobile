import Foundation
import Shared

// MARK: - Type-erased AsyncSequence for protocol conformance

/// Type-erased wrapper for any AsyncSequence with Element type T.
struct AnyAsyncSequence<Element>: AsyncSequence {
    typealias AsyncIterator = AnyAsyncIterator<Element>

    private let makeIteratorClosure: () -> AsyncIterator

    init<S: AsyncSequence>(_ sequence: S) where S.Element == Element {
        var iterator = sequence.makeAsyncIterator()
        makeIteratorClosure = {
            AnyAsyncIterator {
                try? await iterator.next()
            }
        }
    }

    func makeAsyncIterator() -> AsyncIterator {
        makeIteratorClosure()
    }
}

struct AnyAsyncIterator<Element>: AsyncIteratorProtocol {
    private let nextClosure: () async -> Element?

    init(_ next: @escaping () async -> Element?) {
        nextClosure = next
    }

    mutating func next() async -> Element? {
        await nextClosure()
    }
}

// MARK: - Account Repository Protocol

/// Protocol for account repository operations.
/// Wraps the Kotlin `AccountRepository` to enable mocking in tests.
@MainActor
protocol AccountRepositoryProtocol {
    /// Observe all accounts as an async sequence.
    func observeAll() -> AnyAsyncSequence<[AccountInfo]>

    /// Get all accounts.
    func getAll() async throws -> [AccountInfo]

    /// Get account by ID.
    func getById(id: String) async throws -> AccountInfo?

    /// Get account by SS58 address.
    func getByAddress(address: String) async throws -> AccountInfo?

    /// Create a new account.
    func create(
        label: String,
        address: String,
        mode: AccountMode,
        derivationPath: String?
    ) async throws -> AccountInfo

    /// Update account label.
    func updateLabel(id: String, label: String) async throws

    /// Delete account by ID.
    func delete(id: String) async throws

    /// Get account count.
    func count() async throws -> Int64

    // MARK: - Active Account

    /// Get the currently active account ID.
    func getActiveAccountId() async throws -> String?

    /// Observe the currently active account ID as an async sequence.
    func observeActiveAccountId() -> AnyAsyncSequence<String?>

    /// Set the active account.
    func setActiveAccount(accountId: String?) async throws

    /// Observe the currently active account as an async sequence.
    func observeActiveAccount() -> AnyAsyncSequence<AccountInfo?>

    /// Get the currently active account.
    func getActiveAccount() async throws -> AccountInfo?
}

// MARK: - Key Storage Protocol

/// Protocol for secure key storage operations.
/// Wraps the Kotlin `KeyStorage` to enable mocking in tests.
@MainActor
protocol KeyStorageProtocol {
    /// Check if biometric-protected storage is available.
    func isAvailable() async throws -> Bool

    /// Check if hardware-backed key storage is available.
    func isHardwareBackedAvailable() async throws -> Bool

    /// Store a keypair with biometric protection.
    func saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject>

    /// Retrieve a keypair with biometric authentication.
    func getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject>

    /// Delete a keypair.
    func deleteKeypair(accountId: String) async throws -> KeyStorageResult<AnyObject>

    /// Check if a keypair exists for the given account.
    func hasKeypair(accountId: String) async throws -> Bool

    /// List all stored account IDs.
    func listAccountIds() async throws -> [String]
}

// MARK: - Mnemonic Provider Protocol

/// Protocol for BIP39 mnemonic operations.
/// Wraps the Kotlin `MnemonicProvider` to enable mocking in tests.
protocol MnemonicProviderProtocol {
    /// Generate a new random mnemonic phrase.
    func generate(wordCount: MnemonicWordCount) -> String

    /// Validate a mnemonic phrase against BIP39 specification.
    func validate(mnemonic: String) -> MnemonicValidationResult

    /// Derive a seed from a mnemonic phrase.
    func toSeed(mnemonic: String, passphrase: String) -> KotlinByteArray

    /// Derive an SR25519 keypair from a mnemonic phrase.
    func toKeypair(mnemonic: String, passphrase: String, derivationPath: String) -> Keypair
}
