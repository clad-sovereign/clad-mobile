import SwiftUI
import Observation
import Shared

/// ViewModel for managing the account list using the modern @Observable macro (iOS 17+)
@MainActor
@Observable
final class AccountListViewModel {

    // MARK: - Dependencies
    private let accountRepository: AccountRepository
    private let keyStorage: KeyStorage
    private var observeTask: Task<Void, Never>?

    // MARK: - Observable State
    var accounts: [AccountInfo] = []
    var isLoading: Bool = true
    var errorMessage: String?
    var accountToDelete: AccountInfo?
    var showDeleteConfirmation: Bool = false

    // MARK: - Initialization
    init() {
        let helper = ViewModelHelper()
        self.accountRepository = helper.getAccountRepository()
        self.keyStorage = helper.getKeyStorage()

        startObserving()
    }

    func cleanup() {
        observeTask?.cancel()
    }

    // MARK: - Observation

    private func startObserving() {
        observeTask = Task { [weak self] in
            guard let self = self else { return }

            // Use Kotlin Flow to observe account changes
            for await accountList in self.accountRepository.observeAll() {
                self.accounts = accountList
                self.isLoading = false
            }
        }
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

            accountToDelete = nil
            showDeleteConfirmation = false
        } catch {
            errorMessage = "Failed to delete account: \(error.localizedDescription)"
        }
    }
}
