import XCTest
@testable import CladSigner
import Shared

/// Tests for KeychainHelper using MockKeychainHelper.
/// These tests verify the protocol contract and mock behavior.
final class KeychainHelperTests: XCTestCase {

    var mockKeychain: MockKeychainHelper!

    override func setUp() {
        super.setUp()
        mockKeychain = MockKeychainHelper()
    }

    override func tearDown() {
        mockKeychain.reset()
        mockKeychain = nil
        super.tearDown()
    }

    // MARK: - Save Tests

    func testSaveSuccess() {
        let status = mockKeychain.save(key: "testKey", value: "testValue")

        XCTAssertEqual(status, 0, "Save should return success (0)")
        XCTAssertEqual(mockKeychain.saveCalls.count, 1)
        XCTAssertEqual(mockKeychain.saveCalls.first?.key, "testKey")
        XCTAssertEqual(mockKeychain.saveCalls.first?.value, "testValue")
    }

    func testSaveFailure() {
        mockKeychain.shouldFailSave = true

        let status = mockKeychain.save(key: "testKey", value: "testValue")

        XCTAssertEqual(status, -50, "Save should return errSecParam (-50) when configured to fail")
    }

    // MARK: - Get Tests

    func testGetExistingKey() {
        _ = mockKeychain.save(key: "testKey", value: "testValue")

        let result = mockKeychain.get(key: "testKey")

        XCTAssertEqual(result, "testValue")
        XCTAssertEqual(mockKeychain.getCalls.count, 1)
        XCTAssertEqual(mockKeychain.getCalls.first, "testKey")
    }

    func testGetNonExistingKey() {
        let result = mockKeychain.get(key: "nonExistingKey")

        XCTAssertNil(result)
    }

    // MARK: - Delete Tests

    func testDeleteSuccess() {
        _ = mockKeychain.save(key: "testKey", value: "testValue")

        let status = mockKeychain.delete(key: "testKey")

        XCTAssertEqual(status, 0, "Delete should return success (0)")
        XCTAssertNil(mockKeychain.get(key: "testKey"), "Key should no longer exist after delete")
    }

    func testDeleteFailure() {
        mockKeychain.shouldFailDelete = true

        let status = mockKeychain.delete(key: "testKey")

        XCTAssertEqual(status, -50, "Delete should return error when configured to fail")
    }

    // MARK: - Contains Tests

    func testContainsExistingKey() {
        _ = mockKeychain.save(key: "testKey", value: "testValue")

        let result = mockKeychain.contains(key: "testKey")

        XCTAssertTrue(result)
        XCTAssertEqual(mockKeychain.containsCalls.count, 1)
    }

    func testContainsNonExistingKey() {
        let result = mockKeychain.contains(key: "nonExistingKey")

        XCTAssertFalse(result)
    }

    // MARK: - Clear Tests

    func testClear() {
        _ = mockKeychain.save(key: "key1", value: "value1")
        _ = mockKeychain.save(key: "key2", value: "value2")

        let status = mockKeychain.clear()

        XCTAssertEqual(status, 0, "Clear should return success (0)")
        XCTAssertEqual(mockKeychain.clearCalls, 1)
        XCTAssertNil(mockKeychain.get(key: "key1"))
        XCTAssertNil(mockKeychain.get(key: "key2"))
    }

    // MARK: - Reset Tests

    func testReset() {
        _ = mockKeychain.save(key: "testKey", value: "testValue")
        mockKeychain.shouldFailSave = true
        mockKeychain.shouldFailDelete = true

        mockKeychain.reset()

        XCTAssertNil(mockKeychain.get(key: "testKey"), "Storage should be cleared")
        XCTAssertTrue(mockKeychain.saveCalls.isEmpty, "Save calls should be cleared")
        XCTAssertFalse(mockKeychain.shouldFailSave, "shouldFailSave should be reset")
        XCTAssertFalse(mockKeychain.shouldFailDelete, "shouldFailDelete should be reset")
    }

    // MARK: - Real KeychainHelper Integration Tests

    func testRealKeychainHelperSaveAndGet() {
        // Use a unique service name to avoid conflicts
        let testService = "tech.wideas.clad.test.\(UUID().uuidString)"
        let keychain = KeychainHelper(service: testService)

        // Save
        let saveStatus = keychain.save(key: "integrationTestKey", value: "integrationTestValue")
        XCTAssertEqual(saveStatus, 0, "Real Keychain save should succeed")

        // Get
        let value = keychain.get(key: "integrationTestKey")
        XCTAssertEqual(value, "integrationTestValue", "Real Keychain get should return saved value")

        // Cleanup
        _ = keychain.delete(key: "integrationTestKey")
    }

    func testRealKeychainHelperContains() {
        let testService = "tech.wideas.clad.test.\(UUID().uuidString)"
        let keychain = KeychainHelper(service: testService)

        // Should not contain before save
        XCTAssertFalse(keychain.contains(key: "containsTestKey"))

        // Save
        _ = keychain.save(key: "containsTestKey", value: "value")

        // Should contain after save
        XCTAssertTrue(keychain.contains(key: "containsTestKey"))

        // Cleanup
        _ = keychain.delete(key: "containsTestKey")

        // Should not contain after delete
        XCTAssertFalse(keychain.contains(key: "containsTestKey"))
    }

    func testRealKeychainHelperClear() {
        let testService = "tech.wideas.clad.test.\(UUID().uuidString)"
        let keychain = KeychainHelper(service: testService)

        // Save multiple items
        _ = keychain.save(key: "clearTest1", value: "value1")
        _ = keychain.save(key: "clearTest2", value: "value2")

        // Clear
        let clearStatus = keychain.clear()
        XCTAssertEqual(clearStatus, 0, "Clear should succeed")

        // Verify items are gone
        XCTAssertNil(keychain.get(key: "clearTest1"))
        XCTAssertNil(keychain.get(key: "clearTest2"))
    }
}
