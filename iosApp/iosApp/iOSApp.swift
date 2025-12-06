import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Configure Kermit logger for iOS
        #if DEBUG
        let isDebug = true
        KermitLogger.Companion.shared.initialize(isDebug: true)
        #else
        let isDebug = false
        KermitLogger.Companion.shared.initialize(isDebug: false)
        #endif

        // Set up Swift Keychain helpers for Kotlin storage classes
        // Must be done BEFORE Koin initialization
        let secureStorageHelper = KeychainHelper(service: "tech.wideas.clad.securestorage")
        SecureStorageFactory.shared.setKeychainHelper(helper: secureStorageHelper)

        let keyStorageHelper = BiometricKeychainHelper(service: "tech.wideas.clad.keystorage")
        KeyStorageFactory.shared.setKeychainHelper(helper: keyStorageHelper)

        // Initialize Koin DI with debug flag
        KoinInitializer.shared.initialize(isDebug: isDebug)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .task {
                    // Seed debug accounts on first launch (debug builds only)
                    await seedDebugAccountsIfNeeded()
                }
        }
    }

    /// Trigger debug account seeding on first launch.
    /// Only runs in debug builds when database is empty.
    private func seedDebugAccountsIfNeeded() async {
        await withCheckedContinuation { continuation in
            Task {
                // Call the Kotlin suspend function from Swift
                // Note: seedIfNeeded() returns a sealed class result
                _ = try? await DebugSeederHelper.shared.seedIfNeeded()
                continuation.resume()
            }
        }
    }
}
