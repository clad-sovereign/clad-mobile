package tech.wideas.clad.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.wideas.clad.database.Account
import tech.wideas.clad.database.CladDatabase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val ACTIVE_ACCOUNT_KEY = "active_account_id"

/**
 * Account mode determining which network/environment the account operates on.
 *
 * See docs/live-demo-account-modes.md for design rationale.
 */
enum class AccountMode {
    /**
     * Production mode - real transactions on mainnet.
     * Uses master key (no derivation path).
     * RPC endpoint: wss://rpc.clad.so
     */
    LIVE,

    /**
     * Demo/training mode - safe experimentation on testnet.
     * Uses derivation path "//demo".
     * RPC endpoint: wss://testnet.clad.so
     */
    DEMO;

    companion object {
        /**
         * Parse mode from database string value.
         * Defaults to DEMO for unknown values for safety - better to accidentally
         * be on testnet than to accidentally perform real transactions on mainnet.
         */
        fun fromDbValue(value: String): AccountMode {
            return entries.find { it.name == value } ?: DEMO
        }
    }
}

/**
 * Domain model representing a Substrate/Polkadot account.
 *
 * This contains only non-sensitive metadata. The actual keypair
 * is stored separately in [KeyStorage] with biometric protection.
 * All accounts use SR25519 keys (see issue #60 for rationale).
 *
 * @property id Unique identifier (UUID v4)
 * @property label User-defined display name
 * @property address SS58-encoded public address
 * @property createdAt Timestamp when account was created (epoch milliseconds)
 * @property lastUsedAt Timestamp when account was last used for signing (nullable)
 * @property mode Account mode (LIVE or DEMO) - determines network/RPC endpoint
 * @property derivationPath Derivation path used to derive keypair from mnemonic (null = master key)
 */
data class AccountInfo(
    val id: String,
    val label: String,
    val address: String,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val mode: AccountMode = AccountMode.LIVE,
    val derivationPath: String? = null
)

/**
 * Repository for managing account metadata persistence.
 *
 * This repository handles non-sensitive account data stored in SQLite.
 * For keypair storage with biometric protection, use [KeyStorage].
 *
 * Thread Safety: All operations are dispatched to IO dispatcher and are thread-safe.
 *
 * @property database The SQLDelight database instance
 */
class AccountRepository(private val database: CladDatabase) {

    private val queries get() = database.accountQueries
    private val settingsQueries get() = database.appSettingsQueries

    /**
     * Observe all accounts as a Flow.
     *
     * Emits a new list whenever the accounts table changes.
     * Accounts are ordered by creation time (newest first).
     *
     * @return Flow of account list
     */
    fun observeAll(): Flow<List<AccountInfo>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .mapToAccountInfoList()
    }

    /**
     * Get all accounts.
     *
     * @return List of all accounts, ordered by creation time (newest first)
     */
    suspend fun getAll(): List<AccountInfo> = withContext(Dispatchers.IO) {
        queries.selectAll().executeAsList().map { it.toAccountInfo() }
    }

    /**
     * Get account by ID.
     *
     * @param id Account UUID
     * @return Account if found, null otherwise
     */
    suspend fun getById(id: String): AccountInfo? = withContext(Dispatchers.IO) {
        queries.selectById(id).executeAsOneOrNull()?.toAccountInfo()
    }

    /**
     * Get account by SS58 address.
     *
     * @param address SS58-encoded address
     * @return Account if found, null otherwise
     */
    suspend fun getByAddress(address: String): AccountInfo? = withContext(Dispatchers.IO) {
        queries.selectByAddress(address).executeAsOneOrNull()?.toAccountInfo()
    }

    /**
     * Observe account by ID as a Flow.
     *
     * @param id Account UUID
     * @return Flow emitting the account (or null if not found/deleted)
     */
    fun observeById(id: String): Flow<AccountInfo?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapToAccountInfoOrNull()
    }

    /**
     * Create a new account.
     *
     * @param label User-defined display name
     * @param address SS58-encoded public address
     * @param mode Account mode (LIVE or DEMO), defaults to LIVE
     * @param derivationPath Derivation path used to derive keypair, null for master key
     * @return The created account with generated ID
     * @throws IllegalStateException if address already exists
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(
        label: String,
        address: String,
        mode: AccountMode = AccountMode.LIVE,
        derivationPath: String? = null
    ): AccountInfo = withContext(Dispatchers.IO) {
        // Check for duplicate address before insert
        val existing = queries.selectByAddress(address).executeAsOneOrNull()
        if (existing != null) {
            throw IllegalStateException("Account with address '$address' already exists")
        }

        val id = Uuid.random().toString()
        val createdAt = currentTimeMillis()

        queries.insert(
            id = id,
            label = label,
            address = address,
            createdAt = createdAt,
            lastUsedAt = null,
            mode = mode.name,
            derivationPath = derivationPath
        )

        AccountInfo(
            id = id,
            label = label,
            address = address,
            createdAt = createdAt,
            lastUsedAt = null,
            mode = mode,
            derivationPath = derivationPath
        )
    }

    /**
     * Update account label.
     *
     * @param id Account UUID
     * @param label New display name
     */
    suspend fun updateLabel(id: String, label: String) = withContext(Dispatchers.IO) {
        queries.updateLabel(label = label, id = id)
    }

    /**
     * Update last used timestamp to current time.
     *
     * Call this after a successful signing operation.
     *
     * @param id Account UUID
     */
    suspend fun markAsUsed(id: String) = withContext(Dispatchers.IO) {
        queries.updateLastUsedAt(lastUsedAt = currentTimeMillis(), id = id)
    }

    /**
     * Delete account by ID.
     *
     * Note: This only removes the metadata. The keypair in KeyStorage
     * should be deleted separately.
     *
     * @param id Account UUID
     */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        queries.deleteById(id)
    }

    /**
     * Get total number of accounts.
     *
     * @return Account count
     */
    suspend fun count(): Long = withContext(Dispatchers.IO) {
        queries.countAll().executeAsOne()
    }

    // ==================== Active Account Management ====================

    /**
     * Get the currently active account ID.
     *
     * @return Active account ID, or null if none is set
     */
    suspend fun getActiveAccountId(): String? = withContext(Dispatchers.IO) {
        settingsQueries.selectByKey(ACTIVE_ACCOUNT_KEY).executeAsOneOrNull()
    }

    /**
     * Observe the currently active account ID as a Flow.
     *
     * @return Flow emitting the active account ID (or null)
     */
    fun observeActiveAccountId(): Flow<String?> {
        return settingsQueries.selectByKey(ACTIVE_ACCOUNT_KEY)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    /**
     * Set the active account.
     *
     * @param accountId The account ID to set as active, or null to clear
     */
    suspend fun setActiveAccount(accountId: String?) = withContext(Dispatchers.IO) {
        if (accountId != null) {
            settingsQueries.upsert(ACTIVE_ACCOUNT_KEY, accountId)
        } else {
            settingsQueries.deleteByKey(ACTIVE_ACCOUNT_KEY)
        }
    }

    /**
     * Observe the currently active account as a Flow.
     *
     * Emits the full AccountInfo when active, null otherwise.
     * Automatically clears the active account if it no longer exists.
     *
     * @return Flow emitting the active account (or null)
     */
    fun observeActiveAccount(): Flow<AccountInfo?> {
        return combine(
            observeActiveAccountId(),
            observeAll()
        ) { activeId, accounts ->
            if (activeId == null) {
                null
            } else {
                accounts.find { it.id == activeId }
            }
        }
    }

    /**
     * Get the currently active account.
     *
     * @return Active account, or null if none is set or it doesn't exist
     */
    suspend fun getActiveAccount(): AccountInfo? = withContext(Dispatchers.IO) {
        val activeId = getActiveAccountId() ?: return@withContext null
        getById(activeId)
    }
}

/**
 * Convert SQLDelight generated Account to domain AccountInfo.
 */
private fun Account.toAccountInfo(): AccountInfo {
    return AccountInfo(
        id = id,
        label = label,
        address = address,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        mode = AccountMode.fromDbValue(mode),
        derivationPath = derivationPath
    )
}

/**
 * Map Flow of Account list to Flow of AccountInfo list.
 */
private fun Flow<List<Account>>.mapToAccountInfoList(): Flow<List<AccountInfo>> {
    return map { accounts ->
        accounts.map { it.toAccountInfo() }
    }
}

/**
 * Map Flow of Account? to Flow of AccountInfo?.
 */
private fun Flow<Account?>.mapToAccountInfoOrNull(): Flow<AccountInfo?> {
    return map { account ->
        account?.toAccountInfo()
    }
}

/**
 * Get current time in milliseconds.
 * Extracted for testability.
 */
internal expect fun currentTimeMillis(): Long
