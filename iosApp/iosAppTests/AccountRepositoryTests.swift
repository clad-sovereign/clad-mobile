import XCTest
import Shared

/// Tests for AccountRepository iOS implementation.
///
/// These tests verify the SQLDelight-based account persistence layer
/// using an in-memory database for isolation.
///
/// Note: All accounts use SR25519 keys (see issue #60 for rationale).
final class AccountRepositoryTests: XCTestCase {

    private var repository: AccountRepository!

    override func setUp() {
        super.setUp()
        // Create a fresh in-memory repository for each test
        repository = TestHelpers_iosKt.createTestAccountRepository()
    }

    override func tearDown() {
        repository = nil
        super.tearDown()
    }

    // MARK: - Create Tests

    func testCreateInsertsAccountAndReturnsIt() async throws {
        let account = try await repository.create(
            label: "Test Account",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        )

        XCTAssertFalse(account.id.isEmpty, "ID should not be empty")
        XCTAssertEqual(account.label, "Test Account")
        XCTAssertEqual(account.address, "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY")
        XCTAssertGreaterThan(account.createdAt, 0, "createdAt should be set")
        XCTAssertNil(account.lastUsedAt, "lastUsedAt should be nil initially")
    }

    // MARK: - GetById Tests

    func testGetByIdReturnsAccount() async throws {
        let created = try await repository.create(
            label: "Test",
            address: "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        )

        let retrieved = try await repository.getById(id: created.id)

        XCTAssertNotNil(retrieved)
        XCTAssertEqual(retrieved?.id, created.id)
        XCTAssertEqual(retrieved?.label, created.label)
        XCTAssertEqual(retrieved?.address, created.address)
    }

    func testGetByIdReturnsNilForNonExistent() async throws {
        let result = try await repository.getById(id: "non-existent-id")
        XCTAssertNil(result)
    }

    // MARK: - GetByAddress Tests

    func testGetByAddressReturnsAccount() async throws {
        let address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        let created = try await repository.create(
            label: "Test",
            address: address
        )

        let retrieved = try await repository.getByAddress(address: address)

        XCTAssertNotNil(retrieved)
        XCTAssertEqual(retrieved?.id, created.id)
    }

    func testGetByAddressReturnsNilForNonExistent() async throws {
        let result = try await repository.getByAddress(address: "5Unknown")
        XCTAssertNil(result)
    }

    // MARK: - GetAll Tests

    func testGetAllReturnsAllAccountsOrderedByCreatedAtDesc() async throws {
        // Add small delays to ensure distinct timestamps
        _ = try await repository.create(label: "First", address: "5Address1")
        try await Task.sleep(nanoseconds: 10_000_000) // 10ms
        _ = try await repository.create(label: "Second", address: "5Address2")
        try await Task.sleep(nanoseconds: 10_000_000) // 10ms
        _ = try await repository.create(label: "Third", address: "5Address3")

        let accounts = try await repository.getAll()

        XCTAssertEqual(accounts.count, 3)
        // Most recent first
        XCTAssertEqual(accounts[0].label, "Third")
        XCTAssertEqual(accounts[1].label, "Second")
        XCTAssertEqual(accounts[2].label, "First")
    }

    // MARK: - UpdateLabel Tests

    func testUpdateLabelChangesAccountLabel() async throws {
        let account = try await repository.create(
            label: "Original",
            address: "5Address"
        )

        try await repository.updateLabel(id: account.id, label: "Updated")

        let updated = try await repository.getById(id: account.id)
        XCTAssertNotNil(updated)
        XCTAssertEqual(updated?.label, "Updated")
    }

    // MARK: - MarkAsUsed Tests

    func testMarkAsUsedUpdatesLastUsedAt() async throws {
        let account = try await repository.create(
            label: "Test",
            address: "5Address"
        )
        XCTAssertNil(account.lastUsedAt)

        try await repository.markAsUsed(id: account.id)

        let updated = try await repository.getById(id: account.id)
        XCTAssertNotNil(updated)
        XCTAssertNotNil(updated?.lastUsedAt)
        XCTAssertGreaterThan(updated!.lastUsedAt!.int64Value, 0)
    }

    // MARK: - Delete Tests

    func testDeleteRemovesAccount() async throws {
        let account = try await repository.create(
            label: "Test",
            address: "5Address"
        )

        try await repository.delete(id: account.id)

        let result = try await repository.getById(id: account.id)
        XCTAssertNil(result)
    }

    // MARK: - Count Tests

    func testCountReturnsTotalAccounts() async throws {
        let initialCount = try await repository.count()
        XCTAssertEqual(initialCount, 0)

        _ = try await repository.create(label: "First", address: "5Address1")
        let countAfterFirst = try await repository.count()
        XCTAssertEqual(countAfterFirst, 1)

        _ = try await repository.create(label: "Second", address: "5Address2")
        let countAfterSecond = try await repository.count()
        XCTAssertEqual(countAfterSecond, 2)
    }
}
