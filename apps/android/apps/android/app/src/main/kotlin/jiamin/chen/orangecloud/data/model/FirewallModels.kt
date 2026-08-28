package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.Serializable

// MARK: - IP 访问规则（legacy firewall access rules）
// /zones/{id}/firewall/access_rules/rules，读 firewall-services.read，写 .write。
// 注意：configuration（target+value）创建后不可改，编辑仅改 mode + notes。对应 iOS FirewallAccessRuleModels。

@Serializable
data class FirewallAccessRule(
    val id: String,
    val mode: String? = null,
    val configuration: AccessRuleConfig? = null,
    val notes: String? = null,
)

@Serializable
data class AccessRuleConfig(
    val target: String? = null,
    val value: String? = null,
)

// MARK: - 写入载荷

@Serializable
data class AccessRuleConfigInput(
    val target: String,
    val value: String,
)

@Serializable
data class AccessRuleCreate(
    val mode: String,
    val configuration: AccessRuleConfigInput,
    val notes: String? = null,
)

/** 编辑只动 mode + notes（configuration 不可变）。 */
@Serializable
data class AccessRuleUpdate(
    val mode: String,
    val notes: String? = null,
)
