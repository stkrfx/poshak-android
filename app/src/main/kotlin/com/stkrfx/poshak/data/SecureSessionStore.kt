package com.stkrfx.poshak.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only rotating session credentials, encrypted with a key held by Android Keystore. */
class SecureSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun save(credentials: SessionCredentials) {
        val plaintext = JSONObject()
            .put("access_token", credentials.accessToken)
            .put("refresh_token", credentials.refreshToken)
            .put("session_id", credentials.sessionId)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plaintext)
        val packed = cipher.iv + encrypted
        preferences.edit()
            .putString(CREDENTIALS_KEY, Base64.encodeToString(packed, Base64.NO_WRAP))
            .apply()
    }

    @Synchronized
    fun load(): SessionCredentials? {
        val encoded = preferences.getString(CREDENTIALS_KEY, null) ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > GCM_IV_LENGTH_BYTES) { "Invalid encrypted session data." }
            val iv = packed.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = packed.copyOfRange(GCM_IV_LENGTH_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val json = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            val sessionId = json.optString("session_id")
            if (accessToken.isBlank() || refreshToken.isBlank() || sessionId.isBlank()) {
                clear()
                null
            } else {
                SessionCredentials(accessToken, refreshToken, sessionId)
            }
        } catch (_: Exception) {
            // A corrupted or undecryptable local session must never be treated as authenticated.
            clear()
            null
        }
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(CREDENTIALS_KEY).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "poshak_session_secure"
        const val CREDENTIALS_KEY = "encrypted_credentials"
        const val KEY_ALIAS = "poshak.session.credentials.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
