package tech.wideas.clad.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import tech.wideas.clad.ui.connection.ConnectionViewModel

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

    // Future ViewModels with assisted injection can be added like:
    // viewModel { (accountId: String) ->
    //     AccountDetailViewModel(
    //         accountId = accountId,
    //         repository = get()
    //     )
    // }
}
