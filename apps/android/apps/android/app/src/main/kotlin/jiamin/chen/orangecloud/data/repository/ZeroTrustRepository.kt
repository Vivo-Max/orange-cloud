package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.AccessApp
import jiamin.chen.orangecloud.data.model.AccessAppInput
import jiamin.chen.orangecloud.data.model.AccessPolicyInput
import jiamin.chen.orangecloud.data.model.AccessPolicyResult
import jiamin.chen.orangecloud.data.model.GatewayRule
import jiamin.chen.orangecloud.data.model.GatewayRuleInput
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zero Trust（account 级）：Access 应用列表（access.read）+ Gateway 策略列表/启停（teams.*）。
 * 对应 iOS ZeroTrustService。
 */
@Singleton
class ZeroTrustRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun accessApps(accountId: String): List<AccessApp> =
        api.get("accounts/$accountId/access/apps")

    suspend fun gatewayRules(accountId: String): List<GatewayRule> =
        api.get("accounts/$accountId/gateway/rules")

    /** 启停 Gateway 策略：PUT 全量回写（保留 rule_settings 等未建模设置）。 */
    suspend fun setGatewayRuleEnabled(accountId: String, rule: GatewayRule, enabled: Boolean): GatewayRule =
        api.put("accounts/$accountId/gateway/rules/${rule.id}", GatewayRuleInput.fromRule(rule, enabled))

    suspend fun createGatewayRule(accountId: String, body: GatewayRuleInput): GatewayRule =
        api.post("accounts/$accountId/gateway/rules", body)

    suspend fun updateGatewayRule(accountId: String, ruleId: String, body: GatewayRuleInput): GatewayRule =
        api.put("accounts/$accountId/gateway/rules/$ruleId", body)

    suspend fun deleteGatewayRule(accountId: String, ruleId: String) =
        api.delete("accounts/$accountId/gateway/rules/$ruleId")

    // MARK: - Access 写入（access.write）

    /** 先建可复用策略（include 单键对象数组动态构建），再建/更新应用引用它。 */
    suspend fun createAccessPolicy(accountId: String, name: String, decision: String, rules: List<Pair<String, String>>): String {
        val input = AccessPolicyInput(name, decision, buildInclude(rules))
        val result: AccessPolicyResult = api.post("accounts/$accountId/access/policies", input)
        return result.id ?: error("policy id missing")
    }

    suspend fun createAccessApp(accountId: String, body: AccessAppInput): AccessApp =
        api.post("accounts/$accountId/access/apps", body)

    suspend fun updateAccessApp(accountId: String, appId: String, body: AccessAppInput): AccessApp =
        api.put("accounts/$accountId/access/apps/$appId", body)

    suspend fun deleteAccessApp(accountId: String, appId: String) =
        api.delete("accounts/$accountId/access/apps/$appId")

    /** include 单键对象数组：everyone/email/email_domain/ip/geo。 */
    private fun buildInclude(rules: List<Pair<String, String>>) = buildJsonArray {
        rules.forEach { (kind, value) ->
            add(
                buildJsonObject {
                    when (kind) {
                        "email" -> put("email", buildJsonObject { put("email", value) })
                        "email_domain" -> put("email_domain", buildJsonObject { put("domain", value) })
                        "ip" -> put("ip", buildJsonObject { put("ip", value) })
                        "geo" -> put("geo", buildJsonObject { put("country_code", value) })
                        else -> put("everyone", buildJsonObject {})
                    }
                },
            )
        }
    }
}
