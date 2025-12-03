import Foundation
import Security
import Shared
import os.log

/// Swift Keychain helper that implements Kotlin's KeychainHelperProtocol.
/// Calling Keychain APIs from Swift is more reliable than from Kotlin/Native.
public class KeychainHelper: KeychainHelperProtocol {

    private let service: String
    private let logger = Logger(subsystem: "tech.wideas.clad", category: "KeychainHelper")

    public init(service: String) {
        self.service = service
    }

    public func save(key: String, value: String) -> Int32 {
        guard let data = value.data(using: .utf8) else {
            return -50 // errSecParam
        }

        // Delete existing item first
        let deleteQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(deleteQuery as CFDictionary)

        // Add new item
        let addQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(addQuery as CFDictionary, nil)
        logger.debug("save('\(key, privacy: .public)') status: \(status)")
        return status
    }

    public func get(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        logger.debug("get('\(key, privacy: .public)') status: \(status)")

        if status == errSecSuccess, let data = result as? Data {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }

    public func delete(key: String) -> Int32 {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        logger.debug("delete('\(key, privacy: .public)') status: \(status)")
        return status
    }

    public func contains(key: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    public func clear() -> Int32 {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service
        ]

        let status = SecItemDelete(query as CFDictionary)
        logger.debug("clear() status: \(status)")
        return status
    }
}
