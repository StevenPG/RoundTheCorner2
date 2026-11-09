package com.stevenpg.roundthecorner2.core.data.security

/**
 * Encrypts small secrets (webhook URLs, phone numbers) before they're stored.
 */
interface SecretCipher {
    fun encrypt(plaintext: String): String

    /**
     * @throws SecretUnreadableException if the value can't be decrypted - e.g.
     *   the database was restored onto a new phone, whose Keystore doesn't have
     *   the key (Keystore keys are never backed up, by design).
     */
    fun decrypt(stored: String): String
}

class SecretUnreadableException(cause: Throwable) : Exception("Stored secret could not be decrypted", cause)
