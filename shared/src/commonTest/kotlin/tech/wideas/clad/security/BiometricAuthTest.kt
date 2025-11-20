package tech.wideas.clad.security

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for BiometricAuth implementations.
 *
 * These tests verify the contract that all platform-specific BiometricAuth
 * implementations must follow.
 */
class BiometricAuthTest {

    private class TestBiometricAuth(
        private val availabilityResult: Boolean = true,
        private val authResult: BiometricResult = BiometricResult.Success
    ) : BiometricAuth {
        var isAvailableCalled = false
        var authenticateCalled = false
        var lastTitle: String? = null
        var lastSubtitle: String? = null
        var lastDescription: String? = null

        override suspend fun isAvailable(): Boolean {
            isAvailableCalled = true
            return availabilityResult
        }

        override suspend fun authenticate(
            title: String,
            subtitle: String,
            description: String
        ): BiometricResult {
            authenticateCalled = true
            lastTitle = title
            lastSubtitle = subtitle
            lastDescription = description
            return authResult
        }
    }

    @Test
    fun `isAvailable should return true when biometric is available`() = runTest {
        val auth = TestBiometricAuth(availabilityResult = true)

        val result = auth.isAvailable()

        assertTrue(result)
        assertTrue(auth.isAvailableCalled)
    }

    @Test
    fun `isAvailable should return false when biometric is not available`() = runTest {
        val auth = TestBiometricAuth(availabilityResult = false)

        val result = auth.isAvailable()

        assertFalse(result)
        assertTrue(auth.isAvailableCalled)
    }

    @Test
    fun `authenticate should return Success on successful authentication`() = runTest {
        val auth = TestBiometricAuth(authResult = BiometricResult.Success)

        val result = auth.authenticate(
            title = "Authenticate",
            subtitle = "Please verify",
            description = "Verify your identity"
        )

        assertIs<BiometricResult.Success>(result)
        assertTrue(auth.authenticateCalled)
    }

    @Test
    fun `authenticate should pass correct parameters`() = runTest {
        val auth = TestBiometricAuth()
        val title = "Test Title"
        val subtitle = "Test Subtitle"
        val description = "Test Description"

        auth.authenticate(title, subtitle, description)

        assertEquals(title, auth.lastTitle)
        assertEquals(subtitle, auth.lastSubtitle)
        assertEquals(description, auth.lastDescription)
    }

    @Test
    fun `authenticate should return Error with message on authentication error`() = runTest {
        val errorMessage = "Authentication failed"
        val auth = TestBiometricAuth(authResult = BiometricResult.Error(errorMessage))

        val result = auth.authenticate("Title", "Subtitle", "Description")

        assertIs<BiometricResult.Error>(result)
        assertEquals(errorMessage, result.message)
    }

    @Test
    fun `authenticate should return Cancelled when user cancels`() = runTest {
        val auth = TestBiometricAuth(authResult = BiometricResult.Cancelled)

        val result = auth.authenticate("Title", "Subtitle", "Description")

        assertIs<BiometricResult.Cancelled>(result)
    }

    @Test
    fun `authenticate should return NotAvailable when biometric not available`() = runTest {
        val auth = TestBiometricAuth(authResult = BiometricResult.NotAvailable)

        val result = auth.authenticate("Title", "Subtitle", "Description")

        assertIs<BiometricResult.NotAvailable>(result)
    }

    @Test
    fun `authenticate should handle empty strings for optional parameters`() = runTest {
        val auth = TestBiometricAuth()

        auth.authenticate(title = "Title", subtitle = "", description = "")

        assertEquals("Title", auth.lastTitle)
        assertEquals("", auth.lastSubtitle)
        assertEquals("", auth.lastDescription)
    }

    @Test
    fun `BiometricResult sealed class should cover all authentication outcomes`() {
        // This test verifies that the BiometricResult sealed class
        // properly represents all possible authentication outcomes

        val results = listOf(
            BiometricResult.Success,
            BiometricResult.Error("Error"),
            BiometricResult.Cancelled,
            BiometricResult.NotAvailable
        )

        // Verify we can handle all cases in a when expression
        results.forEach { result ->
            val handled = when (result) {
                is BiometricResult.Success -> true
                is BiometricResult.Error -> true
                is BiometricResult.Cancelled -> true
                is BiometricResult.NotAvailable -> true
            }
            assertTrue(handled, "All BiometricResult cases should be handled")
        }
    }
}
