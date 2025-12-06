import Foundation
@testable import CladSigner
import Shared

/// Mock implementation of AccountRepositoryProtocol for testing.
@MainActor
final class MockAccountRepository: AccountRepositoryProtocol {

    // MARK: - Mock State

    /// Stored accounts for testing
    var accounts: [AccountInfo] = []

    /// The active account ID
    var activeAccountId: String?

    /// Error to throw for testing error scenarios
    var errorToThrow: Error?

    /// Track method calls for verification
    var createCallCount = 0
    var deleteCallCount = 0
    var updateLabelCallCount = 0
    var setActiveAccountCallCount = 0

    /// Captured parameters from method calls
    var lastCreatedLabel: String?
    var lastCreatedAddress: String?
    var lastCreatedMode: AccountMode?
    var lastCreatedDerivationPath: String?
    var lastDeletedId: String?
    var lastUpdatedLabelId: String?
    var lastUpdatedLabel: String?

    // MARK: - AccountRepositoryProtocol

    func observeAll() -> AnyAsyncSequence<[AccountInfo]> {
        AnyAsyncSequence(
            AsyncStream { continuation in
                continuation.yield(self.accounts)
                continuation.finish()
            }
        )
    }

    func getAll() async throws -> [AccountInfo] {
        if let error = errorToThrow { throw error }
        return accounts
    }

    func getById(id: String) async throws -> AccountInfo? {
        if let error = errorToThrow { throw error }
        return accounts.first { $0.id == id }
    }

    func getByAddress(address: String) async throws -> AccountInfo? {
        if let error = errorToThrow { throw error }
        return accounts.first { $0.address == address }
    }

    func create(
        label: String,
        address: String,
        mode: AccountMode,
        derivationPath: String?
    ) async throws -> AccountInfo {
        if let error = errorToThrow { throw error }

        createCallCount += 1
        lastCreatedLabel = label
        lastCreatedAddress = address
        lastCreatedMode = mode
        lastCreatedDerivationPath = derivationPath

        let account = AccountInfo(
            id: UUID().uuidString,
            label: label,
            address: address,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastUsedAt: nil,
            mode: mode,
            derivationPath: derivationPath
        )
        accounts.append(account)
        return account
    }

    func updateLabel(id: String, label: String) async throws {
        if let error = errorToThrow { throw error }

        updateLabelCallCount += 1
        lastUpdatedLabelId = id
        lastUpdatedLabel = label

        if let index = accounts.firstIndex(where: { $0.id == id }) {
            let old = accounts[index]
            accounts[index] = AccountInfo(
                id: old.id,
                label: label,
                address: old.address,
                createdAt: old.createdAt,
                lastUsedAt: old.lastUsedAt,
                mode: old.mode,
                derivationPath: old.derivationPath
            )
        }
    }

    func delete(id: String) async throws {
        if let error = errorToThrow { throw error }

        deleteCallCount += 1
        lastDeletedId = id

        accounts.removeAll { $0.id == id }
        if activeAccountId == id {
            activeAccountId = nil
        }
    }

    func count() async throws -> Int64 {
        if let error = errorToThrow { throw error }
        return Int64(accounts.count)
    }

    func getActiveAccountId() async throws -> String? {
        if let error = errorToThrow { throw error }
        return activeAccountId
    }

    func observeActiveAccountId() -> AnyAsyncSequence<String?> {
        AnyAsyncSequence(
            AsyncStream { continuation in
                continuation.yield(self.activeAccountId)
                continuation.finish()
            }
        )
    }

    func setActiveAccount(accountId: String?) async throws {
        if let error = errorToThrow { throw error }

        setActiveAccountCallCount += 1
        activeAccountId = accountId
    }

    func observeActiveAccount() -> AnyAsyncSequence<AccountInfo?> {
        let activeId = activeAccountId
        let accountsList = accounts
        return AnyAsyncSequence(
            AsyncStream { continuation in
                if let id = activeId {
                    continuation.yield(accountsList.first { $0.id == id })
                } else {
                    continuation.yield(nil)
                }
                continuation.finish()
            }
        )
    }

    func getActiveAccount() async throws -> AccountInfo? {
        if let error = errorToThrow { throw error }
        guard let id = activeAccountId else { return nil }
        return accounts.first { $0.id == id }
    }

    // MARK: - Test Helpers

    /// Reset all mock state
    func reset() {
        accounts = []
        activeAccountId = nil
        errorToThrow = nil
        createCallCount = 0
        deleteCallCount = 0
        updateLabelCallCount = 0
        setActiveAccountCallCount = 0
        lastCreatedLabel = nil
        lastCreatedAddress = nil
        lastCreatedMode = nil
        lastCreatedDerivationPath = nil
        lastDeletedId = nil
        lastUpdatedLabelId = nil
        lastUpdatedLabel = nil
    }

    /// Add a test account directly
    func addAccount(_ account: AccountInfo) {
        accounts.append(account)
    }
}
