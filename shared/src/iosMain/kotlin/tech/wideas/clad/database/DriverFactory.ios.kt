package tech.wideas.clad.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

/**
 * iOS implementation of [DriverFactory].
 *
 * Uses NativeSqliteDriver which wraps SQLite via Kotlin/Native interop.
 * The database file is stored in the app's Documents directory.
 */
actual class DriverFactory {
    /**
     * Creates a native SQLite driver for the CladDatabase.
     *
     * The database is stored in the app's sandboxed Documents directory.
     *
     * @return NativeSqliteDriver instance
     */
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = CladDatabase.Schema,
            name = DATABASE_NAME
        )
    }
}

/**
 * Creates an in-memory SQLite driver for testing on iOS.
 *
 * Uses SQLDelight's built-in in-memory driver support.
 * This is internal and only accessible within the shared module for test helpers.
 */
internal fun createInMemoryDriver(): SqlDriver {
    return inMemoryDriver(CladDatabase.Schema)
}
