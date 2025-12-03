import Foundation
import Security
import LocalAuthentication
import Shared
import os.log

/// Swift helper for biometric-protected Keychain operations.
/// Implements KeyStorage operations with Face ID/Touch ID protection.
public class BiometricKeychainHelper: BiometricKeychainHelperProtocol {

    private let service: String
    private let keypairPrefix: String
    private let logger = Logger(subsystem: "tech.wideas.clad", category: "BiometricKeychainHelper")

    public init(service: String, keypairPrefix: String = "keypair_") {
        self.service = service
        self.keypairPrefix = keypairPrefix
    }

    // MARK: - BiometricKeychainHelperProtocol

    public func isAvailable() -> Bool {
        let context = LAContext()
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
    }

    public func saveKeypair(accountId: String, data: Data, promptTitle: String) -> Int32 {
        // Create access control with biometric protection
        var error: Unmanaged<CFError>?
        guard let accessControl = SecAccessControlCreateWithFlags(
            nil,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            .biometryCurrentSet,
            &error
        ) else {
            logger.error("Failed to create access control: \(error?.takeRetainedValue().localizedDescription ?? "unknown")")
            return -50 // errSecParam
        }

        // Create LAContext for biometric prompt
        let context = LAContext()
        context.localizedReason = promptTitle

        let accountKey = keypairPrefix + accountId

        // Delete existing item first (ignore errors)
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: accountKey
        ]
        SecItemDelete(deleteQuery as CFDictionary)

        // Add new item with biometric protection
        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: accountKey,
            kSecValueData as String: data,
            kSecAttrAccessControl as String: accessControl,
            kSecUseAuthenticationContext as String: context
        ]

        let status = SecItemAdd(addQuery as CFDictionary, nil)
        logger.debug("saveKeypair('\(accountId, privacy: .public)') status: \(status)")
        return status
    }

    public func getKeypair(accountId: String, promptTitle: String) -> any BiometricKeychainResultProtocol {
        let context = LAContext()
        context.localizedReason = promptTitle

        let accountKey = keypairPrefix + accountId

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: accountKey,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecUseAuthenticationContext as String: context
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        logger.debug("getKeypair('\(accountId, privacy: .public)') status: \(status)")

        if status == errSecSuccess, let data = result as? Data {
            return BiometricKeychainResult(status: status, data: data)
        }
        return BiometricKeychainResult(status: status, data: nil)
    }

    public func deleteKeypair(accountId: String) -> Int32 {
        let accountKey = keypairPrefix + accountId

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: accountKey
        ]

        let status = SecItemDelete(query as CFDictionary)
        logger.debug("deleteKeypair('\(accountId, privacy: .public)') status: \(status)")
        return status
    }

    public func hasKeypair(accountId: String) -> Bool {
        let accountKey = keypairPrefix + accountId

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: accountKey,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    public func listAccountIds() -> [String] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        logger.debug("listAccountIds() status: \(status)")

        guard status == errSecSuccess,
              let items = result as? [[String: Any]] else {
            return []
        }

        return items.compactMap { item -> String? in
            guard let account = item[kSecAttrAccount as String] as? String,
                  account.hasPrefix(keypairPrefix) else {
                return nil
            }
            return String(account.dropFirst(keypairPrefix.count))
        }
    }
}

/// Result wrapper for biometric Keychain operations that return data.
/// Implements Kotlin's BiometricKeychainResultProtocol.
public class BiometricKeychainResult: BiometricKeychainResultProtocol {
    public let status: Int32
    public let data: Foundation.Data?

    // BiometricKeychainResultProtocol requires NSData
    public var data_: Foundation.NSData? {
        data as Foundation.NSData?
    }

    public init(status: Int32, data: Foundation.Data?) {
        self.status = status
        self.data = data
    }
}
