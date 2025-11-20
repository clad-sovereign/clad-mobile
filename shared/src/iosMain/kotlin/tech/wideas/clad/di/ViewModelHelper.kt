package tech.wideas.clad.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import tech.wideas.clad.ui.AccountsViewModelIOS
import tech.wideas.clad.ui.ConnectionViewModelIOS

/**
 * Helper class to provide ViewModels to Swift/iOS
 * Swift cannot directly use Koin's get() function, so we provide this wrapper
 */
class ViewModelHelper : KoinComponent {

    fun getConnectionViewModel(): ConnectionViewModelIOS {
        return ConnectionViewModelIOS(
            substrateClient = get(),
            settingsRepository = get()
        )
    }

    fun getAccountsViewModel(): AccountsViewModelIOS {
        return AccountsViewModelIOS(
            substrateClient = get()
        )
    }
}
