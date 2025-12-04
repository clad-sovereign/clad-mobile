package tech.wideas.clad.crypto

/**
 * Junction type in a Substrate derivation path.
 *
 * - HARD: Hard derivation (prefix: `//`). Child key cannot be derived from parent public key.
 * - SOFT: Soft derivation (prefix: `/`). Child key CAN be derived from parent public key.
 */
enum class JunctionType {
    HARD,
    SOFT
}

/**
 * A single junction in a Substrate derivation path.
 *
 * @property type Whether this is a hard (`//`) or soft (`/`) junction
 * @property chaincode The 32-byte chaincode derived from the junction name
 */
data class Junction(
    val type: JunctionType,
    val chaincode: ByteArray
) {
    init {
        require(chaincode.size == CHAINCODE_LENGTH) {
            "Chaincode must be exactly $CHAINCODE_LENGTH bytes, got ${chaincode.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Junction) return false
        return type == other.type && chaincode.contentEquals(other.chaincode)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + chaincode.contentHashCode()
        return result
    }

    companion object {
        const val CHAINCODE_LENGTH = 32
    }
}

/**
 * Decoder for Substrate derivation paths.
 *
 * Parses paths like `//Alice`, `//hard/soft`, or `//Alice//Bob` into typed junctions
 * with computed chaincodes matching Substrate's algorithm.
 *
 * ## Path Syntax
 * - `//name` → Hard junction (double slash)
 * - `/name` → Soft junction (single slash)
 * - Multiple junctions: `//Alice//stash` or `//hard/soft`
 * - Empty path → empty list (no derivation)
 *
 * ## Chaincode Computation
 * Junction names are converted to 32-byte chaincodes:
 * - Numeric strings: 8-byte little-endian encoding, zero-padded to 32 bytes
 * - Hex strings (0x prefix): Decoded hex bytes, zero-padded to 32 bytes
 * - Other strings: UTF-8 bytes, zero-padded to 32 bytes
 * - Names > 32 bytes: Rejected (not supported)
 *
 * @see [Nova SDK SubstrateJunctionDecoder](https://github.com/novasamatech/substrate-sdk-android/blob/master/substrate-sdk-android/src/main/java/io/novasama/substrate_sdk_android/encrypt/junction/SubstrateJunctionDecoder.kt)
 * @see [Substrate DeriveJunction](https://github.com/paritytech/polkadot-sdk/blob/master/substrate/primitives/core/src/crypto.rs)
 */
class JunctionDecoder {

    /**
     * Parse a derivation path into a list of junctions.
     *
     * @param derivationPath Derivation path (e.g., "//Alice", "//hard/soft")
     * @return List of junctions in order, empty if path is empty
     * @throws IllegalArgumentException if path is malformed or contains junction names > 32 bytes
     */
    fun decode(derivationPath: String): List<Junction> {
        if (derivationPath.isEmpty()) {
            return emptyList()
        }

        val junctions = mutableListOf<Junction>()
        var remaining = derivationPath

        while (remaining.isNotEmpty()) {
            val (junction, rest) = parseNextJunction(remaining)
            junctions.add(junction)
            remaining = rest
        }

        return junctions
    }

    /**
     * Parse the next junction from the path and return it with the remaining path.
     */
    private fun parseNextJunction(path: String): Pair<Junction, String> {
        require(path.startsWith("/")) {
            "Derivation path must start with '/' or '//', got: '$path'"
        }

        val isHard = path.startsWith("//")
        val junctionType = if (isHard) JunctionType.HARD else JunctionType.SOFT
        val prefixLength = if (isHard) 2 else 1

        // Find the end of this junction (next / or end of string)
        val afterPrefix = path.substring(prefixLength)
        val nextSlashIndex = afterPrefix.indexOf('/')

        val (junctionName, remaining) = if (nextSlashIndex == -1) {
            afterPrefix to ""
        } else {
            afterPrefix.substring(0, nextSlashIndex) to afterPrefix.substring(nextSlashIndex)
        }

        require(junctionName.isNotEmpty()) {
            "Empty junction name in path: '$path'"
        }

        val chaincode = computeChaincode(junctionName)
        return Junction(junctionType, chaincode) to remaining
    }

    /**
     * Compute the 32-byte chaincode for a junction name.
     *
     * Algorithm (matches Substrate):
     * 1. Try parsing as numeric (u64) → 8-byte little-endian
     * 2. Try parsing as hex (0x prefix) → decoded bytes
     * 3. Otherwise: UTF-8 string bytes
     * 4. Zero-pad to 32 bytes (reject if > 32 bytes)
     */
    private fun computeChaincode(junctionName: String): ByteArray {
        val serialized = serialize(junctionName)

        require(serialized.size <= Junction.CHAINCODE_LENGTH) {
            "Junction name must be ≤ ${Junction.CHAINCODE_LENGTH} bytes (got ${serialized.size}): '$junctionName'"
        }

        return normalize(serialized)
    }

    /**
     * Serialize a junction name to bytes.
     *
     * Tries numeric parsing first (little-endian u64), then hex, then SCALE-encoded UTF-8 string.
     *
     * IMPORTANT: Substrate's DeriveJunction.hard()/soft() methods SCALE-encode the junction name
     * before using it as the chaincode. SCALE encoding for strings includes a length prefix:
     * - Compact integer format: length * 4 for lengths < 64 (single byte)
     * - Then the raw UTF-8 bytes
     *
     * For example, "Alice" (5 bytes) becomes: [0x14, 'A', 'l', 'i', 'c', 'e'] = [0x14, 0x41, 0x6c, 0x69, 0x63, 0x65]
     * where 0x14 = 5 * 4 = 20 in SCALE compact integer format.
     */
    private fun serialize(junctionName: String): ByteArray {
        // Try numeric first (Substrate uses u64 little-endian)
        junctionName.toULongOrNull()?.let { numeric ->
            return numeric.toLittleEndianBytes()
        }

        // Try hex (0x prefix)
        if (junctionName.startsWith("0x", ignoreCase = true)) {
            val hexPart = junctionName.substring(2)
            if (hexPart.isNotEmpty() && hexPart.all { it.isHexDigit() }) {
                return hexPart.hexToByteArray()
            }
        }

        // Default: SCALE-encoded UTF-8 string
        // Substrate's DeriveJunction uses SCALE encoding (length prefix + raw bytes)
        return scaleEncodeString(junctionName)
    }

    /**
     * SCALE-encode a string for use as a chaincode.
     *
     * SCALE compact encoding for length (simplified for lengths < 64 which we enforce):
     * - Length * 4 as a single byte (since lengths < 64 fit in 6 bits, shifted left by 2)
     *
     * @see [SCALE codec specification](https://docs.substrate.io/reference/scale-codec/)
     */
    private fun scaleEncodeString(value: String): ByteArray {
        val utf8Bytes = value.encodeToByteArray()
        val length = utf8Bytes.size

        // We only support lengths < 64 for single-byte compact encoding
        // This is enforced by our 32-byte chaincode limit anyway
        require(length < 64) {
            "Junction name too long for compact SCALE encoding: $length bytes"
        }

        // Compact encoding: length * 4 (left shift by 2, low 2 bits = 0 for single-byte mode)
        val compactLength = (length shl 2).toByte()

        return byteArrayOf(compactLength) + utf8Bytes
    }

    /**
     * Normalize bytes to exactly 32 bytes by zero-padding.
     */
    private fun normalize(bytes: ByteArray): ByteArray {
        return when {
            bytes.size == Junction.CHAINCODE_LENGTH -> bytes
            bytes.size < Junction.CHAINCODE_LENGTH -> bytes + ByteArray(Junction.CHAINCODE_LENGTH - bytes.size)
            else -> error("Cannot normalize ${bytes.size} bytes to ${Junction.CHAINCODE_LENGTH}")
        }
    }

    /**
     * Convert ULong to 8-byte little-endian ByteArray.
     */
    private fun ULong.toLittleEndianBytes(): ByteArray {
        val bytes = ByteArray(8)
        var value = this
        for (i in 0..7) {
            bytes[i] = (value and 0xFFu).toByte()
            value = value shr 8
        }
        return bytes
    }

    /**
     * Check if a character is a valid hex digit.
     */
    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    /**
     * Convert hex string to ByteArray.
     */
    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
