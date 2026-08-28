package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.CacheEntrypointUpdate
import jiamin.chen.orangecloud.data.model.CacheRuleCreate
import jiamin.chen.orangecloud.data.model.CacheRuleToggle
import jiamin.chen.orangecloud.data.model.CacheRuleset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache Rules CRUD（Rulesets entrypoint，phase http_request_cache_settings）。对应 iOS CacheRuleService。
 * 读 cache-settings.read，写 .write；phase 还没有规则集时 entrypoint 返回 404/业务错误，视为 null。
 */
@Singleton
class CacheRuleRepository @Inject constructor(
    private val api: CfApiClient,
) {
    private val phase = "http_request_cache_settings"

    /** 取缓存规则 entrypoint ruleset；无规则集时返回 null。 */
    suspend fun ruleset(zoneId: String): CacheRuleset? = try {
        api.get<CacheRuleset>("zones/$zoneId/rulesets/phases/$phase/entrypoint")
    } catch (e: ApiError.Http) {
        if (e.status == 404) null else throw e
    } catch (e: ApiError.Cloudflare) {
        if (e.errors.any { it.message.contains("could not find entrypoint", ignoreCase = true) }) null else throw e
    }

    suspend fun setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Boolean): CacheRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", CacheRuleToggle(enabled))

    suspend fun addRule(zoneId: String, rulesetId: String, rule: CacheRuleCreate): CacheRuleset =
        api.post("zones/$zoneId/rulesets/$rulesetId/rules", rule)

    suspend fun updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: CacheRuleCreate): CacheRuleset =
        api.patch("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId", rule)

    /** phase 还没有规则集时，用首条规则创建 entrypoint。 */
    suspend fun createEntrypoint(zoneId: String, rule: CacheRuleCreate): CacheRuleset =
        api.put("zones/$zoneId/rulesets/phases/$phase/entrypoint", CacheEntrypointUpdate(listOf(rule)))

    suspend fun deleteRule(zoneId: String, rulesetId: String, ruleId: String) =
        api.delete("zones/$zoneId/rulesets/$rulesetId/rules/$ruleId")
}
