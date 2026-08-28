package jiamin.chen.orangecloud.core.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateStore by preferencesDataStore(name = "orange_cloud_update")

/** 一个可下载的新版本。 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val note: String?,
    val url: String,
    /** 本机低于清单的 `minVersionCode`：强制更新，弹窗不可取消、不提供「忽略此版本」。 */
    val forced: Boolean,
)

/**
 * 自助更新检查。
 *
 * sideload 的 `direct` 包没有应用商店推送更新，必须由 App 自己轮询官网的版本清单。
 * App Store / Google Play 走商店更新、`oss` 由用户自编译，所以实际只有 `direct` 风味会触发
 * （守卫在 [jiamin.chen.orangecloud.ui.update.UpdateViewModel] 的 `IS_DIRECT` 判断里）。
 *
 * 拉取 `latest.json`：若其 `versionCode` 比本机 [BuildConfig.VERSION_CODE] 新则返回更新信息。
 * - `minVersionCode`：本机低于它 → `forced = true`（关键安全更新，强制）。
 * - 「忽略此版本」记在 DataStore，**仅对非强制更新生效**；更高 `versionCode` 仍会再次提示。
 * 任何异常（离线 / 超时 / 解析失败）一律静默吞掉返回 `null`——更新检查绝不该打断正常使用。
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val http = OkHttpClient()

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            http.newCall(Request.Builder().url(LATEST_URL).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val obj = JSONObject(body)
                val current = BuildConfig.VERSION_CODE
                val latest = obj.getInt("versionCode")
                if (latest <= current) return@use null
                val forced = current < obj.optInt("minVersionCode", 0)
                // 非强制更新才尊重「忽略此版本」；强制更新无视忽略，必弹。
                if (!forced && latest <= dismissedVersion()) return@use null
                UpdateInfo(
                    versionCode = latest,
                    versionName = obj.getString("versionName"),
                    note = obj.optString("note").takeIf { it.isNotBlank() },
                    url = obj.getString("url"),
                    forced = forced,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 记住「忽略此版本」：后续不再为该 versionCode 弹窗（更高版本仍提示，强制更新仍弹）。 */
    suspend fun skip(versionCode: Int) {
        context.updateStore.edit { it[KEY_DISMISSED] = versionCode }
    }

    private suspend fun dismissedVersion(): Int =
        context.updateStore.data.first()[KEY_DISMISSED] ?: 0

    companion object {
        private const val LATEST_URL = "https://oss.omail.us.kg/android/latest.json"
        private val KEY_DISMISSED = intPreferencesKey("dismissed_version")
    }
}
