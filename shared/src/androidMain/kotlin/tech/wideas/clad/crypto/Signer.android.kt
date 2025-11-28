package tech.wideas.clad.crypto

/**
 * Android implementation of Signer.
 *
 * Note: Full signing implementation will be completed in issue #23 (Transaction Signing Pipeline).
 * This stub provides the expect/actual structure for now.
 */
class AndroidSigner : Signer {

    override fun sign(message: ByteArray, keypair: Keypair): ByteArray {
        // TODO: Implement in issue #23 using Nova SDK's Signer with MultiChainEncryption
        // The Nova SDK requires creating a Nova Keypair object and using MultiChainEncryption.Substrate
        throw NotImplementedError("Signing will be implemented in issue #23")
    }

    override fun verify(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
        keyType: KeyType
    ): Boolean {
        // TODO: Implement in issue #23 using Nova SDK's SignatureVerifier
        throw NotImplementedError("Signature verification will be implemented in issue #23")
    }
}

actual fun createSigner(): Signer = AndroidSigner()
