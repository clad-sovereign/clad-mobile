package tech.wideas.clad.ui.import

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import tech.wideas.clad.data.AccountInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ImportViewModel state logic and validation.
 *
 * These tests focus on the ImportUiState data class and ImportFlowState
 * sealed class behavior, which can be tested without platform dependencies.
 *
 * Full ViewModel integration tests requiring MnemonicProvider, KeyStorage,
 * and AccountRepository should be done as Android instrumented tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    // region ImportUiState Tests

    @Test
    fun `initial ImportUiState should have correct defaults`() = runTest {
        val state = ImportUiState()

        assertTrue(state.flowState is ImportFlowState.SelectMethod)
        assertNull(state.selectedMethod)
        assertEquals(12, state.wordCount)
        assertEquals(List(12) { "" }, state.seedPhraseWords)
        assertNull(state.mnemonicError)
        assertNull(state.qrCodeError)
        assertEquals("", state.manualAddress)
        assertNull(state.addressError)
        assertEquals("", state.accountLabel)
        assertNull(state.error)
    }

    @Test
    fun `isLoading should be true during Validating state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.Validating)
        assertTrue(state.isLoading)
    }

    @Test
    fun `isLoading should be true during Saving state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.Saving)
        assertTrue(state.isLoading)
    }

    @Test
    fun `isLoading should be false during SelectMethod state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.SelectMethod)
        assertFalse(state.isLoading)
    }

    @Test
    fun `isLoading should be false during EnteringSeedPhrase state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.EnteringSeedPhrase)
        assertFalse(state.isLoading)
    }

    @Test
    fun `isLoading should be false during Success state`() = runTest {
        val account = createTestAccount()
        val state = ImportUiState(flowState = ImportFlowState.Success(account))
        assertFalse(state.isLoading)
    }

    // endregion

    // region canProceed Tests - Seed Phrase

    @Test
    fun `canProceed should be false when seed phrase words are empty`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = List(12) { "" },
            wordCount = 12
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false when some seed phrase words are empty`() = runTest {
        val words = List(12) { if (it < 6) "word$it" else "" }
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = words,
            wordCount = 12
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false when seed phrase has mnemonic error`() = runTest {
        val words = List(12) { "word$it" }
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = words,
            wordCount = 12,
            mnemonicError = "Invalid checksum"
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be true when all seed phrase words filled and no error`() = runTest {
        val words = List(12) { "word$it" }
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = words,
            wordCount = 12,
            mnemonicError = null
        )
        assertTrue(state.canProceed)
    }

    @Test
    fun `canProceed should check only wordCount words for 24-word mnemonic`() = runTest {
        val words = List(24) { "word$it" }
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = words,
            wordCount = 24
        )
        assertTrue(state.canProceed)
    }

    @Test
    fun `canProceed should be false when 24-word has empty words`() = runTest {
        val words = List(24) { if (it < 20) "word$it" else "" }
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringSeedPhrase,
            seedPhraseWords = words,
            wordCount = 24
        )
        assertFalse(state.canProceed)
    }

    // endregion

    // region canProceed Tests - Manual Address

    @Test
    fun `canProceed should be false when manual address is empty`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringAddress,
            manualAddress = ""
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false when manual address is blank`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringAddress,
            manualAddress = "   "
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false when manual address has error`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringAddress,
            manualAddress = "invalid",
            addressError = "Invalid SS58 format"
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be true when manual address is valid`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.EnteringAddress,
            manualAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            addressError = null
        )
        assertTrue(state.canProceed)
    }

    // endregion

    // region canProceed Tests - Confirming

    @Test
    fun `canProceed should be false when account label is empty in Confirming state`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.Confirming(
                address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
                keypair = null
            ),
            accountLabel = ""
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false when account label is blank in Confirming state`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.Confirming(
                address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
                keypair = null
            ),
            accountLabel = "   "
        )
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be true when account label is set in Confirming state`() = runTest {
        val state = ImportUiState(
            flowState = ImportFlowState.Confirming(
                address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
                keypair = null
            ),
            accountLabel = "My Account"
        )
        assertTrue(state.canProceed)
    }

    // endregion

    // region canProceed Tests - Other States

    @Test
    fun `canProceed should be false for SelectMethod state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.SelectMethod)
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false for ScanningQrCode state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.ScanningQrCode)
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false for Validating state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.Validating)
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false for Saving state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.Saving)
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false for Success state`() = runTest {
        val account = createTestAccount()
        val state = ImportUiState(flowState = ImportFlowState.Success(account))
        assertFalse(state.canProceed)
    }

    @Test
    fun `canProceed should be false for Error state`() = runTest {
        val state = ImportUiState(flowState = ImportFlowState.Error("Something went wrong"))
        assertFalse(state.canProceed)
    }

    // endregion

    // region ImportFlowState Tests

    @Test
    fun `ImportFlowState Confirming should hold address and optional keypair`() = runTest {
        val address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        val state = ImportFlowState.Confirming(address = address, keypair = null)

        assertEquals(address, state.address)
        assertNull(state.keypair)
    }

    @Test
    fun `ImportFlowState Success should hold account info`() = runTest {
        val account = createTestAccount()
        val state = ImportFlowState.Success(account)

        assertEquals(account, state.account)
    }

    @Test
    fun `ImportFlowState Error should hold error message`() = runTest {
        val message = "Failed to import account"
        val state = ImportFlowState.Error(message)

        assertEquals(message, state.message)
    }

    // endregion

    // region ImportMethod Tests

    @Test
    fun `ImportMethod should have three values`() = runTest {
        val values = ImportMethod.values()

        assertEquals(3, values.size)
        assertTrue(values.contains(ImportMethod.SEED_PHRASE))
        assertTrue(values.contains(ImportMethod.QR_CODE))
        assertTrue(values.contains(ImportMethod.MANUAL_ADDRESS))
    }

    // endregion

    // region Input Validation Logic Tests

    @Test
    fun `label sanitization should allow alphanumeric characters`() = runTest {
        val input = "MyAccount123"
        val sanitized = sanitizeLabel(input)
        assertEquals("MyAccount123", sanitized)
    }

    @Test
    fun `label sanitization should allow spaces`() = runTest {
        val input = "My Account Name"
        val sanitized = sanitizeLabel(input)
        assertEquals("My Account Name", sanitized)
    }

    @Test
    fun `label sanitization should allow dash underscore dot`() = runTest {
        val input = "My-Account_v1.0"
        val sanitized = sanitizeLabel(input)
        assertEquals("My-Account_v1.0", sanitized)
    }

    @Test
    fun `label sanitization should filter special characters`() = runTest {
        val input = "My<Account>Name"
        val sanitized = sanitizeLabel(input)
        assertEquals("MyAccountName", sanitized)
    }

    @Test
    fun `label sanitization should trim whitespace`() = runTest {
        val input = "  My Account  "
        val sanitized = sanitizeLabel(input)
        assertEquals("My Account", sanitized)
    }

    @Test
    fun `label sanitization should limit length`() = runTest {
        val input = "A".repeat(100)
        val sanitized = sanitizeLabel(input)
        assertEquals(ImportViewModel.MAX_LABEL_LENGTH, sanitized.length)
    }

    @Test
    fun `address sanitization should trim whitespace`() = runTest {
        val input = "  5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY  "
        val sanitized = sanitizeAddress(input)
        assertEquals("5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", sanitized)
    }

    @Test
    fun `address sanitization should limit length`() = runTest {
        val input = "A".repeat(150)
        val sanitized = sanitizeAddress(input)
        assertEquals(ImportViewModel.MAX_ADDRESS_LENGTH, sanitized.length)
    }

    // endregion

    // region Helper Functions

    private fun createTestAccount(): AccountInfo {
        return AccountInfo(
            id = "test-account-id",
            label = "Test Account",
            address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Replicate the label sanitization logic from ImportViewModel
     * to test it independently.
     */
    private fun sanitizeLabel(label: String): String {
        return label
            .take(ImportViewModel.MAX_LABEL_LENGTH)
            .filter { it.isLetterOrDigit() || it.isWhitespace() || it in "-_." }
            .trim()
    }

    /**
     * Replicate the address sanitization logic from ImportViewModel
     * to test it independently.
     */
    private fun sanitizeAddress(address: String): String {
        return address.trim().take(ImportViewModel.MAX_ADDRESS_LENGTH)
    }

    // endregion
}
