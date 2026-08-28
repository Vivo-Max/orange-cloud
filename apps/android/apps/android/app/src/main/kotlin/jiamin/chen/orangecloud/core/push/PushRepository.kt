package jiamin.chen.orangecloud.core.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.BuildConfig
import jiamin.chen.orangecloud.core.logging.AppLog
import jiamin.chen.orangecloud.data.local.PushMessageDao
import jiamin.chen.orangecloud.data.local.PushMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 推送中心：向 orange-cloud-push 中继注册本机 FCM 令牌（不碰 CF token），换取永久 device_key 端点。
 * Firebase 未配置（oss / 未填 FCM 配置）时优雅降级——enable() 返回失败，App 不崩。
 */
@Singleton
class PushRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PushPrefs,
    private val dao: PushMessageDao,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    val serverUrl: Flow<String> = prefs.serverUrl
    val deviceKey: Flow<String?> = prefs.deviceKey
    val enabled: Flow<Boolean> = prefs.enabled

    // 收件箱（Room 单一可信源；FCM 服务写、UI 读）。
    val inbox: Flow<List<PushMessageEntity>> = dao.observe()

    suspend fun record(title: String?, body: String?, group: String?, url: String?) {
        dao.insert(PushMessageEntity(title = title, body = body, group = group, url = url, receivedAt = System.currentTimeMillis()))
    }

    suspend fun clearInbox() = dao.clear()

    suspend fun deleteMessage(id: Long) = dao.delete(id)

    /** Firebase 是否可用（决定推送能否启用）。 */
    val isFirebaseAvailable: Boolean
        get() = BuildConfig.FCM_PROJECT_ID.isNotBlank() && FirebaseApp.getApps(context).isNotEmpty()

    suspend fun endpoint(): String? {
        val key = prefs.deviceKeyNow() ?: return null
        return prefs.serverUrlNow().trimEnd('/') + "/" + key
    }

    suspend fun setServer(url: String) = prefs.setServer(url)

    /** 启用：取 FCM 令牌 → 注册 → 存 device_key，返回端点 URL。 */
    suspend fun enable(): Result<String> {
        if (!isFirebaseAvailable) return Result.failure(IllegalStateException("FCM unavailable"))
        return runCatching {
            val token = fcmToken()
            val key = register(token, prefs.deviceKeyNow())
            prefs.setDeviceKey(key)
            prefs.setEnabled(true)
            prefs.serverUrlNow().trimEnd('/') + "/" + key
        }.onFailure { AppLog.app.error("push enable failed: ${it.message}") }
    }

    suspend fun disable() = prefs.setEnabled(false)

    /** FCM 令牌轮换：已注册过则用同一 device_key 更新令牌。 */
    suspend fun onTokenRefreshed(token: String) {
        if (prefs.deviceKeyNow() == null) return
        runCatching {
            val key = register(token, prefs.deviceKeyNow())
            prefs.setDeviceKey(key)
        }.onFailure { AppLog.app.error("push token refresh failed: ${it.message}") }
    }

    /** 发一条测试推送到本机端点。 */
    suspend fun testPush(title: String, body: String): Result<Unit> = runCatching {
        val key = prefs.deviceKeyNow() ?: error("not registered")
        val server = prefs.serverUrlNow().trimEnd('/')
        val payload = json.encodeToString(PushBody.serializer(), PushBody(title = title, body = body, group = "test"))
        postJson("$server/$key", payload)
        Unit
    }.onFailure { AppLog.app.error("push test failed: ${it.message}") }

    private suspend fun register(token: String, existing: String?): String {
        val server = prefs.serverUrlNow().trimEnd('/')
        val payload = json.encodeToString(
            RegisterRequest.serializer(),
            RegisterRequest(device_token = token, platform = "android", build = BuildConfig.VERSION_NAME, device_key = existing),
        )
        val bytes = postJson("$server/api/register", payload)
        val decoded = json.decodeFromString(RegisterResponse.serializer(), bytes.decodeToString())
        return decoded.device_key ?: decoded.key ?: error("no device_key in response")
    }

    private suspend fun postJson(url: String, bodyJson: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            if (resp.code !in 200..299) throw IOException("HTTP ${resp.code}")
            bytes
        }
    }

    /** 取当前 FCM 注册令牌（Task → 协程）。 */
    private suspend fun fcmToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    @Serializable
    private data class RegisterRequest(
        val device_token: String,
        val platform: String,
        val build: String,
        val device_key: String? = null,
    )

    @Serializable
    private data class RegisterResponse(val device_key: String? = null, val key: String? = null)

    @Serializable
    private data class PushBody(val title: String, val body: String, val group: String? = null)
}
