package jiamin.chen.orangecloud.ui.tracer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.FlatTraceStep
import jiamin.chen.orangecloud.data.model.flattened
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.RequestTracerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TracerUiState(
    val url: String = "",
    val method: String = "GET",
    val statusCode: Int? = null,
    val steps: List<FlatTraceStep> = emptyList(),
    val hasResult: Boolean = false,
    val isTracing: Boolean = false,
    val missingScope: Boolean = false,
) {
    /** 只做最基本的形态校验，具体是否可达交给接口判断 */
    val canTrace: Boolean get() = url.trim().isNotEmpty() && url.contains("://") && !isTracing
}

sealed interface TracerEvent {
    data class Error(val message: String?) : TracerEvent
}

@HiltViewModel
class RequestTracerViewModel @Inject constructor(
    private val repository: RequestTracerRepository,
    private val accountStore: AccountStore,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasRead = authRepository.hasScope(Scopes.REQUEST_TRACER_READ)

    private val _uiState = MutableStateFlow(TracerUiState(missingScope = !hasRead))
    val uiState: StateFlow<TracerUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TracerEvent>(Channel.BUFFERED)
    val events: Flow<TracerEvent> = eventChannel.receiveAsFlow()

    companion object {
        val METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")
    }

    fun setUrl(value: String) = _uiState.update { it.copy(url = value) }

    fun setMethod(value: String) = _uiState.update { it.copy(method = value) }

    fun run() {
        val state = _uiState.value
        if (!hasRead || !state.canTrace) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTracing = true) }
            runCatching {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.trace(accountId, state.method, state.url.trim())
            }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            statusCode = result.statusCode,
                            steps = result.trace.orEmpty().flattened(),
                            hasResult = true,
                        )
                    }
                }
                .onFailure {
                    eventChannel.send(TracerEvent.Error(it.message))
                    _uiState.update { s -> s.copy(steps = emptyList(), hasResult = false) }
                }
            _uiState.update { it.copy(isTracing = false) }
        }
    }
}
