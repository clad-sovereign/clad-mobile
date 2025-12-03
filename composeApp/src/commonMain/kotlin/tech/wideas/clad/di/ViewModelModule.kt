package tech.wideas.clad.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import tech.wideas.clad.ui.accounts.AccountsViewModel
import tech.wideas.clad.ui.connection.ConnectionViewModel
import tech.wideas.clad.ui.import.ImportViewModel

/**
 * Koin module for ViewModels
 *
 * Using explicit factory pattern for better readability and
 * to support future ViewModels with assisted injection.
 */
val viewModelModule = module {
    viewModel {
        ConnectionViewModel(
            substrateClient = get(),
            settingsRepository = get()
        )
    }

    viewModel {
        AccountsViewModel(
            substrateClient = get(),
            accountRepository = get(),
            keyStorage = get()
        )
    }

    viewModel {
        ImportViewModel(
            mnemonicProvider = get(),
            keyStorage = get(),
            accountRepository = get()
        )
    }
}
