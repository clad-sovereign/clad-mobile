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

        // Initialize Koin DI
        KoinInitializer.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}