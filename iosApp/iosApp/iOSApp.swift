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

        // Set up Swift KeychainHelper for Kotlin SecureStorage
        // Must be done BEFORE Koin initialization
        let keychainHelper = KeychainHelper(service: "tech.wideas.clad.securestorage")
        SecureStorageFactory.shared.setKeychainHelper(helper: keychainHelper)

        // Initialize Koin DI
        KoinInitializer.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
