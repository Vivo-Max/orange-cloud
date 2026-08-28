package jiamin.chen.orangecloud.ui.healthcheck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.HealthCheck
import jiamin.chen.orangecloud.core.push.PushPrefs
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.AlertingRepository
import jiamin.chen.orangecloud.data.repository.HealthCheckRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthCheckUiState(
    val zoneName: String = "",
    val checks: List<HealthCheck> = emptyList(),
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val isMutating: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    // 源站异常推送：走 CF 原生 health_check_status_notification（服务端直推，App 关着也收得到）
    val alertAvailable: Boolean = false,
    val alertEnabled: Boolean = false,
    val canWriteAlerts: Boolean = false,
)

sealed interface HealthCheckEvent {
    data class Error(val message: String?) : HealthCheckEvent
}

@HiltViewModel
class HealthCheckViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HealthCheckRepository,
    private val alertingRepository: AlertingRepository,
    private val accountStore: AccountStore,
    private val pushPrefs: PushPrefs,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.HEALTHCHECK_READ)
    private val canWrite = authRepository.hasScope(Scopes.HEALTHCHECK_WRITE)
    private val canReadAlerts = authRepository.hasScope(Scopes.NOTIFICATIONS_READ)
    private val canWriteAlerts = authRepository.hasScope(Scopes.NOTIFICATIONS_WRITE)
    private var alertPolicyId: String? = null
    private var alertWebhookId: String? = null

    private val _uiState = MutableStateFlow(
        HealthCheckUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
            canWriteAlerts = canWriteAlerts,
        ),
    )
    val uiState: StateFlow<HealthCheckUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<HealthCheckEvent>(Channel.BUFFERED)
    val events: Flow<HealthCheckEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
        if (canReadAlerts) loadAlertState()
    }

    /** CF 的健康检查状态告警类型 */
    private val alertType = "health_check_status_notification"

    private suspend fun webhookUrl(): String? =
        pushPrefs.deviceKeyNow()?.let { key -> "${pushPrefs.serverUrlNow()}/$key/cf" }

    private fun loadAlertState() {
        viewModelScope.launch {
            if (webhookUrl() == null) return@launch
            accountStore.ensureLoaded()
            val accountId = accountStore.selectedAccountId.value ?: return@launch
            // 该账号是否支持这个告警类型（免费套餐可能没有）
            val available = runCatching { alertingRepository.availableAlerts(accountId) }
                .getOrNull()?.values?.flatten()?.any { it.type == alertType } ?: false
            val policy = runCatching { alertingRepository.policies(accountId) }
                .getOrNull()?.firstOrNull { it.alertType == alertType }
            alertPolicyId = policy?.id
            alertWebhookId = runCatching { alertingRepository.webhooks(accountId) }
                .getOrNull()?.firstOrNull { it.url == webhookUrl() }?.id
            _uiState.update { it.copy(alertAvailable = available, alertEnabled = policy != null) }
        }
    }

    fun setAlertEnabled(on: Boolean) {
        if (!canWriteAlerts) return
        viewModelScope.launch {
            val url = webhookUrl() ?: return@launch
            val accountId = accountStore.selectedAccountId.value ?: return@launch
            runCatching {
                if (on) {
                    // 复用已有的推送 webhook，没有才建——避免每开一项告警就多一个目标
                    val hookId = alertWebhookId
                        ?: alertingRepository.createWebhook(accountId, "Orange Cloud Push", url)
                            .also { alertWebhookId = it }
                    alertPolicyId = alertingRepository.createPolicy(
                        accountId, "OC: Health Check", alertType, hookId,
                    )
                } else {
                    alertPolicyId?.let { alertingRepository.deletePolicy(accountId, it) }
                    alertPolicyId = null
                }
            }
                .onSuccess { _uiState.update { s -> s.copy(alertEnabled = on) } }
                .onFailure { eventChannel.send(HealthCheckEvent.Error(it.message)); loadAlertState() }
        }
    }

    /** 异常的排在前面——进页第一眼就该看到出问题的源站。 */
    private fun sorted(items: List<HealthCheck>) = items.sortedWith(
        compareBy<HealthCheck> {
            when (it.displayStatus) {
                "unhealthy" -> 0
                "unknown" -> 1
                "healthy" -> 2
                else -> 3
            }
        }.thenBy { it.name.lowercase() },
    )

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { repository.list(zoneId) }
                .onSuccess { list -> _uiState.update { it.copy(checks = sorted(list), loaded = true) } }
                .onFailure { eventChannel.send(HealthCheckEvent.Error(it.message)) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setSuspended(check: HealthCheck, suspended: Boolean) {
        if (!canWrite || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { repository.setSuspended(zoneId, check.id, suspended) }
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(checks = sorted(state.checks.map { if (it.id == updated.id) updated else it }))
                    }
                }
                .onFailure { eventChannel.send(HealthCheckEvent.Error(it.message)); load() }
            _uiState.update { it.copy(isMutating = false) }
        }
    }

    fun delete(check: HealthCheck) {
        if (!canWrite || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { repository.delete(zoneId, check.id) }
                .onSuccess {
                    _uiState.update { state -> state.copy(checks = state.checks.filterNot { it.id == check.id }) }
                }
                .onFailure { eventChannel.send(HealthCheckEvent.Error(it.message)) }
            _uiState.update { it.copy(isMutating = false) }
        }
    }
}
