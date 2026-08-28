package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cloudflare Trace —— 模拟一条请求打过 Cloudflare，看它走了哪些规则、被谁拦的。
 * POST /accounts/{account_id}/request-tracer/trace
 *
 * 本质是「模拟」而非真实流量回放。全套餐可用，但要求 Administrator / Super Administrator 角色。
 */
@Serializable
data class TraceRequest(
    val method: String,
    val url: String,
    @SerialName("protocol") val protocolVersion: String = "HTTP/1.1",
)

@Serializable
data class TraceResult(
    @SerialName("status_code") val statusCode: Int? = null,
    val trace: List<TraceStep>? = null,
)

/** 单个求值步骤。**递归结构**：ruleset 步骤的 trace 里装着它的各条 rule。 */
@Serializable
data class TraceStep(
    val type: String? = null,
    @SerialName("step_name") val stepName: String? = null,
    val action: String? = null,
    val description: String? = null,
    val expression: String? = null,
    val kind: String? = null,
    val name: String? = null,
    /** 该步骤是否真的影响了请求——UI 的重点就是把 true 的挑出来 */
    val matched: Boolean? = null,
    val trace: List<TraceStep>? = null,
) {
    /** 展示名：ruleset 用 name，rule 用 description，都没有才退回 step_name */
    val title: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }
            ?: stepName.orEmpty()
}

/** 把递归树摊平成带层级的行 */
data class FlatTraceStep(val step: TraceStep, val depth: Int)

fun List<TraceStep>.flattened(depth: Int = 0): List<FlatTraceStep> =
    flatMap { step -> listOf(FlatTraceStep(step, depth)) + step.trace.orEmpty().flattened(depth + 1) }
