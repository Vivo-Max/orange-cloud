package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.data.model.StatusPageIncident
import jiamin.chen.orangecloud.data.model.StatusPageIncidentList
import jiamin.chen.orangecloud.data.model.StatusPageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloudflare 官方状态页（cloudflarestatus.com，公开 API，无鉴权、非 CF 信封，不走 CfApiClient）。
 */
@Singleton
class StatusRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun summary(): StatusPageSummary =
        json.decodeFromString(StatusPageSummary.serializer(), get("summary.json"))

    /** 近期事件（含已解决，Statuspage 返回最近 50 条）。 */
    suspend fun recentIncidents(): List<StatusPageIncident> =
        json.decodeFromString(StatusPageIncidentList.serializer(), get("incidents.json")).incidents

    private suspend fun get(path: String): String {
        val request = Request.Builder().url("$BASE/$path").build()
        return withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
        }
    }

    private companion object {
        const val BASE = "https://www.cloudflarestatus.com/api/v2"
    }
}
