package com.stevenpg.roundthecorner2.core.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Loads (or creates on first use) an AES-256 key that lives in the Android
 * Keystore. The key material never enters app memory and can't be extracted,
 * even on a rooted device with hardware-backed keystores.
 *
 * No user-authentication requirement: the monitoring service must decrypt
 * while the phone is locked in a pocket.
 */
internal object KeystoreKeyProvider {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "recipient_secrets_v1"

    @Synchronized
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
