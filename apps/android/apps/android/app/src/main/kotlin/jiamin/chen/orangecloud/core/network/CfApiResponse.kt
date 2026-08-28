package jiamin.chen.orangecloud.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cloudflare API 通用信封 { result, success, errors, messages, result_info }。
 * 与 iOS Models/CFAPIResponse.swift 对应；这里用单一信封承载可选 result_info（列表端点才有）。
 */
@Serializable
data class CfEnvelope<T>(
    val result: T? = null,
    val success: Boolean = false,
    val errors: List<CfApiError> = emptyList(),
    val messages: List<CfMessage> = emptyList(),
    @SerialName("result_info") val resultInfo: ResultInfo? = null,
)

@Serializable
data class CfApiError(val code: Int = 0, val message: String = "")

@Serializable
data class CfMessage(val code: Int = 0, val message: String = "")

@Serializable
data class ResultInfo(
    // 页码分页（Zone/DNS 等）
    val page: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    val count: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    // 游标分页（R2 对象、KV keys 等）
    val cursor: String? = null,
    @SerialName("is_truncated") val isTruncated: Boolean? = null,
    // R2 带 delimiter 列举时的「文件夹」前缀
    @SerialName("delimited_prefixes") val delimitedPrefixes: List<String>? = null,
)

/** 列表端点的解码结果：数据 + 分页信息 */
data class Paged<T>(val items: List<T>, val info: ResultInfo?)

// MARK: - GraphQL（分析端点，信封是 {data, errors}，GraphQL 错误时 HTTP 仍 200）

@Serializable
data class GraphQLRequest<V>(val query: String, val variables: V)

@Serializable
data class GraphQLResponse<D>(
    val data: D? = null,
    val errors: List<GraphQLError> = emptyList(),
)

@Serializable
data class GraphQLError(val message: String = "")
