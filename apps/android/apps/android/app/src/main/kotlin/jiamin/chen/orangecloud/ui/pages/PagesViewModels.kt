package jiamin.chen.orangecloud.ui.pages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.core.system.AppPrefs
import jiamin.chen.orangecloud.core.system.ResourceSort
import jiamin.chen.orangecloud.data.model.CreateDnsRecord
import jiamin.chen.orangecloud.data.model.PagesBuildConfig
import jiamin.chen.orangecloud.data.model.PagesDeployment
import jiamin.chen.orangecloud.data.model.PagesDomain
import jiamin.chen.orangecloud.data.model.PagesProject
import jiamin.chen.orangecloud.data.model.PagesProjectUpdate
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.DnsRepository
import jiamin.chen.orangecloud.data.repository.PagesRepository
import jiamin.chen.orangecloud.ui.storage.StorageListViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// MARK: - 项目列表（account 级）

@HiltViewModel
class PagesListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: PagesRepository,
    private val appPrefs: AppPrefs,
    authRepository: AuthRepository,
) : StorageListViewModel<PagesProject>(accountStore, authRepository.hasScope(Scopes.PAGES_READ)) {
    val canWrite: Boolean = authRepository.hasScope(Scopes.PAGES_WRITE)

    /** 列表排序偏好（持久化，与 iOS @AppStorage 对应）。 */
    val sort: StateFlow<ResourceSort> = appPrefs.listSort("pages")
        .stateIn(viewModelScope, SharingStarted.Eagerly, ResourceSort.NAME)

    fun setSort(sort: ResourceSort) {
        viewModelScope.launch { appPrefs.setListSort("pages", sort) }
    }
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = eventChannel.receiveAsFlow()
    override suspend fun fetch(accountId: String): List<PagesProject> = repository.listProjects(accountId)
    init { load() }

    private fun op(block: suspend (String) -> Unit) {
        if (!canWrite || _busy.value) return
        _busy.update { true }
        viewModelScope.launch {
            try {
                val acct = accountStore.selectedAccountId.value ?: error("no account")
                block(acct); load()
            } catch (e: Exception) {
                eventChannel.send(e.message ?: "")
            } finally {
                _busy.update { false }
            }
        }
    }

    fun create(name: String, productionBranch: String) = op { repository.createProject(it, name, productionBranch.ifBlank { "main" }) }
    fun delete(project: PagesProject) = op { repository.deleteProject(it, project.name) }
}

// MARK: - 项目详情 + 部署 + 自定义域名

sealed interface PagesEvent {
    data object Retried : PagesEvent
    data object RolledBack : PagesEvent
    data object DomainAdded : PagesEvent
    data object DomainDeleted : PagesEvent
    data object DeploymentDeleted : PagesEvent
    data object CnameCreated : PagesEvent
    data class Error(val message: String?) : PagesEvent
}

/** 某个自定义域名的 DNS 解析状态（zone 在当前账号内时才可查/可写）。 */
sealed interface PagesDnsState {
    data class Resolved(val content: String) : PagesDnsState    // 已有记录指向本项目
    data class Conflicting(val content: String) : PagesDnsState // 已有解析记录但未指向本项目
    data object Missing : PagesDnsState                          // 可一键添加 CNAME
    data object External : PagesDnsState                         // zone 不在当前账号
    data object Unknown : PagesDnsState                          // 无 dns.read 或查询失败
}

data class PagesDetailUiState(
    val projectName: String = "",
    val project: PagesProject? = null,
    val deployments: List<PagesDeployment> = emptyList(),
    val domains: List<PagesDomain> = emptyList(),
    val dnsStates: Map<String, PagesDnsState> = emptyMap(),   // key = 域名
    val isLoading: Boolean = false,
    val busyDeploymentId: String? = null,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val canWriteDns: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class PagesDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val repository: PagesRepository,
    private val dnsRepository: DnsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val projectName: String = checkNotNull(savedStateHandle["project"])
    private val hasRead = authRepository.hasScope(Scopes.PAGES_READ)
    private val canWrite = authRepository.hasScope(Scopes.PAGES_WRITE)
    private val canReadDns = authRepository.hasScope(Scopes.DNS_READ)
    private val canWriteDns = authRepository.hasScope(Scopes.DNS_WRITE)

    private val _uiState = MutableStateFlow(
        PagesDetailUiState(
            projectName = projectName, isLoading = hasRead,
            missingScope = !hasRead, canWrite = canWrite, canWriteDns = canWriteDns,
        ),
    )
    val uiState: StateFlow<PagesDetailUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<PagesEvent>(Channel.BUFFERED)
    val events: Flow<PagesEvent> = eventChannel.receiveAsFlow()

    init { if (hasRead) load() }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val project = repository.getProject(accountId, projectName)
                val deployments = repository.listDeployments(accountId, projectName)
                val domains = runCatching { repository.listDomains(accountId, projectName) }.getOrDefault(emptyList())
                _uiState.update { it.copy(project = project, deployments = deployments, domains = domains) }
                refreshDnsStates(project, domains)
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** 逐域名查解析记录（zone 不在账号内或无权限时标记为不可查/外部）。 */
    private suspend fun refreshDnsStates(project: PagesProject?, domains: List<PagesDomain>) {
        val target = project?.subdomain ?: return
        val states = mutableMapOf<String, PagesDnsState>()
        for (domain in domains) {
            val zoneTag = domain.zoneTag
            states[domain.name] = when {
                zoneTag.isNullOrEmpty() -> PagesDnsState.External
                !canReadDns -> PagesDnsState.Unknown
                else -> runCatching {
                    val records = dnsRepository.recordsForName(zoneTag, domain.name)
                        .filter { it.type.uppercase() in setOf("CNAME", "A", "AAAA") }
                    val hit = records.firstOrNull { it.content.equals(target, ignoreCase = true) }
                    when {
                        hit != null -> PagesDnsState.Resolved(hit.content)
                        records.isNotEmpty() -> PagesDnsState.Conflicting(records.first().content)
                        else -> PagesDnsState.Missing
                    }
                }.getOrDefault(PagesDnsState.Unknown) // zone 可能属于别的账号（403）或临时失败
            }
        }
        _uiState.update { it.copy(dnsStates = states) }
    }

    // MARK: 自定义域名

    fun addDomain(name: String) = domainOp { accountId ->
        repository.addDomain(accountId, projectName, name)
        eventChannel.send(PagesEvent.DomainAdded)
    }

    fun retryDomain(domain: PagesDomain) = domainOp { accountId ->
        repository.retryDomain(accountId, projectName, domain.name)
        eventChannel.send(PagesEvent.Retried)
    }

    fun deleteDomain(domain: PagesDomain) = domainOp { accountId ->
        repository.deleteDomain(accountId, projectName, domain.name)
        eventChannel.send(PagesEvent.DomainDeleted)
    }

    /** 一键在域名所在 Zone 添加 CNAME → <project>.pages.dev（dns.write）。 */
    fun createCname(domain: PagesDomain) {
        val zoneTag = domain.zoneTag ?: return
        val target = _uiState.value.project?.subdomain ?: return
        if (!canWriteDns || _uiState.value.busyDeploymentId != null) return
        _uiState.update { it.copy(busyDeploymentId = "domain") }
        viewModelScope.launch {
            try {
                dnsRepository.createRecord(
                    zoneTag,
                    CreateDnsRecord(type = "CNAME", name = domain.name, content = target, proxied = true, ttl = 1),
                )
                _uiState.update { it.copy(dnsStates = it.dnsStates + (domain.name to PagesDnsState.Resolved(target))) }
                eventChannel.send(PagesEvent.CnameCreated)
            } catch (e: Exception) {
                eventChannel.send(PagesEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(busyDeploymentId = null) }
            }
        }
    }

    private inline fun domainOp(crossinline action: suspend (String) -> Unit) {
        if (!canWrite || _uiState.value.busyDeploymentId != null) return
        _uiState.update { it.copy(busyDeploymentId = "domain") }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                action(accountId)
                load()
            } catch (e: Exception) {
                eventChannel.send(PagesEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(busyDeploymentId = null) }
            }
        }
    }

    fun retry(deployment: PagesDeployment) = mutate(deployment) { accountId ->
        repository.retryDeployment(accountId, projectName, deployment.id)
        eventChannel.send(PagesEvent.Retried)
    }

    fun rollback(deployment: PagesDeployment) = mutate(deployment) { accountId ->
        repository.rollbackDeployment(accountId, projectName, deployment.id)
        eventChannel.send(PagesEvent.RolledBack)
    }

    fun deleteDeployment(deployment: PagesDeployment) = mutate(deployment) { accountId ->
        repository.deleteDeployment(accountId, projectName, deployment.id)
        _uiState.update { it.copy(deployments = it.deployments.filterNot { d -> d.id == deployment.id }) }
        eventChannel.send(PagesEvent.DeploymentDeleted)
    }

    /** 编辑构建配置（构建命令 / 输出目录 / 根目录）。 */
    fun updateBuildConfig(buildCommand: String, destinationDir: String, rootDir: String) {
        if (!canWrite || _uiState.value.busyDeploymentId != null) return
        _uiState.update { it.copy(busyDeploymentId = "config") }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val cfg = PagesBuildConfig(
                    buildCommand = buildCommand.ifBlank { null },
                    destinationDir = destinationDir.ifBlank { null },
                    rootDir = rootDir.ifBlank { null },
                )
                repository.updateProject(accountId, projectName, PagesProjectUpdate(buildConfig = cfg))
                eventChannel.send(PagesEvent.Retried)
                load()
            } catch (e: Exception) {
                eventChannel.send(PagesEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(busyDeploymentId = null) }
            }
        }
    }

    private inline fun mutate(deployment: PagesDeployment, crossinline action: suspend (String) -> Unit) {
        if (!canWrite || _uiState.value.busyDeploymentId != null) return
        _uiState.update { it.copy(busyDeploymentId = deployment.id) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                action(accountId)
                load()
            } catch (e: Exception) {
                eventChannel.send(PagesEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(busyDeploymentId = null) }
            }
        }
    }
}
