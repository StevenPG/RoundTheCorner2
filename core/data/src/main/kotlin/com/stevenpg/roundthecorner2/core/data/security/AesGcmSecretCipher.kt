package com.stevenpg.roundthecorner2.core.data.security

import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM (authenticated encryption: tampering is detected, not just hidden).
 *
 * Stored format: `enc:v1:` + Base64(12-byte IV ‖ ciphertext ‖ 16-byte tag).
 * The version tag lets a future scheme coexist with old rows. Values without
 * the prefix are treated as legacy plaintext and returned unchanged, so rows
 * written before encryption existed still read correctly.
 *
 * The key comes from [keyProvider] - the Android Keystore in production, a
 * plain software key in JVM tests.
 */
class AesGcmSecretCipher(private val keyProvider: () -> SecretKey) : SecretCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // No IV passed: the provider generates a fresh random one (required by Keystore keys).
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val iv = cipher.iv
        check(iv.size == IV_LENGTH) { "Unexpected IV length ${iv.size}" }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored // Legacy plaintext.
        return try {
            val bytes = Base64.getDecoder().decode(stored.removePrefix(PREFIX))
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_LENGTH))
            String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw SecretUnreadableException(e)
        } catch (e: IllegalArgumentException) { // Bad Base64.
            throw SecretUnreadableException(e)
        }
    }

    companion object {
        const val PREFIX = "enc:v1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}
