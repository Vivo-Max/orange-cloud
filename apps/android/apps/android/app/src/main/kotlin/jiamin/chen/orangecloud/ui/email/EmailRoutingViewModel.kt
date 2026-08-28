package jiamin.chen.orangecloud.ui.email

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.EmailDestinationAddress
import jiamin.chen.orangecloud.data.model.EmailRoutingRule
import jiamin.chen.orangecloud.data.model.EmailSuppression
import jiamin.chen.orangecloud.data.model.EmailRoutingRuleInput
import jiamin.chen.orangecloud.data.model.EmailRoutingSettings
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.EmailRoutingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EmailEvent {
    data object Saved : EmailEvent
    data object Deleted : EmailEvent
    data object AddressAdded : EmailEvent
    data class Error(val message: String?) : EmailEvent
}

data class EmailRoutingUiState(
    val zoneName: String = "",
    val settings: EmailRoutingSettings? = null,
    val rules: List<EmailRoutingRule> = emptyList(),
    val addresses: List<EmailDestinationAddress> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    // 抑制列表：独立 scope，读不到不连累主加载
    val suppressions: List<EmailSuppression> = emptyList(),
    val suppressionsAvailable: Boolean = false,
)

@HiltViewModel
class EmailRoutingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmailRoutingRepository,
    private val accountStore: AccountStore,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.EMAIL_RULE_READ)
    private val canWrite = authRepository.hasScope(Scopes.EMAIL_RULE_WRITE)
    private val canReadAddr = authRepository.hasScope(Scopes.EMAIL_ADDR_READ)
    private val canWriteAddr = authRepository.hasScope(Scopes.EMAIL_ADDR_WRITE)

    private val _uiState = MutableStateFlow(
        EmailRoutingUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<EmailRoutingUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EmailEvent>(Channel.BUFFERED)
    val events: Flow<EmailEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val settings = runCatching { repository.settings(zoneId) }.getOrNull()
                val rules = runCatching { repository.rules(zoneId) }.getOrDefault(emptyList())
                val addresses = if (canReadAddr) {
                    accountStore.ensureLoaded()
                    accountStore.selectedAccountId.value?.let { acct ->
                        runCatching { repository.addresses(acct) }.getOrDefault(emptyList())
                    } ?: emptyList()
                } else emptyList()
                // 抑制列表独立 scope，未授权时静默跳过，不连累主加载
                val suppressions = runCatching { repository.suppressions(zoneId) }.getOrNull()
                _uiState.update {
                    it.copy(
                        settings = settings, rules = rules, addresses = addresses,
                        suppressions = suppressions.orEmpty(),
                        suppressionsAvailable = suppressions != null,
                    )
                }
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.setEnabled(zoneId, enabled)
                load()
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            }
        }
    }

    /** 新建转发规则：to=matchAddress → destination。 */
    fun createForwardRule(name: String?, matchAddress: String, destination: String) {
        if (!canWrite || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val input = EmailRoutingRuleInput.forward(name, matchAddress, destination, enabled = true)
                repository.createRule(zoneId, input)
                eventChannel.send(EmailEvent.Saved)
                load()
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteRule(rule: EmailRoutingRule) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteRule(zoneId, rule.id)
                _uiState.update { it.copy(rules = it.rules.filterNot { r -> r.id == rule.id }) }
                eventChannel.send(EmailEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            }
        }
    }

    val canAddAddress: Boolean get() = canWriteAddr

    fun addAddress(email: String) {
        if (!canWriteAddr || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.createAddress(accountId, email)
                eventChannel.send(EmailEvent.AddressAdded)
                load()
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteAddress(address: EmailDestinationAddress) {
        if (!canWriteAddr) return
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.deleteAddress(accountId, address.id)
                _uiState.update { it.copy(addresses = it.addresses.filterNot { a -> a.id == address.id }) }
                eventChannel.send(EmailEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(EmailEvent.Error(e.message))
            }
        }
    }
    /** 解除抑制。成功后本地移除，不必整页重载。 */
    fun removeSuppression(item: EmailSuppression) {
        if (!canWrite) return
        viewModelScope.launch {
            runCatching { repository.deleteSuppression(zoneId, item.id) }
                .onSuccess {
                    _uiState.update { s -> s.copy(suppressions = s.suppressions.filterNot { it.id == item.id }) }
                }
                .onFailure { eventChannel.send(EmailEvent.Error(it.message)) }
        }
    }

}
