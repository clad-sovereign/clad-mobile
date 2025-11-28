package tech.wideas.clad.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * iOS-specific extension functions for converting between Kotlin ByteArray and NSData.
 *
 * These utilities are used by crypto modules that interface with Objective-C/Swift
 * libraries via Kotlin/Native cinterop.
 */

/**
 * Converts NSData to Kotlin ByteArray.
 *
 * @return A new ByteArray containing the bytes from this NSData, or an empty array if NSData is empty.
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)

    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

/**
 * Converts Kotlin ByteArray to NSData.
 *
 * @return A new NSData containing the bytes from this array, or an empty NSData if array is empty.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (this.isEmpty()) return NSData()

    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}
