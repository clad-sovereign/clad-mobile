package tech.wideas.clad.di

import androidx.fragment.app.FragmentActivity
import org.koin.dsl.module
import tech.wideas.clad.security.AndroidBiometricAuth
import tech.wideas.clad.security.AndroidSecureStorage
import tech.wideas.clad.security.BiometricAuth
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
}
