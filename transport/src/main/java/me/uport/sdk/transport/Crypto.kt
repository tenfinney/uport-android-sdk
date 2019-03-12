package me.uport.sdk.transport

import me.uport.knacl.nacl
import me.uport.sdk.core.decodeBase64
import me.uport.sdk.core.padBase64
import me.uport.sdk.core.toBase64
import me.uport.sdk.transport.EncryptedMessage.Companion.ASYNC_ENC_ALGORITHM

/**
 * This class exposes methods to encrypt and decrypt messages according to the uPort spec at
 * https://github.com/uport-project/specs/blob/develop/messages/encryption.md
 */
object Crypto {

    /**
     * Calculates the publicKey usable for encryption corresponding to the given [secretKey]
     *
     * @return the base64 encoded public key
     */
    fun getEncryptionPublicKey(secretKey: ByteArray): String {
        val (pk, _) = nacl.box.keyPairFromSecretKey(secretKey)
        return pk.toBase64().padBase64()
    }

    /**
     *  Encrypts a message
     *
     *  @param message the plaintext message to be encrypted
     *  @param boxPub  the public encryption key of the receiver, encoded as a base64 [String]
     *  @return an [EncryptedMessage] containing a `version`, `nonce`, `ephemPublicKey` and `ciphertext`
     */
    fun encryptMessage(message: String, boxPub: String): EncryptedMessage {

        val (publicKey, secretKey) = nacl.box.keyPair()
        val nonce = nacl.randomBytes(nacl.crypto_box_NONCEBYTES)
        val padded = message.padToBlock()
        val ciphertext = nacl.box.seal(padded, nonce, boxPub.decodeBase64(), secretKey)
        return EncryptedMessage(
                version = ASYNC_ENC_ALGORITHM,
                nonce = nonce.toBase64(),
                ephemPublicKey = publicKey.toBase64(),
                ciphertext = ciphertext.toBase64())
    }


    /**
     *  Decrypts a message
     *
     *  @param encrypted The [EncryptedMessage] containing `version`, `nonce`, `ephemPublicKey` and `ciphertext`
     *  @param secretKey The secret key as a [ByteArray]
     *  @return The decrypted plaintext [String]
     */
    fun decryptMessage(encrypted: EncryptedMessage, secretKey: ByteArray): String {
        if (encrypted.version != ASYNC_ENC_ALGORITHM) throw IllegalArgumentException("Unsupported encryption algorithm: ${encrypted.version}")
        if (encrypted.ciphertext.isBlank() || encrypted.nonce.isBlank() || encrypted.ephemPublicKey.isBlank()) throw IllegalArgumentException("Invalid encrypted message")
        val decrypted = nacl.box.open(
                encrypted.ciphertext.decodeBase64(),
                encrypted.nonce.decodeBase64(),
                encrypted.ephemPublicKey.decodeBase64(),
                secretKey) ?: throw DecryptionError("Could not decrypt message")
        return decrypted.unpadFromBlock()
    }

    /**
     * Constructs an exception that represents a failure to decrypt a message.
     */
    class DecryptionError(message: String) : IllegalArgumentException(message)

}