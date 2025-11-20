package tech.wideas.clad.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.substrate.SubstrateClient

/**
 * Common Koin module for shared dependencies
 */
val commonModule = module {
    // Provide a coroutine scope for SubstrateClient
    single {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    // SubstrateClient with default parameters
    single {
        SubstrateClient(
            scope = get(),
            autoReconnect = true
        )
    }

    // SettingsRepository depends on SecureStorage
    singleOf(::SettingsRepository)
}
