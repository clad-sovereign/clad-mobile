package tech.wideas.clad.database

import tech.wideas.clad.data.AccountRepository

/**
 * Creates an in-memory CladDatabase for testing.
 *
 * This function is exposed to Swift for use in XCTest.
 * The database is isolated and will be destroyed when the driver is closed.
 *
 * @return CladDatabase instance backed by in-memory SQLite
 */
fun createTestDatabase(): CladDatabase {
    return CladDatabase(createInMemoryDriver())
}

/**
 * Creates an AccountRepository with an in-memory database for testing.
 *
 * Convenience function for Swift tests that need an isolated repository.
 *
 * @return AccountRepository backed by in-memory database
 */
fun createTestAccountRepository(): AccountRepository {
    return AccountRepository(createTestDatabase())
}
