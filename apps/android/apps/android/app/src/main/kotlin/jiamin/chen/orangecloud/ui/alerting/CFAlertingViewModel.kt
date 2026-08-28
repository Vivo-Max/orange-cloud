package jiamin.chen.orangecloud.ui.alerting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.core.push.PushRepository
import jiamin.chen.orangecloud.data.model.CFAlertPolicy
import jiamin.chen.orangecloud.data.model.CFAvailableAlert
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.AlertingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertGroup(val category: String, val alerts: List<CFAvailableAlert>)

data class AlertingUiState(
    val isLoading: Boolean = false,
    val missingScope: Boolean = false,
    val noEndpoint: Boolean = false,
    val error: Boolean = false,
    val groups: List<AlertGroup> = emptyList(),
    val enabledTypes: Set<String> = emptySet(),
    val busyType: String? = null,
)

@HiltViewModel
class CFAlertingViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: AlertingRepository,
    private val pushRepository: PushRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasScope = authRepository.hasScope(Scopes.NOTIFICATIONS_READ)
    private var endpoint: String? = null
    private var pushWebhookId: String? = null
    private var accountId: String? = null

    private val _uiState = MutableStateFlow(AlertingUiState(missingScope = !hasScope, isLoading = hasScope))
    val uiState = _uiState.asStateFlow()

    init {
        if (hasScope) load()
    }

    fun load() {
        if (!hasScope) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false, noEndpoint = false) }
            val ep = pushRepository.endpoint()
            if (ep == null) {
                _uiState.update { it.copy(isLoading = false, noEndpoint = true) }
                return@launch
            }
            endpoint = ep
            try {
                accountStore.ensureLoaded()
                val acc = accountStore.selectedAccountId.value ?: run {
                    _uiState.update { it.copy(isLoading = false, error = true) }
                    return@launch
                }
                accountId = acc
                val available = repository.availableAlerts(acc)
                val hooks = repository.webhooks(acc)
                pushWebhookId = hooks.firstOrNull { it.url == cfWebhookUrl() }?.id
                val policies = repository.policies(acc)
                val groups = available.entries.map { AlertGroup(it.key, it.value) }.sortedBy { it.category }
                _uiState.update {
                    it.copy(isLoading = false, groups = groups, enabledTypes = enabledTypesOf(policies))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun toggle(alert: CFAvailableAlert) {
        val acc = accountId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(busyType = alert.type, error = false) }
            try {
                val current = repository.policies(acc)
                val existing = current.firstOrNull { p ->
                    p.alertType == alert.type && p.mechanisms?.webhooks?.any { it.id == pushWebhookId } == true
                }
                if (existing != null) {
                    repository.deletePolicy(acc, existing.id)
                } else {
                    val webhookId = pushWebhookId
                        ?: repository.createWebhook(acc, "Orange Cloud Push", cfWebhookUrl()).also { pushWebhookId = it }
                    repository.createPolicy(acc, "OC: ${alert.label}", alert.type, webhookId)
                }
                val updated = repository.policies(acc)
                _uiState.update { it.copy(busyType = null, enabledTypes = enabledTypesOf(updated)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(busyType = null, error = true) }
            }
        }
    }

    private fun cfWebhookUrl(): String = "$endpoint/cf"

    private fun enabledTypesOf(policies: List<CFAlertPolicy>): Set<String> {
        val webhookId = pushWebhookId ?: return emptySet()
        return policies
            .filter { p -> p.mechanisms?.webhooks?.any { it.id == webhookId } == true }
            .mapNotNull { it.alertType }
            .toSet()
    }
}
