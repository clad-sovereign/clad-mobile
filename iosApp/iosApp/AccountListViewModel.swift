import SwiftUI
import Observation
import Shared

/// ViewModel for managing the account list using the modern @Observable macro (iOS 17+)
@MainActor
@Observable
final class AccountListViewModel {

    // MARK: - Dependencies
    private let accountRepository: any AccountRepositoryProtocol
    private let keyStorage: any KeyStorageProtocol
    private var observeTask: Task<Void, Never>?
    private var observeActiveTask: Task<Void, Never>?

    // MARK: - Observable State
    var accounts: [AccountInfo] = []
    var activeAccountId: String?
    var isLoading: Bool = true
    var errorMessage: String?
    var accountToDelete: AccountInfo?
    var showDeleteConfirmation: Bool = false
    var selectedAccount: AccountInfo?
    var showAccountDetails: Bool = false

    // MARK: - Initialization

    /// Production initializer using DependencyContainer
    init() {
        let container = DependencyContainer.shared
        self.accountRepository = container.accountRepository
        self.keyStorage = container.keyStorage

        startObserving()
    }

    /// Test initializer for dependency injection with protocols
    init(
        accountRepository: any AccountRepositoryProtocol,
        keyStorage: any KeyStorageProtocol,
        startObserving: Bool = true
    ) {
        self.accountRepository = accountRepository
        self.keyStorage = keyStorage

        if startObserving {
            self.startObserving()
        }
    }

    func cleanup() {
        observeTask?.cancel()
        observeActiveTask?.cancel()
    }

    // MARK: - Observation

    private func startObserving() {
        // Observe accounts list
        observeTask = Task { [weak self] in
            guard let self = self else { return }

            for await accountList in self.accountRepository.observeAll() {
                self.accounts = accountList
                self.isLoading = false
            }
        }

        // Observe active account ID
        observeActiveTask = Task { [weak self] in
            guard let self = self else { return }

            for await activeId in self.accountRepository.observeActiveAccountId() {
                self.activeAccountId = activeId
            }
        }
    }

    // MARK: - Helper Methods

    func isActive(_ account: AccountInfo) -> Bool {
        account.id == activeAccountId
    }

    // MARK: - Actions

    func refreshAccounts() async {
        isLoading = true
        errorMessage = nil

        do {
            accounts = try await accountRepository.getAll()
        } catch {
            errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    func confirmDelete(account: AccountInfo) {
        accountToDelete = account
        showDeleteConfirmation = true
    }

    func cancelDelete() {
        accountToDelete = nil
        showDeleteConfirmation = false
    }

    func deleteAccount() async {
        guard let account = accountToDelete else { return }

        do {
            // Delete keypair from secure storage (if exists)
            _ = try await keyStorage.deleteKeypair(accountId: account.id)

            // Delete account metadata
            try await accountRepository.delete(id: account.id)

            // Clear active account if we just deleted it
            if activeAccountId == account.id {
                try await accountRepository.setActiveAccount(accountId: nil)
            }

            accountToDelete = nil
            showDeleteConfirmation = false
        } catch {
            errorMessage = "Failed to delete account: \(error.localizedDescription)"
        }
    }

    func setActiveAccount(_ account: AccountInfo) async {
        do {
            try await accountRepository.setActiveAccount(accountId: account.id)
        } catch {
            errorMessage = "Failed to set active account: \(error.localizedDescription)"
        }
    }

    func updateAccountLabel(accountId: String, newLabel: String) async {
        do {
            try await accountRepository.updateLabel(id: accountId, label: newLabel)
        } catch {
            errorMessage = "Failed to update account label: \(error.localizedDescription)"
        }
    }

    func showDetails(for account: AccountInfo) {
        selectedAccount = account
        showAccountDetails = true
    }

    func dismissDetails() {
        selectedAccount = nil
        showAccountDetails = false
    }
}
