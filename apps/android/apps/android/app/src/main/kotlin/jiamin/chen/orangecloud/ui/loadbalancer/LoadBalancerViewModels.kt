package jiamin.chen.orangecloud.ui.loadbalancer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.LoadBalancer
import jiamin.chen.orangecloud.data.model.LoadBalancerUpdate
import jiamin.chen.orangecloud.data.model.Monitor
import jiamin.chen.orangecloud.data.model.MonitorUpdate
import jiamin.chen.orangecloud.data.model.OriginInput
import jiamin.chen.orangecloud.data.model.Pool
import jiamin.chen.orangecloud.data.model.PoolHealthResponse
import jiamin.chen.orangecloud.data.model.PoolUpdate
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.LoadBalancerRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// MARK: - Load Balancer（zone 级，列表 + 启停 + 增删改）

data class ZoneLbUiState(
    val zoneName: String = "",
    val loadBalancers: List<LoadBalancer> = emptyList(),
    val pools: List<Pool> = emptyList(), // 供创建/编辑时选 default_pools
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val hasError: Boolean = false,
    val togglingId: String? = null,
)

@HiltViewModel
class ZoneLoadBalancerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val repository: LoadBalancerRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.LB_READ)
    private val canWrite = authRepository.hasScope(Scopes.LB_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneLbUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneLbUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value
                val lbs = repository.listLoadBalancers(zoneId)
                val pools = if (accountId != null) runCatching { repository.listPools(accountId) }.getOrDefault(emptyList()) else emptyList()
                _uiState.update { it.copy(loadBalancers = lbs, pools = pools) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggle(lb: LoadBalancer, enabled: Boolean) {
        if (!canWrite || _uiState.value.togglingId != null) return
        _uiState.update { it.copy(togglingId = lb.id) }
        viewModelScope.launch {
            try {
                val updated = repository.setLoadBalancerEnabled(zoneId, lb.id, enabled)
                _uiState.update { st -> st.copy(loadBalancers = st.loadBalancers.map { if (it.id == lb.id) updated else it }) }
            } catch (e: Exception) {
                eventChannel.send(e.message ?: ""); load()
            } finally {
                _uiState.update { it.copy(togglingId = null) }
            }
        }
    }

    /** 新建（lbId == null）或编辑负载均衡器。 */
    fun save(lbId: String?, name: String, steering: String, defaultPools: List<String>, fallbackPool: String) {
        if (!canWrite || _uiState.value.isBusy) return
        if (name.isBlank() || defaultPools.isEmpty() || fallbackPool.isBlank()) {
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val body = LoadBalancerUpdate(
                    name = name,
                    steeringPolicy = steering,
                    defaultPools = defaultPools,
                    fallbackPool = fallbackPool,
                    enabled = true,
                )
                if (lbId == null) repository.createLoadBalancer(zoneId, body)
                else repository.updateLoadBalancer(zoneId, lbId, body)
                load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun delete(lb: LoadBalancer) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                repository.deleteLoadBalancer(zoneId, lb.id)
                _uiState.update { st -> st.copy(loadBalancers = st.loadBalancers.filterNot { it.id == lb.id }) }
            } catch (e: Exception) {
                eventChannel.send(e.message ?: ""); load()
            }
        }
    }
}

// MARK: - 源站池 Pool（account 级，只读 + 健康 + 增删改）

data class PoolUiState(
    val pools: List<Pool> = emptyList(),
    val monitors: List<Monitor> = emptyList(), // 供创建/编辑选 monitor
    val healthByPool: Map<String, PoolHealthResponse> = emptyMap(),
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class PoolListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: LoadBalancerRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasRead = authRepository.hasScope(Scopes.LB_POOLS_READ)
    private val canWrite = authRepository.hasScope(Scopes.LB_POOLS_WRITE)

    private val _uiState = MutableStateFlow(PoolUiState(isLoading = hasRead, missingScope = !hasRead, canWrite = canWrite))
    val uiState: StateFlow<PoolUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val accountId = accountStore.run { ensureLoaded(); selectedAccountId.value } ?: error("no account")
                val pools = repository.listPools(accountId)
                val monitors = runCatching { repository.listMonitors(accountId) }.getOrDefault(emptyList())
                _uiState.update { it.copy(pools = pools, monitors = monitors) }
                val health = coroutineScope {
                    pools.map { pool -> async { pool.id to runCatching { repository.poolHealth(accountId, pool.id) }.getOrNull() } }.awaitAll()
                }.mapNotNull { (id, h) -> h?.let { id to it } }.toMap()
                _uiState.update { it.copy(healthByPool = health) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun op(block: suspend (String) -> Unit) {
        if (!canWrite || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val acct = accountStore.selectedAccountId.value ?: error("no account")
                block(acct); load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun save(poolId: String?, name: String, monitor: String?, enabled: Boolean, origins: List<OriginInput>) = op {
        val body = PoolUpdate(name = name, enabled = enabled, monitor = monitor?.ifBlank { null }, origins = origins)
        if (poolId == null) repository.createPool(it, body) else repository.updatePool(it, poolId, body)
    }

    fun delete(pool: Pool) = op { repository.deletePool(it, pool.id) }
}

// MARK: - 健康监测 Monitor（account 级，只读 + 增删改）

data class MonitorUiState(
    val monitors: List<Monitor> = emptyList(),
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class MonitorListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: LoadBalancerRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val hasRead = authRepository.hasScope(Scopes.LB_POOLS_READ)
    private val canWrite = authRepository.hasScope(Scopes.LB_POOLS_WRITE)

    private val _uiState = MutableStateFlow(MonitorUiState(isLoading = hasRead, missingScope = !hasRead, canWrite = canWrite))
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val accountId = accountStore.run { ensureLoaded(); selectedAccountId.value } ?: error("no account")
                _uiState.update { it.copy(monitors = repository.listMonitors(accountId)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun op(block: suspend (String) -> Unit) {
        if (!canWrite || _uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val acct = accountStore.selectedAccountId.value ?: error("no account")
                block(acct); load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun save(
        monitorId: String?, type: String, method: String, path: String, expectedCodes: String,
        interval: Int, timeout: Int, retries: Int, port: Int?,
    ) = op {
        val isHttp = type == "http" || type == "https"
        val body = MonitorUpdate(
            type = type,
            method = if (isHttp) method else null,
            path = if (isHttp) path.ifBlank { "/" } else null,
            expectedCodes = if (isHttp) expectedCodes.ifBlank { "2xx" } else null,
            interval = interval,
            timeout = timeout,
            retries = retries,
            port = if (!isHttp) port else null,
        )
        if (monitorId == null) repository.createMonitor(it, body) else repository.updateMonitor(it, monitorId, body)
    }

    fun delete(monitor: Monitor) = op { repository.deleteMonitor(it, monitor.id) }
}
