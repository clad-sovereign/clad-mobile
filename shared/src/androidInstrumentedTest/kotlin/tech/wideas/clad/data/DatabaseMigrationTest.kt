package tech.wideas.clad.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.wideas.clad.crypto.KeyType
import tech.wideas.clad.database.CladDatabase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests database migrations work correctly.
 *
 * These tests verify that:
 * 1. Existing data is preserved during migrations
 * 2. New tables are created correctly
 * 3. The app can function after migration
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }
    private val testDbName = "migration_test.db"

    @Before
    fun setup() {
        // Delete test database before each test
        context.deleteDatabase(testDbName)
    }

    @After
    fun teardown() {
        // Clean up test database
        context.deleteDatabase(testDbName)
    }

    /**
     * Tests migration from schema version 1 to version 2.
     *
     * Version 1 had only the Account table.
     * Version 2 adds the AppSettings table.
     */
    @Test
    fun migration_v1_to_v2_createsAppSettingsTable() = runTest {
        // Step 1: Create a version 1 database manually
        createVersion1Database()

        // Step 2: Insert test data into v1 schema
        insertV1TestData()

        // Step 3: Open database with current schema (triggers migration)
        val driver = AndroidSqliteDriver(
            schema = CladDatabase.Schema,
            context = context,
            name = testDbName
        )
        val database = CladDatabase(driver)
        val repository = AccountRepository(database)

        // Step 4: Verify existing accounts survived migration
        val accounts = repository.getAll()
        assertEquals(2, accounts.size)

        val alice = accounts.find { it.label == "Alice" }
        assertNotNull(alice)
        assertEquals("5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", alice.address)
        assertEquals(KeyType.SR25519, alice.keyType)

        val bob = accounts.find { it.label == "Bob" }
        assertNotNull(bob)
        assertEquals("5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty", bob.address)
        assertEquals(KeyType.ED25519, bob.keyType)

        // Step 5: Verify new AppSettings table works
        assertNull(repository.getActiveAccountId())
        repository.setActiveAccount(alice.id)
        assertEquals(alice.id, repository.getActiveAccountId())

        driver.close()
    }

    /**
     * Tests that fresh install creates both tables correctly.
     */
    @Test
    fun freshInstall_createsBothTables() = runTest {
        val driver = AndroidSqliteDriver(
            schema = CladDatabase.Schema,
            context = context,
            name = testDbName
        )
        val database = CladDatabase(driver)
        val repository = AccountRepository(database)

        // Verify Account table works
        val account = repository.create(
            label = "Test",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            keyType = KeyType.SR25519
        )
        assertNotNull(account.id)

        // Verify AppSettings table works
        repository.setActiveAccount(account.id)
        assertEquals(account.id, repository.getActiveAccountId())

        driver.close()
    }

    /**
     * Creates a database with version 1 schema (only Account table).
     */
    private fun createVersion1Database() {
        val db = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(testDbName),
            null
        )

        // Create Account table (v1 schema)
        db.execSQL("""
            CREATE TABLE Account (
                id TEXT NOT NULL PRIMARY KEY,
                label TEXT NOT NULL,
                address TEXT NOT NULL UNIQUE,
                keyType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())

        // Set schema version to 1
        db.version = 1

        db.close()
    }

    /**
     * Inserts test data into a v1 database.
     */
    private fun insertV1TestData() {
        val db = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(testDbName),
            null
        )

        val now = System.currentTimeMillis()

        // Insert Alice
        db.execSQL("""
            INSERT INTO Account (id, label, address, keyType, createdAt, lastUsedAt)
            VALUES ('alice-id-123', 'Alice', '5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY', 'SR25519', $now, NULL)
        """.trimIndent())

        // Insert Bob
        db.execSQL("""
            INSERT INTO Account (id, label, address, keyType, createdAt, lastUsedAt)
            VALUES ('bob-id-456', 'Bob', '5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty', 'ED25519', ${now - 1000}, ${now - 500})
        """.trimIndent())

        db.close()
    }
}
