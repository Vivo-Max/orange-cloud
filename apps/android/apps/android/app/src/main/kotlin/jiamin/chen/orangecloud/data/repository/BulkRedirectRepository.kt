package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.BulkOperation
import jiamin.chen.orangecloud.data.model.BulkOperationRef
import jiamin.chen.orangecloud.data.model.ItemDeleteBody
import jiamin.chen.orangecloud.data.model.ItemRef
import jiamin.chen.orangecloud.data.model.RedirectEntrypointUpdate
import jiamin.chen.orangecloud.data.model.RedirectItemInput
import jiamin.chen.orangecloud.data.model.RedirectList
import jiamin.chen.orangecloud.data.model.RedirectListCreate
import jiamin.chen.orangecloud.data.model.RedirectListItem
import jiamin.chen.orangecloud.data.model.RedirectRuleCreate
import jiamin.chen.orangecloud.data.model.RedirectRuleset
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bulk Redirects（account 级）：列表（同步）+ 条目（异步批量 + 轮询）+ 启用规则（ruleset）。
 * 对应 iOS BulkRedirectService。读 account-rule-lists.read + mass-url-redirects.read。
 */
@Singleton
class BulkRedirectRepository @Inject constructor(
    private val api: CfApiClient,
) {
    private val redirectPhase = "http_request_redirect"

    // MARK: - 重定向列表（同步）

    /** 仅返回 kind == "redirect" 的列表。 */
    suspend fun listRedirectLists(accountId: String): List<RedirectList> =
        api.get<List<RedirectList>>("accounts/$accountId/rules/lists").filter { it.kind == "redirect" }

    suspend fun createList(accountId: String, name: String, description: String?): RedirectList =
        api.post("accounts/$accountId/rules/lists", RedirectListCreate(name, "redirect", description))

    suspend fun deleteList(accountId: String, listId: String) =
        api.delete("accounts/$accountId/rules/lists/$listId")

    // MARK: - 条目（异步：create/delete 返回 operation_id 需轮询）

    suspend fun listItems(accountId: String, listId: String): List<RedirectListItem> =
        api.get("accounts/$accountId/rules/lists/$listId/items")

    /** 追加条目，返回 operation_id。 */
    suspend fun createItems(accountId: String, listId: String, items: List<RedirectItemInput>): String =
        api.post<BulkOperationRef, List<RedirectItemInput>>(
            "accounts/$accountId/rules/lists/$listId/items", items,
        ).operationId

    /** 删除条目（按 id），返回 operation_id。 */
    suspend fun deleteItems(accountId: String, listId: String, itemIds: List<String>): String =
        api.requestJson<BulkOperationRef, ItemDeleteBody>(
            "DELETE", "accounts/$accountId/rules/lists/$listId/items",
            ItemDeleteBody(itemIds.map { ItemRef(it) }),
        ).operationId

    suspend fun operationStatus(accountId: String, operationId: String): BulkOperation =
        api.get("accounts/$accountId/rules/lists/bulk_operations/$operationId")

    /** 轮询批量操作直至完成；failed / 超时抛错。 */
    suspend fun waitForOperation(accountId: String, operationId: String, timeoutMs: Long = 30_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val op = operationStatus(accountId, operationId)
            when (op.status) {
                "completed" -> return
                "failed" -> throw ApiError.Cloudflare(listOf(ApiError.CfError(-1, op.error ?: "批量操作失败")))
                else -> delay(800)
            }
        }
        throw ApiError.Cloudflare(listOf(ApiError.CfError(-1, "批量操作超时，请稍后刷新查看结果")))
    }

    // MARK: - 启用规则（account ruleset，phase http_request_redirect）

    suspend fun redirectEntrypoint(accountId: String): RedirectRuleset? = try {
        api.get<RedirectRuleset>("accounts/$accountId/rulesets/phases/$redirectPhase/entrypoint")
    } catch (e: ApiError.Http) {
        if (e.status == 404) null else throw e
    } catch (e: ApiError.Cloudflare) {
        if (e.errors.any { it.message.contains("could not find entrypoint", ignoreCase = true) }) null else throw e
    }

    suspend fun addRule(accountId: String, rulesetId: String, rule: RedirectRuleCreate): RedirectRuleset =
        api.post("accounts/$accountId/rulesets/$rulesetId/rules", rule)

    suspend fun createEntrypoint(accountId: String, rule: RedirectRuleCreate): RedirectRuleset =
        api.put("accounts/$accountId/rulesets/phases/$redirectPhase/entrypoint", RedirectEntrypointUpdate(listOf(rule)))

    /** 确保某列表已被启用规则引用（幂等：已存在则跳过）。 */
    suspend fun ensureEnabled(accountId: String, listName: String) {
        val rule = RedirectRuleCreate.enabling(listName)
        val entrypoint = redirectEntrypoint(accountId)
        if (entrypoint == null) {
            createEntrypoint(accountId, rule)
        } else if (entrypoint.rules.orEmpty().none { it.expression?.contains("\$$listName") == true }) {
            addRule(accountId, entrypoint.id, rule)
        }
    }
}
