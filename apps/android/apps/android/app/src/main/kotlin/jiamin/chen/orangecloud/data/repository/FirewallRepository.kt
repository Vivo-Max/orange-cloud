package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.AccessRuleCreate
import jiamin.chen.orangecloud.data.model.AccessRuleUpdate
import jiamin.chen.orangecloud.data.model.FirewallAccessRule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IP 访问规则 CRUD（legacy firewall access rules）。对应 iOS FirewallAccessRuleService。
 * 读 firewall-services.read，写 .write。
 */
@Singleton
class FirewallRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 列出该 Zone 的 IP 访问规则（最多 100 条）。 */
    suspend fun rules(zoneId: String): List<FirewallAccessRule> =
        api.getList<FirewallAccessRule>(
            "zones/$zoneId/firewall/access_rules/rules",
            listOf("per_page" to "100"),
        ).items

    suspend fun createRule(zoneId: String, draft: AccessRuleCreate): FirewallAccessRule =
        api.post("zones/$zoneId/firewall/access_rules/rules", draft)

    /** 仅改 mode + notes（target/value 不可变）。 */
    suspend fun updateRule(zoneId: String, ruleId: String, update: AccessRuleUpdate): FirewallAccessRule =
        api.patch("zones/$zoneId/firewall/access_rules/rules/$ruleId", update)

    suspend fun deleteRule(zoneId: String, ruleId: String) =
        api.delete("zones/$zoneId/firewall/access_rules/rules/$ruleId")
}
