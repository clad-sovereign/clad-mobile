package tech.wideas.clad.di

import org.koin.core.context.startKoin

/**
 * Helper object to initialize Koin for iOS
 * This should be called once at app startup from Swift
 */
object KoinInitializer {
    fun initialize() {
        startKoin {
            modules(
                platformModule,
                commonModule
            )
        }
    }
}
