package jiamin.chen.orangecloud.ui.zones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.Zone
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.ZoneRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ZoneListUiState(
    val zones: List<Zone> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

/** 添加域名（新建 Zone）状态：createdZone 非空时表单切到「名称服务器」结果页。 */
data class AddZoneUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdZone: Zone? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ZoneListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val zoneRepository: ZoneRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** 有 zone.write 才展示「添加域名」入口。 */
    val canWrite: Boolean = authRepository.hasScope(Scopes.ZONE_WRITE)

    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow(false)

    private val _addState = MutableStateFlow(AddZoneUiState())
    val addState: StateFlow<AddZoneUiState> = _addState.asStateFlow()

    // 切账号自动重查当前账号的域名缓存
    private val zones: Flow<List<Zone>> = accountStore.selectedAccountId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else zoneRepository.observeZones(id)
    }

    val uiState: StateFlow<ZoneListUiState> =
        combine(zones, loading, error) { zoneList, isLoading, hasError ->
            ZoneListUiState(zones = zoneList, isLoading = isLoading, hasError = hasError)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZoneListUiState(isLoading = true))

    init {
        refresh()
    }

    /** 当前选中账号名（添加域名表单展示用）。 */
    fun currentAccountName(): String? = accountStore.selectedAccount?.name

    /** 新建域名。成功后 createdZone 非空，表单切到名称服务器结果页。 */
    fun createZone(name: String) {
        if (_addState.value.isSaving) return
        viewModelScope.launch {
            _addState.update { it.copy(isSaving = true, error = null) }
            try {
                accountStore.ensureLoaded()
                val id = accountStore.selectedAccountId.value ?: error("no account")
                val zone = zoneRepository.createZone(id, name)
                _addState.update { it.copy(createdZone = zone, isSaving = false) }
            } catch (e: Exception) {
                _addState.update { it.copy(error = e.message ?: "error", isSaving = false) }
            }
        }
    }

    /** 关闭/重置添加域名表单。 */
    fun resetAddState() {
        _addState.value = AddZoneUiState()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = false
            try {
                accountStore.ensureLoaded()
                val id = accountStore.selectedAccountId.value
                if (id == null) {
                    error.value = true
                } else {
                    zoneRepository.refreshZones(id)
                }
            } catch (e: Exception) {
                error.value = true
            } finally {
                loading.value = false
            }
        }
    }
}
