package jiamin.chen.orangecloud.ui.transform

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.TransformRule
import jiamin.chen.orangecloud.data.model.TransformRuleCreate
import jiamin.chen.orangecloud.data.model.TransformRuleset
import jiamin.chen.orangecloud.data.repository.TransformRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Transform 三个 phase（对应 iOS TransformPhase）。 */
enum class TransformPhase(val raw: String, @StringRes val titleRes: Int, val isUrlRewrite: Boolean) {
    REQUEST_URL("http_request_transform", R.string.tf_phase_url, true),
    REQUEST_HEAD("http_request_late_transform", R.string.tf_phase_req_head, false),
    RESPONSE_HEAD("http_response_headers_transform", R.string.tf_phase_resp_head, false),
}

/** 请求/响应头操作类型。 */
enum class HeaderOperation(val raw: String, @StringRes val labelRes: Int) {
    SET("set", R.string.tf_op_set),
    ADD("add", R.string.tf_op_add),
    REMOVE("remove", R.string.tf_op_remove);

    companion object {
        fun fromRaw(raw: String?): HeaderOperation = entries.firstOrNull { it.raw == raw } ?: SET
    }
}

sealed interface TransformEvent {
    data object Saved : TransformEvent
    data object Deleted : TransformEvent
    data class Error(val message: String?) : TransformEvent
}

data class ZoneTransformUiState(
    val zoneName: String = "",
    val rulesetByPhase: Map<String, TransformRuleset> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loaded: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val togglingRuleId: String? = null,
) {
    fun rules(phase: TransformPhase): List<TransformRule> = rulesetByPhase[phase.raw]?.rules.orEmpty()
}

@HiltViewModel
class ZoneTransformViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TransformRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.TRANSFORM_READ)
    private val canWrite = authRepository.hasScope(Scopes.TRANSFORM_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneTransformUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneTransformUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TransformEvent>(Channel.BUFFERED)
    val events: Flow<TransformEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val map = buildMap {
                    TransformPhase.entries.forEach { phase ->
                        repository.ruleset(zoneId, phase.raw)?.let { put(phase.raw, it) }
                    }
                }
                _uiState.update { it.copy(rulesetByPhase = map, loaded = true) }
            } catch (e: Exception) {
                eventChannel.send(TransformEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(phase: TransformPhase, rule: TransformRule, enabled: Boolean) {
        val rulesetId = _uiState.value.rulesetByPhase[phase.raw]?.id ?: return
        if (!canWrite || _uiState.value.togglingRuleId != null) return
        _uiState.update { it.copy(togglingRuleId = rule.id) }
        viewModelScope.launch {
            try {
                val updated = repository.setRuleEnabled(zoneId, rulesetId, rule.id, enabled)
                _uiState.update { it.copy(rulesetByPhase = it.rulesetByPhase + (phase.raw to updated)) }
            } catch (e: Exception) {
                eventChannel.send(TransformEvent.Error(e.message))
                load()
            } finally {
                _uiState.update { it.copy(togglingRuleId = null) }
            }
        }
    }

    /** 新建（ruleId == null）或更新单条规则。 */
    fun save(phase: TransformPhase, ruleId: String?, draft: TransformRuleCreate) {
        if (!canWrite || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val rulesetId = _uiState.value.rulesetByPhase[phase.raw]?.id
                val updated = when {
                    ruleId != null && rulesetId != null -> repository.updateRule(zoneId, rulesetId, ruleId, draft)
                    rulesetId != null -> repository.addRule(zoneId, rulesetId, draft)
                    else -> repository.createEntrypoint(zoneId, phase.raw, draft)
                }
                _uiState.update { it.copy(rulesetByPhase = it.rulesetByPhase + (phase.raw to updated)) }
                eventChannel.send(TransformEvent.Saved)
            } catch (e: Exception) {
                eventChannel.send(TransformEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete(phase: TransformPhase, rule: TransformRule) {
        val rulesetId = _uiState.value.rulesetByPhase[phase.raw]?.id ?: return
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteRule(zoneId, rulesetId, rule.id)
                // 重读该 phase 以拿到最新 ruleset（可能因删空而 entrypoint 消失）
                val refreshed = repository.ruleset(zoneId, phase.raw)
                _uiState.update {
                    val map = if (refreshed != null) it.rulesetByPhase + (phase.raw to refreshed)
                    else it.rulesetByPhase - phase.raw
                    it.copy(rulesetByPhase = map)
                }
                eventChannel.send(TransformEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(TransformEvent.Error(e.message))
                load()
            }
        }
    }
}
