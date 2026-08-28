package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cloudflare Registrar 注册的域名。
 *
 * ⚠️ 必须用新版 API：旧的 /accounts/{id}/registrar/domains 于 2026-06-29 弃用、
 * **2026-09-27 停用**；新版在 /accounts/{id}/registrar/registrations 下。
 *
 * 两个由接口决定的边界：
 * · PATCH **只支持 auto_renew**（规范原文：currently supports updating auto_renew only），
 *   [locked] 转移锁是只读的，不要做成开关
 * · PATCH 返回异步 workflow 状态而非更新后的对象，写入后需回读列表
 */
@Serializable
data class DomainRegistration(
    @SerialName("domain_name") val domainName: String,
    /** 到期时间。registration_pending 期间可能为 null */
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    /** 自动续费：开启即授权 Cloudflare 在到期前 30 天内扣默认支付方式 */
    @SerialName("auto_renew") val autoRenew: Boolean? = null,
    /** 是否锁定转移。**只读** */
    val locked: Boolean? = null,
    @SerialName("privacy_mode") val privacyMode: String? = null,
    /** active / registration_pending / expired / suspended / redemption_period */
    val status: String? = null,
)

/** PATCH 体。接口目前只认 auto_renew。 */
@Serializable
data class RegistrationUpdate(@SerialName("auto_renew") val autoRenew: Boolean)

/** PATCH 的异步 workflow 结果，只取判定完成所需字段 */
@Serializable
data class RegistrarWorkflowStatus(
    val completed: Boolean? = null,
    val state: String? = null,
)
