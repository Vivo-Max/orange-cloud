package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.core.network.CfApiClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * R2 SQL：对 R2 Data Catalog（Iceberg）表跑只读分析查询。对应 iOS R2SQLService。
 *
 * 查询端点在独立主机 api.sql.cloudflarestorage.com（**不是** client/v4）：
 * POST /api/v1/accounts/{account_id}/r2-sql/query/{bucket_name}  body {"query": "..."}
 * 鉴权是 Cloudflare Bearer token（需 r2-catalog-sql.read + r2-catalog.read + workers-r2.read）。
 * 该主机对 OAuth token 的接受度未见官方明说，401/403 由 ViewModel 给出定向指引。
 *
 * 响应格式官方未列 schema，按宽容策略解析：在常见容器键（result/data/rows…）下
 * 找「对象数组」当作行集。**计费提醒**：按扫描量计费（$0.0025/GB，10GB/月免费，
 * 单次查询最少计 10MB），UI 需带提示。
 */
@Serializable
private data class R2SqlQueryRequest(val query: String)

/** 展平后的查询结果（值统一字符串化用于展示） */
data class R2SqlResult(val columns: List<String>, val rows: List<List<String>>)

@Singleton
class R2SqlRepository @Inject constructor(
    private val api: CfApiClient,
    private val json: Json,
) {
    suspend fun query(accountId: String, bucket: String, sql: String): R2SqlResult {
        val url = "https://api.sql.cloudflarestorage.com/api/v1/accounts/$accountId/r2-sql/query/$bucket"
        val body = json.encodeToString(R2SqlQueryRequest.serializer(), R2SqlQueryRequest(sql)).encodeToByteArray()
        val bytes = api.postExternalJson(url, body)
        return parse(json.parseToJsonElement(bytes.decodeToString()))
    }

    companion object {
        /** 从响应 JSON 里挖出行集：顶层或 result/data 容器下的第一个「对象数组」。 */
        fun parse(root: JsonElement): R2SqlResult {
            if (root is JsonArray) return flatten(root)
            val dict = root as? JsonObject
                ?: throw ApiError.Decoding(IllegalStateException("R2 SQL 响应不是 JSON 对象"))

            // CF 信封形态：success=false 时抛出首个错误
            val success = (dict["success"] as? JsonPrimitive)?.content
            if (success == "false") {
                val err = (dict["errors"] as? JsonArray)?.firstOrNull() as? JsonObject
                val message = (err?.get("message") as? JsonPrimitive)?.content ?: "查询失败"
                val code = (err?.get("code") as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                throw ApiError.Cloudflare(listOf(ApiError.CfError(code, message)))
            }

            val containerKeys = listOf("result", "data", "rows", "results", "records")
            val candidates = buildList {
                for (key in containerKeys) dict[key]?.let { add(it) }
                (dict["result"] as? JsonObject)?.let { inner ->
                    for (key in containerKeys) inner[key]?.let { add(it) }
                }
            }
            for (candidate in candidates) {
                if (candidate is JsonArray && candidate.all { it is JsonObject }) {
                    return flatten(candidate)
                }
            }
            // 无行集但也没报错：当作执行成功、零行
            return R2SqlResult(emptyList(), emptyList())
        }

        /** 对象数组 → 列名（字母序，稳定可预期）+ 字符串矩阵（缺失键补 NULL） */
        private fun flatten(rows: JsonArray): R2SqlResult {
            val objects = rows.filterIsInstance<JsonObject>()
            if (objects.isEmpty()) return R2SqlResult(emptyList(), emptyList())
            val columns = objects.flatMap { it.keys }.toSortedSet().toList()
            val matrix = objects.map { row -> columns.map { display(row[it]) } }
            return R2SqlResult(columns, matrix)
        }

        private fun display(value: JsonElement?): String = when (value) {
            null, is JsonNull -> "NULL"
            is JsonPrimitive -> value.content
            else -> value.toString() // 嵌套对象/数组压成紧凑 JSON
        }
    }
}
