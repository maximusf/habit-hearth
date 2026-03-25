package com.project.habithearth.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

internal object CredentialHasher {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        md.update(password.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun verify(password: String, salt: String, storedHash: String): Boolean {
        return hash(password, salt) == storedHash
    }
}
