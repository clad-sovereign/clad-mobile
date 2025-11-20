package tech.wideas.clad.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

/**
 * iOS-specific Kermit logger initialization
 * Exposed to Swift for app startup configuration
 */
class KermitLogger {
    companion object {
        fun initialize(isDebug: Boolean = false) {
            Logger.setLogWriters(platformLogWriter())
            // Set minimum log level based on build type
            Logger.setMinSeverity(if (isDebug) Severity.Debug else Severity.Warn)
        }
    }
}
