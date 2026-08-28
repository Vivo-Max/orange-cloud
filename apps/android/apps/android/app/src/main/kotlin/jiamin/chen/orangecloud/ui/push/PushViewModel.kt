package jiamin.chen.orangecloud.ui.push

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.push.DEFAULT_PUSH_SERVER
import jiamin.chen.orangecloud.core.push.PushRepository
import jiamin.chen.orangecloud.data.local.PushMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PushUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val endpoint: String? = null,
    val serverUrl: String = DEFAULT_PUSH_SERVER,
    val working: Boolean = false,
    val failed: Boolean = false,
    val messages: List<PushMessageEntity> = emptyList(),
)

@HiltViewModel
class PushViewModel @Inject constructor(
    private val repository: PushRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PushUiState(available = repository.isFirebaseAvailable))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.enabled,
                repository.serverUrl,
                repository.deviceKey,
                repository.inbox,
            ) { enabled, server, key, msgs ->
                Snapshot(enabled, server, key?.let { server.trimEnd('/') + "/" + it }, msgs)
            }.collect { s ->
                _uiState.update {
                    it.copy(enabled = s.enabled, serverUrl = s.server, endpoint = s.endpoint, messages = s.messages)
                }
            }
        }
    }

    fun enable() {
        if (!_uiState.value.available) {
            _uiState.update { it.copy(failed = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(working = true, failed = false) }
            val r = repository.enable()
            _uiState.update { it.copy(working = false, failed = r.isFailure) }
        }
    }

    fun disable() = viewModelScope.launch { repository.disable() }.let {}

    fun setServer(url: String) = viewModelScope.launch { repository.setServer(url) }.let {}

    fun testPush(title: String, body: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(working = true) }
            repository.testPush(title, body)
            _uiState.update { it.copy(working = false) }
        }
    }

    fun clearInbox() = viewModelScope.launch { repository.clearInbox() }.let {}

    fun deleteMessage(id: Long) = viewModelScope.launch { repository.deleteMessage(id) }.let {}

    fun dismissError() = _uiState.update { it.copy(failed = false) }

    private data class Snapshot(
        val enabled: Boolean,
        val server: String,
        val endpoint: String?,
        val messages: List<PushMessageEntity>,
    )
}
