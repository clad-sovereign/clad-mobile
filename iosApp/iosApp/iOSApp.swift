import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Configure Kermit logger for iOS
        #if DEBUG
        KermitLogger.Companion.shared.initialize(isDebug: true)
        #else
        KermitLogger.Companion.shared.initialize(isDebug: false)
        #endif

        // Set up Swift Keychain helpers for Kotlin storage classes
        // Must be done BEFORE Koin initialization
        let secureStorageHelper = KeychainHelper(service: "tech.wideas.clad.securestorage")
        SecureStorageFactory.shared.setKeychainHelper(helper: secureStorageHelper)

        let keyStorageHelper = BiometricKeychainHelper(service: "tech.wideas.clad.keystorage")
        KeyStorageFactory.shared.setKeychainHelper(helper: keyStorageHelper)

        // Initialize Koin DI
        KoinInitializer.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
