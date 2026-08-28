package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 托管请求/响应头（Managed Transforms）。GET/PATCH /zones/{zone_id}/managed_headers
 *
 * 由 Cloudflare 维护的一组开关式头部改写（add_security_headers、
 * add_bot_protection_headers、add_true_client_ip_headers 等），无需自己写 Transform 规则。
 *
 * PATCH 是**分区合并**：两个数组都可选，只更新给出的那一节；节内按 id 匹配，
 * 所以只发要改的那一条即可。null 项在 explicitNulls=false 下不会被序列化。
 */
@Serializable
data class ManagedTransforms(
    @SerialName("managed_request_headers") val requestHeaders: List<ManagedTransform>? = null,
    @SerialName("managed_response_headers") val responseHeaders: List<ManagedTransform>? = null,
)

@Serializable
data class ManagedTransform(
    val id: String,
    val enabled: Boolean? = null,
    /** 与之冲突的其它托管头（只读）。开启前应提示会互斥。 */
    @SerialName("conflicts_with") val conflictsWith: List<String>? = null,
) {
    /** 接口给的是 add_security_headers 这类蛇形 id，直接展示不友好 */
    val displayName: String
        get() = id.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}

/** PATCH 体：只带要改的那一节 */
@Serializable
data class ManagedTransformsPatch(
    @SerialName("managed_request_headers") val requestHeaders: List<ManagedTransformToggle>? = null,
    @SerialName("managed_response_headers") val responseHeaders: List<ManagedTransformToggle>? = null,
)

@Serializable
data class ManagedTransformToggle(val id: String, val enabled: Boolean)
