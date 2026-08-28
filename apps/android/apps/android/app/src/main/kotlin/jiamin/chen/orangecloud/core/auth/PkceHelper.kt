package jiamin.chen.orangecloud.core.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PKCE (RFC 7636)：code_verifier 随机生成，code_challenge = BASE64URL(SHA256(verifier))，method 固定 S256。
 * 与 iOS Core/Auth/PKCEHelper.swift 直译对应。
 */
object PkceHelper {
    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    /** 32 字节随机数 → 43 字符 base64url（无 padding） */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, FLAGS)
    }

    /** code_challenge = BASE64URL(SHA256(code_verifier)) */
    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, FLAGS)
    }
}
