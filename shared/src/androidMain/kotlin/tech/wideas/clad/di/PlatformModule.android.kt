package tech.wideas.clad.di

import androidx.fragment.app.FragmentActivity
import app.cash.sqldelight.db.SqlDriver
import org.koin.dsl.module
import tech.wideas.clad.database.DriverFactory
import tech.wideas.clad.security.AndroidBiometricAuth
import tech.wideas.clad.security.AndroidKeyStorage
import tech.wideas.clad.security.AndroidSecureStorage
import tech.wideas.clad.security.BiometricAuth
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.SecureStorage

/**
 * Android-specific Koin module
 */
actual val platformModule = module {
    // SecureStorage using application context
    single<SecureStorage> {
        AndroidSecureStorage(get())
    }

    // BiometricAuth factory - requires FragmentActivity parameter
    // Usage: val biometricAuth = koinInject<BiometricAuth>(parameters = { parametersOf(activity) })
    factory<BiometricAuth> { params ->
        val activity = params.get<FragmentActivity>()
        AndroidBiometricAuth(activity)
    }

    // KeyStorage factory - requires activity provider for biometric prompt
    // Usage: val keyStorage = koinInject<KeyStorage>(parameters = { parametersOf({ activity }) })
    factory<KeyStorage> { params ->
        val activityProvider = params.get<() -> FragmentActivity>()
        AndroidKeyStorage(get(), activityProvider)
    }

    // SQLDelight driver for Android
    single<SqlDriver> {
        DriverFactory(get()).createDriver()
    }
}
