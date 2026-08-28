package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// MARK: - 账号审计日志（Audit Logs v2）。对应 iOS AuditLogModels。
// GET /accounts/{id}/logs/audit?since=&before=&cursor=&limit=&direction=
// 仅需 account-settings.read（账号模块必选 scope），无需新增授权。
// v2 的 result_info.count 是字符串——我们只声明 cursor，靠 ignoreUnknownKeys 跳过 count，避免与 Int 冲突。

@Serializable
data class AuditLogEntry(
    val id: String? = null,
    val account: AuditLogScopeRef? = null,
    val action: AuditLogAction? = null,
    val actor: AuditLogActor? = null,
    val raw: AuditLogRaw? = null,
    val resource: AuditLogResource? = null,
    val zone: AuditLogScopeRef? = null,
) {
    /** action.time 解析为毫秒时间戳（ISO8601 / RFC3339），失败返回 null。 */
    val timestampMillis: Long?
        get() = action?.time?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    /** action.result == "success" → true；明确失败 → false；缺省 → null。 */
    val succeeded: Boolean?
        get() = action?.result?.lowercase()?.takeIf { it.isNotEmpty() }?.let { it == "success" }
}

@Serializable
data class AuditLogScopeRef(val id: String? = null, val name: String? = null)

@Serializable
data class AuditLogAction(
    val description: String? = null,
    val result: String? = null,
    val time: String? = null,
    val type: String? = null,
)

@Serializable
data class AuditLogActor(
    val id: String? = null,
    val context: String? = null, // api_key / api_token / dash / oauth / origin_ca_key
    val email: String? = null,
    val type: String? = null, // account / cloudflare_admin / system / user
    @SerialName("ip_address") val ipAddress: String? = null,
)

@Serializable
data class AuditLogRaw(
    val method: String? = null,
    val uri: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
)

@Serializable
data class AuditLogResource(
    val id: String? = null,
    val product: String? = null,
    val type: String? = null,
)

@Serializable
data class AuditResultInfo(
    val cursor: String? = null,
    /**
     * 仅资源变更历史返回：exact / approximate / unavailable。
     * approximate 表示没拿到资源 URI、靠其它字段近似匹配，结果可能混入无关条目——必须告诉用户。
     */
    @SerialName("history_status") val historyStatus: String? = null,
)

@Serializable
data class AuditLogPage(
    val result: List<AuditLogEntry>? = null,
    val success: Boolean = false,
    val errors: List<jiamin.chen.orangecloud.core.network.CfApiError>? = null,
    @SerialName("result_info") val resultInfo: AuditResultInfo? = null,
) {
    val cursor: String? get() = resultInfo?.cursor
}
