import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Configure Kermit logger for iOS
        KermitLogger().initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}