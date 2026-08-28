package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.di.ApplicationScope
import jiamin.chen.orangecloud.core.system.AppPrefs
import jiamin.chen.orangecloud.data.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前身份下的账号列表与选中账号（对应 iOS SessionStore 的账号选择职责）。
 * 所有账号级模块（Zones / Workers / 存储 / 分析）统一从这里取 selectedAccountId 作用域，
 * 切账号时各 @flatMapLatest 自动重查（规避多账号资源错配）。
 */
@Singleton
class AccountStore @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val appPrefs: AppPrefs,
    @ApplicationScope private val externalScope: CoroutineScope,
) {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    val selectedAccount: Account?
        get() = _accounts.value.firstOrNull { it.id == _selectedAccountId.value }

    private val mutex = Mutex()
    private var loaded = false

    init {
        // 登录身份变化（切换 / 新增登录 / 登出）时重置账号作用域——否则 loaded 短路会让这里
        // 一直端着上一个身份的账号列表，切完身份全 App 仍显示旧账号数据。冷启动首值跳过
        // （由各页 ensureLoaded 正常加载）。借鉴 fork a422015028，保留 ensureLoaded 幂等。
        externalScope.launch {
            authRepository.state
                .filter { it.isReady }
                .map { it.currentSessionId }
                .distinctUntilChanged()
                .drop(1)
                .collect { sessionId ->
                    mutex.withLock {
                        _accounts.value = emptyList()
                        _selectedAccountId.value = null
                        loaded = false
                    }
                    if (sessionId != null) {
                        runCatching { refresh() }
                    }
                }
        }
    }

    /** 幂等加载账号列表，首个账号设为当前账号。 */
    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            applyAccounts(accountRepository.listAccounts())
        }
    }

    /** 强制刷新账号列表（添加账号后等）。 */
    suspend fun refresh() {
        mutex.withLock {
            applyAccounts(accountRepository.listAccounts())
        }
    }

    fun select(accountId: String) {
        if (_accounts.value.any { it.id == accountId }) {
            _selectedAccountId.value = accountId
            // 记住这个身份的默认账号，下次冷启动直接进它（issue #71）
            val sessionId = authRepository.state.value.currentSessionId
            if (sessionId != null) {
                externalScope.launch { appPrefs.setDefaultAccountId(sessionId, accountId) }
            }
        }
    }

    private suspend fun applyAccounts(list: List<Account>) {
        _accounts.value = list
        val current = _selectedAccountId.value
        if (current == null || list.none { it.id == current }) {
            // 优先用用户上次选定的默认账号，落空（首次登录 / 账号已被移除）才回退列表第一个
            val sessionId = authRepository.state.value.currentSessionId
            val preferred = sessionId?.let { id -> appPrefs.defaultAccountId(id) }
            _selectedAccountId.value = list.firstOrNull { it.id == preferred }?.id ?: list.firstOrNull()?.id
        }
        loaded = list.isNotEmpty()
    }
}
