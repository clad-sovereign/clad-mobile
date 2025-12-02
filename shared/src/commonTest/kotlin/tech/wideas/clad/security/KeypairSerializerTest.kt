package tech.wideas.clad.security

import tech.wideas.clad.crypto.Keypair
import tech.wideas.clad.crypto.KeyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KeypairSerializerTest {

    @Test
    fun `serialize and deserialize SR25519 keypair`() {
        val original = Keypair(
            publicKey = ByteArray(32) { it.toByte() },
            privateKey = ByteArray(64) { (it + 100).toByte() },
            keyType = KeyType.SR25519
        )

        val serialized = KeypairSerializer.serialize(original)
        val deserialized = KeypairSerializer.deserialize(serialized)

        assertTrue(original.publicKey.contentEquals(deserialized.publicKey))
        assertTrue(original.privateKey.contentEquals(deserialized.privateKey))
        assertEquals(original.keyType, deserialized.keyType)
    }

    @Test
    fun `serialize and deserialize ED25519 keypair`() {
        val original = Keypair(
            publicKey = ByteArray(32) { (255 - it).toByte() },
            privateKey = ByteArray(32) { (it * 2).toByte() },
            keyType = KeyType.ED25519
        )

        val serialized = KeypairSerializer.serialize(original)
        val deserialized = KeypairSerializer.deserialize(serialized)

        assertTrue(original.publicKey.contentEquals(deserialized.publicKey))
        assertTrue(original.privateKey.contentEquals(deserialized.privateKey))
        assertEquals(original.keyType, deserialized.keyType)
    }

    @Test
    fun `serialized format has correct structure`() {
        val keypair = Keypair(
            publicKey = ByteArray(32) { 0xAA.toByte() },
            privateKey = ByteArray(64) { 0xBB.toByte() },
            keyType = KeyType.SR25519
        )

        val serialized = KeypairSerializer.serialize(keypair)

        // version(1) + keyType(1) + pubLen(4) + pub(32) + privLen(4) + priv(64) = 106
        assertEquals(106, serialized.size)

        // Version byte
        assertEquals(1, serialized[0].toInt())

        // Key type (0 = SR25519)
        assertEquals(0, serialized[1].toInt())

        // Public key length (big-endian: 0, 0, 0, 32)
        assertEquals(0, serialized[2].toInt())
        assertEquals(0, serialized[3].toInt())
        assertEquals(0, serialized[4].toInt())
        assertEquals(32, serialized[5].toInt())
    }

    @Test
    fun `deserialize fails with invalid version`() {
        val invalidData = ByteArray(20) { 0 }
        invalidData[0] = 99 // Invalid version

        assertFailsWith<IllegalArgumentException> {
            KeypairSerializer.deserialize(invalidData)
        }
    }

    @Test
    fun `deserialize fails with data too short`() {
        val shortData = ByteArray(5)

        assertFailsWith<IllegalArgumentException> {
            KeypairSerializer.deserialize(shortData)
        }
    }

    @Test
    fun `deserialize fails with invalid key type`() {
        val invalidData = ByteArray(20) { 0 }
        invalidData[0] = 1 // Valid version
        invalidData[1] = 99 // Invalid key type

        assertFailsWith<IllegalArgumentException> {
            KeypairSerializer.deserialize(invalidData)
        }
    }

    @Test
    fun `roundtrip preserves all key sizes`() {
        // Test various key sizes within valid range
        val keySizes = listOf(1 to 1, 32 to 32, 32 to 64, 64 to 64)

        for ((pubSize, privSize) in keySizes) {
            val original = Keypair(
                publicKey = ByteArray(pubSize) { it.toByte() },
                privateKey = ByteArray(privSize) { it.toByte() },
                keyType = KeyType.ED25519
            )

            val serialized = KeypairSerializer.serialize(original)
            val deserialized = KeypairSerializer.deserialize(serialized)

            assertEquals(pubSize, deserialized.publicKey.size)
            assertEquals(privSize, deserialized.privateKey.size)
        }
    }
}
