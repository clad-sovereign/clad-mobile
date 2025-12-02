package tech.wideas.clad.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.wideas.clad.data.AccountRepository
import tech.wideas.clad.data.SettingsRepository
import tech.wideas.clad.database.CladDatabase
import tech.wideas.clad.substrate.SubstrateClient

/**
 * Common Koin module for shared dependencies
 */
val commonModule = module {
    // SubstrateClient with configurable autoReconnect
    // The scope will be set by the ViewModel to tie lifecycle properly
    single {
        SubstrateClient(
            autoReconnect = getProperty("substrate.autoReconnect", "true").toBoolean()
        )
    }

    // SettingsRepository depends on SecureStorage
    singleOf(::SettingsRepository)

    // CladDatabase - created from platform-specific DriverFactory
    single {
        CladDatabase(get())
    }

    // AccountRepository for account metadata persistence
    singleOf(::AccountRepository)
}
