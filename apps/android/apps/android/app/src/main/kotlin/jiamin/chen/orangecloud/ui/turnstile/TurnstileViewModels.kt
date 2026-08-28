package jiamin.chen.orangecloud.ui.turnstile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.TurnstileWidget
import jiamin.chen.orangecloud.data.model.TurnstileWidgetInput
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.TurnstileRepository
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

sealed interface TurnstileListEvent {
    data class Created(val widget: TurnstileWidget) : TurnstileListEvent
    data object Deleted : TurnstileListEvent
    data class Error(val message: String?) : TurnstileListEvent
}

@HiltViewModel
class TurnstileListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: TurnstileRepository,
    authRepository: AuthRepository,
) : StorageListViewModel<TurnstileWidget>(accountStore, authRepository.hasScope(Scopes.CHALLENGE_WIDGETS_READ)) {

    val canWrite: Boolean = authRepository.hasScope(Scopes.CHALLENGE_WIDGETS_WRITE)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val eventChannel = Channel<TurnstileListEvent>(Channel.BUFFERED)
    val events: Flow<TurnstileListEvent> = eventChannel.receiveAsFlow()

    override suspend fun fetch(accountId: String) = repository.listWidgets(accountId)
    init { load() }

    fun create(input: TurnstileWidgetInput) {
        if (!canWrite || _isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val widget = repository.createWidget(accountId, input)
                state.update { it.copy(items = listOf(widget) + it.items) }
                eventChannel.send(TurnstileListEvent.Created(widget))
            } catch (e: Exception) {
                eventChannel.send(TurnstileListEvent.Error(e.message))
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(widget: TurnstileWidget) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.deleteWidget(accountId, widget.sitekey)
                state.update { it.copy(items = it.items.filterNot { w -> w.sitekey == widget.sitekey }) }
                eventChannel.send(TurnstileListEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(TurnstileListEvent.Error(e.message))
            }
        }
    }
}

data class TurnstileDetailUiState(
    val widget: TurnstileWidget? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val canWrite: Boolean = false,
    val error: String? = null,
)

sealed interface TurnstileDetailEvent {
    data object Deleted : TurnstileDetailEvent
    data object Rotated : TurnstileDetailEvent
    data class Error(val message: String?) : TurnstileDetailEvent
}

@HiltViewModel
class TurnstileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val repository: TurnstileRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val sitekey: String = checkNotNull(savedStateHandle["sitekey"])

    private val _uiState = MutableStateFlow(
        TurnstileDetailUiState(canWrite = authRepository.hasScope(Scopes.CHALLENGE_WIDGETS_WRITE))
    )
    val uiState: StateFlow<TurnstileDetailUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TurnstileDetailEvent>(Channel.BUFFERED)
    val events: Flow<TurnstileDetailEvent> = eventChannel.receiveAsFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val accountId = accountId()
                _uiState.update { it.copy(widget = repository.widget(accountId, sitekey)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun update(input: TurnstileWidgetInput, onDone: () -> Unit) {
        if (!_uiState.value.canWrite || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val widget = repository.updateWidget(accountId(), sitekey, input)
                _uiState.update { it.copy(widget = widget) }
                onDone()
            } catch (e: Exception) {
                eventChannel.send(TurnstileDetailEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /** 轮换服务端密钥。immediately = false 时旧密钥保留 2 小时宽限。 */
    fun rotateSecret(immediately: Boolean) {
        if (!_uiState.value.canWrite || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val widget = repository.rotateSecret(accountId(), sitekey, immediately)
                _uiState.update { it.copy(widget = widget) }
                eventChannel.send(TurnstileDetailEvent.Rotated)
            } catch (e: Exception) {
                eventChannel.send(TurnstileDetailEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete() {
        if (!_uiState.value.canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteWidget(accountId(), sitekey)
                eventChannel.send(TurnstileDetailEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(TurnstileDetailEvent.Error(e.message))
            }
        }
    }

    private fun accountId(): String = accountStore.selectedAccountId.value ?: error("no account")
}
