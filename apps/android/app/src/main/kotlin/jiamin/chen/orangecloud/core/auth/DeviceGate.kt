package jiamin.chen.orangecloud.core.auth

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备门禁：登录链路经自建 Worker 上报设备 ID 并查询封禁状态。
 * - 机器码取 ANDROID_ID（16 位十六进制，同设备同签名稳定）；
 * - 所有请求 fail-open：网络失败一律放行，避免 Worker 故障误伤正常用户；
 * - 被封禁时 App 只报通用登录失败，不暴露封禁语义。
 */
@Singleton
class DeviceGate @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        /** 节流间隔：5 分钟内复用上次封禁检查结果 */
        const val THROTTLE_MS = 5 * 60 * 1000L

        /** Widevine DRM 的全局 UUID（业界通用标识，非密钥） */
        val WIDEVINE_UUID: java.util.UUID = java.util.UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
    }

    /** 由 OAuth 回调地址推导控制台 API 基址（同一 Worker 域名）。 */
    private val baseUrl: String by lazy {
        val uri = android.net.Uri.parse(OAuthConfig.redirectUri)
        "${uri.scheme}://${uri.host}"
    }

    /**
     * 机器码。优先取 Widevine DRM 设备唯一 ID：
     * - 与 App 签名密钥无关 → 所有构建版本（OSS/direct/CI）在同一台设备上生成同一机器码；
     * - 由设备 DRM/TEE 生成 → 同型号不同设备互不相同；
     * - 卸载重装不变（恢复出厂设置才会重置）。
     * 取 SHA-256 前 16 位十六进制；DRM 不可用的设备回退 ANDROID_ID。
     */
    val deviceId: String by lazy {
        widevineId() ?: androidId()
    }

    private fun sha256hex16(raw: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(raw)
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun widevineId(): String? = runCatching {
        val drm = android.media.MediaDrm(WIDEVINE_UUID)
        try {
            val id = drm.getPropertyByteArray(android.media.MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (id == null || id.isEmpty()) null else sha256hex16(id)
        } finally {
            drm.release()
        }
    }.getOrNull()

    @get:SuppressLint("HardwareIds")
    private fun androidId(): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (raw.isNullOrBlank()) "unknown" else sha256hex16(raw.lowercase().toByteArray())
    }

    // —— 节流缓存：高频调用点（每个 API 请求）不能每次都访问 Worker ——
    private var lastCheckAt = 0L
    private var lastResult = false

    /** 节流版封禁检查：5 分钟内复用上次结果。供 API 请求入口使用。 */
    suspend fun isBannedThrottled(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastCheckAt < THROTTLE_MS) return lastResult
        val result = isBanned()
        lastCheckAt = now
        lastResult = result
        return result
    }

    /** 被封禁返回 true；任何异常（无网/Worker 故障）返回 false 放行。 */
    suspend fun isBanned(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("$baseUrl/api/device/status?id=$deviceId").openConnection() as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.requestMethod = "GET"
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            body.contains("\"banned\":true")
        }.getOrDefault(false)
    }

    /** 登录成功后静默上报设备信息，结果忽略。 */
    suspend fun checkin() = withContext(Dispatchers.IO) {
        runCatching {
            val payload = """{"id":"$deviceId","app":"${BuildConfig.FLAVOR}","version":"${BuildConfig.VERSION_NAME}"}"""
            val conn = URL("$baseUrl/api/device/checkin").openConnection() as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(payload.toByteArray()) }
            conn.responseCode
            conn.disconnect()
        }
        Unit
    }
}
