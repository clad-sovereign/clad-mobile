package tech.wideas.clad

/**
 * Shared test utility functions for hex encoding/decoding.
 *
 * NOTE: This file is duplicated in androidInstrumentedTest because KMP source set
 * isolation prevents androidInstrumentedTest from accessing commonTest sources.
 * See: shared/src/androidInstrumentedTest/kotlin/tech/wideas/clad/TestUtils.kt
 */
object TestUtils {

    /**
     * Converts a ByteArray to a lowercase hex string.
     */
    fun ByteArray.toHexString(): String =
        joinToString("") { byte ->
            val unsigned = byte.toInt() and 0xFF
            val hex = unsigned.toString(16)
            if (hex.length == 1) "0$hex" else hex
        }

    /**
     * Converts a hex string to a ByteArray.
     * Handles optional "0x" prefix.
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x")
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
