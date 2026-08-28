package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Turnstile 人机验证组件（账号级 /accounts/{id}/challenges/widgets，
 * OAuth scope challenge-widgets.read / .write）。与 iOS TurnstileModels 对齐。
 *
 * 端点与字段以 api-schemas OpenAPI 为准（2026-08 核对）：
 * · widget 主键是 sitekey；secret 在 list/get/create/rotate 响应携带
 * · mode 枚举：managed / non-interactive / invisible
 * · domains 最多 10 条（主机名或 IP，含子域生效）；空数组 = 允许任意主机名
 * · region（world / china）创建后不可改；offlabel / ephemeral_id 是 ENT 专属，
 *   只透传显示、不提供编辑（免费/付费账号写它必 403）
 */
@Serializable
data class TurnstileWidget(
    val sitekey: String = "",
    val name: String = "",
    val mode: String = "managed",
    val domains: List<String> = emptyList(),
    /** 服务端 siteverify 用密钥。UI 默认遮蔽、点按显示/复制。 */
    val secret: String? = null,
    @SerialName("bot_fight_mode") val botFightMode: Boolean? = null,
    @SerialName("clearance_level") val clearanceLevel: String? = null,
    val region: String? = null,
    val offlabel: Boolean? = null,
    @SerialName("ephemeral_id") val ephemeralId: Boolean? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
)

/**
 * 创建 / 更新载荷（PUT 必填 name + mode + domains）。
 * region 仅创建时有效；clearance_level 编辑时原样回写避免 PUT 丢配置。
 */
@Serializable
data class TurnstileWidgetInput(
    val name: String,
    val mode: String,
    val domains: List<String>,
    @SerialName("bot_fight_mode") val botFightMode: Boolean? = null,
    val region: String? = null,
    @SerialName("clearance_level") val clearanceLevel: String? = null,
)

/** 轮换密钥请求体。invalidateImmediately = false 时旧密钥保留 2 小时宽限。 */
@Serializable
data class TurnstileRotateRequest(
    @SerialName("invalidate_immediately") val invalidateImmediately: Boolean,
)
