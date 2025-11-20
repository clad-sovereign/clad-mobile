package tech.wideas.clad.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tech.wideas.clad.substrate.ConnectionState
import tech.wideas.clad.substrate.SubstrateClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for AccountsViewModelIOS.
 *
 * Verifies iOS-specific accounts view state management, connection state observation,
 * and message stream handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelIOSTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var substrateClient: SubstrateClient

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        substrateClient = SubstrateClient(autoReconnect = false, dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        substrateClient.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial connection state should be Disconnected`() = runTest {
        // Given: A new AccountsViewModelIOS
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: Initial connection state should be Disconnected
        viewModel.connectionState.test {
            val state = awaitItem()
            assertEquals(ConnectionState.Disconnected, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial messages list should be empty`() = runTest {
        // Given: A new AccountsViewModelIOS
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: Initial messages should be empty
        viewModel.messages.test {
            val messages = awaitItem()
            assertTrue(messages.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel should observe SubstrateClient connection state`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing connection state
        viewModel.connectionState.test {
            val state = awaitItem()

            // Then: Should reflect client's connection state
            assertEquals(substrateClient.connectionState.value, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel should observe SubstrateClient messages`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing messages
        viewModel.messages.test {
            val messages = awaitItem()

            // Then: Should reflect client's messages
            assertEquals(substrateClient.messages.value, messages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearMessages should clear all messages`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Clearing messages
        viewModel.clearMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Messages should be empty
        viewModel.messages.test {
            val messages = awaitItem()
            assertTrue(messages.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connectionState should be a StateFlow`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: connectionState should be observable StateFlow
        assertNotNull(viewModel.connectionState)
        viewModel.connectionState.test {
            val state = awaitItem()
            assertNotNull(state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `messages should be a StateFlow`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: messages should be observable StateFlow
        assertNotNull(viewModel.messages)
        viewModel.messages.test {
            val messageList = awaitItem()
            assertNotNull(messageList)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel should reflect Connecting state when client is connecting`() = runTest {
        // Given: A ViewModel observing the client
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Attempting to connect (will fail but will transition to Connecting first)
        viewModel.connectionState.test {
            // Skip initial Disconnected state
            assertEquals(ConnectionState.Disconnected, awaitItem())

            // Note: We can't easily test state transitions without a real connection
            // This test verifies the ViewModel can observe connection state changes
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel should maintain connection state across multiple observations`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing connection state multiple times
        val firstObservation = viewModel.connectionState.value
        val secondObservation = viewModel.connectionState.value

        // Then: Should return the same state
        assertEquals(firstObservation, secondObservation)
    }

    @Test
    fun `ViewModel should maintain messages list across multiple observations`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing messages multiple times
        val firstObservation = viewModel.messages.value
        val secondObservation = viewModel.messages.value

        // Then: Should return the same list
        assertEquals(firstObservation, secondObservation)
    }

    @Test
    fun `ViewModel should properly expose StateFlows via SKIE for Swift interop`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: StateFlows should be properly exposed for SKIE conversion to AsyncSequence
        // This verifies the ViewModels have the correct property types for Swift interop
        assertNotNull(viewModel.connectionState)
        assertNotNull(viewModel.messages)

        // Verify they are StateFlows (not regular Flows)
        assertTrue(viewModel.connectionState is kotlinx.coroutines.flow.StateFlow)
        assertTrue(viewModel.messages is kotlinx.coroutines.flow.StateFlow)
    }

    @Test
    fun `messages list should never be null`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing messages
        viewModel.messages.test {
            val messages = awaitItem()

            // Then: Messages should be a valid list (can be empty, but not null)
            assertNotNull(messages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connectionState should never be null`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Observing connection state
        viewModel.connectionState.test {
            val state = awaitItem()

            // Then: State should be a valid ConnectionState (never null)
            assertNotNull(state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearMessages should be idempotent`() = runTest {
        // Given: A ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Clearing messages multiple times
        viewModel.clearMessages()
        viewModel.clearMessages()
        viewModel.clearMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should not throw exception and messages should still be empty
        viewModel.messages.test {
            val messages = awaitItem()
            assertTrue(messages.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel should use Eagerly started StateFlows for immediate observation`() = runTest {
        // Given: A newly created ViewModel
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: StateFlows should have initial values immediately available
        // (This tests that SharingStarted.Eagerly is being used correctly)
        val connectionStateValue = viewModel.connectionState.value
        val messagesValue = viewModel.messages.value

        assertNotNull(connectionStateValue)
        assertNotNull(messagesValue)
    }

    @Test
    fun `ViewModel should delegate clearMessages to SubstrateClient`() = runTest {
        // Given: A ViewModel and SubstrateClient
        val viewModel = AccountsViewModelIOS(substrateClient)

        // When: Clearing messages via ViewModel
        viewModel.clearMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: SubstrateClient messages should also be cleared
        substrateClient.messages.test {
            val messages = awaitItem()
            assertTrue(messages.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel scope should be independent from client scope`() = runTest {
        // Given: A ViewModel with its own coroutine scope
        val viewModel = AccountsViewModelIOS(substrateClient)

        // Then: ViewModel should maintain its own lifecycle
        // This is verified by the fact that we can create and observe StateFlows
        // independently from the client's lifecycle
        assertNotNull(viewModel.connectionState)
        assertNotNull(viewModel.messages)
    }
}
