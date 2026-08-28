package jiamin.chen.orangecloud.ui.firewall

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.AccessRuleConfigInput
import jiamin.chen.orangecloud.data.model.AccessRuleCreate
import jiamin.chen.orangecloud.data.model.AccessRuleUpdate
import jiamin.chen.orangecloud.data.model.FirewallAccessRule
import jiamin.chen.orangecloud.data.repository.FirewallRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 访问规则动作（对应 iOS AccessRuleMode）。顺序贴合 Cloudflare 仪表盘。 */
enum class AccessRuleMode(val raw: String, @StringRes val labelRes: Int) {
    BLOCK("block", R.string.ip_mode_block),
    MANAGED_CHALLENGE("managed_challenge", R.string.ip_mode_managed),
    JS_CHALLENGE("js_challenge", R.string.ip_mode_js),
    CHALLENGE("challenge", R.string.ip_mode_challenge),
    WHITELIST("whitelist", R.string.ip_mode_allow);

    companion object {
        fun fromRaw(raw: String?): AccessRuleMode = entries.firstOrNull { it.raw == raw } ?: BLOCK
    }
}

/** 匹配目标（对应 iOS AccessRuleTarget）。 */
enum class AccessRuleTarget(val raw: String, @StringRes val labelRes: Int, val placeholder: String) {
    IP("ip", R.string.ip_target_ip, "192.0.2.1"),
    IP6("ip6", R.string.ip_target_ip6, "2001:db8::1"),
    IP_RANGE("ip_range", R.string.ip_target_range, "192.0.2.0/24"),
    ASN("asn", R.string.ip_target_asn, "AS13335"),
    COUNTRY("country", R.string.ip_target_country, "US");

    companion object {
        fun fromRaw(raw: String?): AccessRuleTarget = entries.firstOrNull { it.raw == raw } ?: IP
    }
}

sealed interface AccessRuleEvent {
    data object Saved : AccessRuleEvent
    data object Deleted : AccessRuleEvent
    data class Error(val message: String?) : AccessRuleEvent
}

data class ZoneAccessRulesUiState(
    val zoneName: String = "",
    val rules: List<FirewallAccessRule> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loaded: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
)

@HiltViewModel
class ZoneAccessRulesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FirewallRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.FIREWALL_READ)
    private val canWrite = authRepository.hasScope(Scopes.FIREWALL_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneAccessRulesUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneAccessRulesUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<AccessRuleEvent>(Channel.BUFFERED)
    val events: Flow<AccessRuleEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val rules = repository.rules(zoneId)
                _uiState.update { it.copy(rules = rules, loaded = true) }
            } catch (e: Exception) {
                eventChannel.send(AccessRuleEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun create(mode: AccessRuleMode, target: AccessRuleTarget, value: String, notes: String?) {
        if (!canWrite || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val created = repository.createRule(
                    zoneId,
                    AccessRuleCreate(
                        mode = mode.raw,
                        configuration = AccessRuleConfigInput(target.raw, value.trim()),
                        notes = notes?.trim()?.ifBlank { null },
                    ),
                )
                _uiState.update { it.copy(rules = listOf(created) + it.rules) }
                eventChannel.send(AccessRuleEvent.Saved)
            } catch (e: Exception) {
                eventChannel.send(AccessRuleEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun update(ruleId: String, mode: AccessRuleMode, notes: String?) {
        if (!canWrite || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val updated = repository.updateRule(zoneId, ruleId, AccessRuleUpdate(mode.raw, notes?.trim()?.ifBlank { null }))
                _uiState.update { st -> st.copy(rules = st.rules.map { if (it.id == ruleId) updated else it }) }
                eventChannel.send(AccessRuleEvent.Saved)
            } catch (e: Exception) {
                eventChannel.send(AccessRuleEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete(rule: FirewallAccessRule) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteRule(zoneId, rule.id)
                _uiState.update { it.copy(rules = it.rules.filterNot { r -> r.id == rule.id }) }
                eventChannel.send(AccessRuleEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(AccessRuleEvent.Error(e.message))
                load()
            }
        }
    }
}
