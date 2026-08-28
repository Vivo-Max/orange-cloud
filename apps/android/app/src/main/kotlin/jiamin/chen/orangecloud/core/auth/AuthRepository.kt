package jiamin.chen.orangecloud.core.auth

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.di.ApplicationScope
import jiamin.chen.orangecloud.core.network.AccessTokenProvider
import jiamin.chen.orangecloud.core.network.ApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** 认证 UI 状态（sessions + 当前身份）。 */
data class AuthState(
    val sessions: List<AuthSessionMeta> = emptyList(),
    val currentSessionId: String? = null,
    /** 持久化已读取完毕（用于启动期决定显示登录页还是主界面，避免闪烁） */
    val isReady: Boolean = false,
    /** 最近一次回调失败原因（UI 映射为本地化文案后展示），成功登录或重试时清空 */
    val redirectError: String? = null,
) {
    val isLoggedIn: Boolean get() = currentSessionId != null
    val currentSession: AuthSessionMeta? get() = sessions.firstOrNull { it.id == currentSessionId }
    val grantedScopes: List<String> get() = currentSession?.scopes.orEmpty()
}

/**
 * OAuth 2.0 + PKCE 多身份认证编排（对应 iOS AuthManager）。
 * - 每次登录新增一个身份；退出单身份只移除它；全部退出回登录页。
 * - 实现 AccessTokenProvider 供 CfApiClient 取 token / 刷新。
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    private val dataStore: DataStore<Preferences>,
    private val tokenStore: TokenStore,
    private val oauthApi: CloudflareOAuthApi,
    private val json: Json,
    private val deviceGate: DeviceGate,
) : AccessTokenProvider {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** 发起授权到回调之间的 PKCE 上下文（持久化以扛进程被杀） */
    private data class Pending(val verifier: String, val state: String)

    init {
        externalScope.launch { loadPersisted() }
    }

    private suspend fun loadPersisted() {
        val prefs = dataStore.data.firstOrNull()
        val sessions = prefs?.get(KEY_SESSIONS)?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(AuthSessionMeta.serializer()), raw) }.getOrNull()
        }.orEmpty()
        val current = prefs?.get(KEY_CURRENT)?.takeIf { id -> sessions.any { it.id == id } }
            ?: sessions.firstOrNull()?.id
        _state.value = AuthState(sessions = sessions, currentSessionId = current, isReady = true)
    }

    fun hasScope(scope: String): Boolean = _state.value.grantedScopes.contains(scope)

    fun clearRedirectError() {
        if (_state.value.redirectError != null) {
            _state.value = _state.value.copy(redirectError = null)
        }
    }

    /** 授权 URL 生成失败时向登录页反馈，避免点击后静默无响应。 */
    fun reportRedirectError(reason: String) {
        _state.value = _state.value.copy(redirectError = reason)
    }

    // MARK: - 登录

    /**
     * 构造授权 URL（PKCE + state）。
     *
     * freshLogin（添加账号）不再包 `dash.cloudflare.com/logout?to=<authorize>` 登出跳板——
     * 那会把用户系统浏览器里的 Cloudflare 登录态一并登出。现在 freshLogin 由调用方改走
     * 无痕 WebView（[jiamin.chen.orangecloud.core.util.WebAuthActivity]，进出清 Cookie），
     * 授权 URL 本身两种场景一致。（`prompt=login` 被 Cloudflare 忽略、Chrome 不给第三方
     * 开无痕标签，实测均无效，勿走回头路。）
     */
    suspend fun buildAuthorizationUri(scopeString: String): Uri {
        if (OAuthConfig.clientId.isBlank() || OAuthConfig.redirectUri.isBlank()) {
            throw OAuthRedirectException("missing_oauth_config")
        }

        val verifier = PkceHelper.generateCodeVerifier()
        val challenge = PkceHelper.generateCodeChallenge(verifier)
        val state = UUID.randomUUID().toString()
        savePending(Pending(verifier, state))

        // CF dash OAuth（Hydra 系）只在请求 offline_access 时才签发 refresh token；
        // 2026-06-29 client 轮换后不带它的登录拿不到 refresh token，access token 到期后
        // refreshAccessToken 走 removeSession → 用户被「自动退出账号」（issue #44 楼层反馈）。
        // 与 iOS 1.8.2(26) 同修：在唯一咽喉点统一追加，勿在 UI 层散落。
        val scopeWithOffline =
            if (scopeString.split(" ").contains("offline_access")) scopeString
            else "$scopeString offline_access"

        return Uri.parse(OAuthConfig.AUTHORIZATION_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", OAuthConfig.clientId)
            .appendQueryParameter("redirect_uri", OAuthConfig.redirectUri)
            .appendQueryParameter("scope", scopeWithOffline)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /** 处理 orangecloud://oauth/callback：验 state → 换 token → 新增身份并切到它。 */
    suspend fun handleRedirect(uri: Uri): Result<Unit> {
        val result = runCatching { performRedirect(uri) }
        result.exceptionOrNull()?.let { e ->
            val reason = (e as? OAuthRedirectException)?.reason ?: e.message ?: "error"
            _state.value = _state.value.copy(redirectError = reason)
        }
        return result
    }

    private suspend fun performRedirect(uri: Uri) {
        uri.getQueryParameter("error")?.let { throw OAuthRedirectException(it) }
        val code = uri.getQueryParameter("code") ?: throw OAuthRedirectException("invalid_callback")
        val state = uri.getQueryParameter("state") ?: throw OAuthRedirectException("invalid_callback")
        val pending = loadPending() ?: throw OAuthRedirectException("invalid_callback")
        if (state != pending.state) throw OAuthRedirectException("state_mismatch")

        val token = exchangeCode(code, pending.verifier)
        val id = UUID.randomUUID().toString()
        tokenStore.save(id, token)
        val scopes = token.scope.split(" ").filter { it.isNotEmpty() }.sorted()
        val label = oauthApi.fetchUserInfo(token.accessToken)?.let { it.email ?: it.name }
            ?: context.getString(R.string.default_account_label, _state.value.sessions.size + 1)
        val sessions = _state.value.sessions + AuthSessionMeta(id, label, scopes)
        _state.value = _state.value.copy(sessions = sessions, currentSessionId = id, redirectError = null)
        persist()
        clearPending()
        // 登录成功静默登记设备（用于管理台设备名单；失败忽略，不影响登录）
        externalScope.launch { deviceGate.checkin() }
    }

    private suspend fun exchangeCode(code: String, verifier: String): StoredToken =
        oauthApi.requestToken(
            mapOf(
                "grant_type" to "authorization_code",
                "client_id" to OAuthConfig.clientId,
                "code" to code,
                "redirect_uri" to OAuthConfig.redirectUri,
                "code_verifier" to verifier,
            ),
        ).toStoredToken(previousScope = "", previousRefresh = null)

    // MARK: - AccessTokenProvider

    override suspend fun validAccessToken(): String {
        val sessionId = _state.value.currentSessionId ?: throw ApiError.Unauthorized
        // 设备门禁（节流）：已登录设备被封后 5 分钟内生效——吊销 token 并清除本地身份，
        // 用户表现为普通的"登录失效"，不暴露封禁语义
        if (deviceGate.isBannedThrottled()) {
            logout(sessionId)
            throw ApiError.Unauthorized
        }
        val token = tokenStore.load(sessionId) ?: throw ApiError.Unauthorized
        val secondsLeft = token.expiresAtEpochSeconds - nowSeconds()
        return if (secondsLeft < 60) refreshAccessToken() else token.accessToken
    }

    override suspend fun refreshAccessToken(): String {
        val sessionId = _state.value.currentSessionId
        val stored = sessionId?.let { tokenStore.load(it) }
        val refresh = stored?.refreshToken
        if (sessionId == null || stored == null || refresh == null) {
            sessionId?.let { removeSession(it) }
            throw ApiError.Unauthorized
        }
        return try {
            val newToken = oauthApi.requestToken(
                mapOf(
                    "grant_type" to "refresh_token",
                    "client_id" to OAuthConfig.clientId,
                    "refresh_token" to refresh,
                ),
            ).toStoredToken(previousScope = stored.scope, previousRefresh = refresh)
            tokenStore.save(sessionId, newToken)
            newToken.accessToken
        } catch (e: Exception) {
            // refresh_token 失效：移除该身份（其他身份不受影响）
            removeSession(sessionId)
            throw ApiError.Unauthorized
        }
    }

    // MARK: - 身份管理

    fun switchSession(id: String) {
        if (_state.value.sessions.none { it.id == id }) return
        _state.value = _state.value.copy(currentSessionId = id)
        externalScope.launch { persist() }
    }

    fun updateSessionLabel(id: String, label: String) {
        if (label.isEmpty()) return
        val updated = _state.value.sessions.map {
            if (it.id == id && it.label != label) it.copy(label = label) else it
        }
        if (updated == _state.value.sessions) return
        _state.value = _state.value.copy(sessions = updated)
        externalScope.launch { persist() }
    }

    suspend fun logout(sessionId: String, revoke: Boolean = true) {
        if (revoke) {
            tokenStore.load(sessionId)?.let { token ->
                oauthApi.revoke(
                    mapOf(
                        "client_id" to OAuthConfig.clientId,
                        "token" to (token.refreshToken ?: token.accessToken),
                    ),
                )
            }
        }
        removeSession(sessionId)
    }

    private suspend fun removeSession(id: String) {
        tokenStore.clear(id)
        val sessions = _state.value.sessions.filterNot { it.id == id }
        val current = if (_state.value.currentSessionId == id) sessions.firstOrNull()?.id
        else _state.value.currentSessionId
        _state.value = _state.value.copy(sessions = sessions, currentSessionId = current)
        persist()
    }

    // MARK: - 持久化

    private suspend fun persist() {
        dataStore.edit { prefs ->
            prefs[KEY_SESSIONS] =
                json.encodeToString(ListSerializer(AuthSessionMeta.serializer()), _state.value.sessions)
            _state.value.currentSessionId?.let { prefs[KEY_CURRENT] = it } ?: prefs.remove(KEY_CURRENT)
        }
    }

    private suspend fun savePending(pending: Pending) {
        dataStore.edit {
            it[KEY_PENDING_VERIFIER] = pending.verifier
            it[KEY_PENDING_STATE] = pending.state
        }
    }

    private suspend fun loadPending(): Pending? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        val v = prefs[KEY_PENDING_VERIFIER] ?: return null
        val s = prefs[KEY_PENDING_STATE] ?: return null
        return Pending(v, s)
    }

    private suspend fun clearPending() {
        dataStore.edit {
            it.remove(KEY_PENDING_VERIFIER)
            it.remove(KEY_PENDING_STATE)
        }
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    companion object {
        private val KEY_SESSIONS = stringPreferencesKey("auth_sessions")
        private val KEY_CURRENT = stringPreferencesKey("auth_current_session")
        private val KEY_PENDING_VERIFIER = stringPreferencesKey("auth_pending_verifier")
        private val KEY_PENDING_STATE = stringPreferencesKey("auth_pending_state")
    }
}

private fun TokenResponse.toStoredToken(previousScope: String, previousRefresh: String?): StoredToken =
    StoredToken(
        accessToken = accessToken,
        refreshToken = refreshToken ?: previousRefresh,
        expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + expiresIn,
        scope = scope ?: previousScope,
    )

/** 回调处理失败原因（reason 由 UI 层映射为本地化文案）。 */
class OAuthRedirectException(val reason: String) : Exception(reason)
