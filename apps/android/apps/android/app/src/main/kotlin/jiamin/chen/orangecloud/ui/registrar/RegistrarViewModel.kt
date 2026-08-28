package jiamin.chen.orangecloud.ui.registrar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.DomainRegistration
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.RegistrarRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class RegistrarUiState(
    val registrations: List<DomainRegistration> = emptyList(),
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val isMutating: Boolean = false,
    val missingScope: Boolean = false,
    val canAdmin: Boolean = false,
)

sealed interface RegistrarEvent {
    data class Error(val message: String?) : RegistrarEvent
}

/** 距到期天数；null 表示无到期日。负数表示已过期。 */
fun DomainRegistration.daysUntilExpiry(): Long? = expiresAt?.let {
    runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(it)) }.getOrNull()
}

/** 30 天内到期且未开自动续费——需要用户尽快处理 */
fun DomainRegistration.needsAttention(): Boolean =
    daysUntilExpiry()?.let { it <= 30 && autoRenew != true } ?: false

@HiltViewModel
class RegistrarViewModel @Inject constructor(
    private val repository: RegistrarRepository,
    private val accountStore: AccountStore,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasRead = authRepository.hasScope(Scopes.REGISTRAR_READ)
    private val canAdmin = authRepository.hasScope(Scopes.REGISTRAR_ADMIN)

    private val _uiState = MutableStateFlow(
        RegistrarUiState(isLoading = hasRead, missingScope = !hasRead, canAdmin = canAdmin),
    )
    val uiState: StateFlow<RegistrarUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<RegistrarEvent>(Channel.BUFFERED)
    val events: Flow<RegistrarEvent> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    /** 快到期又没开自动续费的排最前，其余按到期日升序 */
    private fun sorted(items: List<DomainRegistration>) = items.sortedWith(
        compareByDescending<DomainRegistration> { it.needsAttention() }
            .thenBy { it.daysUntilExpiry() ?: Long.MAX_VALUE }
            .thenBy { it.domainName },
    )

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.registrations(accountId)
            }
                .onSuccess { list -> _uiState.update { it.copy(registrations = sorted(list), loaded = true) } }
                .onFailure { eventChannel.send(RegistrarEvent.Error(it.message)) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** 接口是异步 workflow，返回后回读列表拿真实状态 */
    fun setAutoRenew(registration: DomainRegistration, enabled: Boolean) {
        if (!canAdmin || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.setAutoRenew(accountId, registration.domainName, enabled)
            }
                .onSuccess { load() }
                .onFailure { eventChannel.send(RegistrarEvent.Error(it.message)) }
            _uiState.update { it.copy(isMutating = false) }
        }
    }
}
