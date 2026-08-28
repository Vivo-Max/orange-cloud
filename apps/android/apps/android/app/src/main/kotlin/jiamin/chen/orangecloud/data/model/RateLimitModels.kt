package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Rate Limiting（现代版）：Rulesets 的 http_ratelimit phase entrypoint。
// 与 Transform Rules 同构。复用 zone-waf.read/.write（与 WAF 自定义规则同权限组）。对应 iOS RateLimitModels。

@Serializable
data class RateLimitRuleset(
    val id: String,
    val name: String? = null,
    val phase: String? = null,
    val rules: List<RateLimitRule>? = null,
)

@Serializable
data class RateLimitRule(
    val id: String,
    val action: String? = null,
    val expression: String? = null,
    val description: String? = null,
    val enabled: Boolean? = null,
    val ratelimit: RateLimitConfig? = null,
)

@Serializable
data class RateLimitConfig(
    val characteristics: List<String>? = null,
    val period: Int? = null,
    @SerialName("requests_per_period") val requestsPerPeriod: Int? = null,
    @SerialName("mitigation_timeout") val mitigationTimeout: Int? = null,
    @SerialName("counting_expression") val countingExpression: String? = null,
)

// MARK: - 写入载荷

@Serializable
data class RateLimitRuleCreate(
    val action: String,
    val expression: String,
    val description: String? = null,
    val enabled: Boolean,
    val ratelimit: RateLimitConfigInput,
) {
    companion object {
        /** 便捷构造：按 IP（按 colo 本地计数）在 period 秒内超过 requests 次即触发 action。 */
        fun make(
            expression: String,
            requests: Int,
            period: Int,
            action: String,
            mitigationTimeout: Int,
            description: String?,
            enabled: Boolean,
        ) = RateLimitRuleCreate(
            action = action,
            expression = expression,
            description = description,
            enabled = enabled,
            ratelimit = RateLimitConfigInput(
                characteristics = listOf("ip.src", "cf.colo.id"),
                period = period,
                requestsPerPeriod = requests,
                mitigationTimeout = mitigationTimeout,
            ),
        )
    }
}

@Serializable
data class RateLimitConfigInput(
    val characteristics: List<String>,
    val period: Int,
    @SerialName("requests_per_period") val requestsPerPeriod: Int,
    @SerialName("mitigation_timeout") val mitigationTimeout: Int,
)

@Serializable
data class RateLimitToggle(val enabled: Boolean)

@Serializable
data class RateLimitEntrypointUpdate(val rules: List<RateLimitRuleCreate>)
