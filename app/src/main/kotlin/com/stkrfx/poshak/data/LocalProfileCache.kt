package com.stkrfx.poshak.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted, session-scoped profile snapshot. The snapshot is only a rendering cache;
 * every protected request still validates the session with the backend.
 */
class LocalProfileCache(context: Context) {
    private val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME).apply {
        mkdirs()
    }

    @Synchronized
    fun save(sessionId: String, profile: EmployeeProfile) {
        if (sessionId.isBlank() || profile.id.isBlank()) return
        runCatching {
            val plaintext = JSONObject()
                .put("session_id", sessionId)
                .put("profile", profile.toJson())
                .toString()
                .toByteArray(Charsets.UTF_8)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val packed = cipher.iv + cipher.doFinal(plaintext)

            val target = fileFor(sessionId)
            val temporary = File(directory, "${target.name}.tmp")
            temporary.writeBytes(packed)
            if (!temporary.renameTo(target)) {
                target.writeBytes(packed)
                temporary.delete()
            }
        }
    }

    @Synchronized
    fun load(sessionId: String): EmployeeProfile? {
        if (sessionId.isBlank()) return null
        val file = fileFor(sessionId)
        if (!file.isFile) return null
        return try {
            val packed = file.readBytes()
            require(packed.size > IV_LENGTH) { "Profile cache is incomplete." }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, packed.copyOfRange(0, IV_LENGTH)),
            )
            val root = JSONObject(String(cipher.doFinal(packed.copyOfRange(IV_LENGTH, packed.size)), Charsets.UTF_8))
            require(root.optString("session_id") == sessionId) { "Profile cache belongs to another session." }
            root.optJSONObject("profile")?.toProfile()
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    @Synchronized
    fun clear(sessionId: String) {
        if (sessionId.isNotBlank()) fileFor(sessionId).delete()
    }

    @Synchronized
    fun clearAll() {
        directory.listFiles()?.forEach { if (it.isFile) it.delete() }
    }

    private fun fileFor(sessionId: String): File = File(directory, "profile-${sha256(sessionId)}.enc")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun EmployeeProfile.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("username", username)
        .put("mobile_number", mobileNumber)
        .put("gender", gender)
        .putNullable("email", email)
        .putNullable("dob", dateOfBirth)
        .put("is_dob_locked", isDateOfBirthLocked)
        .putNullable("address", address)
        .putNullable("avatar_url", avatarUrl)
        .put("can_edit_profile", canEditProfile)
        .put("can_upload_documents", canUploadDocuments)
        .put("is_blocked", isBlocked)
        .putNullable("blocked_reason", blockedReason)

    private fun JSONObject.toProfile(): EmployeeProfile = EmployeeProfile(
        id = optString("id"),
        name = optString("name", "Employee"),
        username = optString("username"),
        mobileNumber = optString("mobile_number"),
        gender = optString("gender"),
        email = nullableString("email"),
        dateOfBirth = nullableString("dob"),
        isDateOfBirthLocked = optBoolean("is_dob_locked", false),
        address = nullableString("address"),
        avatarUrl = nullableString("avatar_url"),
        canEditProfile = optBoolean("can_edit_profile", true),
        canUploadDocuments = optBoolean("can_upload_documents", true),
        isBlocked = optBoolean("is_blocked", false),
        blockedReason = nullableString("blocked_reason"),
    )

    private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.nullableString(key: String): String? =
        optString(key).takeUnless { it.isBlank() || it == "null" }

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
        const val DIRECTORY_NAME = "profile-cache"
        const val KEY_ALIAS = "poshak.profile.cache.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
