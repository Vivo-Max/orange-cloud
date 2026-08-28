package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - 负载均衡：Load Balancer（zone 级）+ 源站池 Pool / 健康监测 Monitor（account 级）。
// 对应 iOS LoadBalancerModels。读 load-balancers.read / load-balancing-monitors-and-pools.read。
// Android v1：LB 列表 + 启停；Pool / Monitor 只读查看（含池健康）。

@Serializable
data class LoadBalancer(
    val id: String,
    val name: String? = null,
    val enabled: Boolean? = null,
    val ttl: Int? = null,
    val proxied: Boolean? = null,
    @SerialName("default_pools") val defaultPools: List<String>? = null,
    @SerialName("fallback_pool") val fallbackPool: String? = null,
    @SerialName("steering_policy") val steeringPolicy: String? = null,
    @SerialName("session_affinity") val sessionAffinity: String? = null,
)

/** LB 启停 PATCH（顶层合并，只带 enabled）。 */
@Serializable
data class LoadBalancerToggle(val enabled: Boolean)

/** 创建/编辑负载均衡器（PATCH 顶层合并；create 时 name/default_pools/fallback_pool 必填）。 */
@Serializable
data class LoadBalancerUpdate(
    val name: String? = null,
    val enabled: Boolean? = null,
    val proxied: Boolean? = null,
    @SerialName("default_pools") val defaultPools: List<String>? = null,
    @SerialName("fallback_pool") val fallbackPool: String? = null,
    @SerialName("steering_policy") val steeringPolicy: String? = null,
    @SerialName("session_affinity") val sessionAffinity: String? = null,
)

/** 写回 origin（去只读字段）。 */
@Serializable
data class OriginInput(
    val name: String? = null,
    val address: String? = null,
    val enabled: Boolean? = null,
    val weight: Double? = null,
    val port: Int? = null,
)

@Serializable
data class PoolUpdate(
    val name: String? = null,
    val enabled: Boolean? = null,
    val description: String? = null,
    val monitor: String? = null,
    @SerialName("notification_email") val notificationEmail: String? = null,
    val origins: List<OriginInput>? = null,
)

@Serializable
data class MonitorUpdate(
    val type: String? = null,
    val method: String? = null,
    val path: String? = null,
    @SerialName("expected_codes") val expectedCodes: String? = null,
    val interval: Int? = null,
    val timeout: Int? = null,
    val retries: Int? = null,
    val port: Int? = null,
    val description: String? = null,
)

@Serializable
data class Pool(
    val id: String,
    val name: String? = null,
    val enabled: Boolean? = null,
    val description: String? = null,
    val monitor: String? = null,
    @SerialName("notification_email") val notificationEmail: String? = null,
    @SerialName("minimum_origins") val minimumOrigins: Int? = null,
    val origins: List<Origin>? = null,
) {
    val enabledOriginsCount: Int get() = origins.orEmpty().count { it.enabled ?: true }
    val originsCount: Int get() = origins?.size ?: 0
}

@Serializable
data class Origin(
    val name: String? = null,
    val address: String? = null,
    val enabled: Boolean? = null,
    val weight: Double? = null,
    val port: Int? = null,
)

@Serializable
data class PoolHealthResponse(
    @SerialName("pool_id") val poolId: String? = null,
    @SerialName("pop_health") val popHealth: Map<String, PoolPopHealth>? = null,
) {
    val healthyCount: Int get() = popHealth?.values?.count { it.healthy == true } ?: 0
    val totalCount: Int get() = popHealth?.size ?: 0
}

@Serializable
data class PoolPopHealth(val healthy: Boolean? = null)

@Serializable
data class Monitor(
    val id: String,
    val type: String? = null,
    val method: String? = null,
    val path: String? = null,
    @SerialName("expected_codes") val expectedCodes: String? = null,
    val interval: Int? = null,
    val timeout: Int? = null,
    val retries: Int? = null,
    val port: Int? = null,
    val description: String? = null,
) {
    val typeLabel: String get() = (type ?: "").uppercase()
}

/** Steering 策略可读标签 key。 */
fun steeringLabelKey(policy: String?): String = policy ?: "off"
