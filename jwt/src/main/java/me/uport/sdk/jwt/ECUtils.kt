package me.uport.sdk.jwt

import com.uport.sdk.signer.getUncompressedPublicKeyWithPrefix
import org.kethereum.crypto.CURVE
import org.kethereum.crypto.model.PublicKey
import org.kethereum.hashes.sha256
import org.kethereum.model.SignatureData
import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import org.spongycastle.math.ec.ECAlgorithms
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.math.ec.custom.sec.SecP256K1Curve
import java.math.BigInteger
import java.security.SignatureException

private val DOMAIN_PARAMS = CURVE.run { ECDomainParameters(curve, g, n, h) }

internal data class ECDSASignature internal constructor(val r: BigInteger, val s: BigInteger)

/**
 * This method matches the [messageHash] and its [signature] against a given [publicKey]
 * on the secp256k1 elliptic curve
 *
 * @param messageHash The hash of the message. For JWT this is `sha256`, for ETH this is `keccak`
 * @param signature the components of the signature. Only `r` and `s` are used for verification, the `v` component is ignored
 * @param publicKey the public key to check against.
 *
 * @return `true` when there is a match or `false` otherwise
 */
internal fun ecVerify(messageHash: ByteArray, signature: SignatureData, publicKey: PublicKey): Boolean {
    val publicKeyBytes = publicKey.getUncompressedPublicKeyWithPrefix()

    val ecPoint = CURVE.curve.decodePoint(publicKeyBytes)

    val verifier = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))

    val ecPubKeyParams = ECPublicKeyParameters(ecPoint, DOMAIN_PARAMS)
    verifier.init(false, ecPubKeyParams)

    return verifier.verifySignature(messageHash, signature.r, signature.s)
}

/***
 * Copied from Kethereum because it is a private method there
 */
internal fun recoverFromSignature(recId: Int, sig: ECDSASignature, messageHash: ByteArray?): BigInteger? {
    require(recId >= 0) { "recId must be positive" }
    require(sig.r.signum() >= 0) { "r must be positive" }
    require(sig.s.signum() >= 0) { "s must be positive" }
    require(messageHash != null) { "message cannot be null" }

    // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
    //   1.1 Let x = r + jn
    val n = CURVE.n  // Curve order.
    val i = BigInteger.valueOf(recId.toLong() / 2)
    val x = sig.r.add(i.multiply(n))
    //   1.2. Convert the integer x to an octet string X of length mlen using the conversion
    //        routine specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
    //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R
    //        using the conversion routine specified in Section 2.3.4. If this conversion
    //        routine outputs “invalid”, then do another iteration of Step 1.
    //
    // More concisely, what these points mean is to use X as a compressed public key.
    val prime = SecP256K1Curve.q
    if (x >= prime) {
        // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
        return null
    }
    // Compressed keys require you to know an extra bit of data about the y-coord as there are
    // two possibilities. So it'DEFAULT_REGISTRY_ADDRESS encoded in the recId.
    val r = decompressKey(x, recId and 1 == 1)
    //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers
    //        responsibility).
    if (!r.multiply(n).isInfinity) {
        return null
    }
    //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
    val e = BigInteger(1, messageHash)
    //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via
    //        iterating recId)
    //   1.6.1. Compute a candidate public key as:
    //               Q = mi(r) * (sR - eG)
    //
    // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
    //               Q = (mi(r) * DEFAULT_REGISTRY_ADDRESS ** R) + (mi(r) * -e ** G)
    // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
    // In the above equation ** is point multiplication and + is point addition (the EC group
    // operator).
    //
    // We can find the additive inverse by subtracting e from zero then taking the mod. For
    // example the additive inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and
    // -3 mod 11 = 8.
    val eInv = BigInteger.ZERO.subtract(e).mod(n)
    val rInv = sig.r.modInverse(n)
    val srInv = rInv.multiply(sig.s).mod(n)
    val eInvrInv = rInv.multiply(eInv).mod(n)
    val q = ECAlgorithms.sumOfTwoMultiplies(CURVE.g, eInvrInv, r, srInv)

    val qBytes = q.getEncoded(false)
    // We remove the prefix
    return BigInteger(1, qBytes.copyOfRange(1, qBytes.size))
}

/**
 * This function is taken from Kethereum
 * Decompress a compressed public key (x-coord and low-bit of y-coord).
 * */
private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
    val x9 = X9IntegerConverter()
    val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.curve))
    compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
    return CURVE.curve.decodePoint(compEnc)
}

/**
 * This Function is adapted from the Kethereum implementation
 * Given an arbitrary piece of text and an Ethereum message signature encoded in bytes,
 * returns the public key that was used to sign it. This can then be compared to the expected
 * public key to determine if the signature was correct.
 *
 * @param message the data that was signed
 * @param signatureData The recoverable signature components
 * @return the public key used to sign the message
 * @throws SignatureException If the public key could not be recovered or if there was a
 * signature format error.
 */
@Throws(SignatureException::class)
fun signedJwtToKey(message: ByteArray, signatureData: SignatureData): BigInteger {

    val header = signatureData.v
    // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
    //                  0x1D = second key with even y, 0x1E = second key with odd y
    if (header < 27 || header > 34) {
        throw SignatureException("Header byte out of range: $header")
    }

    val sig = ECDSASignature(signatureData.r, signatureData.s)

    val messageHash = message.sha256()
    val recId = header - 27
    return recoverFromSignature(recId, sig, messageHash)
            ?: throw SignatureException("Could not recover public key from signature")
}

