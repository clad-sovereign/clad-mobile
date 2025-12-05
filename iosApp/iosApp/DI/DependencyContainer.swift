import Foundation
import Shared

/// Container for dependency injection.
/// Provides production implementations by default, but can be overridden for testing.
@MainActor
final class DependencyContainer {

    /// Shared instance for production use.
    static let shared = DependencyContainer()

    private let viewModelHelper: ViewModelHelper

    /// The account repository instance.
    lazy var accountRepository: any AccountRepositoryProtocol = {
        AccountRepositoryWrapper(repository: viewModelHelper.getAccountRepository())
    }()

    /// The key storage instance.
    lazy var keyStorage: any KeyStorageProtocol = {
        KeyStorageWrapper(storage: viewModelHelper.getKeyStorage())
    }()

    /// The mnemonic provider instance.
    lazy var mnemonicProvider: any MnemonicProviderProtocol = {
        MnemonicProviderWrapper(provider: Mnemonic_iosKt.createMnemonicProvider())
    }()

    private init() {
        self.viewModelHelper = ViewModelHelper()
    }

    /// Creates a container with custom dependencies for testing.
    init(
        accountRepository: (any AccountRepositoryProtocol)?,
        keyStorage: (any KeyStorageProtocol)?,
        mnemonicProvider: (any MnemonicProviderProtocol)?
    ) {
        self.viewModelHelper = ViewModelHelper()

        if let repo = accountRepository {
            self.accountRepository = repo
        }
        if let storage = keyStorage {
            self.keyStorage = storage
        }
        if let provider = mnemonicProvider {
            self.mnemonicProvider = provider
        }
    }
}
