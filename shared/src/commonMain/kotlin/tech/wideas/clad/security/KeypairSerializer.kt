package tech.wideas.clad.security

import tech.wideas.clad.crypto.Keypair

/**
 * Serialization utilities for Keypair storage.
 *
 * Binary format (version 1):
 * ```
 * [1 byte: version]
 * [1 byte: keyType (always 0=SR25519, kept for backward compatibility)]
 * [4 bytes: publicKey length (big-endian)]
 * [N bytes: publicKey]
 * [4 bytes: privateKey length (big-endian)]
 * [M bytes: privateKey]
 * ```
 *
 * This format is designed to be:
 * - Forward-compatible (version byte allows future changes)
 * - Self-describing (includes lengths for variable-size keys)
 * - Platform-independent (big-endian integers)
 *
 * Note: The keyType byte is kept for backward compatibility with existing
 * serialized keypairs. New keypairs always use SR25519 (0x00).
 */
object KeypairSerializer {
    private const val VERSION: Byte = 1
    private const val KEY_TYPE_SR25519: Byte = 0

    /**
     * Serialize a Keypair to a byte array.
     *
     * @param keypair The keypair to serialize.
     * @return Serialized byte array.
     */
    fun serialize(keypair: Keypair): ByteArray {
        val pubKey = keypair.publicKey
        val privKey = keypair.privateKey

        // Calculate total size: version(1) + keyType(1) + pubLen(4) + pub + privLen(4) + priv
        val totalSize = 1 + 1 + 4 + pubKey.size + 4 + privKey.size
        val buffer = ByteArray(totalSize)
        var offset = 0

        // Write version
        buffer[offset++] = VERSION

        // Write key type (always SR25519)
        buffer[offset++] = KEY_TYPE_SR25519

        // Write public key length and data
        offset = writeInt(buffer, offset, pubKey.size)
        pubKey.copyInto(buffer, offset)
        offset += pubKey.size

        // Write private key length and data
        offset = writeInt(buffer, offset, privKey.size)
        privKey.copyInto(buffer, offset)

        return buffer
    }

    /**
     * Deserialize a byte array to a Keypair.
     *
     * @param data The serialized byte array.
     * @return Deserialized Keypair.
     * @throws IllegalArgumentException if the data is invalid or uses an unsupported version.
     */
    fun deserialize(data: ByteArray): Keypair {
        require(data.size >= 10) { "Data too short: minimum 10 bytes required" }

        var offset = 0

        // Read and validate version
        val version = data[offset++]
        require(version == VERSION) { "Unsupported serialization version: $version" }

        // Read key type (kept for backward compatibility, must be SR25519)
        val keyTypeByte = data[offset++]
        require(keyTypeByte == KEY_TYPE_SR25519) {
            "Unsupported key type: $keyTypeByte. Only SR25519 (0) is supported."
        }

        // Read public key
        val pubKeyLen = readInt(data, offset)
        offset += 4
        require(pubKeyLen in 1..128) { "Invalid public key length: $pubKeyLen" }
        require(data.size >= offset + pubKeyLen + 4) { "Data too short for public key" }
        val pubKey = data.copyOfRange(offset, offset + pubKeyLen)
        offset += pubKeyLen

        // Read private key
        val privKeyLen = readInt(data, offset)
        offset += 4
        require(privKeyLen in 1..128) { "Invalid private key length: $privKeyLen" }
        require(data.size >= offset + privKeyLen) { "Data too short for private key" }
        val privKey = data.copyOfRange(offset, offset + privKeyLen)

        return Keypair(pubKey, privKey)
    }

    /**
     * Write a 32-bit integer in big-endian format.
     */
    private fun writeInt(buffer: ByteArray, offset: Int, value: Int): Int {
        buffer[offset] = (value shr 24).toByte()
        buffer[offset + 1] = (value shr 16).toByte()
        buffer[offset + 2] = (value shr 8).toByte()
        buffer[offset + 3] = value.toByte()
        return offset + 4
    }

    /**
     * Read a 32-bit integer in big-endian format.
     */
    private fun readInt(buffer: ByteArray, offset: Int): Int {
        return (buffer[offset].toInt() and 0xFF shl 24) or
            (buffer[offset + 1].toInt() and 0xFF shl 16) or
            (buffer[offset + 2].toInt() and 0xFF shl 8) or
            (buffer[offset + 3].toInt() and 0xFF)
    }
}
