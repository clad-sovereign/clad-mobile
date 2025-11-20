package tech.wideas.clad.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter

/**
 * iOS-specific Kermit logger initialization
 * Exposed to Swift for app startup configuration
 */
class KermitLogger {
    fun initialize() {
        Logger.setLogWriters(platformLogWriter())
        Logger.setTag("CladSigner")
    }
}
