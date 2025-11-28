package tech.wideas.clad.crypto

/**
 * iOS implementation of Signer.
 *
 * Note: Full signing implementation will be completed in issue #23 (Transaction Signing Pipeline).
 * This stub provides the expect/actual structure for now.
 *
 * TODO: Integrate with Nova SubstrateSdk via CocoaPods/cinterop
 * Pod: SubstrateSdk
 * Classes needed: IRSigningWrapper, IRSignatureVerifier
 */
class IOSSigner : Signer {

    override fun sign(message: ByteArray, keypair: Keypair): ByteArray {
        // TODO: Implement in issue #23 using SubstrateSdk's IRSigningWrapper
        throw NotImplementedError("iOS signing requires SubstrateSdk integration (issue #23)")
    }

    override fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean {
        // TODO: Implement in issue #23 using SubstrateSdk's IRSignatureVerifier
        throw NotImplementedError("iOS signature verification requires SubstrateSdk integration (issue #23)")
    }
}

actual fun createSigner(): Signer = IOSSigner()
