package jiamin.chen.orangecloud.ui.zerotrust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.AccessApp
import jiamin.chen.orangecloud.data.model.AccessAppInput
import jiamin.chen.orangecloud.data.model.GatewayRule
import jiamin.chen.orangecloud.data.model.GatewayRuleInput
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.ZeroTrustRepository
import jiamin.chen.orangecloud.ui.storage.StorageListViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// MARK: - Access 应用（account 级，只读）

@HiltViewModel
class AccessAppsViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: ZeroTrustRepository,
    authRepository: AuthRepository,
) : StorageListViewModel<AccessApp>(accountStore, authRepository.hasScope(Scopes.ACCESS_READ)) {
    val canWrite: Boolean = authRepository.hasScope(Scopes.ACCESS_WRITE)
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()
    override suspend fun fetch(accountId: String): List<AccessApp> = repository.accessApps(accountId)
    init { load() }

    private fun op(block: suspend (String) -> Unit) {
        if (!canWrite || _busy.value) return
        _busy.update { true }
        viewModelScope.launch {
            try {
                val acct = accountStore.selectedAccountId.value ?: error("no account")
                block(acct); load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _busy.update { false }
            }
        }
    }

    /** 建/改应用：先建一条可复用策略（decision + include 规则），再建/改应用引用它。 */
    fun save(
        appId: String?, name: String, domain: String, type: String, sessionDuration: String,
        decision: String, includeRules: List<Pair<String, String>>,
    ) = op { acct ->
        val policyId = repository.createAccessPolicy(acct, "$name policy", decision, includeRules.ifEmpty { listOf("everyone" to "") })
        val body = AccessAppInput(name, domain, type, sessionDuration.ifBlank { null }, listOf(policyId))
        if (appId == null) repository.createAccessApp(acct, body) else repository.updateAccessApp(acct, appId, body)
    }

    fun delete(app: AccessApp) = op { repository.deleteAccessApp(it, app.id) }
}

// MARK: - Gateway 策略（account 级，列表 + 启停）

data class GatewayUiState(
    val rules: List<GatewayRule> = emptyList(),
    val isLoading: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val hasError: Boolean = false,
    val togglingId: String? = null,
)

@HiltViewModel
class GatewayRulesViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: ZeroTrustRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasRead = authRepository.hasScope(Scopes.TEAMS_READ)
    private val canWrite = authRepository.hasScope(Scopes.TEAMS_WRITE)

    private val _uiState = MutableStateFlow(GatewayUiState(isLoading = hasRead, missingScope = !hasRead, canWrite = canWrite))
    val uiState: StateFlow<GatewayUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val rules = repository.gatewayRules(accountId).sortedBy { it.precedence ?: Int.MAX_VALUE }
                _uiState.update { it.copy(rules = rules) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(rule: GatewayRule, enabled: Boolean) {
        if (!canWrite || _uiState.value.togglingId != null) return
        _uiState.update { it.copy(togglingId = rule.id) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val updated = repository.setGatewayRuleEnabled(accountId, rule, enabled)
                _uiState.update { st -> st.copy(rules = st.rules.map { if (it.id == rule.id) updated else it }) }
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
                load()
            } finally {
                _uiState.update { it.copy(togglingId = null) }
            }
        }
    }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** 新建（rule == null）或编辑 Gateway 策略；编辑时保留原 rule_settings 等未建模字段。 */
    fun save(rule: GatewayRule?, name: String, filterType: String, action: String, traffic: String, enabled: Boolean) {
        if (!canWrite || _busy.value) return
        _busy.update { true }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val body = if (rule != null) {
                    GatewayRuleInput.fromRule(rule).copy(name = name, filters = listOf(filterType), action = action, traffic = traffic.ifBlank { null }, enabled = enabled)
                } else {
                    GatewayRuleInput(name = name, description = null, action = action, enabled = enabled, filters = listOf(filterType), traffic = traffic.ifBlank { null }, identity = null, devicePosture = null, precedence = null, ruleSettings = null)
                }
                if (rule == null) repository.createGatewayRule(accountId, body) else repository.updateGatewayRule(accountId, rule.id, body)
                load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _busy.update { false }
            }
        }
    }

    fun delete(rule: GatewayRule) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.deleteGatewayRule(accountId, rule.id)
                _uiState.update { st -> st.copy(rules = st.rules.filterNot { it.id == rule.id }) }
            } catch (e: Exception) {
                eventChannel.send(e.message ?: ""); load()
            }
        }
    }
}
