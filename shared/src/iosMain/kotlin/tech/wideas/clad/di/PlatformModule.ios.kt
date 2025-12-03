package tech.wideas.clad.di

import app.cash.sqldelight.db.SqlDriver
import org.koin.dsl.module
import tech.wideas.clad.crypto.MnemonicProvider
import tech.wideas.clad.crypto.createMnemonicProvider
import tech.wideas.clad.database.DriverFactory
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.IOSBiometricAuth
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.KeyStorageFactory
import tech.wideas.clad.security.SecureStorage
import tech.wideas.clad.security.SecureStorageFactory

/**
 * iOS-specific Koin module
 */
actual val platformModule = module {
    // SecureStorage (uses Swift KeychainHelper via factory)
    single<SecureStorage> {
        SecureStorageFactory.create()
    }

    // BiometricAuth
    single<BiometricAuth> {
        IOSBiometricAuth()
    }

    // KeyStorage with biometric protection (uses Swift BiometricKeychainHelper via factory)
    single<KeyStorage> {
        KeyStorageFactory.create()
    }

    // MnemonicProvider for BIP39 operations
    single<MnemonicProvider> {
        createMnemonicProvider()
    }

    // SQLDelight driver for iOS
    single<SqlDriver> {
        DriverFactory().createDriver()
    }
}
