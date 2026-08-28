package jiamin.chen.orangecloud.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.AuditLogEntry
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.AuditLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class AuditLogUiState(
    val entries: List<AuditLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasError: Boolean = false,
    val missingScope: Boolean = false,
    val hasMore: Boolean = false,
    /** 正在查看变更历史的那条日志（null 表示未打开）。 */
    val historyOf: AuditLogEntry? = null,
    val history: List<AuditLogEntry> = emptyList(),
    val isLoadingHistory: Boolean = false,
    /** exact / approximate / unavailable —— 识别质量，approximate 时必须提醒用户可能混入无关条目。 */
    val historyStatus: String? = null,
    val historyError: Boolean = false,
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: AuditLogRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    // 审计日志只需账号必选 scope（account-settings.read），通常恒有。
    private val hasScope = authRepository.hasScope(Scopes.ACCOUNT_READ)
    private var cursor: String? = null

    private val _uiState = MutableStateFlow(
        AuditLogUiState(isLoading = hasScope, missingScope = !hasScope),
    )
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init {
        if (hasScope) loadFirst()
    }

    fun loadFirst() {
        if (!hasScope) return
        cursor = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false, entries = emptyList()) }
            fetchPage(reset = true)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadMore() {
        if (!hasScope || cursor == null || _uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            fetchPage(reset = false)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    /** 打开某条日志对应资源的变更历史（用与列表相同的 30 天窗口）。 */
    fun openHistory(entry: AuditLogEntry) {
        val entryId = entry.id ?: return
        val actionTime = entry.timestampMillis?.let(Instant::ofEpochMilli) ?: return
        _uiState.update {
            it.copy(historyOf = entry, history = emptyList(), historyStatus = null,
                historyError = false, isLoadingHistory = true)
        }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val before = Instant.now()
                val since = before.minus(30, ChronoUnit.DAYS)
                val page = repository.resourceHistory(accountId, entryId, actionTime, since, before)
                _uiState.update {
                    it.copy(
                        history = page.result.orEmpty(),
                        historyStatus = page.resultInfo?.historyStatus,
                        isLoadingHistory = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(historyError = true, isLoadingHistory = false) }
            }
        }
    }

    fun closeHistory() {
        _uiState.update { it.copy(historyOf = null, history = emptyList(), historyStatus = null) }
    }

    private suspend fun fetchPage(reset: Boolean) {
        try {
            accountStore.ensureLoaded()
            val accountId = accountStore.selectedAccountId.value ?: run {
                _uiState.update { it.copy(hasError = true) }
                return
            }
            val before = Instant.now()
            val since = before.minus(30, ChronoUnit.DAYS)
            val page = repository.list(accountId, since, before, cursor)
            cursor = page.cursor
            val entries = page.result.orEmpty()
            _uiState.update {
                it.copy(
                    entries = if (reset) entries else it.entries + entries,
                    hasMore = !page.cursor.isNullOrEmpty() && entries.isNotEmpty(),
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(hasError = true) }
        }
    }
}
