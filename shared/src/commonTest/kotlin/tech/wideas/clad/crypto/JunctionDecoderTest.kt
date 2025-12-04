package tech.wideas.clad.crypto

import tech.wideas.clad.TestUtils.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for JunctionDecoder.
 *
 * These tests verify that derivation path parsing and chaincode computation
 * match Substrate's algorithm exactly.
 *
 * ## Test Vectors
 *
 * Chaincodes are computed as follows (matching Substrate):
 * - String "Alice" → UTF-8 [65, 108, 105, 99, 101] → zero-pad to 32 bytes
 * - Numeric "42" → little-endian u64 [42, 0, 0, 0, 0, 0, 0, 0] → zero-pad to 32 bytes
 * - Hex "0xabcd" → decoded [0xab, 0xcd] → zero-pad to 32 bytes
 */
class JunctionDecoderTest {

    private val decoder = JunctionDecoder()

    // ============================================================================
    // Empty Path Tests
    // ============================================================================

    @Test
    fun `empty path returns empty list`() {
        val junctions = decoder.decode("")
        assertTrue(junctions.isEmpty())
    }

    // ============================================================================
    // Single Junction Tests
    // ============================================================================

    @Test
    fun `single hard junction with string name`() {
        val junctions = decoder.decode("//Alice")

        assertEquals(1, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)

        // "Alice" → UTF-8 bytes + zero padding
        val expected = "Alice".encodeToByteArray() + ByteArray(27)
        assertTrue(
            expected.contentEquals(junctions[0].chaincode),
            "Chaincode should be 'Alice' UTF-8 bytes zero-padded to 32"
        )
    }

    @Test
    fun `single soft junction with string name`() {
        val junctions = decoder.decode("/soft")

        assertEquals(1, junctions.size)
        assertEquals(JunctionType.SOFT, junctions[0].type)

        val expected = "soft".encodeToByteArray() + ByteArray(28)
        assertTrue(expected.contentEquals(junctions[0].chaincode))
    }

    @Test
    fun `single hard junction with numeric name`() {
        val junctions = decoder.decode("//42")

        assertEquals(1, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)

        // 42 as u64 little-endian: [42, 0, 0, 0, 0, 0, 0, 0] + 24 zeros
        val expected = byteArrayOf(42, 0, 0, 0, 0, 0, 0, 0) + ByteArray(24)
        assertTrue(
            expected.contentEquals(junctions[0].chaincode),
            "Numeric 42 should be encoded as little-endian u64"
        )
    }

    @Test
    fun `single hard junction with large numeric name`() {
        val junctions = decoder.decode("//256")

        assertEquals(1, junctions.size)

        // 256 as u64 little-endian: [0, 1, 0, 0, 0, 0, 0, 0] + 24 zeros
        val expected = byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0) + ByteArray(24)
        assertTrue(
            expected.contentEquals(junctions[0].chaincode),
            "Numeric 256 should be [0, 1, 0, 0, 0, 0, 0, 0] in little-endian"
        )
    }

    @Test
    fun `single hard junction with hex name`() {
        val junctions = decoder.decode("//0xabcd")

        assertEquals(1, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)

        // 0xabcd → [0xab, 0xcd] + 30 zeros
        val expected = byteArrayOf(0xab.toByte(), 0xcd.toByte()) + ByteArray(30)
        assertTrue(
            expected.contentEquals(junctions[0].chaincode),
            "Hex 0xabcd should decode to [0xab, 0xcd] + padding"
        )
    }

    @Test
    fun `hex with uppercase`() {
        val junctions = decoder.decode("//0xABCD")

        assertEquals(1, junctions.size)

        val expected = byteArrayOf(0xab.toByte(), 0xcd.toByte()) + ByteArray(30)
        assertTrue(expected.contentEquals(junctions[0].chaincode))
    }

    // ============================================================================
    // Multiple Junction Tests
    // ============================================================================

    @Test
    fun `two hard junctions`() {
        val junctions = decoder.decode("//Alice//Bob")

        assertEquals(2, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)
        assertEquals(JunctionType.HARD, junctions[1].type)

        val aliceExpected = "Alice".encodeToByteArray() + ByteArray(27)
        val bobExpected = "Bob".encodeToByteArray() + ByteArray(29)

        assertTrue(aliceExpected.contentEquals(junctions[0].chaincode))
        assertTrue(bobExpected.contentEquals(junctions[1].chaincode))
    }

    @Test
    fun `hard then soft junction`() {
        val junctions = decoder.decode("//hard/soft")

        assertEquals(2, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)
        assertEquals(JunctionType.SOFT, junctions[1].type)

        val hardExpected = "hard".encodeToByteArray() + ByteArray(28)
        val softExpected = "soft".encodeToByteArray() + ByteArray(28)

        assertTrue(hardExpected.contentEquals(junctions[0].chaincode))
        assertTrue(softExpected.contentEquals(junctions[1].chaincode))
    }

    @Test
    fun `multiple mixed junctions`() {
        val junctions = decoder.decode("//Alice/stash//controller")

        assertEquals(3, junctions.size)
        assertEquals(JunctionType.HARD, junctions[0].type)   // //Alice
        assertEquals(JunctionType.SOFT, junctions[1].type)   // /stash
        assertEquals(JunctionType.HARD, junctions[2].type)   // //controller

        val aliceExpected = "Alice".encodeToByteArray() + ByteArray(27)
        val stashExpected = "stash".encodeToByteArray() + ByteArray(27)
        val controllerExpected = "controller".encodeToByteArray() + ByteArray(22)

        assertTrue(aliceExpected.contentEquals(junctions[0].chaincode))
        assertTrue(stashExpected.contentEquals(junctions[1].chaincode))
        assertTrue(controllerExpected.contentEquals(junctions[2].chaincode))
    }

    @Test
    fun `numeric and string junctions mixed`() {
        val junctions = decoder.decode("//0/Alice/1")

        assertEquals(3, junctions.size)

        // //0 → numeric 0 as little-endian u64
        val zeroExpected = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0) + ByteArray(24)
        assertTrue(zeroExpected.contentEquals(junctions[0].chaincode))

        // /Alice → string
        val aliceExpected = "Alice".encodeToByteArray() + ByteArray(27)
        assertTrue(aliceExpected.contentEquals(junctions[1].chaincode))

        // /1 → numeric 1 as little-endian u64
        val oneExpected = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0) + ByteArray(24)
        assertTrue(oneExpected.contentEquals(junctions[2].chaincode))
    }

    // ============================================================================
    // Well-Known Test Vector: Alice
    // ============================================================================

    @Test
    fun `Alice chaincode matches expected value`() {
        val junctions = decoder.decode("//Alice")

        // "Alice" = [65, 108, 105, 99, 101] in UTF-8
        val aliceUtf8 = byteArrayOf(65, 108, 105, 99, 101)
        val expectedChaincode = aliceUtf8 + ByteArray(27)

        assertEquals(32, junctions[0].chaincode.size)
        assertTrue(
            expectedChaincode.contentEquals(junctions[0].chaincode),
            "Alice chaincode should be UTF-8 bytes [65, 108, 105, 99, 101] + 27 zeros"
        )

        // Verify hex representation
        val expectedHex = "416c696365" + "00".repeat(27)
        assertEquals(expectedHex, junctions[0].chaincode.toHexString())
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun `junction name at exactly 32 bytes`() {
        val name32 = "a".repeat(32)
        val junctions = decoder.decode("//$name32")

        assertEquals(1, junctions.size)
        assertEquals(32, junctions[0].chaincode.size)
        assertTrue(name32.encodeToByteArray().contentEquals(junctions[0].chaincode))
    }

    @Test
    fun `junction name over 32 bytes throws`() {
        val name33 = "a".repeat(33)

        val exception = assertFailsWith<IllegalArgumentException> {
            decoder.decode("//$name33")
        }

        assertTrue(exception.message?.contains("≤ 32 bytes") == true)
    }

    @Test
    fun `special characters in junction name`() {
        val junctions = decoder.decode("//test-account_1")

        assertEquals(1, junctions.size)
        val expected = "test-account_1".encodeToByteArray() + ByteArray(18)
        assertTrue(expected.contentEquals(junctions[0].chaincode))
    }

    @Test
    fun `unicode characters in junction name`() {
        // UTF-8 for emoji or other unicode
        val junctions = decoder.decode("//café")

        assertEquals(1, junctions.size)
        val cafeBytes = "café".encodeToByteArray() // 5 bytes in UTF-8 (é is 2 bytes)
        assertEquals(5, cafeBytes.size) // c=1, a=1, f=1, é=2
        val expected = cafeBytes + ByteArray(27)
        assertTrue(expected.contentEquals(junctions[0].chaincode))
    }

    // ============================================================================
    // Error Cases
    // ============================================================================

    @Test
    fun `path not starting with slash throws`() {
        assertFailsWith<IllegalArgumentException> {
            decoder.decode("Alice")
        }
    }

    @Test
    fun `empty junction name throws`() {
        assertFailsWith<IllegalArgumentException> {
            decoder.decode("//")
        }
    }

    @Test
    fun `empty junction name in middle throws`() {
        assertFailsWith<IllegalArgumentException> {
            decoder.decode("//Alice//")
        }
    }

    @Test
    fun `triple slash treated as hard then empty throws`() {
        // "///" → "//" (hard) + "/" (soft with empty name)
        assertFailsWith<IllegalArgumentException> {
            decoder.decode("///")
        }
    }

    // ============================================================================
    // Junction Data Class Tests
    // ============================================================================

    @Test
    fun `Junction requires exactly 32 byte chaincode`() {
        assertFailsWith<IllegalArgumentException> {
            Junction(JunctionType.HARD, ByteArray(31))
        }

        assertFailsWith<IllegalArgumentException> {
            Junction(JunctionType.HARD, ByteArray(33))
        }
    }

    @Test
    fun `Junction equality by content`() {
        val chaincode = ByteArray(32) { it.toByte() }
        val j1 = Junction(JunctionType.HARD, chaincode.copyOf())
        val j2 = Junction(JunctionType.HARD, chaincode.copyOf())

        assertEquals(j1, j2)
        assertEquals(j1.hashCode(), j2.hashCode())
    }

    @Test
    fun `Junction inequality by type`() {
        val chaincode = ByteArray(32)
        val hard = Junction(JunctionType.HARD, chaincode)
        val soft = Junction(JunctionType.SOFT, chaincode)

        assertTrue(hard != soft)
    }
}
