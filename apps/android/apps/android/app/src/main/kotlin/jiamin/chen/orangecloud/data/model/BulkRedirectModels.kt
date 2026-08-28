package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - Bulk Redirects（account 级，三段式）。对应 iOS BulkRedirectModels。
// 1) 重定向列表（kind=redirect）/accounts/{id}/rules/lists
// 2) 列表条目（增删为异步，返回 operation_id 需轮询）
// 3) 启用规则（account ruleset，phase http_request_redirect，action=redirect + from_list）
// 读 mass-url-redirects.read + account-rule-lists.read。

@Serializable
data class RedirectList(
    val id: String,
    val name: String? = null,
    val kind: String? = null, // "redirect"
    val description: String? = null,
    @SerialName("num_items") val numItems: Int? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)

@Serializable
data class RedirectListCreate(
    val name: String,
    val kind: String, // 恒 "redirect"
    val description: String? = null,
)

@Serializable
data class RedirectListUpdate(val description: String? = null)

@Serializable
data class RedirectListItem(
    val id: String,
    val redirect: RedirectRule? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)

@Serializable
data class RedirectRule(
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("target_url") val targetUrl: String,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("include_subdomains") val includeSubdomains: Boolean? = null,
    @SerialName("subpath_matching") val subpathMatching: Boolean? = null,
    @SerialName("preserve_query_string") val preserveQueryString: Boolean? = null,
    @SerialName("preserve_path_suffix") val preservePathSuffix: Boolean? = null,
)

@Serializable
data class RedirectItemInput(val redirect: RedirectRule)

@Serializable
data class ItemDeleteBody(val items: List<ItemRef>)

@Serializable
data class ItemRef(val id: String)

@Serializable
data class BulkOperationRef(
    @SerialName("operation_id") val operationId: String,
)

@Serializable
data class BulkOperation(
    val id: String? = null,
    val status: String? = null, // pending | running | completed | failed
    val error: String? = null,
)

// MARK: - 启用规则（account ruleset，phase http_request_redirect）

@Serializable
data class RedirectRuleset(
    val id: String,
    val rules: List<RedirectRulesetRule>? = null,
)

@Serializable
data class RedirectRulesetRule(
    val id: String,
    val expression: String? = null,
    val action: String? = null,
    val enabled: Boolean? = null,
    val description: String? = null,
    @SerialName("action_parameters") val actionParameters: RedirectActionParameters? = null,
)

@Serializable
data class RedirectActionParameters(
    @SerialName("from_list") val fromList: FromList? = null,
)

@Serializable
data class FromList(val name: String? = null, val key: String? = null)

@Serializable
data class RedirectRuleCreate(
    val action: String, // 恒 "redirect"
    val expression: String,
    val description: String? = null,
    val enabled: Boolean,
    @SerialName("action_parameters") val actionParameters: RedirectActionParametersInput,
) {
    companion object {
        /** 启用某重定向列表的标准规则：http.request.full_uri in $<name> + from_list。 */
        fun enabling(listName: String) = RedirectRuleCreate(
            action = "redirect",
            expression = "http.request.full_uri in \$$listName",
            description = "Bulk Redirect: $listName",
            enabled = true,
            actionParameters = RedirectActionParametersInput(
                fromList = FromListInput(name = listName, key = "http.request.full_uri"),
            ),
        )
    }
}

@Serializable
data class RedirectActionParametersInput(
    @SerialName("from_list") val fromList: FromListInput,
)

@Serializable
data class FromListInput(val name: String, val key: String)

@Serializable
data class RedirectRuleToggle(val enabled: Boolean)

@Serializable
data class RedirectEntrypointUpdate(val rules: List<RedirectRuleCreate>)
