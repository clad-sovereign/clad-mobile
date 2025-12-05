import XCTest
@testable import CladSigner
import Shared

/// Tests for AccountListViewModel using protocol-based dependency injection.
@MainActor
final class AccountListViewModelTests: XCTestCase {

    // MARK: - Test Dependencies

    private var mockAccountRepository: MockAccountRepository!
    private var mockKeyStorage: MockKeyStorage!
    private var viewModel: AccountListViewModel!

    override func setUp() {
        super.setUp()
        mockAccountRepository = MockAccountRepository()
        mockKeyStorage = MockKeyStorage()
        viewModel = AccountListViewModel(
            accountRepository: mockAccountRepository,
            keyStorage: mockKeyStorage,
            startObserving: false  // Disable observation for unit tests
        )
    }

    override func tearDown() {
        viewModel.cleanup()
        mockAccountRepository = nil
        mockKeyStorage = nil
        viewModel = nil
        super.tearDown()
    }

    // MARK: - Initial State Tests

    func testInitialState() {
        XCTAssertTrue(viewModel.accounts.isEmpty)
        XCTAssertNil(viewModel.activeAccountId)
        XCTAssertTrue(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertNil(viewModel.accountToDelete)
        XCTAssertFalse(viewModel.showDeleteConfirmation)
        XCTAssertNil(viewModel.selectedAccount)
        XCTAssertFalse(viewModel.showAccountDetails)
    }

    // MARK: - Refresh Tests

    func testRefreshAccountsSuccess() async {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        mockAccountRepository.addAccount(account)

        await viewModel.refreshAccounts()

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.accounts.count, 1)
        XCTAssertEqual(viewModel.accounts[0].id, "test-id")
        XCTAssertNil(viewModel.errorMessage)
    }

    func testRefreshAccountsError() async {
        struct TestError: Error, LocalizedError {
            var errorDescription: String? { "Test error message" }
        }
        mockAccountRepository.errorToThrow = TestError()

        await viewModel.refreshAccounts()

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.errorMessage, "Test error message")
    }

    // MARK: - Active Account Tests

    func testIsActiveAccountTrue() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        viewModel.activeAccountId = "test-id"

        XCTAssertTrue(viewModel.isActive(account))
    }

    func testIsActiveAccountFalse() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        viewModel.activeAccountId = "other-id"

        XCTAssertFalse(viewModel.isActive(account))
    }

    func testSetActiveAccountSuccess() async {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )

        await viewModel.setActiveAccount(account)

        XCTAssertEqual(mockAccountRepository.setActiveAccountCallCount, 1)
        XCTAssertNil(viewModel.errorMessage)
    }

    func testSetActiveAccountError() async {
        struct TestError: Error, LocalizedError {
            var errorDescription: String? { "Failed to set active" }
        }
        mockAccountRepository.errorToThrow = TestError()

        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )

        await viewModel.setActiveAccount(account)

        XCTAssertTrue(viewModel.errorMessage?.contains("Failed to set active account") ?? false)
    }

    // MARK: - Delete Tests

    func testConfirmDelete() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )

        viewModel.confirmDelete(account: account)

        XCTAssertEqual(viewModel.accountToDelete?.id, "test-id")
        XCTAssertTrue(viewModel.showDeleteConfirmation)
    }

    func testCancelDelete() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        viewModel.confirmDelete(account: account)

        viewModel.cancelDelete()

        XCTAssertNil(viewModel.accountToDelete)
        XCTAssertFalse(viewModel.showDeleteConfirmation)
    }

    func testDeleteAccountSuccess() async {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        mockAccountRepository.addAccount(account)
        viewModel.confirmDelete(account: account)

        await viewModel.deleteAccount()

        XCTAssertEqual(mockKeyStorage.deleteKeypairCallCount, 1)
        XCTAssertEqual(mockKeyStorage.lastDeletedAccountId, "test-id")
        XCTAssertEqual(mockAccountRepository.deleteCallCount, 1)
        XCTAssertEqual(mockAccountRepository.lastDeletedId, "test-id")
        XCTAssertNil(viewModel.accountToDelete)
        XCTAssertFalse(viewModel.showDeleteConfirmation)
    }

    func testDeleteAccountClearsActiveIfDeleted() async {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        mockAccountRepository.addAccount(account)
        mockAccountRepository.activeAccountId = "test-id"
        viewModel.activeAccountId = "test-id"
        viewModel.confirmDelete(account: account)

        await viewModel.deleteAccount()

        XCTAssertEqual(mockAccountRepository.setActiveAccountCallCount, 1)
    }

    func testDeleteAccountDoesNotClearActiveIfDifferent() async {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        mockAccountRepository.addAccount(account)
        viewModel.activeAccountId = "other-id"  // Different account is active
        viewModel.confirmDelete(account: account)

        await viewModel.deleteAccount()

        XCTAssertEqual(mockAccountRepository.setActiveAccountCallCount, 0)
    }

    func testDeleteAccountWithNoAccountToDelete() async {
        await viewModel.deleteAccount()

        // Should not crash, and should not call delete
        XCTAssertEqual(mockAccountRepository.deleteCallCount, 0)
    }

    // MARK: - Update Label Tests

    func testUpdateAccountLabelSuccess() async {
        await viewModel.updateAccountLabel(accountId: "test-id", newLabel: "New Label")

        XCTAssertEqual(mockAccountRepository.updateLabelCallCount, 1)
        XCTAssertEqual(mockAccountRepository.lastUpdatedLabelId, "test-id")
        XCTAssertEqual(mockAccountRepository.lastUpdatedLabel, "New Label")
        XCTAssertNil(viewModel.errorMessage)
    }

    func testUpdateAccountLabelError() async {
        struct TestError: Error, LocalizedError {
            var errorDescription: String? { "Update failed" }
        }
        mockAccountRepository.errorToThrow = TestError()

        await viewModel.updateAccountLabel(accountId: "test-id", newLabel: "New Label")

        XCTAssertTrue(viewModel.errorMessage?.contains("Failed to update account label") ?? false)
    }

    // MARK: - Details Tests

    func testShowDetails() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )

        viewModel.showDetails(for: account)

        XCTAssertEqual(viewModel.selectedAccount?.id, "test-id")
        XCTAssertTrue(viewModel.showAccountDetails)
    }

    func testDismissDetails() {
        let account = AccountInfo(
            id: "test-id",
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 0,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        viewModel.showDetails(for: account)

        viewModel.dismissDetails()

        XCTAssertNil(viewModel.selectedAccount)
        XCTAssertFalse(viewModel.showAccountDetails)
    }

    // MARK: - Multiple Accounts Tests

    func testRefreshAccountsMultiple() async {
        let account1 = AccountInfo(
            id: "id-1",
            label: "Account 1",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt: 1000,
            lastUsedAt: nil,
            mode: .live,
            derivationPath: nil
        )
        let account2 = AccountInfo(
            id: "id-2",
            label: "Account 2",
            address: "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty",
            createdAt: 2000,
            lastUsedAt: nil,
            mode: .demo,
            derivationPath: "//demo"
        )
        mockAccountRepository.addAccount(account1)
        mockAccountRepository.addAccount(account2)

        await viewModel.refreshAccounts()

        XCTAssertEqual(viewModel.accounts.count, 2)
    }
}
