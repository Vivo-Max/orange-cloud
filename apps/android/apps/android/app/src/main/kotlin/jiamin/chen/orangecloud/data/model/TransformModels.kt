package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Transform Rules（Rulesets API，三个 phase；动作恒为 "rewrite"，差异在 action_parameters）
// 读 zone-transform-rules.read，写 .write。对应 iOS TransformRuleModels。

@Serializable
data class TransformRuleset(
    val id: String,
    val name: String? = null,
    val phase: String? = null,
    val rules: List<TransformRule>? = null,
)

@Serializable
data class TransformRule(
    val id: String,
    val expression: String? = null,
    val description: String? = null,
    val enabled: Boolean? = null,
    val action: String? = null,
    @SerialName("action_parameters") val actionParameters: TransformActionParameters? = null,
)

@Serializable
data class TransformActionParameters(
    val uri: UriRewrite? = null,
    val headers: Map<String, HeaderTransform>? = null,
)

@Serializable
data class UriRewrite(
    val path: RewriteTarget? = null,
    val query: RewriteTarget? = null,
)

@Serializable
data class RewriteTarget(
    val value: String? = null,
    val expression: String? = null,
)

@Serializable
data class HeaderTransform(
    val operation: String,        // "set" | "add" | "remove"
    val value: String? = null,
    val expression: String? = null,
)

// MARK: - 写入载荷（POST rules / PATCH rule / PUT entrypoint 共用）

@Serializable
data class TransformRuleCreate(
    val action: String,           // 恒为 "rewrite"
    val expression: String,
    val description: String? = null,
    val enabled: Boolean,
    @SerialName("action_parameters") val actionParameters: TransformActionParameters? = null,
)

@Serializable
data class TransformRuleToggle(val enabled: Boolean)

@Serializable
data class TransformEntrypointUpdate(val rules: List<TransformRuleCreate>)
