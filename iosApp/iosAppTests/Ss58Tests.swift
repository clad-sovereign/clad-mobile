import XCTest
import Shared

/// Tests for SS58 address encoding iOS implementation.
///
/// These tests verify the iOS implementation using NovaCrypto's SS58AddressFactory
/// for SS58 address encoding and decoding.
final class Ss58Tests: XCTestCase {

    private var provider: MnemonicProvider!

    override func setUp() {
        super.setUp()
        provider = Mnemonic_iosKt.createMnemonicProvider()
    }

    override func tearDown() {
        provider = nil
        super.tearDown()
    }

    // MARK: - Encoding Tests

    func testEncodeProducesValidSs58Address() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: ""
        )

        let address = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertFalse(address.isEmpty, "Address should not be blank")
        XCTAssertGreaterThan(address.count, 40, "SS58 address should be longer than 40 characters")
    }

    // MARK: - Decoding Tests

    func testDecodeExtractsCorrectPublicKeyAndPrefix() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: ""
        )

        let address = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )
        let decoded = Ss58.shared.decode(address: address)

        XCTAssertTrue(byteArraysEqual(decoded.first!, keypair.publicKey), "Decoded public key should match original")
        XCTAssertEqual(
            decoded.second?.int16Value,
            NetworkPrefix.shared.GENERIC_SUBSTRATE,
            "Decoded prefix should match original"
        )
    }

    func testEncodeAndDecodeRoundtripWorks() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: ""
        )

        let address = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )
        let decoded = Ss58.shared.decode(address: address)

        let reEncodedAddress = Ss58.shared.encode(
            publicKey: decoded.first!,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )
        XCTAssertEqual(address, reEncodedAddress, "Re-encoded address should match original")
    }

    // MARK: - Validation Tests

    func testIsValidReturnsTrueForValidAddress() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: ""
        )
        let address = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertTrue(Ss58.shared.isValid(address: address), "Encoded address should be valid")
    }

    func testIsValidReturnsFalseForInvalidAddress() {
        XCTAssertFalse(Ss58.shared.isValid(address: "not-a-valid-address"), "Garbage should be invalid")
        XCTAssertFalse(Ss58.shared.isValid(address: ""), "Empty string should be invalid")
        XCTAssertFalse(Ss58.shared.isValid(address: "5"), "Too short should be invalid")
    }

    // MARK: - Keypair Convenience Method Tests

    func testKeypairToSs58AddressConvenienceMethodWorks() {
        let mnemonic = provider.generate(wordCount: .words12)
        let keypair = provider.toKeypair(
            mnemonic: mnemonic,
            passphrase: "",
            derivationPath: ""
        )

        let address = keypair.toSs58Address(networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE)
        let directAddress = Ss58.shared.encode(
            publicKey: keypair.publicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )

        XCTAssertEqual(address, directAddress, "Convenience method should match direct call")
    }

    // MARK: - Known Test Vector Tests

    func testKnownTestVectorAliceWellKnownAddress() {
        // Alice's well-known SS58 address for generic Substrate (prefix 42)
        // Public key: 0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d
        let alicePublicKeyHex = "d43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d"
        let alicePublicKey = hexToKotlinByteArray(alicePublicKeyHex)
        let expectedAliceAddress = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        let encodedAddress = Ss58.shared.encode(
            publicKey: alicePublicKey,
            networkPrefix: NetworkPrefix.shared.GENERIC_SUBSTRATE
        )
        XCTAssertEqual(encodedAddress, expectedAliceAddress, "Alice's address should match test vector")

        let decoded = Ss58.shared.decode(address: expectedAliceAddress)
        XCTAssertTrue(byteArraysEqual(decoded.first!, alicePublicKey), "Decoded key should match Alice's public key")
        XCTAssertEqual(
            decoded.second?.int16Value,
            NetworkPrefix.shared.GENERIC_SUBSTRATE,
            "Prefix should be GENERIC_SUBSTRATE (42)"
        )
    }

    // MARK: - Helper Methods

    private func hexToKotlinByteArray(_ hex: String) -> KotlinByteArray {
        let cleanHex = hex.hasPrefix("0x") ? String(hex.dropFirst(2)) : hex
        let bytes = KotlinByteArray(size: Int32(cleanHex.count / 2))

        var index = cleanHex.startIndex
        for i in 0..<(cleanHex.count / 2) {
            let nextIndex = cleanHex.index(index, offsetBy: 2)
            let byteString = String(cleanHex[index..<nextIndex])
            if let byte = UInt8(byteString, radix: 16) {
                bytes.set(index: Int32(i), value: Int8(bitPattern: byte))
            }
            index = nextIndex
        }

        return bytes
    }

    private func byteArraysEqual(_ a: KotlinByteArray, _ b: KotlinByteArray) -> Bool {
        guard a.size == b.size else { return false }
        for i in 0..<a.size {
            if a.get(index: i) != b.get(index: i) {
                return false
            }
        }
        return true
    }
}
