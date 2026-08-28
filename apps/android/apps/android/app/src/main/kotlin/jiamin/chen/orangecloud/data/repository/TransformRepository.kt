package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.TransformEntrypointUpdate
import jiamin.chen.orangecloud.data.model.TransformRuleCreate
import jiamin.chen.orangecloud.data.model.TransformRuleToggle
import jiamin.chen.orangecloud.data.model.TransformRuleset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transform Rules CRUD（Rulesets entrypoint，按 phase）。对应 iOS TransformRuleService。
 * 读 zone-transform-rules.read，写 .write；phase 还没有规则集时 entrypoint 返回 404/业务错误，视为 null。
 */
@Singleton
class TransformRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 取某 phase 的 entrypoint ruleset；无规则集时返回 null。 */
    suspend fun ruleset(zoneId: String, phase: String): TransformRuleset? = try {
        api.get<TransformRuleset>("zones/$zoneId/rulesets/phases/$phase/entrypoint")
    } catch (e: ApiError.Http) {
        if (e.status == 404) null else throw e
    } catch (e: ApiError.Cloudflare) {
        if (e.errors.any { it.message.contains("could not find entrypoint", ignoreCase = true) }) null else throw e
    }

    suspend fun setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Boolean): TransformRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", TransformRuleToggle(enabled))

    suspend fun addRule(zoneId: String, rulesetId: String, rule: TransformRuleCreate): TransformRuleset =
        api.post("zones/$zoneId/rulesets/$rulesetId/rules", rule)

    suspend fun updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: TransformRuleCreate): TransformRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", rule)

    /** phase 还没有规则集时，用首条规则创建 entrypoint。 */
    suspend fun createEntrypoint(zoneId: String, phase: String, rule: TransformRuleCreate): TransformRuleset =
        api.put("zones/$zoneId/rulesets/phases/$phase/entrypoint", TransformEntrypointUpdate(listOf(rule)))

    suspend fun deleteRule(zoneId: String, rulesetId: String, ruleId: String) =
        api.delete("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId")
}
