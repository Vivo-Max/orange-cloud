package jiamin.chen.orangecloud.ui.snippets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.Snippet
import jiamin.chen.orangecloud.data.repository.SnippetRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// MARK: - 列表

data class SnippetsUiState(
    val zoneName: String = "",
    val snippets: List<Snippet> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
)

@HiltViewModel
class SnippetsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val snippetRepository: SnippetRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.SNIPPETS_READ)
    private val canWrite = authRepository.hasScope(Scopes.SNIPPETS_WRITE)

    private val _uiState = MutableStateFlow(
        SnippetsUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<SnippetsUiState> = _uiState.asStateFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                _uiState.update { it.copy(snippets = snippetRepository.list(zoneId)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

// MARK: - 编辑器

sealed interface SnippetEditEvent {
    data object Saved : SnippetEditEvent
    data object Deleted : SnippetEditEvent
    data class Error(val message: String?) : SnippetEditEvent
}

data class SnippetEditUiState(
    val name: String = "",
    val code: String = "",
    val isNew: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val canWrite: Boolean = false,
)

@HiltViewModel
class SnippetEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val snippetRepository: SnippetRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val initialName: String = savedStateHandle.get<String>("name").orEmpty()
    private val canWrite = authRepository.hasScope(Scopes.SNIPPETS_WRITE)

    private val _uiState = MutableStateFlow(
        SnippetEditUiState(
            name = initialName,
            isNew = initialName.isEmpty(),
            isLoading = initialName.isNotEmpty(),
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<SnippetEditUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SnippetEditEvent>(Channel.BUFFERED)
    val events: Flow<SnippetEditEvent> = eventChannel.receiveAsFlow()

    private val _rules = MutableStateFlow<List<jiamin.chen.orangecloud.data.model.SnippetRule>>(emptyList())
    val rules: StateFlow<List<jiamin.chen.orangecloud.data.model.SnippetRule>> = _rules.asStateFlow()

    init {
        if (initialName.isNotEmpty()) {
            viewModelScope.launch {
                val code = runCatching { snippetRepository.content(zoneId, initialName) }.getOrNull().orEmpty()
                _uiState.update { it.copy(code = code, isLoading = false) }
            }
            viewModelScope.launch {
                val all = runCatching { snippetRepository.rules(zoneId) }.getOrNull().orEmpty()
                _rules.value = all.filter { it.snippetName == initialName }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateCode(code: String) = _uiState.update { it.copy(code = code) }

    fun save() {
        val s = _uiState.value
        if (!canWrite || s.name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                snippetRepository.put(zoneId, s.name.trim(), s.code)
                eventChannel.send(SnippetEditEvent.Saved)
            } catch (e: Exception) {
                eventChannel.send(SnippetEditEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete() {
        val s = _uiState.value
        if (!canWrite || s.isNew) return
        viewModelScope.launch {
            try {
                snippetRepository.delete(zoneId, s.name)
                eventChannel.send(SnippetEditEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(SnippetEditEvent.Error(e.message))
            }
        }
    }
}
