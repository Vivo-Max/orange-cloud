package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.RateLimitEntrypointUpdate
import jiamin.chen.orangecloud.data.model.RateLimitRuleCreate
import jiamin.chen.orangecloud.data.model.RateLimitRuleset
import jiamin.chen.orangecloud.data.model.RateLimitToggle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rate Limiting CRUD（Rulesets http_ratelimit phase entrypoint）。对应 iOS RateLimitService。
 * 读写复用 zone-waf.read/.write；phase 没有规则时 entrypoint 返回 404/业务错误，视为 null。
 */
@Singleton
class RateLimitRepository @Inject constructor(
    private val api: CfApiClient,
) {
    private val phase = "http_ratelimit"

    suspend fun ruleset(zoneId: String): RateLimitRuleset? = try {
        api.get<RateLimitRuleset>("zones/$zoneId/rulesets/phases/$phase/entrypoint")
    } catch (e: ApiError.Http) {
        if (e.status == 404) null else throw e
    } catch (e: ApiError.Cloudflare) {
        if (e.errors.any { it.message.contains("could not find entrypoint", ignoreCase = true) }) null else throw e
    }

    suspend fun setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Boolean): RateLimitRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", RateLimitToggle(enabled))

    suspend fun addRule(zoneId: String, rulesetId: String, rule: RateLimitRuleCreate): RateLimitRuleset =
        api.post("zones/$zoneId/rulesets/$rulesetId/rules", rule)

    suspend fun updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: RateLimitRuleCreate): RateLimitRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", rule)

    suspend fun createEntrypoint(zoneId: String, rule: RateLimitRuleCreate): RateLimitRuleset =
        api.put("zones/$zoneId/rulesets/phases/$phase/entrypoint", RateLimitEntrypointUpdate(listOf(rule)))

    suspend fun deleteRule(zoneId: String, rulesetId: String, ruleId: String) =
        api.delete("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId")
}
