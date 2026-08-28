package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - 开发者平台模块：Queues / AI Gateway / Durable Objects / Workers AI / Hyperdrive。
// 对应 iOS DeveloperPlatformModels。Android v1：均只读列表 + Workers AI 文本生成试运行。

// —— Queues（queues.read）——

@Serializable
data class CFQueue(
    @SerialName("queue_id") val queueId: String,
    @SerialName("queue_name") val queueName: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("producers_total_count") val producersTotalCount: Int? = null,
    @SerialName("consumers_total_count") val consumersTotalCount: Int? = null,
    val settings: CFQueueSettings? = null,
) {
    val name: String get() = queueName ?: queueId
}

@Serializable
data class CFQueueSettings(
    @SerialName("delivery_delay") val deliveryDelay: Int? = null,
    @SerialName("delivery_paused") val deliveryPaused: Boolean? = null,
    @SerialName("message_retention_period") val messageRetentionPeriod: Int? = null,
)

@Serializable
data class CFQueueCreate(@SerialName("queue_name") val queueName: String)

@Serializable
data class CFQueueUpdate(
    @SerialName("queue_name") val queueName: String? = null,
    val settings: CFQueueSettingsPatch? = null,
)

@Serializable
data class CFQueueSettingsPatch(
    @SerialName("delivery_paused") val deliveryPaused: Boolean? = null,
    @SerialName("delivery_delay") val deliveryDelay: Int? = null,
    @SerialName("message_retention_period") val messageRetentionPeriod: Int? = null,
)

@Serializable
data class CFQueuePurge(@SerialName("delete_messages_permanently") val deleteMessagesPermanently: Boolean)

// —— AI Gateway（aig.read）——

@Serializable
data class AIGateway(
    val id: String,
    @SerialName("cache_ttl") val cacheTtl: Int? = null,
    @SerialName("collect_logs") val collectLogs: Boolean? = null,
    @SerialName("rate_limiting_interval") val rateLimitingInterval: Int? = null,
    @SerialName("rate_limiting_limit") val rateLimitingLimit: Int? = null,
    @SerialName("created_on") val createdOn: String? = null,
)

/** POST /ai-gateway/gateways（全字段必填）。 */
@Serializable
data class AIGatewayCreate(
    val id: String,
    @SerialName("cache_invalidate_on_update") val cacheInvalidateOnUpdate: Boolean,
    @SerialName("cache_ttl") val cacheTtl: Int,
    @SerialName("collect_logs") val collectLogs: Boolean,
    @SerialName("rate_limiting_interval") val rateLimitingInterval: Int,
    @SerialName("rate_limiting_limit") val rateLimitingLimit: Int,
)

// —— Durable Objects（只读，workers-scripts.read）——

@Serializable
data class DurableObjectNamespace(
    val id: String,
    val name: String? = null,
    @SerialName("class") val className: String? = null,
    val script: String? = null,
    @SerialName("use_sqlite") val useSqlite: Boolean? = null,
)

// —— Workers AI（ai.read 目录 / ai.write 运行）——

@Serializable
data class AIModel(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val task: AITask? = null,
) {
    /** 模型短名（去掉 @cf/ 前缀的最后一段）。 */
    val shortName: String get() = (name ?: id).substringAfterLast('/')
    val taskName: String get() = task?.name ?: ""
}

@Serializable
data class AITask(val id: String? = null, val name: String? = null)

@Serializable
data class AIChatMessage(val role: String, val content: String)

@Serializable
data class AITextGenRequest(val messages: List<AIChatMessage>)

@Serializable
data class AITextGenResult(val response: String? = null)

// —— Hyperdrive（query-cache.read）——

@Serializable
data class HyperdriveConfig(
    val id: String,
    val name: String? = null,
    val origin: HyperdriveOrigin? = null,
    val caching: HyperdriveCaching? = null,
    @SerialName("origin_connection_limit") val originConnectionLimit: Int? = null,
) {
    val displayName: String get() = name ?: id
}

@Serializable
data class HyperdriveOrigin(
    val scheme: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val database: String? = null,
    val user: String? = null,
) {
    val summary: String
        get() {
            val s = scheme ?: "postgres"
            val h = host ?: "—"
            val db = database?.let { "/$it" } ?: ""
            return "$s://$h$db"
        }
}

@Serializable
data class HyperdriveCaching(
    val disabled: Boolean? = null,
    @SerialName("max_age") val maxAge: Int? = null,
    @SerialName("stale_while_revalidate") val staleWhileRevalidate: Int? = null,
)

/** POST /hyperdrive/configs（origin 含写专用 password，响应永不返回）。 */
@Serializable
data class HyperdriveCreate(val name: String, val origin: HyperdriveCreateOrigin)

@Serializable
data class HyperdriveCreateOrigin(
    val scheme: String,
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
)

/** PATCH /hyperdrive/configs/{id}（全字段可选）。 */
@Serializable
data class HyperdrivePatch(
    val name: String? = null,
    val caching: HyperdriveCachingPatch? = null,
)

@Serializable
data class HyperdriveCachingPatch(
    val disabled: Boolean? = null,
    @SerialName("max_age") val maxAge: Int? = null,
    @SerialName("stale_while_revalidate") val staleWhileRevalidate: Int? = null,
)

// MARK: - Durable Objects 内存指标（GraphQL durableObjectsPeriodicGroups）

/**
 * 一个 namespace 24 小时内的内存分位数。
 *
 * 注意口径：这是 **V8 isolate** 的内存，不是单个 DO 实例的——一个 isolate 可能同时承载
 * 同类的多个对象与外围 Worker 代码，每个 isolate 上限 128 MB。
 */
data class DurableObjectMemory(
    val p50: Long,
    val p90: Long,
    val p99: Long,
    val p999: Long,
)

@Serializable
data class DOMemoryVariables(
    val accountTag: String,
    val namespaceId: String,
    val since: String,
    val until: String,
)

@Serializable
data class DOMemoryData(val viewer: DOMemoryViewer)

@Serializable
data class DOMemoryViewer(val accounts: List<DOMemoryAccount> = emptyList())

@Serializable
data class DOMemoryAccount(
    @SerialName("durableObjectsPeriodicGroups") val groups: List<DOMemoryGroup> = emptyList(),
)

@Serializable
data class DOMemoryGroup(val quantiles: DOMemoryQuantiles? = null)

@Serializable
data class DOMemoryQuantiles(
    @SerialName("memoryUsageBytesP50") val p50: Long? = null,
    @SerialName("memoryUsageBytesP90") val p90: Long? = null,
    @SerialName("memoryUsageBytesP99") val p99: Long? = null,
    @SerialName("memoryUsageBytesP999") val p999: Long? = null,
)

object DOMemoryQuery {
    val text: String = """
        query DOMemory(${'$'}accountTag: String!, ${'$'}namespaceId: String!, ${'$'}since: Time!, ${'$'}until: Time!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              durableObjectsPeriodicGroups(
                limit: 1
                filter: { datetime_geq: ${'$'}since, datetime_leq: ${'$'}until, namespaceId: ${'$'}namespaceId }
              ) {
                quantiles {
                  memoryUsageBytesP50
                  memoryUsageBytesP90
                  memoryUsageBytesP99
                  memoryUsageBytesP999
                }
              }
            }
          }
        }
    """.trimIndent()
}
