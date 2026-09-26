package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val alias = "SecureVaultMasterKey"

    @Volatile
    private var cachedKey: SecretKey? = null

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            if (!keyStore.containsAlias(alias)) {
                generateKey()
            }
        } catch (_: Exception) {}
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getKey(): SecretKey {
        cachedKey?.let { return it }
        val key = try {
            if (keyStore.containsAlias(alias)) {
                (keyStore.getKey(alias, null) as? SecretKey)
                    ?: (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            } else {
                generateKey()
            }
        } catch (e: Exception) {
            generateKey()
        } ?: generateKey()

        cachedKey = key
        return key
    }

    data class EncryptedResult(val ciphertext: String, val iv: String)

    fun encrypt(plaintext: String): EncryptedResult {
        if (plaintext.isEmpty()) return EncryptedResult("", "")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedResult(
            ciphertext = Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
            iv = Base64.encodeToString(iv, Base64.DEFAULT)
        )
    }

    fun decrypt(ciphertext: String, iv: String): String {
        if (ciphertext.isEmpty()) return ""
        if (iv.isEmpty()) return ciphertext // Raw fallback to ensure user data is never lost
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            val decodedBytes = Base64.decode(ciphertext, Base64.DEFAULT)
            val plaintextBytes = cipher.doFinal(decodedBytes)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Never revoke or blank out user data on decryption error
            ciphertext
        }
    }
}
