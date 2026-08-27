package jiamin.chen.orangecloud.core.auth

import jiamin.chen.orangecloud.BuildConfig

/**
 * OAuth 配置（与 iOS Core/Auth/OAuthConfig.swift 对应）。
 * clientId / redirectUri 经 Gradle 注入：play / direct / oss 默认与官方客户端一致；
 * 自编译者可通过 local.properties 或 -P 覆盖为自己的 OAuth Client 与回调中转。
 * Cloudflare OAuth 只接受 https redirect，指向 Web 后端回调中转，再 302 跳回自定义 scheme。
 */
object OAuthConfig {
    /** OAuth Client 为 PKCE 公开标识符（非机密），由 BuildConfig 注入。 */
    val clientId: String = BuildConfig.OAUTH_CLIENT_ID

    const val CALLBACK_SCHEME = "orangecloud"
    const val CALLBACK_HOST = "oauth"

    /** 按风味由 Gradle 注入；自编译者可用 OAUTH_REDIRECT_URI 指向自建回调中转。 */
    val redirectUri: String = BuildConfig.OAUTH_REDIRECT_URI

    const val AUTHORIZATION_URL = "https://dash.cloudflare.com/oauth2/auth"
    /** 网页登出端点：添加账号时先登出再续跳授权页，避免复用上一个登录态（支持 ?to= 续跳）。 */
    const val LOGOUT_URL = "https://dash.cloudflare.com/logout"
    const val TOKEN_URL = "https://dash.cloudflare.com/oauth2/token"
    const val REVOKE_URL = "https://dash.cloudflare.com/oauth2/revoke"
    const val USERINFO_URL = "https://dash.cloudflare.com/oauth2/userinfo"
}
