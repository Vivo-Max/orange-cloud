package jiamin.chen.orangecloud.data.repository

import android.util.Base64
import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.AIChatMessage
import jiamin.chen.orangecloud.data.model.AIGateway
import jiamin.chen.orangecloud.data.model.AIModel
import jiamin.chen.orangecloud.data.model.AITextGenRequest
import jiamin.chen.orangecloud.data.model.AITextGenResult
import jiamin.chen.orangecloud.data.model.AIGatewayCreate
import jiamin.chen.orangecloud.data.model.CFQueue
import jiamin.chen.orangecloud.data.model.CFQueueCreate
import jiamin.chen.orangecloud.data.model.CFQueuePurge
import jiamin.chen.orangecloud.data.model.CFQueueUpdate
import jiamin.chen.orangecloud.data.model.DOMemoryData
import jiamin.chen.orangecloud.data.model.DOMemoryQuery
import jiamin.chen.orangecloud.data.model.DOMemoryVariables
import jiamin.chen.orangecloud.data.model.DurableObjectMemory
import jiamin.chen.orangecloud.data.model.DurableObjectNamespace
import jiamin.chen.orangecloud.data.model.HyperdriveConfig
import jiamin.chen.orangecloud.data.model.HyperdriveCreate
import jiamin.chen.orangecloud.data.model.HyperdrivePatch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/** 文生图请求体（ai run 端点对图像模型只吃 prompt）。 */
@Serializable
data class AIImageGenRequest(val prompt: String)

/**
 * 文生图端点返回了非图片内容（多为 CF 业务错误或该模型输出不是图像）。
 * detail 是响应正文截断后的原文，供 UI 附在提示后面，绝不静默吞掉。
 */
class AINotImageException(val detail: String?) : Exception(detail)

/**
 * 开发者平台只读模块（account 级）：Queues / AI Gateway / Durable Objects / Workers AI / Hyperdrive。
 * 对应 iOS QueueService / AIGatewayService / DurableObjectService / WorkersAIService / HyperdriveService。
 */
@Singleton
class DeveloperPlatformRepository @Inject constructor(
    private val api: CfApiClient,
    private val json: Json,
) {
    suspend fun listQueues(accountId: String): List<CFQueue> =
        api.get("accounts/$accountId/queues")

    suspend fun createQueue(accountId: String, name: String): CFQueue =
        api.post("accounts/$accountId/queues", CFQueueCreate(name))

    suspend fun updateQueue(accountId: String, queueId: String, body: CFQueueUpdate): CFQueue =
        api.put("accounts/$accountId/queues/$queueId", body)

    suspend fun purgeQueue(accountId: String, queueId: String) =
        api.postChecked("accounts/$accountId/queues/$queueId/purge", CFQueuePurge(true))

    suspend fun deleteQueue(accountId: String, queueId: String) =
        api.delete("accounts/$accountId/queues/$queueId")

    suspend fun listGateways(accountId: String): List<AIGateway> =
        api.get("accounts/$accountId/ai-gateway/gateways")

    suspend fun createGateway(accountId: String, body: AIGatewayCreate): AIGateway =
        api.post("accounts/$accountId/ai-gateway/gateways", body)

    suspend fun deleteGateway(accountId: String, gatewayId: String) =
        api.delete("accounts/$accountId/ai-gateway/gateways/$gatewayId")

    suspend fun listDurableObjectNamespaces(accountId: String): List<DurableObjectNamespace> =
        api.get("accounts/$accountId/workers/durable_objects/namespaces")

    /**
     * namespace 的 24h 内存分位数。REST 没有这项，只在 GraphQL 的
     * durableObjectsPeriodicGroups 里；免费账号的账户级 GraphQL 会被 authz 挡，调用方需容错。
     */
    suspend fun durableObjectMemory(accountId: String, namespaceId: String): DurableObjectMemory? {
        val until = java.time.Instant.now()
        val since = until.minus(24, java.time.temporal.ChronoUnit.HOURS)
        val data = api.graphQL<DOMemoryData, DOMemoryVariables>(
            DOMemoryQuery.text,
            DOMemoryVariables(accountId, namespaceId, since.toString(), until.toString()),
        )
        val q = data.viewer.accounts.firstOrNull()?.groups?.firstOrNull()?.quantiles ?: return null
        return DurableObjectMemory(
            p50 = q.p50 ?: return null,
            p90 = q.p90 ?: 0L,
            p99 = q.p99 ?: 0L,
            p999 = q.p999 ?: 0L,
        )
    }

    suspend fun listHyperdrive(accountId: String): List<HyperdriveConfig> =
        api.get("accounts/$accountId/hyperdrive/configs")

    suspend fun createHyperdrive(accountId: String, body: HyperdriveCreate): HyperdriveConfig =
        api.post("accounts/$accountId/hyperdrive/configs", body)

    suspend fun updateHyperdrive(accountId: String, configId: String, body: HyperdrivePatch): HyperdriveConfig =
        api.patch("accounts/$accountId/hyperdrive/configs/$configId", body)

    suspend fun deleteHyperdrive(accountId: String, configId: String) =
        api.delete("accounts/$accountId/hyperdrive/configs/$configId")

    suspend fun listAIModels(accountId: String): List<AIModel> =
        api.getList<AIModel>("accounts/$accountId/ai/models/search", listOf("per_page" to "100")).items

    /** 文本生成试运行（POST /accounts/{id}/ai/run/{model}）。model 含 @cf/ 前缀，作 path 段直拼。 */
    suspend fun runTextGeneration(accountId: String, model: String, messages: List<AIChatMessage>): String {
        val result: AITextGenResult = api.post(
            "accounts/$accountId/ai/run/$model", AITextGenRequest(messages),
        )
        return result.response ?: ""
    }

    /**
     * 文生图试运行：与文本生成同一个 ai run 端点，但响应体是图片原始字节而非 JSON 信封。
     * model 形如 "@cf/black-forest-labs/flux-1-schnell"，其中的斜杠就是真实 path 分隔符、
     * "@" 在 path 段里合法，OkHttp 的 toHttpUrl 会原样保留，无需（也不能）再做百分号编码——
     * 这点与 KV key 不同：KV key 是单个 path 段里的任意字节，才需要调用方预编码。
     *
     * 少数模型（flux 系）仍回 JSON 信封、图片放在 result.image 的 base64 字段，这里一并兼容。
     * 两条路都不成立时抛 AINotImageException，由上层给出明确提示。
     */
    suspend fun runTextToImage(accountId: String, model: String, prompt: String): ByteArray {
        val bytes = api.postRaw("accounts/$accountId/ai/run/$model", AIImageGenRequest(prompt))
        if (looksLikeImage(bytes)) return bytes

        val text = bytes.decodeToString()
        val base64 = runCatching {
            json.parseToJsonElement(text).jsonObject["result"]?.jsonObject?.get("image")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        if (!base64.isNullOrBlank()) {
            val decoded = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
            if (decoded != null && decoded.isNotEmpty()) return decoded
        }
        throw AINotImageException(text.take(300).trim().ifBlank { null })
    }

    /** 按魔数判断是否图片（PNG / JPEG / GIF / WebP），避免把错误 JSON 当图片解码。 */
    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        fun at(index: Int) = bytes[index].toInt() and 0xFF
        val png = at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47
        val jpeg = at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF
        val gif = at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46
        val webp = at(0) == 0x52 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x46 &&
            at(8) == 0x57 && at(9) == 0x45 && at(10) == 0x42 && at(11) == 0x50
        return png || jpeg || gif || webp
    }
}
