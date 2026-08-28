package jiamin.chen.orangecloud.ui.builds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.core.push.BuildWatchStore
import jiamin.chen.orangecloud.core.push.BuildWatchWorker
import jiamin.chen.orangecloud.data.model.BuildLogLine
import jiamin.chen.orangecloud.data.model.WorkerBuild
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.WorkerBuildRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkerBuildsUiState(
    val scriptName: String = "",
    val builds: List<WorkerBuild> = emptyList(),
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val isMutating: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val logsBuildUuid: String? = null,
    val logs: List<BuildLogLine> = emptyList(),
    val isLoadingLogs: Boolean = false,
    val watching: Boolean = false,
)

sealed interface WorkerBuildsEvent {
    data class Error(val message: String?) : WorkerBuildsEvent
}

@HiltViewModel
class WorkerBuildsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkerBuildRepository,
    private val accountStore: AccountStore,
    private val watchStore: BuildWatchStore,
    @param:ApplicationContext private val appContext: Context,
    authRepository: AuthRepository,
) : ViewModel() {

    private val scriptName: String = checkNotNull(savedStateHandle["scriptName"])
    private val hasRead = authRepository.hasScope(Scopes.WORKERS_CI_READ)
    private val canWrite = authRepository.hasScope(Scopes.WORKERS_CI_WRITE)

    private val _uiState = MutableStateFlow(
        WorkerBuildsUiState(
            scriptName = scriptName,
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<WorkerBuildsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<WorkerBuildsEvent>(Channel.BUFFERED)
    val events: Flow<WorkerBuildsEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
        viewModelScope.launch {
            _uiState.update { it.copy(watching = watchStore.isWatching(scriptName)) }
        }
    }

    /**
     * 勾选后由 WorkManager 周期比对最新构建。
     * CF 没有 Workers Builds 的告警类型，只能这样；时机由系统调度，可能延迟。
     */
    fun setWatching(on: Boolean) {
        _uiState.update { it.copy(watching = on) }
        viewModelScope.launch {
            watchStore.setWatching(scriptName, on)
            if (watchStore.watchedNow().isEmpty()) {
                BuildWatchWorker.cancel(appContext)
            } else {
                BuildWatchWorker.schedule(appContext)
            }
        }
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                repository.builds(accountId, scriptName)
            }
                // 未接 CI 的 Worker 会 404 —— 视为「没有构建」，不当错误弹
                .onSuccess { list -> _uiState.update { it.copy(builds = list, loaded = true) } }
                .onFailure { _uiState.update { it.copy(builds = emptyList(), loaded = true) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadLogs(build: WorkerBuild) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLogs = true, logsBuildUuid = build.buildUuid, logs = emptyList()) }
            val accountId = accountStore.selectedAccountId.value
            if (accountId != null) {
                runCatching { repository.logs(accountId, build.buildUuid) }
                    .onSuccess { lines -> _uiState.update { it.copy(logs = lines) } }
                    .onFailure { eventChannel.send(WorkerBuildsEvent.Error(it.message)) }
            }
            _uiState.update { it.copy(isLoadingLogs = false) }
        }
    }

    fun closeLogs() = _uiState.update { it.copy(logsBuildUuid = null, logs = emptyList()) }

    fun cancel(build: WorkerBuild) {
        if (!canWrite || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            val accountId = accountStore.selectedAccountId.value
            if (accountId != null) {
                runCatching { repository.cancel(accountId, build.buildUuid) }
                    .onSuccess { load() }
                    .onFailure { eventChannel.send(WorkerBuildsEvent.Error(it.message)) }
            }
            _uiState.update { it.copy(isMutating = false) }
        }
    }
}
