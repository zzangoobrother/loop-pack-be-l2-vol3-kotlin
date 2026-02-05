package com.loopers.infrastructure.user

import com.loopers.domain.user.PasswordEncoder
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Component
class Pbkdf2PasswordEncoder : PasswordEncoder {

    override fun encode(rawPassword: String): String {
        val salt = generateSalt()
        val hash = hash(rawPassword, salt)
        return "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        val (saltBase64, hashBase64) = encodedPassword.split(":").takeIf { it.size == 2 } ?: return false
        val salt = Base64.getDecoder().decode(saltBase64)
        val hash = Base64.getDecoder().decode(hashBase64)
        val inputHash = hash(rawPassword, salt)
        return hash.contentEquals(inputHash)
    }

    private fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
