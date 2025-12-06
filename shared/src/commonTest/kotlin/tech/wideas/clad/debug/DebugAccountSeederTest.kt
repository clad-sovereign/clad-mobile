package tech.wideas.clad.debug

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for DebugAccountSeeder.
 *
 * These tests verify the seeding logic using mock/fake implementations.
 * Platform-specific integration tests (Android instrumented, iOS XCTest)
 * verify actual keypair derivation with real crypto libraries.
 */
class DebugAccountSeederTest {

    @BeforeTest
    fun setup() {
        // Reset debug config before each test
        DebugConfigFactory.setDebugMode(false)
    }

    // ============================================================================
    // DebugConfig Tests
    // ============================================================================

    @Test
    fun `DebugConfig isDebug defaults to false`() {
        // Fresh state should be false (set in setup)
        assertEquals(false, DebugConfig.isDebug)
    }

    @Test
    fun `DebugConfigFactory setDebugMode updates DebugConfig`() {
        DebugConfigFactory.setDebugMode(true)
        assertEquals(true, DebugConfig.isDebug)

        DebugConfigFactory.setDebugMode(false)
        assertEquals(false, DebugConfig.isDebug)
    }

    // ============================================================================
    // Test Constants Verification
    // ============================================================================

    @Test
    fun `DEV_MNEMONIC matches expected well-known Substrate dev mnemonic`() {
        assertEquals(
            "bottom drive obey lake curtain smoke basket hold race lonely fit walk",
            DebugAccountSeeder.DEV_MNEMONIC,
            "Dev mnemonic should match well-known Substrate dev mnemonic"
        )
    }

    @Test
    fun `ALICE_DERIVATION_PATH is correct hard derivation path`() {
        assertEquals(
            "//Alice",
            DebugAccountSeeder.ALICE_DERIVATION_PATH,
            "Alice derivation path should be //Alice"
        )
    }

    @Test
    fun `ALICE_ADDRESS is well-known SS58 address`() {
        assertEquals(
            "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
            DebugAccountSeeder.ALICE_ADDRESS,
            "Alice address should be the well-known address"
        )
    }

    @Test
    fun `BOB_ADDRESS is well-known SS58 address`() {
        assertEquals(
            "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty",
            DebugAccountSeeder.BOB_ADDRESS,
            "Bob address should be the well-known address"
        )
    }

    @Test
    fun `DEV_MNEMONIC has correct word count`() {
        val words = DebugAccountSeeder.DEV_MNEMONIC.split(" ")
        assertEquals(12, words.size, "Dev mnemonic should have 12 words")
    }

    @Test
    fun `ALICE_ADDRESS has correct length for SS58 address`() {
        // SS58 addresses are typically 47-48 characters for Substrate
        val length = DebugAccountSeeder.ALICE_ADDRESS.length
        assertTrue(length in 46..50, "Alice address should be 46-50 characters, got $length")
    }

    @Test
    fun `BOB_ADDRESS has correct length for SS58 address`() {
        val length = DebugAccountSeeder.BOB_ADDRESS.length
        assertTrue(length in 46..50, "Bob address should be 46-50 characters, got $length")
    }

    @Test
    fun `ALICE_ADDRESS starts with 5 indicating generic Substrate prefix`() {
        assertTrue(
            DebugAccountSeeder.ALICE_ADDRESS.startsWith("5"),
            "Alice address should start with 5 (generic Substrate prefix 42)"
        )
    }

    @Test
    fun `BOB_ADDRESS starts with 5 indicating generic Substrate prefix`() {
        assertTrue(
            DebugAccountSeeder.BOB_ADDRESS.startsWith("5"),
            "Bob address should start with 5 (generic Substrate prefix 42)"
        )
    }
}
