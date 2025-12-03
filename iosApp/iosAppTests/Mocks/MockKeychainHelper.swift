import Foundation
import Shared

/// Mock implementation of KeychainHelperProtocol for testing.
/// Stores data in memory instead of the actual Keychain.
class MockKeychainHelper: KeychainHelperProtocol {

    private var storage: [String: String] = [:]

    /// Track method calls for verification in tests
    var saveCalls: [(key: String, value: String)] = []
    var getCalls: [String] = []
    var deleteCalls: [String] = []
    var containsCalls: [String] = []
    var clearCalls: Int = 0

    /// Simulate error responses
    var shouldFailSave: Bool = false
    var shouldFailDelete: Bool = false

    func save(key: String, value: String) -> Int32 {
        saveCalls.append((key: key, value: value))

        if shouldFailSave {
            return -50 // errSecParam
        }

        storage[key] = value
        return 0 // errSecSuccess
    }

    func get(key: String) -> String? {
        getCalls.append(key)
        return storage[key]
    }

    func delete(key: String) -> Int32 {
        deleteCalls.append(key)

        if shouldFailDelete {
            return -50
        }

        storage.removeValue(forKey: key)
        return 0
    }

    func contains(key: String) -> Bool {
        containsCalls.append(key)
        return storage[key] != nil
    }

    func clear() -> Int32 {
        clearCalls += 1
        storage.removeAll()
        return 0
    }

    // MARK: - Test Helpers

    func reset() {
        storage.removeAll()
        saveCalls.removeAll()
        getCalls.removeAll()
        deleteCalls.removeAll()
        containsCalls.removeAll()
        clearCalls = 0
        shouldFailSave = false
        shouldFailDelete = false
    }
}
