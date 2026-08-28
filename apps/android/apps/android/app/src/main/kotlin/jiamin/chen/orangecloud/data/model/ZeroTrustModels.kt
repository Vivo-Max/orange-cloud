package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// MARK: - Zero Trust：Access 应用（access.read）+ Gateway 策略（teams.read）。对应 iOS ZeroTrustModels。
// Android v1：Access 应用只读列表；Gateway 策略列表 + 启停（PUT 全量回写，rule_settings 原样保留）。

@Serializable
data class AccessApp(
    val id: String,
    val name: String? = null,
    val domain: String? = null,
    val type: String? = null,
    @SerialName("session_duration") val sessionDuration: String? = null,
)

/** 创建可复用策略请求体（include 为单键对象数组，VM/repo 动态构建）。 */
@Serializable
data class AccessPolicyInput(
    val name: String,
    val decision: String,
    val include: JsonElement,
)

/** 创建策略响应只取 id。 */
@Serializable
data class AccessPolicyResult(val id: String? = null)

/** 创建/更新 Access 应用请求体（policies 引用策略 id）。 */
@Serializable
data class AccessAppInput(
    val name: String,
    val domain: String,
    val type: String,
    @SerialName("session_duration") val sessionDuration: String? = null,
    val policies: List<String>,
)

@Serializable
data class GatewayRule(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val action: String? = null,
    val enabled: Boolean? = null,
    val precedence: Int? = null,
    val filters: List<String>? = null,
    val traffic: String? = null,
    val identity: String? = null,
    @SerialName("device_posture") val devicePosture: String? = null,
    @SerialName("rule_settings") val ruleSettings: JsonElement? = null,
) {
    val isEnabled: Boolean get() = enabled ?: false
}

/** PUT /gateway/rules 请求体。可选字段为 null 时省略（Json explicitNulls=false）。 */
@Serializable
data class GatewayRuleInput(
    val name: String,
    val description: String? = null,
    val action: String,
    val enabled: Boolean,
    val filters: List<String>,
    val traffic: String? = null,
    val identity: String? = null,
    @SerialName("device_posture") val devicePosture: String? = null,
    val precedence: Int? = null,
    @SerialName("rule_settings") val ruleSettings: JsonElement? = null,
) {
    companion object {
        /** 从既有规则构造（启停回写用），保留全部未建模设置。 */
        fun fromRule(rule: GatewayRule, enabledOverride: Boolean? = null) = GatewayRuleInput(
            name = rule.name ?: "",
            description = rule.description,
            action = rule.action ?: "block",
            enabled = enabledOverride ?: rule.isEnabled,
            filters = rule.filters ?: listOf("dns"),
            traffic = rule.traffic,
            identity = rule.identity,
            devicePosture = rule.devicePosture,
            precedence = rule.precedence,
            ruleSettings = rule.ruleSettings,
        )
    }
}
