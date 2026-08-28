package jiamin.chen.orangecloud.ui.managedheaders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.ManagedTransform
import jiamin.chen.orangecloud.data.model.ManagedTransforms
import jiamin.chen.orangecloud.data.repository.ManagedHeaderRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagedHeadersUiState(
    val requestHeaders: List<ManagedTransform> = emptyList(),
    val responseHeaders: List<ManagedTransform> = emptyList(),
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val isMutating: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
)

sealed interface ManagedHeadersEvent {
    data class Error(val message: String?) : ManagedHeadersEvent
}

@HiltViewModel
class ManagedHeadersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ManagedHeaderRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.MANAGED_HEADERS_READ)
    private val canWrite = authRepository.hasScope(Scopes.MANAGED_HEADERS_WRITE)

    private val _uiState = MutableStateFlow(
        ManagedHeadersUiState(isLoading = hasRead, missingScope = !hasRead, canWrite = canWrite),
    )
    val uiState: StateFlow<ManagedHeadersUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ManagedHeadersEvent>(Channel.BUFFERED)
    val events: Flow<ManagedHeadersEvent> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    /** 接口不保证顺序，按 id 排一下，避免每次刷新行位置跳动 */
    private fun apply(t: ManagedTransforms) = _uiState.update {
        it.copy(
            requestHeaders = t.requestHeaders.orEmpty().sortedBy { h -> h.id },
            responseHeaders = t.responseHeaders.orEmpty().sortedBy { h -> h.id },
            loaded = true,
        )
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { repository.transforms(zoneId) }
                .onSuccess { apply(it) }
                .onFailure { eventChannel.send(ManagedHeadersEvent.Error(it.message)) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setEnabled(item: ManagedTransform, enabled: Boolean, isRequest: Boolean) {
        if (!canWrite || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { repository.setEnabled(zoneId, item.id, enabled, isRequest) }
                .onSuccess { apply(it) }
                .onFailure { eventChannel.send(ManagedHeadersEvent.Error(it.message)); load() }
            _uiState.update { it.copy(isMutating = false) }
        }
    }
}
