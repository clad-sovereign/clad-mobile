package tech.wideas.clad.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of [DriverFactory].
 *
 * Uses AndroidSqliteDriver which wraps Android's built-in SQLite support.
 * The database file is stored in the app's private data directory.
 *
 * @property context Application context for database creation
 */
actual class DriverFactory(private val context: Context) {
    /**
     * Creates an Android SQLite driver for the CladDatabase.
     *
     * The database is stored at: /data/data/{package}/databases/clad.db
     *
     * @return AndroidSqliteDriver instance
     */
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = CladDatabase.Schema,
            context = context,
            name = DATABASE_NAME
        )
    }
}

/**
 * Creates an in-memory SQLite driver for testing on Android.
 *
 * Note: This requires instrumented tests (androidTest) since AndroidSqliteDriver
 * needs a Context. For unit tests, use Robolectric or mock the driver.
 */
actual fun createInMemoryDriver(): SqlDriver {
    // This will only work in instrumented tests where we have access to a context
    throw UnsupportedOperationException(
        "In-memory driver for Android requires Context. Use instrumented tests."
    )
}
