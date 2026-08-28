package jiamin.chen.orangecloud.ui.redirects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.RedirectList
import jiamin.chen.orangecloud.data.model.RedirectListItem
import jiamin.chen.orangecloud.data.model.RedirectItemInput
import jiamin.chen.orangecloud.data.model.RedirectRule
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.BulkRedirectRepository
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

sealed interface RedirectEvent {
    data object Created : RedirectEvent
    data object Deleted : RedirectEvent
    data class Error(val message: String?) : RedirectEvent
}

// MARK: - 重定向列表（账号级）

@HiltViewModel
class RedirectListsViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: BulkRedirectRepository,
    authRepository: AuthRepository,
) : StorageListViewModel<RedirectList>(
    accountStore,
    authRepository.hasScope(Scopes.REDIRECTS_READ) && authRepository.hasScope(Scopes.RULE_LISTS_READ),
) {
    val canWrite: Boolean = authRepository.hasScope(Scopes.REDIRECTS_WRITE) && authRepository.hasScope(Scopes.RULE_LISTS_WRITE)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val eventChannel = Channel<RedirectEvent>(Channel.BUFFERED)
    val events: Flow<RedirectEvent> = eventChannel.receiveAsFlow()

    override suspend fun fetch(accountId: String): List<RedirectList> = repository.listRedirectLists(accountId)
    init { load() }

    fun create(name: String, description: String?) {
        if (!canWrite || _busy.value) return
        viewModelScope.launch {
            _busy.update { true }
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.createList(accountId, name, description)
                eventChannel.send(RedirectEvent.Created)
                load()
            } catch (e: Exception) {
                eventChannel.send(RedirectEvent.Error(e.message))
            } finally {
                _busy.update { false }
            }
        }
    }

    fun delete(list: RedirectList) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.deleteList(accountId, list.id)
                eventChannel.send(RedirectEvent.Deleted)
                load()
            } catch (e: Exception) {
                eventChannel.send(RedirectEvent.Error(e.message))
            }
        }
    }
}

// MARK: - 列表条目（重定向）

data class RedirectItemsUiState(
    val listName: String = "",
    val items: List<RedirectListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val missingScope: Boolean = false,
    val hasError: Boolean = false,
    val canWrite: Boolean = false,
)

@HiltViewModel
class RedirectItemsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val repository: BulkRedirectRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    private val listName: String = savedStateHandle.get<String>("listName").orEmpty()
    private val hasRead = authRepository.hasScope(Scopes.REDIRECTS_READ) && authRepository.hasScope(Scopes.RULE_LISTS_READ)
    private val canWrite = authRepository.hasScope(Scopes.REDIRECTS_WRITE) && authRepository.hasScope(Scopes.RULE_LISTS_WRITE)

    private val _uiState = MutableStateFlow(
        RedirectItemsUiState(listName = listName, isLoading = hasRead, missingScope = !hasRead, canWrite = canWrite),
    )
    val uiState: StateFlow<RedirectItemsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<RedirectEvent>(Channel.BUFFERED)
    val events: Flow<RedirectEvent> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val items = repository.listItems(accountId, listId)
                _uiState.update { it.copy(items = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** 添加一条重定向并启用列表规则（异步操作完成后刷新）。 */
    fun addRedirect(sourceUrl: String, targetUrl: String, statusCode: Int, preserveQuery: Boolean, subpathMatching: Boolean) {
        if (!canWrite || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val item = RedirectItemInput(
                    RedirectRule(
                        sourceUrl = sourceUrl,
                        targetUrl = targetUrl,
                        statusCode = statusCode,
                        preserveQueryString = preserveQuery,
                        subpathMatching = subpathMatching,
                    ),
                )
                val opId = repository.createItems(accountId, listId, listOf(item))
                repository.waitForOperation(accountId, opId)
                if (listName.isNotBlank()) runCatching { repository.ensureEnabled(accountId, listName) }
                eventChannel.send(RedirectEvent.Created)
                load()
            } catch (e: Exception) {
                eventChannel.send(RedirectEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun delete(item: RedirectListItem) {
        if (!canWrite || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val opId = repository.deleteItems(accountId, listId, listOf(item.id))
                repository.waitForOperation(accountId, opId)
                eventChannel.send(RedirectEvent.Deleted)
                load()
            } catch (e: Exception) {
                eventChannel.send(RedirectEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }
}
