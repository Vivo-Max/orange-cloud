package jiamin.chen.orangecloud.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jiamin.chen.orangecloud.data.repository.AccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 账号级一次性列表的通用状态（存储各列表共用）。 */
data class StorageListUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val missingScope: Boolean = false,
)

/**
 * 账号级只读列表 VM 基类：权限前置拦截 + 选中账号作用域 + 一次性加载。
 * 子类实现 fetch(accountId)。
 */
abstract class StorageListViewModel<T>(
    private val accountStore: AccountStore,
    private val hasScope: Boolean,
) : ViewModel() {

    protected val state = MutableStateFlow(
        StorageListUiState<T>(isLoading = hasScope, missingScope = !hasScope),
    )
    val uiState: StateFlow<StorageListUiState<T>> = state.asStateFlow()

    protected abstract suspend fun fetch(accountId: String): List<T>

    fun load() {
        if (!hasScope) return
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value
                if (accountId == null) {
                    state.update { it.copy(hasError = true) }
                } else {
                    val items = fetch(accountId)
                    state.update { it.copy(items = items) }
                }
            } catch (e: Exception) {
                state.update { it.copy(hasError = true) }
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }
    }
}
