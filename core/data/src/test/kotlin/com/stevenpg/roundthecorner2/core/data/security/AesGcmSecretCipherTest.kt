package com.stevenpg.roundthecorner2.core.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class AesGcmSecretCipherTest {

    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val key = newKey()
    private val cipher = AesGcmSecretCipher { key }
    private val secret = "https://hooks.slack.com/services/T000/B000/XXXXXXXX"

    @Test
    fun `round trips and never stores plaintext`() {
        val stored = cipher.encrypt(secret)
        assertTrue(stored.startsWith(AesGcmSecretCipher.PREFIX))
        assertFalse(stored.contains("hooks.slack.com"))
        assertEquals(secret, cipher.decrypt(stored))
    }

    @Test
    fun `same plaintext encrypts differently each time (random IV)`() {
        assertNotEquals(cipher.encrypt(secret), cipher.encrypt(secret))
    }

    @Test
    fun `legacy plaintext rows pass through`() {
        assertEquals("+15555550100", cipher.decrypt("+15555550100"))
    }

    @Test(expected = SecretUnreadableException::class)
    fun `a different key (restored backup on a new phone) is detected`() {
        val stored = cipher.encrypt(secret)
        AesGcmSecretCipher { newKey() }.decrypt(stored)
    }

    @Test(expected = SecretUnreadableException::class)
    fun `tampering is detected`() {
        val stored = cipher.encrypt(secret)
        val tampered = stored.dropLast(4) + if (stored.endsWith("AAAA")) "BBBB" else "AAAA"
        cipher.decrypt(tampered)
    }
}
