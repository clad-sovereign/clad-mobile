package tech.wideas.clad.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.SecureStorage
import tech.wideas.clad.security.createBiometricAuth
import tech.wideas.clad.security.createSecureStorage

/**
 * Helper object to initialize Koin for iOS
 * This should be called once at app startup from Swift
 */
object KoinInitializer {
    fun initialize() {
        startKoin {
            modules(
                platformModule,
                iosSecurityModule,
                commonModule
            )
        }
    }
}

/**
 * iOS security module - provides BiometricAuth and SecureStorage
 */
private val iosSecurityModule = module {
    single<BiometricAuth> { createBiometricAuth() }
    single<SecureStorage> { createSecureStorage() }
}
