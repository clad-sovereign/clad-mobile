package tech.wideas.clad.crypto

/**
 * iOS implementation of SS58 address encoding.
 *
 * TODO: Integrate with Nova SubstrateSdk via CocoaPods/cinterop
 * Pod: SubstrateSdk
 * Classes needed: SS58AddressFactory, SS58AddressType
 *
 * Setup required:
 * 1. Add Podfile with: pod 'SubstrateSdk', :git => 'https://github.com/nova-wallet/substrate-sdk-ios.git'
 * 2. Create cinterop .def file for SubstrateSdk
 * 3. Configure cinterop in shared/build.gradle.kts
 */
actual object Ss58 {
    actual fun encode(publicKey: ByteArray, networkPrefix: Short): String {
        // TODO: Use SubstrateSdk's SS58AddressFactory.encode()
        throw NotImplementedError("iOS SS58 encoding requires SubstrateSdk integration")
    }

    actual fun decode(address: String): Pair<ByteArray, Short> {
        // TODO: Use SubstrateSdk's SS58AddressFactory.decode()
        throw NotImplementedError("iOS SS58 decoding requires SubstrateSdk integration")
    }

    actual fun isValid(address: String): Boolean {
        // TODO: Use SubstrateSdk's SS58AddressFactory validation
        throw NotImplementedError("iOS SS58 validation requires SubstrateSdk integration")
    }
}
