import Foundation
@testable import CladSigner
import Shared

/// Mock implementation of KeyStorageProtocol for testing.
@MainActor
final class MockKeyStorage: KeyStorageProtocol {

    // MARK: - Mock State

    /// Stored keypairs by account ID
    var storedKeypairs: [String: Keypair] = [:]

    /// Whether biometric storage is available
    var isAvailableResult = true

    /// Whether hardware-backed storage is available
    var isHardwareBackedResult = true

    /// Result to return from saveKeypair
    var saveKeypairResult: KeyStorageResult<AnyObject>?

    /// Result to return from getKeypair
    var getKeypairResult: KeyStorageResult<AnyObject>?

    /// Result to return from deleteKeypair
    var deleteKeypairResult: KeyStorageResult<AnyObject>?

    /// Track method calls for verification
    var saveKeypairCallCount = 0
    var getKeypairCallCount = 0
    var deleteKeypairCallCount = 0

    /// Captured parameters from method calls
    var lastSavedAccountId: String?
    var lastSavedKeypair: Keypair?
    var lastGetAccountId: String?
    var lastDeletedAccountId: String?

    // MARK: - KeyStorageProtocol

    func isAvailable() async throws -> Bool {
        isAvailableResult
    }

    func isHardwareBackedAvailable() async throws -> Bool {
        isHardwareBackedResult
    }

    func saveKeypair(
        accountId: String,
        keypair: Keypair,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject> {
        saveKeypairCallCount += 1
        lastSavedAccountId = accountId
        lastSavedKeypair = keypair

        if let result = saveKeypairResult {
            return result
        }

        storedKeypairs[accountId] = keypair
        return KeyStorageResultSuccess<AnyObject>(data: KotlinUnit())
    }

    func getKeypair(
        accountId: String,
        promptConfig: BiometricPromptConfig
    ) async throws -> KeyStorageResult<AnyObject> {
        getKeypairCallCount += 1
        lastGetAccountId = accountId

        if let result = getKeypairResult {
            return result
        }

        if let keypair = storedKeypairs[accountId] {
            return KeyStorageResultSuccess<AnyObject>(data: keypair)
        }

        return KeyStorageResultKeyNotFound() as! KeyStorageResult<AnyObject>
    }

    func deleteKeypair(accountId: String) async throws -> KeyStorageResult<AnyObject> {
        deleteKeypairCallCount += 1
        lastDeletedAccountId = accountId

        if let result = deleteKeypairResult {
            return result
        }

        storedKeypairs.removeValue(forKey: accountId)
        return KeyStorageResultSuccess<AnyObject>(data: KotlinUnit())
    }

    func hasKeypair(accountId: String) async throws -> Bool {
        storedKeypairs[accountId] != nil
    }

    func listAccountIds() async throws -> [String] {
        Array(storedKeypairs.keys)
    }

    // MARK: - Test Helpers

    /// Reset all mock state
    func reset() {
        storedKeypairs = [:]
        isAvailableResult = true
        isHardwareBackedResult = true
        saveKeypairResult = nil
        getKeypairResult = nil
        deleteKeypairResult = nil
        saveKeypairCallCount = 0
        getKeypairCallCount = 0
        deleteKeypairCallCount = 0
        lastSavedAccountId = nil
        lastSavedKeypair = nil
        lastGetAccountId = nil
        lastDeletedAccountId = nil
    }

    /// Store a keypair directly for testing
    func storeKeypair(accountId: String, keypair: Keypair) {
        storedKeypairs[accountId] = keypair
    }

    /// Configure to return biometric cancelled
    func simulateBiometricCancelled() {
        saveKeypairResult = KeyStorageResultBiometricCancelled() as! KeyStorageResult<AnyObject>
        getKeypairResult = KeyStorageResultBiometricCancelled() as! KeyStorageResult<AnyObject>
    }

    /// Configure to return biometric error
    func simulateBiometricError(message: String) {
        saveKeypairResult = KeyStorageResultBiometricError(message: message) as! KeyStorageResult<AnyObject>
        getKeypairResult = KeyStorageResultBiometricError(message: message) as! KeyStorageResult<AnyObject>
    }

    /// Configure to return storage error
    func simulateStorageError(message: String) {
        saveKeypairResult = KeyStorageResultStorageError(message: message) as! KeyStorageResult<AnyObject>
        getKeypairResult = KeyStorageResultStorageError(message: message) as! KeyStorageResult<AnyObject>
        deleteKeypairResult = KeyStorageResultStorageError(message: message) as! KeyStorageResult<AnyObject>
    }
}
