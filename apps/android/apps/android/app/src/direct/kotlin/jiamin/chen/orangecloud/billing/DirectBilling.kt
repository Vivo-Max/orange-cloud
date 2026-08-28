package jiamin.chen.orangecloud.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.purchase.EntitlementStore
import jiamin.chen.orangecloud.core.purchase.RedeemOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * direct 风味（非 Play 中国大陆直发）：无商店计费，Pro 走激活码兑换。
 * - 购买：打开 Web 售卖页（付款后成功页 deeplink `orangecloud://redeem` 回填，由 MainActivity 接住）。
 * - 兑换：POST /api/redeem（code + 稳定 install_id），结果落 DataStore；isPro 缓存在本地，
 *   离线保留缓存、联网时复核（被退款撤销则翻 false）。客户端标志可被改，但 OSS 本就免费解锁，
 *   不做重型防破解——在线复核足以尊重退款。
 */
@Singleton
class DirectBillingGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlementStore: EntitlementStore,
) : BillingGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val store = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("orange_cloud_direct")
    }

    override fun connect() {
        scope.launch {
            val prefs = store.data.first()
            entitlementStore.setPro(prefs[KEY_PRO] ?: false) // 先用本地缓存（离线也可用）
            val saved = prefs[KEY_CODE]
            if (!saved.isNullOrBlank()) redeem(saved) // 在线复核：撤销→翻 false，断网→保缓存
        }
    }

    override fun launchPurchase(activity: Activity, planId: String) {
        // 无商店计费：打开 Web 售卖页。付款后成功页用 deeplink 回填激活码。
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BUY_URL))) }
    }

    override suspend fun redeem(code: String): RedeemOutcome {
        val outcome = postRedeem(code)
        when (outcome) {
            RedeemOutcome.SUCCESS -> persist(code = code, pro = true)
            RedeemOutcome.REVOKED ->
                // 仅当被撤销的是本机已保存的码时才下调（避免误输他人已退款码影响自身）
                if (store.data.first()[KEY_CODE] == normalize(code)) persist(pro = false)
            else -> Unit // INVALID / DEVICE_LIMIT / NETWORK：不改动现有状态
        }
        return outcome
    }

    override suspend fun deactivate(): Boolean {
        val code = store.data.first()[KEY_CODE]
        if (code.isNullOrBlank()) {
            // 本地无已保存码：直接清本地 Pro
            store.edit { it[KEY_PRO] = false }
            entitlementStore.setPro(false)
            return true
        }
        val ok = postDeactivate(code, deviceId())
        if (ok) {
            store.edit {
                it[KEY_PRO] = false
                it.remove(KEY_CODE)
            }
            entitlementStore.setPro(false)
        }
        return ok
    }

    private suspend fun postDeactivate(code: String, id: String): Boolean = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(RedeemRequest(normalize(code), id))
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(DEACTIVATE_URL).post(payload).build()
        try {
            http.newCall(req).execute().use { resp ->
                val parsed = resp.body?.string()
                    ?.let { runCatching { json.decodeFromString<RedeemResponse>(it) }.getOrNull() }
                parsed?.ok ?: resp.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun postRedeem(code: String): RedeemOutcome = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(RedeemRequest(normalize(code), deviceId()))
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(REDEEM_URL).post(payload).build()
        try {
            http.newCall(req).execute().use { resp ->
                val parsed = resp.body?.string()
                    ?.let { runCatching { json.decodeFromString<RedeemResponse>(it) }.getOrNull() }
                when (parsed?.reason) {
                    "ok" -> RedeemOutcome.SUCCESS
                    "revoked" -> RedeemOutcome.REVOKED
                    "device_limit" -> RedeemOutcome.DEVICE_LIMIT
                    "not_found", "bad_request" -> RedeemOutcome.INVALID
                    else -> if (parsed?.ok == true) RedeemOutcome.SUCCESS else RedeemOutcome.NETWORK_ERROR
                }
            }
        } catch (_: Exception) {
            RedeemOutcome.NETWORK_ERROR
        }
    }

    private suspend fun persist(code: String? = null, pro: Boolean) {
        store.edit { p ->
            if (code != null) p[KEY_CODE] = normalize(code)
            p[KEY_PRO] = pro
        }
        entitlementStore.setPro(pro)
    }

    /**
     * 设备 ID：优先用 ANDROID_ID（按 签名密钥 + 设备 + 用户 限定，卸载重装不变 → 绑定跟手机走，
     * 仅恢复出厂 / 换签名密钥才变）。极少数设备返回空 / 无效值时回退到本地持久化 UUID（退化为跟安装走）。
     */
    @SuppressLint("HardwareIds")
    private suspend fun deviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c" && androidId.any { it != '0' }) {
            return androidId
        }
        store.data.first()[KEY_INSTALL]?.let { return it }
        val id = UUID.randomUUID().toString()
        store.edit { it[KEY_INSTALL] = id }
        return id
    }

    private fun normalize(code: String) = code.trim().uppercase()

    companion object {
        private const val BUY_URL = "https://90dd.adsl8.workers.dev/zh-Hans/buy"
        private const val REDEEM_URL = "https://90dd.adsl8.workers.dev/api/redeem"
        private const val DEACTIVATE_URL = "https://90dd.adsl8.workers.dev/api/deactivate"
        private val KEY_INSTALL = stringPreferencesKey("install_id")
        private val KEY_CODE = stringPreferencesKey("code")
        private val KEY_PRO = booleanPreferencesKey("pro")
    }
}

@Serializable
private data class RedeemRequest(val code: String, @SerialName("install_id") val installId: String)

@Serializable
private data class RedeemResponse(val ok: Boolean = false, val reason: String? = null)

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {
    @Binds
    abstract fun bindBillingGateway(impl: DirectBillingGateway): BillingGateway
}
