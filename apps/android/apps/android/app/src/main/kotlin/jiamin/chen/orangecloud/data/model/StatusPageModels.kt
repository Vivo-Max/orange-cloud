package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cloudflare 官方状态页（cloudflarestatus.com Statuspage v2，公开无 CF 信封）。 */
@Serializable
data class StatusPageSummary(
    val status: StatusPageOverall,
    val components: List<StatusPageComponent> = emptyList(),
    val incidents: List<StatusPageIncident> = emptyList(),
    @SerialName("scheduled_maintenances") val scheduledMaintenances: List<StatusPageIncident> = emptyList(),
)

/** GET /api/v2/incidents.json（含已解决的历史事件，最近 50 条）。 */
@Serializable
data class StatusPageIncidentList(
    val incidents: List<StatusPageIncident> = emptyList(),
)

@Serializable
data class StatusPageOverall(
    val indicator: String,   // none | minor | major | critical | maintenance
    val description: String,
)

@Serializable
data class StatusPageComponent(
    val id: String,
    val name: String,
    val status: String,      // operational | degraded_performance | partial_outage | major_outage | under_maintenance
    val group: Boolean? = null,            // true = 分组容器（如各大洲 PoP 分组），本身不是服务
    @SerialName("group_id") val groupId: String? = null,
)

/** 事件与计划维护共用结构（维护多 scheduled_for 字段）。 */
@Serializable
data class StatusPageIncident(
    val id: String,
    val name: String,
    val status: String = "",   // 事件 investigating… / 维护 scheduled…
    val impact: String = "",   // none | minor | major | critical | maintenance
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("scheduled_for") val scheduledFor: String? = null,
    val shortlink: String? = null,
    @SerialName("incident_updates") val incidentUpdates: List<StatusPageIncidentUpdate> = emptyList(),
)

@Serializable
data class StatusPageIncidentUpdate(
    val id: String,
    val status: String = "",
    val body: String = "",
    @SerialName("display_at") val displayAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** 边缘网络大区汇总（由 ViewModel 按分组聚合，非 API 原始结构）。 */
data class StatusPageRegion(
    val id: String,
    val name: String,   // API 原文（英文）
    val total: Int,
    val impacted: Int,  // 非 operational 的节点数
)
