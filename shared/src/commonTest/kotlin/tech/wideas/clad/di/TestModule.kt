package tech.wideas.clad.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.BiometricResult
import tech.wideas.clad.security.SecureStorage
import tech.wideas.clad.substrate.SubstrateClient

/**
 * Fake SecureStorage for testing
 */
class FakeSecureStorage : SecureStorage {
    private val storage = mutableMapOf<String, String>()

    override suspend fun save(key: String, value: String) {
        storage[key] = value
    }

    override suspend fun get(key: String): String? {
        return storage[key]
    }

    override suspend fun delete(key: String) {
        storage.remove(key)
    }

    override suspend fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }

    override suspend fun clear() {
        storage.clear()
    }

    // Test helper
    fun getAll(): Map<String, String> = storage.toMap()
}

/**
 * Fake BiometricAuth for testing
 */
class FakeBiometricAuth(
    private var isAvailableResult: Boolean = true,
    private var authenticateResult: BiometricResult = BiometricResult.Success
) : BiometricAuth {
    var authenticateCalls = 0
        private set

    override suspend fun isAvailable(): Boolean = isAvailableResult

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        description: String
    ): BiometricResult {
        authenticateCalls++
        return authenticateResult
    }

    // Test helpers
    fun setAvailable(available: Boolean) {
        isAvailableResult = available
    }

    fun setAuthenticateResult(result: BiometricResult) {
        authenticateResult = result
    }
}

/**
 * Koin test module with fake implementations
 *
 * Usage in tests:
 * ```
 * @BeforeTest
 * fun setup() {
 *     startKoin {
 *         modules(testModule)
 *     }
 * }
 *
 * @AfterTest
 * fun tearDown() {
 *     stopKoin()
 * }
 * ```
 */
val testModule = module {
    // Fake implementations for security components
    single<SecureStorage> { FakeSecureStorage() }
    single<BiometricAuth> { FakeBiometricAuth() }

    // Use real SubstrateClient with autoReconnect disabled for tests
    single {
        SubstrateClient(autoReconnect = false)
    }

    // Real implementations that depend on fakes
    singleOf(::SettingsRepository)
}
