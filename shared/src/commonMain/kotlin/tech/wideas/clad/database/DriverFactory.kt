package tech.wideas.clad.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating platform-specific SQLDelight database drivers.
 *
 * Each platform provides its own implementation:
 * - Android: Uses AndroidSqliteDriver with application context
 * - iOS: Uses NativeSqliteDriver
 */
expect class DriverFactory {
    /**
     * Creates a SQLite driver for the CladDatabase.
     *
     * @return Platform-specific SqlDriver instance
     */
    fun createDriver(): SqlDriver
}

/**
 * Database file name used across all platforms.
 */
internal const val DATABASE_NAME = "clad.db"
