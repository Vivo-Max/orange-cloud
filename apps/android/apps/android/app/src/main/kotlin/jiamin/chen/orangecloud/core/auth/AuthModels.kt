package jiamin.chen.orangecloud.core.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 持久化的 token（加密后存 DataStore，按身份 UUID）。与 iOS TokenStore.StoredToken 对应。 */
@Serializable
data class StoredToken(
    val accessToken: String,
    val refreshToken: String? = null,
    /** 绝对过期时刻（epoch 秒） */
    val expiresAtEpochSeconds: Long,
    val scope: String = "",
)

/** 登录身份的展示信息（token 本体在 TokenStore）。与 iOS AuthSessionMeta 对应。 */
@Serializable
data class AuthSessionMeta(
    val id: String,
    val label: String,
    val scopes: List<String> = emptyList(),
)

/** OAuth token 端点响应（form 换 token / refresh）。 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)

/** userinfo 端点（取邮箱作身份标签，best-effort）。 */
@Serializable
data class UserInfo(
    val email: String? = null,
    val name: String? = null,
)
