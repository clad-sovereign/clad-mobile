package tech.wideas.clad.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.wideas.clad.crypto.KeyType
import tech.wideas.clad.crypto.Keypair
import tech.wideas.clad.crypto.MnemonicProvider
import tech.wideas.clad.crypto.MnemonicValidationResult
import tech.wideas.clad.crypto.Ss58
import tech.wideas.clad.data.AccountInfo
import tech.wideas.clad.data.AccountRepository
import tech.wideas.clad.security.BiometricPromptConfig
import tech.wideas.clad.security.KeyStorage
import tech.wideas.clad.security.KeyStorageResult

/**
 * Import method selected by the user.
 */
enum class ImportMethod {
    SEED_PHRASE,
    QR_CODE,
    MANUAL_ADDRESS
}

/**
 * Represents the current state of the import flow.
 */
sealed class ImportFlowState {
    /** Initial state - user selecting import method */
    data object SelectMethod : ImportFlowState()

    /** Entering seed phrase words */
    data object EnteringSeedPhrase : ImportFlowState()

    /** Scanning QR code */
    data object ScanningQrCode : ImportFlowState()

    /** Entering address manually */
    data object EnteringAddress : ImportFlowState()

    /** Validating input (mnemonic or address) */
    data object Validating : ImportFlowState()

    /** Showing derived/entered account for confirmation */
    data class Confirming(
        val address: String,
        val keypair: Keypair? = null // null for watch-only imports
    ) : ImportFlowState()

    /** Saving account with biometric protection */
    data object Saving : ImportFlowState()

    /** Import completed successfully */
    data class Success(val account: AccountInfo) : ImportFlowState()

    /** Error occurred during import */
    data class Error(val message: String) : ImportFlowState()
}

/**
 * UI state for the import screen.
 */
data class ImportUiState(
    val flowState: ImportFlowState = ImportFlowState.SelectMethod,
    val selectedMethod: ImportMethod? = null,

    // Seed phrase input
    val seedPhraseWords: List<String> = List(12) { "" },
    val wordCount: Int = 12, // 12 or 24
    val mnemonicError: String? = null,

    // QR code scanning
    val qrCodeError: String? = null,

    // Manual address input
    val manualAddress: String = "",
    val addressError: String? = null,

    // Labeling
    val accountLabel: String = "",

    // Key type selection
    val keyType: KeyType = KeyType.SR25519,

    // General error
    val error: String? = null
) {
    val isLoading: Boolean
        get() = flowState is ImportFlowState.Validating || flowState is ImportFlowState.Saving

    val canProceed: Boolean
        get() = when (flowState) {
            is ImportFlowState.EnteringSeedPhrase -> {
                seedPhraseWords.take(wordCount).all { it.isNotBlank() } && mnemonicError == null
            }
            is ImportFlowState.EnteringAddress -> {
                manualAddress.isNotBlank() && addressError == null
            }
            is ImportFlowState.Confirming -> {
                accountLabel.isNotBlank()
            }
            else -> false
        }
}

/**
 * ViewModel for account import flows.
 *
 * Handles three import methods:
 * 1. Seed phrase (12/24 words) - derives keypair with biometric storage
 * 2. QR code scan - parses SS58 address or substrate URI
 * 3. Manual address entry - watch-only account (no keypair)
 */
class ImportViewModel(
    private val mnemonicProvider: MnemonicProvider,
    private val keyStorage: KeyStorage,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    // Temporary keypair holder - cleared after save or cancel
    private var derivedKeypair: Keypair? = null

    // region Method Selection

    fun selectMethod(method: ImportMethod) {
        val newFlowState = when (method) {
            ImportMethod.SEED_PHRASE -> ImportFlowState.EnteringSeedPhrase
            ImportMethod.QR_CODE -> ImportFlowState.ScanningQrCode
            ImportMethod.MANUAL_ADDRESS -> ImportFlowState.EnteringAddress
        }

        _uiState.value = _uiState.value.copy(
            flowState = newFlowState,
            selectedMethod = method,
            error = null
        )
    }

    fun goBack() {
        clearDerivedKeypair()

        val currentState = _uiState.value.flowState
        val newFlowState = when (currentState) {
            is ImportFlowState.EnteringSeedPhrase,
            is ImportFlowState.ScanningQrCode,
            is ImportFlowState.EnteringAddress -> ImportFlowState.SelectMethod

            is ImportFlowState.Confirming -> when (_uiState.value.selectedMethod) {
                ImportMethod.SEED_PHRASE -> ImportFlowState.EnteringSeedPhrase
                ImportMethod.QR_CODE -> ImportFlowState.ScanningQrCode
                ImportMethod.MANUAL_ADDRESS -> ImportFlowState.EnteringAddress
                null -> ImportFlowState.SelectMethod
            }

            is ImportFlowState.Error -> when (_uiState.value.selectedMethod) {
                ImportMethod.SEED_PHRASE -> ImportFlowState.EnteringSeedPhrase
                ImportMethod.QR_CODE -> ImportFlowState.ScanningQrCode
                ImportMethod.MANUAL_ADDRESS -> ImportFlowState.EnteringAddress
                null -> ImportFlowState.SelectMethod
            }

            else -> ImportFlowState.SelectMethod
        }

        _uiState.value = _uiState.value.copy(
            flowState = newFlowState,
            error = null,
            mnemonicError = null,
            addressError = null,
            qrCodeError = null
        )
    }

    fun reset() {
        clearDerivedKeypair()
        _uiState.value = ImportUiState()
    }

    // endregion

    // region Seed Phrase Import

    fun setWordCount(count: Int) {
        require(count == 12 || count == 24) { "Word count must be 12 or 24" }

        val currentWords = _uiState.value.seedPhraseWords
        val newWords = if (count > currentWords.size) {
            currentWords + List(count - currentWords.size) { "" }
        } else {
            currentWords.take(count)
        }

        _uiState.value = _uiState.value.copy(
            wordCount = count,
            seedPhraseWords = newWords,
            mnemonicError = null
        )
    }

    fun updateWord(index: Int, word: String) {
        val words = _uiState.value.seedPhraseWords.toMutableList()
        if (index in words.indices) {
            words[index] = word.lowercase().trim()
            _uiState.value = _uiState.value.copy(
                seedPhraseWords = words,
                mnemonicError = null
            )
        }
    }

    fun validateAndDeriveSeedPhrase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                flowState = ImportFlowState.Validating,
                mnemonicError = null
            )

            val mnemonic = _uiState.value.seedPhraseWords
                .take(_uiState.value.wordCount)
                .joinToString(" ")

            when (val result = mnemonicProvider.validate(mnemonic)) {
                is MnemonicValidationResult.Valid -> {
                    try {
                        val keypair = mnemonicProvider.toKeypair(
                            mnemonic = mnemonic,
                            keyType = _uiState.value.keyType
                        )

                        derivedKeypair = keypair
                        val address = keypair.toSs58Address()

                        // Check if account already exists
                        val existing = accountRepository.getByAddress(address)
                        if (existing != null) {
                            keypair.clear()
                            derivedKeypair = null
                            _uiState.value = _uiState.value.copy(
                                flowState = ImportFlowState.EnteringSeedPhrase,
                                mnemonicError = "An account with this address already exists"
                            )
                            return@launch
                        }

                        _uiState.value = _uiState.value.copy(
                            flowState = ImportFlowState.Confirming(
                                address = address,
                                keypair = keypair
                            )
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            flowState = ImportFlowState.EnteringSeedPhrase,
                            mnemonicError = "Failed to derive keypair: ${e.message}"
                        )
                    }
                }
                is MnemonicValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        flowState = ImportFlowState.EnteringSeedPhrase,
                        mnemonicError = result.reason
                    )
                }
            }
        }
    }

    // endregion

    // region QR Code Import

    fun onQrCodeScanned(content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                flowState = ImportFlowState.Validating,
                qrCodeError = null
            )

            val address = parseQrContent(content)

            if (address == null) {
                _uiState.value = _uiState.value.copy(
                    flowState = ImportFlowState.ScanningQrCode,
                    qrCodeError = "Invalid QR code. Expected SS58 address."
                )
                return@launch
            }

            // Check if account already exists
            val existing = accountRepository.getByAddress(address)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    flowState = ImportFlowState.ScanningQrCode,
                    qrCodeError = "An account with this address already exists"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                flowState = ImportFlowState.Confirming(
                    address = address,
                    keypair = null // Watch-only import from QR
                )
            )
        }
    }

    /**
     * Parse QR code content for SS58 address.
     * Supports:
     * - Direct SS58 address
     * - substrate: URI scheme (substrate:5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY)
     */
    private fun parseQrContent(content: String): String? {
        val trimmed = content.trim()

        // Try substrate: URI scheme
        if (trimmed.startsWith("substrate:", ignoreCase = true)) {
            val address = trimmed.substringAfter(":", "").substringBefore("?")
            if (Ss58.isValid(address)) {
                return address
            }
        }

        // Try direct SS58 address
        if (Ss58.isValid(trimmed)) {
            return trimmed
        }

        return null
    }

    // endregion

    // region Manual Address Import

    fun updateManualAddress(address: String) {
        _uiState.value = _uiState.value.copy(
            manualAddress = address,
            addressError = null
        )
    }

    fun validateManualAddress() {
        viewModelScope.launch {
            val address = _uiState.value.manualAddress.trim()

            _uiState.value = _uiState.value.copy(
                flowState = ImportFlowState.Validating,
                addressError = null
            )

            if (address.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    flowState = ImportFlowState.EnteringAddress,
                    addressError = "Please enter an address"
                )
                return@launch
            }

            if (!Ss58.isValid(address)) {
                _uiState.value = _uiState.value.copy(
                    flowState = ImportFlowState.EnteringAddress,
                    addressError = "Invalid SS58 address format"
                )
                return@launch
            }

            // Check if account already exists
            val existing = accountRepository.getByAddress(address)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    flowState = ImportFlowState.EnteringAddress,
                    addressError = "An account with this address already exists"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                flowState = ImportFlowState.Confirming(
                    address = address,
                    keypair = null // Watch-only import
                )
            )
        }
    }

    // endregion

    // region Confirmation & Saving

    fun updateAccountLabel(label: String) {
        _uiState.value = _uiState.value.copy(accountLabel = label)
    }

    fun updateKeyType(keyType: KeyType) {
        _uiState.value = _uiState.value.copy(keyType = keyType)
    }

    /**
     * Save the imported account.
     * For seed phrase imports, requires biometric authentication.
     * For watch-only imports (QR/manual), no biometric needed.
     */
    suspend fun saveAccount(): ImportFlowState {
        val state = _uiState.value
        val confirmingState = state.flowState as? ImportFlowState.Confirming
            ?: return ImportFlowState.Error("Invalid state for saving")

        val label = state.accountLabel.trim()
        if (label.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter an account label"
            )
            return state.flowState
        }

        _uiState.value = _uiState.value.copy(
            flowState = ImportFlowState.Saving,
            error = null
        )

        return try {
            val keypair = confirmingState.keypair

            // Create account in database
            val account = accountRepository.create(
                label = label,
                address = confirmingState.address,
                keyType = if (keypair != null) keypair.keyType else state.keyType
            )

            // If we have a keypair, save it with biometric protection
            if (keypair != null) {
                val promptConfig = BiometricPromptConfig(
                    title = "Protect Account",
                    subtitle = "Biometric authentication",
                    promptDescription = "Use biometric to secure your account keys"
                )

                when (val result = keyStorage.saveKeypair(account.id, keypair, promptConfig)) {
                    is KeyStorageResult.Success -> {
                        // Clear the keypair after successful save
                        keypair.clear()
                        derivedKeypair = null

                        val successState = ImportFlowState.Success(account)
                        _uiState.value = _uiState.value.copy(flowState = successState)
                        successState
                    }
                    is KeyStorageResult.BiometricCancelled -> {
                        // User cancelled - delete the account we just created
                        accountRepository.delete(account.id)

                        _uiState.value = _uiState.value.copy(
                            flowState = ImportFlowState.Confirming(
                                address = confirmingState.address,
                                keypair = keypair
                            ),
                            error = null
                        )
                        _uiState.value.flowState
                    }
                    is KeyStorageResult.BiometricNotAvailable -> {
                        accountRepository.delete(account.id)
                        val errorState = ImportFlowState.Error("Biometric authentication is not available")
                        _uiState.value = _uiState.value.copy(flowState = errorState)
                        errorState
                    }
                    is KeyStorageResult.BiometricError -> {
                        accountRepository.delete(account.id)
                        val errorState = ImportFlowState.Error("Biometric error: ${result.message}")
                        _uiState.value = _uiState.value.copy(flowState = errorState)
                        errorState
                    }
                    is KeyStorageResult.StorageError -> {
                        accountRepository.delete(account.id)
                        val errorState = ImportFlowState.Error("Storage error: ${result.message}")
                        _uiState.value = _uiState.value.copy(flowState = errorState)
                        errorState
                    }
                    is KeyStorageResult.KeyNotFound -> {
                        accountRepository.delete(account.id)
                        val errorState = ImportFlowState.Error("Key not found error")
                        _uiState.value = _uiState.value.copy(flowState = errorState)
                        errorState
                    }
                }
            } else {
                // Watch-only account - no keypair to save
                val successState = ImportFlowState.Success(account)
                _uiState.value = _uiState.value.copy(flowState = successState)
                successState
            }
        } catch (e: Exception) {
            val errorState = ImportFlowState.Error(e.message ?: "Failed to save account")
            _uiState.value = _uiState.value.copy(flowState = errorState)
            errorState
        }
    }

    // endregion

    // region Cleanup

    private fun clearDerivedKeypair() {
        derivedKeypair?.clear()
        derivedKeypair = null
    }

    override fun onCleared() {
        super.onCleared()
        clearDerivedKeypair()
    }

    // endregion
}
