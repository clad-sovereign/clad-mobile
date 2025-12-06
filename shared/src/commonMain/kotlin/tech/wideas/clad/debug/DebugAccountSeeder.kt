package tech.wideas.clad.debug

import co.touchlab.kermit.Logger
import tech.wideas.clad.crypto.Keypair
import tech.wideas.clad.crypto.MnemonicProvider
import tech.wideas.clad.data.AccountInfo
import tech.wideas.clad.data.AccountMode
import tech.wideas.clad.data.AccountRepository
import tech.wideas.clad.security.BiometricPromptConfig
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.KeyStorageResult

/**
 * Seeder for debug-only test accounts.
 *
 * This class seeds well-known Substrate dev accounts (Alice, Bob) on first launch
 * in debug builds only. These accounts are publicly documented and contain no real funds.
 *
 * IMPORTANT: This code should ONLY run in debug builds. Release builds must not
 * contain any seeding logic.
 *
 * Test accounts seeded:
 * - Alice: Full keypair using dev mnemonic + //Alice derivation path
 *   Address: 5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY
 *
 * - Bob: Watch-only account (no keypair)
 *   Address: 5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty
 *
 * @see <a href="https://docs.substrate.io/reference/command-line-tools/subkey/#well-known-keys">Substrate Well-Known Keys</a>
 */
class DebugAccountSeeder(
    private val accountRepository: AccountRepository,
    private val keyStorage: KeyStorage,
    private val mnemonicProvider: MnemonicProvider
) {
    companion object {
        private const val TAG = "DebugAccountSeeder"

        /**
         * Well-known Substrate dev mnemonic.
         *
         * This is publicly documented and used across the Substrate ecosystem for testing.
         * It should NEVER be used for real funds.
         *
         * @see <a href="https://docs.substrate.io/reference/command-line-tools/subkey/#well-known-keys">Substrate Docs</a>
         */
        const val DEV_MNEMONIC = "bottom drive obey lake curtain smoke basket hold race lonely fit walk"

        /**
         * Alice's derivation path from the dev mnemonic.
         */
        const val ALICE_DERIVATION_PATH = "//Alice"

        /**
         * Alice's expected SS58 address (generic Substrate prefix 42).
         *
         * This is the deterministic result of:
         * DEV_MNEMONIC + ALICE_DERIVATION_PATH -> SR25519 keypair -> SS58 encode
         */
        const val ALICE_ADDRESS = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        /**
         * Bob's SS58 address for watch-only account.
         *
         * This is the result of DEV_MNEMONIC + //Bob -> SR25519 keypair -> SS58 encode.
         * We only store the address (no keypair) as a watch-only account.
         */
        const val BOB_ADDRESS = "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty"
    }

    /**
     * Result of the seeding operation.
     */
    sealed class SeedResult {
        /** Seeding completed successfully. */
        data class Success(
            val aliceAccount: AccountInfo?,
            val bobAccount: AccountInfo?
        ) : SeedResult()

        /** Seeding skipped because database already has accounts. */
        data object Skipped : SeedResult()

        /** Seeding skipped because app is not in debug mode. */
        data object NotDebugBuild : SeedResult()

        /** Seeding failed with biometric cancellation. */
        data object BiometricCancelled : SeedResult()

        /** Seeding failed with an error. */
        data class Error(val message: String) : SeedResult()
    }

    /**
     * Seed test accounts if conditions are met.
     *
     * Conditions:
     * 1. App is running in debug mode
     * 2. Database is empty (no existing accounts)
     *
     * This will trigger a biometric prompt to save Alice's keypair.
     *
     * @return SeedResult indicating outcome
     */
    suspend fun seedIfNeeded(): SeedResult {
        // Guard: Only seed in debug builds
        if (!DebugConfig.isDebug) {
            Logger.d(TAG) { "Skipping seed: not a debug build" }
            return SeedResult.NotDebugBuild
        }

        // Guard: Only seed if database is empty
        val accountCount = accountRepository.count()
        if (accountCount > 0) {
            Logger.d(TAG) { "Skipping seed: database already has $accountCount accounts" }
            return SeedResult.Skipped
        }

        Logger.i(TAG) { "Starting debug account seeding..." }

        return try {
            val aliceAccount = seedAlice()
            val bobAccount = seedBob()

            // Set Alice as the active account if she was created
            if (aliceAccount != null) {
                accountRepository.setActiveAccount(aliceAccount.id)
                Logger.i(TAG) { "Set Alice as active account" }
            }

            Logger.i(TAG) { "Debug account seeding completed successfully" }
            SeedResult.Success(aliceAccount, bobAccount)
        } catch (e: BiometricCancelledException) {
            Logger.w(TAG) { "Seeding cancelled by user" }
            SeedResult.BiometricCancelled
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Debug account seeding failed" }
            SeedResult.Error(e.message ?: "Unknown error during seeding")
        }
    }

    /**
     * Seed Alice account with full keypair.
     *
     * Derives keypair from dev mnemonic + //Alice derivation path.
     * Stores keypair with biometric protection.
     */
    private suspend fun seedAlice(): AccountInfo? {
        Logger.d(TAG) { "Seeding Alice account..." }

        // Check if Alice already exists (by address)
        val existingAlice = accountRepository.getByAddress(ALICE_ADDRESS)
        if (existingAlice != null) {
            Logger.d(TAG) { "Alice account already exists, skipping" }
            return existingAlice
        }

        // Derive Alice's keypair
        val keypair = mnemonicProvider.toKeypair(
            mnemonic = DEV_MNEMONIC,
            passphrase = "",
            derivationPath = ALICE_DERIVATION_PATH
        )

        try {
            // Verify address matches expected
            val derivedAddress = keypair.toSs58Address()
            if (derivedAddress != ALICE_ADDRESS) {
                throw IllegalStateException(
                    "Alice address mismatch! Expected: $ALICE_ADDRESS, Got: $derivedAddress"
                )
            }
            Logger.d(TAG) { "Alice address verified: $derivedAddress" }

            // Create account in database
            val account = accountRepository.create(
                label = "Alice (Dev)",
                address = ALICE_ADDRESS,
                mode = AccountMode.DEMO,
                derivationPath = ALICE_DERIVATION_PATH
            )

            // Save keypair with biometric protection
            val promptConfig = BiometricPromptConfig(
                title = "Protect Dev Account",
                subtitle = "Secure Alice's keypair",
                promptDescription = "Use biometrics to protect the development account keys"
            )

            when (val result = keyStorage.saveKeypair(account.id, keypair, promptConfig)) {
                is KeyStorageResult.Success -> {
                    Logger.i(TAG) { "Alice account seeded successfully: ${account.id}" }
                    return account
                }
                is KeyStorageResult.BiometricCancelled -> {
                    // Clean up the account since keypair wasn't saved
                    accountRepository.delete(account.id)
                    throw BiometricCancelledException()
                }
                is KeyStorageResult.BiometricError -> {
                    accountRepository.delete(account.id)
                    throw Exception("Biometric error: ${result.message}")
                }
                is KeyStorageResult.BiometricNotAvailable -> {
                    accountRepository.delete(account.id)
                    throw Exception("Biometrics not available")
                }
                is KeyStorageResult.StorageError -> {
                    accountRepository.delete(account.id)
                    throw Exception("Storage error: ${result.message}")
                }
                is KeyStorageResult.KeyNotFound -> {
                    accountRepository.delete(account.id)
                    throw Exception("Key not found after save")
                }
            }
        } finally {
            keypair.clear()
        }
    }

    /**
     * Seed Bob as a watch-only account.
     *
     * No keypair is stored - this is address-only for testing watch-only functionality.
     */
    private suspend fun seedBob(): AccountInfo? {
        Logger.d(TAG) { "Seeding Bob account (watch-only)..." }

        // Check if Bob already exists
        val existingBob = accountRepository.getByAddress(BOB_ADDRESS)
        if (existingBob != null) {
            Logger.d(TAG) { "Bob account already exists, skipping" }
            return existingBob
        }

        // Create watch-only account (no keypair)
        val account = accountRepository.create(
            label = "Bob (Watch-only)",
            address = BOB_ADDRESS,
            mode = AccountMode.DEMO,
            derivationPath = null  // Watch-only, no derivation
        )

        Logger.i(TAG) { "Bob account seeded successfully: ${account.id}" }
        return account
    }
}

/**
 * Exception thrown when biometric authentication is cancelled during seeding.
 */
internal class BiometricCancelledException : Exception("Biometric authentication cancelled")
