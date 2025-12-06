package tech.wideas.clad.di

import co.touchlab.kermit.Logger
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.wideas.clad.debug.DebugAccountSeeder
import tech.wideas.clad.debug.DebugConfigFactory

/**
 * Helper object to initialize Koin for iOS
 * This should be called once at app startup from Swift
 */
object KoinInitializer {
    /**
     * Initialize Koin with the given debug mode flag.
     *
     * @param isDebug true for debug builds (#if DEBUG in Swift), false for release
     */
    fun initialize(isDebug: Boolean = false) {
        // Set debug mode before Koin initialization
        DebugConfigFactory.setDebugMode(isDebug)

        startKoin {
            modules(
                platformModule,
                commonModule
            )
        }
    }
}

/**
 * Helper object for iOS to trigger debug account seeding.
 *
 * Called from Swift after Koin initialization.
 * This is separate from KoinInitializer because seeding requires
 * async operations and potential UI (biometric prompt).
 */
object DebugSeederHelper : KoinComponent {
    private val seeder: DebugAccountSeeder by inject()

    /**
     * Trigger debug account seeding if needed.
     *
     * @param onComplete Callback with success status and optional error message
     */
    suspend fun seedIfNeeded(): DebugAccountSeeder.SeedResult {
        return try {
            val result = seeder.seedIfNeeded()
            when (result) {
                is DebugAccountSeeder.SeedResult.Success -> {
                    Logger.i("DebugSeederHelper") {
                        "Debug accounts seeded: Alice=${result.aliceAccount != null}, Bob=${result.bobAccount != null}"
                    }
                }
                is DebugAccountSeeder.SeedResult.Skipped -> {
                    Logger.d("DebugSeederHelper") { "Debug seeding skipped: accounts already exist" }
                }
                is DebugAccountSeeder.SeedResult.NotDebugBuild -> {
                    // Expected in release builds - no logging needed
                }
                is DebugAccountSeeder.SeedResult.BiometricCancelled -> {
                    Logger.w("DebugSeederHelper") { "Debug seeding cancelled by user" }
                }
                is DebugAccountSeeder.SeedResult.Error -> {
                    Logger.e("DebugSeederHelper") { "Debug seeding failed: ${result.message}" }
                }
            }
            result
        } catch (e: Exception) {
            Logger.e("DebugSeederHelper", e) { "Debug seeding failed with exception" }
            DebugAccountSeeder.SeedResult.Error(e.message ?: "Unknown error")
        }
    }
}
