package jiamin.chen.orangecloud.ui.ratelimit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.RateLimitRule
import jiamin.chen.orangecloud.data.model.RateLimitRuleCreate
import jiamin.chen.orangecloud.data.model.RateLimitRuleset
import jiamin.chen.orangecloud.data.repository.RateLimitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RateLimitEvent {
    data object Saved : RateLimitEvent
    data object Deleted : RateLimitEvent
    data class Error(val message: String?) : RateLimitEvent
}

data class ZoneRateLimitUiState(
    val zoneName: String = "",
    val ruleset: RateLimitRuleset? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val togglingRuleId: String? = null,
) {
    val rules: List<RateLimitRule> get() = ruleset?.rules.orEmpty()
}

@HiltViewModel
class ZoneRateLimitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RateLimitRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    // Rate Limiting 复用 WAF 权限组（zone-waf.read/.write）。
    private val hasRead = authRepository.hasScope(Scopes.WAF_READ)
    private val canWrite = authRepository.hasScope(Scopes.WAF_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneRateLimitUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneRateLimitUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<RateLimitEvent>(Channel.BUFFERED)
    val events: Flow<RateLimitEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val ruleset = repository.ruleset(zoneId)
                _uiState.update { it.copy(ruleset = ruleset) }
            } catch (e: Exception) {
                eventChannel.send(RateLimitEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(rule: RateLimitRule, enabled: Boolean) {
        val rulesetId = _uiState.value.ruleset?.id ?: return
        if (!canWrite || _uiState.value.togglingRuleId != null) return
        _uiState.update { it.copy(togglingRuleId = rule.id) }
        viewModelScope.launch {
            try {
                val updated = repository.setRuleEnabled(zoneId, rulesetId, rule.id, enabled)
                _uiState.update { it.copy(ruleset = updated) }
            } catch (e: Exception) {
                eventChannel.send(RateLimitEvent.Error(e.message))
                load()
            } finally {
                _uiState.update { it.copy(togglingRuleId = null) }
            }
        }
    }

    fun save(
        ruleId: String?,
        expression: String,
        requests: Int,
        period: Int,
        action: String,
        mitigationTimeout: Int,
        description: String?,
        enabled: Boolean,
    ) {
        if (!canWrite || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val draft = RateLimitRuleCreate.make(
                    expression = expression,
                    requests = requests,
                    period = period,
                    action = action,
                    mitigationTimeout = mitigationTimeout,
                    description = description?.takeIf { it.isNotBlank() },
                    enabled = enabled,
                )
                val rulesetId = _uiState.value.ruleset?.id
                val updated = when {
                    ruleId != null && rulesetId != null -> repository.updateRule(zoneId, rulesetId, ruleId, draft)
                    rulesetId != null -> repository.addRule(zoneId, rulesetId, draft)
                    else -> repository.createEntrypoint(zoneId, draft)
                }
                _uiState.update { it.copy(ruleset = updated) }
                eventChannel.send(RateLimitEvent.Saved)
            } catch (e: Exception) {
                eventChannel.send(RateLimitEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete(rule: RateLimitRule) {
        val rulesetId = _uiState.value.ruleset?.id ?: return
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteRule(zoneId, rulesetId, rule.id)
                val refreshed = repository.ruleset(zoneId)
                _uiState.update { it.copy(ruleset = refreshed) }
                eventChannel.send(RateLimitEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(RateLimitEvent.Error(e.message))
                load()
            }
        }
    }
}
