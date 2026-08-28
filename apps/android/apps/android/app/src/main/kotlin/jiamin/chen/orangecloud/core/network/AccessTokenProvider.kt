package jiamin.chen.orangecloud.core.network

/**
 * 给 CfApiClient 提供当前身份的有效 access token，并在 401 时刷新。
 * 由 AuthRepository 实现，避免 CfApiClient 直接依赖认证编排（打破循环）。
 */
interface AccessTokenProvider {
    /** 当前身份的有效 token（临期 60s 内则先刷新）。无登录态抛 ApiError.Unauthorized。 */
    suspend fun validAccessToken(): String

    /** 强制刷新当前身份 token，返回新的 access token。 */
    suspend fun refreshAccessToken(): String
}
