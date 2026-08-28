package jiamin.chen.orangecloud.core.auth

import jiamin.chen.orangecloud.core.network.ApiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** OAuth token / refresh / revoke / userinfo 端点（form-urlencoded），不经 CfApiClient。 */
@Singleton
class CloudflareOAuthApi @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun requestToken(params: Map<String, String>): TokenResponse {
        val request = Request.Builder().url(OAuthConfig.TOKEN_URL).post(formBody(params)).build()
        val (code, text) = execute(request)
        if (code !in 200..299) throw TokenExchangeException("HTTP $code $text")
        return runCatching { json.decodeFromString(TokenResponse.serializer(), text) }
            .getOrElse { throw TokenExchangeException("token response decode failed") }
    }

    suspend fun revoke(params: Map<String, String>) {
        val request = Request.Builder().url(OAuthConfig.REVOKE_URL).post(formBody(params)).build()
        runCatching { execute(request) } // 尽力撤销，失败不阻塞
    }

    suspend fun fetchUserInfo(accessToken: String): UserInfo? {
        val request = Request.Builder()
            .url(OAuthConfig.USERINFO_URL)
            .header("Authorization", "Bearer $accessToken")
            .build()
        val (code, text) = runCatching { execute(request) }.getOrNull() ?: return null
        if (code !in 200..299) return null
        return runCatching { json.decodeFromString(UserInfo.serializer(), text) }.getOrNull()
    }

    private fun formBody(params: Map<String, String>): FormBody =
        FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()

    private suspend fun execute(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { resp ->
                resp.code to (resp.body?.string() ?: "")
            }
        } catch (e: IOException) {
            throw ApiError.Network(e)
        }
    }
}

class TokenExchangeException(message: String) : Exception(message)
