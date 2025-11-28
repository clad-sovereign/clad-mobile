package tech.wideas.clad.crypto

import tech.wideas.clad.TestUtils.hexToBytes
import tech.wideas.clad.TestUtils.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform determinism tests for SS58 address encoding.
 *
 * These tests verify that SS58 encoding/decoding works identically across platforms.
 * They use well-known test vectors that don't require native crypto libraries.
 *
 * ## Test Vector Architecture
 *
 * This file is the SINGLE SOURCE OF TRUTH for all cross-platform test vectors.
 * Platform-specific tests (Android instrumented tests, iOS XCTests) must duplicate
 * these values because:
 * - androidInstrumentedTest cannot access commonTest sources (KMP source set isolation)
 * - iOS/Swift cannot import Kotlin test sources
 *
 * When adding or modifying test vectors:
 * 1. Update the constants in this file first
 * 2. Update the duplicated values in:
 *    - shared/src/androidInstrumentedTest/.../MnemonicProviderTest.kt
 *    - iosApp/iosAppTests/MnemonicProviderTests.swift
 *
 * Test vectors sourced from:
 * - Substrate subkey documentation
 * - https://github.com/polkadot-developers/substrate-developer-hub.github.io
 */
class CrossPlatformDeterminismTest {

    /**
     * Test Vector: Alice's well-known public key and SS58 address.
     * Used to verify SS58 encoding/decoding without native crypto.
     */
    object AliceTestVector {
        const val PUBLIC_KEY_HEX = "d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d"
        const val SS58_ADDRESS = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
    }

    /**
     * Test Vector 1 (SR25519): Expected output for mnemonic without derivation path.
     *
     * SINGLE SOURCE OF TRUTH - duplicated in platform-specific tests.
     * Source: `subkey inspect "caution juice atom organ advance problem want pledge someone senior holiday very"`
     */
    object TestVector1 {
        const val MNEMONIC = "caution juice atom organ advance problem want pledge someone senior holiday very"
        const val EXPECTED_PUBLIC_KEY_HEX = "d6a3105d6768e956e9e5d41050ac29843f98561410d3a47f9dd5b3b227ab8746"
        const val EXPECTED_SS58_ADDRESS = "5Gv8YYFu8H1btvmrJy9FjjAWfb99wrhV3uhPFoNEr918utyR"
    }

    /**
     * Test Vector 2 (ED25519): Expected output for mnemonic without derivation path.
     *
     * SINGLE SOURCE OF TRUTH - duplicated in platform-specific tests.
     * Source: `subkey inspect --scheme ed25519 "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"`
     */
    object TestVector2Ed25519 {
        const val MNEMONIC = "infant salmon buzz patrol maple subject turtle cute legend song vital leisure"
        const val EXPECTED_PUBLIC_KEY_HEX = "1a0e2bf1e0195a1f5396c5fd209a620a48fe90f6f336d89c89405a0183a857a3"
        const val EXPECTED_SS58_ADDRESS = "5CesK3uTmn4NGfD3oyGBd1jrp4EfRyYdtqL3ERe9SXv8jUHb"
    }

    // ============================================================================
    // SS58 Address Encoding Determinism (No native crypto required)
    // ============================================================================

    @Test
    fun `SS58 encoding produces correct address for Alice test vector`() {
        val alicePublicKey = hexToBytes(AliceTestVector.PUBLIC_KEY_HEX)
        val encodedAddress = Ss58.encode(alicePublicKey, NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            AliceTestVector.SS58_ADDRESS,
            encodedAddress,
            "SS58 encoding should produce Alice's well-known address"
        )
    }

    @Test
    fun `SS58 decode extracts correct public key from Alice address`() {
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(AliceTestVector.SS58_ADDRESS)

        assertEquals(
            AliceTestVector.PUBLIC_KEY_HEX,
            decodedPublicKey.toHexString(),
            "Decoded public key should match Alice's known public key"
        )
        assertEquals(
            NetworkPrefix.GENERIC_SUBSTRATE,
            decodedPrefix,
            "Decoded prefix should be GENERIC_SUBSTRATE (42)"
        )
    }

    @Test
    fun `SS58 decode and re-encode produces identical address`() {
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(AliceTestVector.SS58_ADDRESS)
        val reEncodedAddress = Ss58.encode(decodedPublicKey, decodedPrefix)

        assertEquals(
            AliceTestVector.SS58_ADDRESS,
            reEncodedAddress,
            "Decode and re-encode should produce identical address"
        )
    }

    @Test
    fun `SS58 encoding for TestVector1 expected public key produces expected address`() {
        val publicKey = hexToBytes(TestVector1.EXPECTED_PUBLIC_KEY_HEX)
        val address = Ss58.encode(publicKey, NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            TestVector1.EXPECTED_SS58_ADDRESS,
            address,
            "SS58 encoding of TestVector1 (SR25519) public key should produce expected address"
        )
    }

    @Test
    fun `SS58 encoding for TestVector2 ED25519 expected public key produces expected address`() {
        val publicKey = hexToBytes(TestVector2Ed25519.EXPECTED_PUBLIC_KEY_HEX)
        val address = Ss58.encode(publicKey, NetworkPrefix.GENERIC_SUBSTRATE)

        assertEquals(
            TestVector2Ed25519.EXPECTED_SS58_ADDRESS,
            address,
            "SS58 encoding of TestVector2 (ED25519) public key should produce expected address"
        )
    }

    @Test
    fun `SS58 decode extracts correct public key from ED25519 address`() {
        val (decodedPublicKey, decodedPrefix) = Ss58.decode(TestVector2Ed25519.EXPECTED_SS58_ADDRESS)

        assertEquals(
            TestVector2Ed25519.EXPECTED_PUBLIC_KEY_HEX,
            decodedPublicKey.toHexString(),
            "Decoded public key should match ED25519 known public key"
        )
        assertEquals(
            NetworkPrefix.GENERIC_SUBSTRATE,
            decodedPrefix,
            "Decoded prefix should be GENERIC_SUBSTRATE (42)"
        )
    }

}
