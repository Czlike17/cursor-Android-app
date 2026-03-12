package com.example.myapp.util

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 加密工具类
 */
object EncryptUtils {

    /**
     * MD5 加密
     */
    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * SHA-256 加密
     */
    fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Base64 编码
     */
    fun base64Encode(input: String): String {
        return try {
            Base64.getEncoder().encodeToString(input.toByteArray())
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Base64 解码
     */
    fun base64Decode(input: String): String {
        return try {
            String(Base64.getDecoder().decode(input))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * AES 加密
     */
    fun aesEncrypt(data: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * AES 解密
     */
    fun aesDecrypt(data: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decoded = Base64.getDecoder().decode(data)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            ""
        }
    }
}

