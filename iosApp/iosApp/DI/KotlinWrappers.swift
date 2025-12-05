import Foundation
import Shared

// MARK: - Account Repository Wrapper

/// Wrapper that implements `AccountRepositoryProtocol` by delegating to the Kotlin `AccountRepository`.
@MainActor
final class AccountRepositoryWrapper: AccountRepositoryProtocol {
    private let repository: AccountRepository

    init(repository: AccountRepository) {
        self.repository = repository
    }

    func observeAll() -> AnyAsyncSequence<[AccountInfo]> {
        AnyAsyncSequence(repository.observeAll())
    }

    func getAll() async throws -> [AccountInfo] {
        try await repository.getAll()
    }

    func getById(id: String) async throws -> AccountInfo? {
        try await repository.getById(id: id)
    }

    func getByAddress(address: String) async throws -> AccountInfo? {
        try await repository.getByAddress(address: address)
    }

    func create(
        label: String,
        address: String,
        mode: AccountMode,
        derivationPath: String?
    ) async throws -> AccountInfo {
        try await repository.create(
            label: label,
            address: address,
            mode: mode,
            derivationPath: derivationPath
        )
    }

    func updateLabel(id: String, label: String) async throws {
        _ = try await repository.updateLabel(id: id, label: label)
    }

    func delete(id: String) async throws {
        _ = try await repository.delete(id: id)
    }

    func count() async throws -> Int64 {
        try await repository.count().int64Value
    }

    func getActiveAccountId() async throws -> String? {
        try await repository.getActiveAccountId()
    }

    func observeActiveAccountId() -> AnyAsyncSequence<String?> {
        AnyAsyncSequence(repository.observeActiveAccountId())
    }

    func setActiveAccount(accountId: String?) async throws {
        _ = try await repository.setActiveAccount(accountId: accountId)
    }

    func observeActiveAccount() -> AnyAsyncSequence<AccountInfo?> {
        AnyAsyncSequence(repository.observeActiveAccount())
    }

    func getActiveAccount() async throws -> AccountInfo? {
        try await repository.getActiveAccount()
    }
}

// MARK: - Key Storage Wrapper

/// Wrapper that implements `KeyStorageProtocol` by delegating to the Kotlin `KeyStorage`.
@MainActor
final class KeyStorageWrapper: KeyStorageProtocol {
    private let storage: KeyStorage

    init(storage: KeyStorage) {
        self.storage = storage
    }

    func isAvailable() async throws -> Bool {
        try await storage.isAvailable().boolValue
    }

    func isHardwareBackedAvailable() async throws -> Bool {
        try await storage.isHardwareBackedAvailable().boolValue
    }

    func saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject> {
        let result = try await storage.saveKeypair(
            accountId: accountId,
            keypair: keypair,
            promptConfig: promptConfig
        )
        return result as! KeyStorageResult<AnyObject>
    }

    func getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject> {
        let result = try await storage.getKeypair(accountId: accountId, promptConfig: promptConfig)
        return result as! KeyStorageResult<AnyObject>
    }

    func deleteKeypair(accountId: String) async throws -> KeyStorageResult<AnyObject> {
        let result = try await storage.deleteKeypair(accountId: accountId)
        return result as! KeyStorageResult<AnyObject>
    }

    func hasKeypair(accountId: String) async throws -> Bool {
        try await storage.hasKeypair(accountId: accountId).boolValue
    }

    func listAccountIds() async throws -> [String] {
        try await storage.listAccountIds()
    }
}

// MARK: - Mnemonic Provider Wrapper

/// Wrapper that implements `MnemonicProviderProtocol` by delegating to the Kotlin `MnemonicProvider`.
final class MnemonicProviderWrapper: MnemonicProviderProtocol {
    private let provider: MnemonicProvider

    init(provider: MnemonicProvider) {
        self.provider = provider
    }

    func generate(wordCount: MnemonicWordCount) -> String {
        provider.generate(wordCount: wordCount)
    }

    func validate(mnemonic: String) -> MnemonicValidationResult {
        provider.validate(mnemonic: mnemonic)
    }

    func toSeed(mnemonic: String, passphrase: String) -> KotlinByteArray {
        provider.toSeed(mnemonic: mnemonic, passphrase: passphrase)
    }

    func toKeypair(mnemonic: String, passphrase: String, derivationPath: String) -> Keypair {
        provider.toKeypair(mnemonic: mnemonic, passphrase: passphrase, derivationPath: derivationPath)
    }
}
