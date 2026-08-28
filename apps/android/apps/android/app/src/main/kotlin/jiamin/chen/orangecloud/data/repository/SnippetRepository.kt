package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.Snippet
import jiamin.chen.orangecloud.data.model.SnippetRule
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloudflare Snippets（zone 级边缘 JS）。列表/正文（getRaw）/ 创建更新（multipart 文件）/ 删除。
 * 对应 iOS SnippetService 的 snippet CRUD（触发规则 putRules 整组回写留增量）。
 * snippet 名受限于 [a-zA-Z0-9_]，可直接拼 path 无需编码。
 */
@Singleton
class SnippetRepository @Inject constructor(
    private val api: CfApiClient,
    private val json: Json,
) {
    suspend fun list(zoneId: String): List<Snippet> =
        api.getList<Snippet>("zones/$zoneId/snippets").items

    /**
     * Zone 下全部 snippet 触发规则。响应 result 形态不固定（裸数组 / {rules:[…]} / {}），容错解码。
     */
    suspend fun rules(zoneId: String): List<SnippetRule> {
        val result = runCatching { api.get<JsonElement>("zones/$zoneId/snippets/snippet_rules") }.getOrNull() ?: return emptyList()
        val array: JsonArray? = when (result) {
            is JsonArray -> result
            is JsonObject -> result["rules"] as? JsonArray
            else -> null
        }
        return array?.let { runCatching { json.decodeFromJsonElement(ListSerializer(SnippetRule.serializer()), it) }.getOrNull() }.orEmpty()
    }

    /** snippet 原始 JS 正文（非 JSON 信封）。 */
    suspend fun content(zoneId: String, name: String): String =
        api.getRaw("zones/$zoneId/snippets/$name/content").decodeToString()

    /** 创建或更新（multipart：metadata + JS 模块），返回更新后的元数据。 */
    suspend fun put(zoneId: String, name: String, code: String, mainModule: String = "snippet.js"): Snippet =
        api.putMultipartFile(
            "zones/$zoneId/snippets/$name",
            metadataJson = """{"main_module":"$mainModule"}""",
            fileName = mainModule,
            fileText = code,
            fileContentType = "application/javascript+module",
        )

    suspend fun delete(zoneId: String, name: String) =
        api.delete("zones/$zoneId/snippets/$name")
}
