package jiamin.chen.orangecloud.ui.workers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.WorkerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface WorkerCreateEvent {
    data object Created : WorkerCreateEvent
    data class Error(val message: String?) : WorkerCreateEvent
}

/** 原地编辑时读取现有源码的状态。 */
data class WorkerSourceState(
    val isLoading: Boolean = false,
    val code: String? = null,        // 预填源码（就绪后非空）
    val uneditable: Boolean = false, // 多模块打包产物：无法安全原地替换
    val error: String? = null,
)

@HiltViewModel
class WorkerCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val workerRepository: WorkerRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** 非空 = 原地编辑现有脚本；空 = 新建。 */
    val editScript: String? = savedStateHandle.get<String>("editScript")?.takeIf { it.isNotBlank() }
    val isEdit: Boolean = editScript != null

    val canWrite: Boolean = authRepository.hasScope(Scopes.WORKERS_WRITE)
    val defaultCompatibilityDate: String = LocalDate.now().toString() // yyyy-MM-dd

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _sourceState = MutableStateFlow(WorkerSourceState(isLoading = isEdit))
    val sourceState: StateFlow<WorkerSourceState> = _sourceState.asStateFlow()
    private var isModule = true

    private val eventChannel = Channel<WorkerCreateEvent>(Channel.BUFFERED)
    val events: Flow<WorkerCreateEvent> = eventChannel.receiveAsFlow()

    init {
        if (isEdit) loadSource()
    }

    /** 原地编辑：读现有源码预填（/content/v2）。多模块产物置 uneditable。 */
    private fun loadSource() {
        val script = editScript ?: return
        viewModelScope.launch {
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val content = workerRepository.content(accountId, script)
                isModule = content.isModule
                if (!content.isEditable) {
                    _sourceState.update { it.copy(isLoading = false, uneditable = true) }
                } else {
                    _sourceState.update { it.copy(isLoading = false, code = content.mainModule?.body.orEmpty()) }
                }
            } catch (e: Exception) {
                _sourceState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Worker 名：小写字母/数字开头，其后可含小写字母/数字/连字符/下划线（对齐 iOS）。 */
    fun isValidName(name: String): Boolean = name.matches(Regex("^[a-z0-9][a-z0-9_-]*$"))

    /** 提交：编辑模式走整体替换（保留绑定），否则新建。 */
    fun submit(name: String, code: String, compatibilityDate: String) {
        if (isEdit) replace(code) else create(name, code, compatibilityDate)
    }

    private fun create(name: String, code: String, compatibilityDate: String) {
        if (!canWrite || _isUploading.value) return
        if (!isValidName(name) || code.isBlank()) return
        _isUploading.update { true }
        viewModelScope.launch {
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                workerRepository.deployScript(accountId, name, code, compatibilityDate.ifBlank { defaultCompatibilityDate })
                eventChannel.send(WorkerCreateEvent.Created)
            } catch (e: Exception) {
                eventChannel.send(WorkerCreateEvent.Error(e.message))
            } finally {
                _isUploading.update { false }
            }
        }
    }

    private fun replace(code: String) {
        val script = editScript ?: return
        if (!canWrite || _isUploading.value || code.isBlank()) return
        _isUploading.update { true }
        viewModelScope.launch {
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                workerRepository.replaceScript(accountId, script, code, isModule)
                eventChannel.send(WorkerCreateEvent.Created)
            } catch (e: Exception) {
                eventChannel.send(WorkerCreateEvent.Error(e.message))
            } finally {
                _isUploading.update { false }
            }
        }
    }
}
