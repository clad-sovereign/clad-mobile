package tech.wideas.clad.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.turbine.test
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
 * Instrumented tests for AccountRepository.
 *
 * Uses an in-memory SQLite database for fast, isolated tests.
 */
@RunWith(AndroidJUnit4::class)
class AccountRepositoryTest {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: CladDatabase
    private lateinit var repository: AccountRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Use null name for in-memory database
        driver = AndroidSqliteDriver(
            schema = CladDatabase.Schema,
            context = context,
            name = null
        )
        database = CladDatabase(driver)
        repository = AccountRepository(database)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun create_insertsAccountAndReturnsIt() = runTest {
        val account = repository.create(
            label = "Test Account",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            keyType = KeyType.SR25519
        )

        assertNotNull(account.id)
        assertEquals("Test Account", account.label)
        assertEquals("5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", account.address)
        assertEquals(KeyType.SR25519, account.keyType)
        assertTrue(account.createdAt > 0)
        assertNull(account.lastUsedAt)
    }

    @Test
    fun getById_returnsAccount() = runTest {
        val created = repository.create(
            label = "Test",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            keyType = KeyType.ED25519
        )

        val retrieved = repository.getById(created.id)

        assertNotNull(retrieved)
        assertEquals(created.id, retrieved.id)
        assertEquals(created.label, retrieved.label)
        assertEquals(created.address, retrieved.address)
        assertEquals(created.keyType, retrieved.keyType)
    }

    @Test
    fun getById_returnsNullForNonExistent() = runTest {
        val result = repository.getById("non-existent-id")
        assertNull(result)
    }

    @Test
    fun getByAddress_returnsAccount() = runTest {
        val address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        val created = repository.create(
            label = "Test",
            address = address,
            keyType = KeyType.SR25519
        )

        val retrieved = repository.getByAddress(address)

        assertNotNull(retrieved)
        assertEquals(created.id, retrieved.id)
    }

    @Test
    fun getByAddress_returnsNullForNonExistent() = runTest {
        val result = repository.getByAddress("5Unknown")
        assertNull(result)
    }

    @Test
    fun getAll_returnsAllAccountsOrderedByCreatedAtDesc() = runTest {
        repository.create("First", "5Address1", KeyType.SR25519)
        repository.create("Second", "5Address2", KeyType.ED25519)
        repository.create("Third", "5Address3", KeyType.SR25519)

        val accounts = repository.getAll()

        assertEquals(3, accounts.size)
        // Most recent first
        assertEquals("Third", accounts[0].label)
        assertEquals("Second", accounts[1].label)
        assertEquals("First", accounts[2].label)
    }

    @Test
    fun updateLabel_changesAccountLabel() = runTest {
        val account = repository.create("Original", "5Address", KeyType.SR25519)

        repository.updateLabel(account.id, "Updated")

        val updated = repository.getById(account.id)
        assertNotNull(updated)
        assertEquals("Updated", updated.label)
    }

    @Test
    fun markAsUsed_updatesLastUsedAt() = runTest {
        val account = repository.create("Test", "5Address", KeyType.SR25519)
        assertNull(account.lastUsedAt)

        repository.markAsUsed(account.id)

        val updated = repository.getById(account.id)
        assertNotNull(updated)
        assertNotNull(updated.lastUsedAt)
        assertTrue(updated.lastUsedAt!! > 0)
    }

    @Test
    fun delete_removesAccount() = runTest {
        val account = repository.create("Test", "5Address", KeyType.SR25519)

        repository.delete(account.id)

        val result = repository.getById(account.id)
        assertNull(result)
    }

    @Test
    fun count_returnsTotalAccounts() = runTest {
        assertEquals(0, repository.count())

        repository.create("First", "5Address1", KeyType.SR25519)
        assertEquals(1, repository.count())

        repository.create("Second", "5Address2", KeyType.ED25519)
        assertEquals(2, repository.count())
    }

    @Test
    fun observeAll_emitsUpdatesOnChanges() = runTest {
        repository.observeAll().test {
            // Initial empty list
            assertEquals(emptyList(), awaitItem())

            // Add first account
            val first = repository.create("First", "5Address1", KeyType.SR25519)
            val afterFirst = awaitItem()
            assertEquals(1, afterFirst.size)
            assertEquals("First", afterFirst[0].label)

            // Add second account
            repository.create("Second", "5Address2", KeyType.ED25519)
            val afterSecond = awaitItem()
            assertEquals(2, afterSecond.size)
            assertEquals("Second", afterSecond[0].label) // Most recent first

            // Delete first account
            repository.delete(first.id)
            val afterDelete = awaitItem()
            assertEquals(1, afterDelete.size)
            assertEquals("Second", afterDelete[0].label)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeById_emitsUpdatesForSpecificAccount() = runTest {
        val account = repository.create("Original", "5Address", KeyType.SR25519)

        repository.observeById(account.id).test {
            // Initial value
            val initial = awaitItem()
            assertNotNull(initial)
            assertEquals("Original", initial.label)

            // Update label
            repository.updateLabel(account.id, "Updated")
            val updated = awaitItem()
            assertNotNull(updated)
            assertEquals("Updated", updated.label)

            // Delete account
            repository.delete(account.id)
            val deleted = awaitItem()
            assertNull(deleted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun create_supportsED25519KeyType() = runTest {
        val account = repository.create(
            label = "ED25519 Account",
            address = "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty",
            keyType = KeyType.ED25519
        )

        assertEquals(KeyType.ED25519, account.keyType)

        val retrieved = repository.getById(account.id)
        assertNotNull(retrieved)
        assertEquals(KeyType.ED25519, retrieved.keyType)
    }

    @Test
    fun create_supportsSR25519KeyType() = runTest {
        val account = repository.create(
            label = "SR25519 Account",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            keyType = KeyType.SR25519
        )

        assertEquals(KeyType.SR25519, account.keyType)

        val retrieved = repository.getById(account.id)
        assertNotNull(retrieved)
        assertEquals(KeyType.SR25519, retrieved.keyType)
    }

    // ==================== Active Account Tests ====================

    @Test
    fun getActiveAccountId_returnsNullWhenNotSet() = runTest {
        val activeId = repository.getActiveAccountId()
        assertNull(activeId)
    }

    @Test
    fun setActiveAccount_setsAndGetsActiveAccountId() = runTest {
        val account = repository.create("Test", "5Address", KeyType.SR25519)

        repository.setActiveAccount(account.id)

        val activeId = repository.getActiveAccountId()
        assertEquals(account.id, activeId)
    }

    @Test
    fun setActiveAccount_clearsWhenSetToNull() = runTest {
        val account = repository.create("Test", "5Address", KeyType.SR25519)
        repository.setActiveAccount(account.id)

        repository.setActiveAccount(null)

        val activeId = repository.getActiveAccountId()
        assertNull(activeId)
    }

    @Test
    fun getActiveAccount_returnsAccountWhenSet() = runTest {
        val account = repository.create("Test Account", "5Address", KeyType.SR25519)
        repository.setActiveAccount(account.id)

        val activeAccount = repository.getActiveAccount()

        assertNotNull(activeAccount)
        assertEquals(account.id, activeAccount.id)
        assertEquals("Test Account", activeAccount.label)
    }

    @Test
    fun getActiveAccount_returnsNullWhenActiveAccountDeleted() = runTest {
        val account = repository.create("Test", "5Address", KeyType.SR25519)
        repository.setActiveAccount(account.id)
        repository.delete(account.id)

        val activeAccount = repository.getActiveAccount()

        assertNull(activeAccount)
    }

    @Test
    fun observeActiveAccountId_emitsUpdatesOnChanges() = runTest {
        val account1 = repository.create("First", "5Address1", KeyType.SR25519)
        val account2 = repository.create("Second", "5Address2", KeyType.ED25519)

        repository.observeActiveAccountId().test {
            // Initial null
            assertNull(awaitItem())

            // Set first account as active
            repository.setActiveAccount(account1.id)
            assertEquals(account1.id, awaitItem())

            // Change to second account
            repository.setActiveAccount(account2.id)
            assertEquals(account2.id, awaitItem())

            // Clear active account
            repository.setActiveAccount(null)
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActiveAccount_emitsFullAccountInfo() = runTest {
        val account = repository.create("Test Account", "5Address", KeyType.SR25519)

        repository.observeActiveAccount().test {
            // Initial null
            assertNull(awaitItem())

            // Set active account
            repository.setActiveAccount(account.id)
            val activeAccount = awaitItem()
            assertNotNull(activeAccount)
            assertEquals("Test Account", activeAccount.label)

            // Update the account label
            repository.updateLabel(account.id, "Updated Label")
            val updatedAccount = awaitItem()
            assertNotNull(updatedAccount)
            assertEquals("Updated Label", updatedAccount.label)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
