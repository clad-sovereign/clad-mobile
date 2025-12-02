package tech.wideas.clad.di

import org.koin.dsl.module
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.IOSBiometricAuth
import tech.wideas.clad.security.IOSKeyStorage
import tech.wideas.clad.security.IOSSecureStorage
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.SecureStorage

/**
 * iOS-specific Koin module
 */
actual val platformModule = module {
    // SecureStorage
    single<SecureStorage> {
        IOSSecureStorage()
    }

    // BiometricAuth
    single<BiometricAuth> {
        IOSBiometricAuth()
    }

    // KeyStorage with biometric protection
    single<KeyStorage> {
        IOSKeyStorage()
    }
}
