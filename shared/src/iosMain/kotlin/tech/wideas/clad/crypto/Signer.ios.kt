package tech.wideas.clad.crypto

/**
 * iOS implementation of [Signer].
 *
 * This is a stub implementation. Full signing will be completed in issue #23 (Transaction Signing Pipeline).
 *
 * **Required Setup:**
 * - Add to Podfile: `pod 'SubstrateSdk', :git => 'https://github.com/nova-wallet/substrate-sdk-ios.git'`
 * - Configure cinterop in `shared/build.gradle.kts`
 *
 * **Classes needed from SubstrateSdk:**
 * - `IRSigningWrapper` - Message signing
 * - `IRSignatureVerifier` - Signature verification
 *
 * Thread Safety: This class is NOT thread-safe.
 *
 * @see <a href="https://github.com/nova-wallet/substrate-sdk-ios">Nova SubstrateSdk (iOS)</a>
 */
class IOSSigner : Signer {

    override fun sign(message: ByteArray, keypair: Keypair): ByteArray {
        // TODO(#23): Implement using SubstrateSdk's IRSigningWrapper
        throw NotImplementedError("iOS signing requires SubstrateSdk integration (issue #23)")
    }

    override fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean {
        // TODO(#23): Implement using SubstrateSdk's IRSignatureVerifier
        throw NotImplementedError("iOS signature verification requires SubstrateSdk integration (issue #23)")
    }
}

actual fun createSigner(): Signer = IOSSigner()
