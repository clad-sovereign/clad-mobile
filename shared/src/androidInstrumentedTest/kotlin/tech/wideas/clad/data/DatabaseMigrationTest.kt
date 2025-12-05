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
     * Version 1 had the Account table with keyType column and no AppSettings table.
     * Version 2 removes the keyType column (SR25519-only) and adds AppSettings table.
     */
    @Test
    fun migration_v1_to_v2_removesKeyTypeAndCreatesAppSettings() = runTest {
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

        // Step 4: Verify existing accounts survived migration (keyType column removed)
        val accounts = repository.getAll()
        assertEquals(2, accounts.size)

        val alice = accounts.find { it.label == "Alice" }
        assertNotNull(alice)
        assertEquals("5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", alice.address)

        val bob = accounts.find { it.label == "Bob" }
        assertNotNull(bob)
        assertEquals("5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty", bob.address)

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

        // Verify Account table works (no keyType parameter - SR25519-only)
        val account = repository.create(
            label = "Test",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
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

    /**
     * Tests migration from schema version 3 to version 4.
     *
     * Version 3 had Account table without mode/derivationPath.
     * Version 4 adds mode (defaults to 'LIVE') and derivationPath (nullable).
     */
    @Test
    fun migration_v3_to_v4_addsModeAndDerivationPath() = runTest {
        // Step 1: Create a version 3 database manually
        createVersion3Database()

        // Step 2: Insert test data into v3 schema
        insertV3TestData()

        // Step 3: Open database with current schema (triggers migration)
        val driver = AndroidSqliteDriver(
            schema = CladDatabase.Schema,
            context = context,
            name = testDbName
        )
        val database = CladDatabase(driver)
        val repository = AccountRepository(database)

        // Step 4: Verify existing accounts survived migration with default values
        val accounts = repository.getAll()
        assertEquals(2, accounts.size)

        val alice = accounts.find { it.label == "Alice" }
        assertNotNull(alice)
        assertEquals("5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", alice.address)
        // Migration should set default mode to LIVE
        assertEquals(AccountMode.LIVE, alice.mode)
        // Migration should leave derivationPath as null
        assertNull(alice.derivationPath)

        val bob = accounts.find { it.label == "Bob" }
        assertNotNull(bob)
        assertEquals(AccountMode.LIVE, bob.mode)
        assertNull(bob.derivationPath)

        // Step 5: Verify new accounts can use mode and derivationPath
        val demoAccount = repository.create(
            label = "Demo Test",
            address = "5DemoTestAddress123",
            mode = AccountMode.DEMO,
            derivationPath = "//demo"
        )
        assertEquals(AccountMode.DEMO, demoAccount.mode)
        assertEquals("//demo", demoAccount.derivationPath)

        driver.close()
    }

    /**
     * Creates a database with version 3 schema (Account without mode/derivationPath).
     */
    private fun createVersion3Database() {
        val db = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(testDbName),
            null
        )

        // Create Account table (v3 schema - without mode and derivationPath)
        db.execSQL("""
            CREATE TABLE Account (
                id TEXT NOT NULL PRIMARY KEY,
                label TEXT NOT NULL,
                address TEXT NOT NULL UNIQUE,
                createdAt INTEGER NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())

        // Create AppSettings table (added in v2)
        db.execSQL("""
            CREATE TABLE AppSettings (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
            )
        """.trimIndent())

        // Set schema version to 3
        db.version = 3

        db.close()
    }

    /**
     * Inserts test data into a v3 database.
     */
    private fun insertV3TestData() {
        val db = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(testDbName),
            null
        )

        val now = System.currentTimeMillis()

        // Insert Alice (no mode or derivationPath columns)
        db.execSQL("""
            INSERT INTO Account (id, label, address, createdAt, lastUsedAt)
            VALUES ('alice-id-123', 'Alice', '5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY', $now, NULL)
        """.trimIndent())

        // Insert Bob
        db.execSQL("""
            INSERT INTO Account (id, label, address, createdAt, lastUsedAt)
            VALUES ('bob-id-456', 'Bob', '5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty', ${now - 1000}, ${now - 500})
        """.trimIndent())

        db.close()
    }
}
