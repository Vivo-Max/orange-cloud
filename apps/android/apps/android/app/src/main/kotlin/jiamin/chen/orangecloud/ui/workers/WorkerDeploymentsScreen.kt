package jiamin.chen.orangecloud.ui.workers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.WorkerDeployment
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
import javax.inject.Inject

data class WorkerDeploymentsUiState(
    val deployments: List<WorkerDeployment> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val canWrite: Boolean = false,
    val busyId: String? = null,
)

sealed interface WorkerDeploymentEvent {
    data object Deleted : WorkerDeploymentEvent
    data class Error(val message: String?) : WorkerDeploymentEvent
}

@HiltViewModel
class WorkerDeploymentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val workerRepository: WorkerRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val scriptName: String = checkNotNull(savedStateHandle["scriptName"])
    private val canWrite = authRepository.hasScope(Scopes.WORKERS_WRITE)

    private val _uiState = MutableStateFlow(WorkerDeploymentsUiState(isLoading = true, canWrite = canWrite))
    val uiState: StateFlow<WorkerDeploymentsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<WorkerDeploymentEvent>(Channel.BUFFERED)
    val events: Flow<WorkerDeploymentEvent> = eventChannel.receiveAsFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val list = workerRepository.listDeployments(accountId, scriptName)
                _uiState.update { it.copy(deployments = list) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasError = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** 删除某次部署。活跃部署 Cloudflare 会拒绝（错误经事件透出）。 */
    fun delete(deployment: WorkerDeployment) {
        if (!canWrite || _uiState.value.busyId != null) return
        _uiState.update { it.copy(busyId = deployment.id) }
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                workerRepository.deleteDeployment(accountId, scriptName, deployment.id)
                _uiState.update { it.copy(deployments = it.deployments.filterNot { d -> d.id == deployment.id }) }
                eventChannel.send(WorkerDeploymentEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(WorkerDeploymentEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(busyId = null) }
            }
        }
    }
}

@Composable
fun WorkerDeploymentsScreen(
    onBack: () -> Unit,
    viewModel: WorkerDeploymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<WorkerDeployment?>(null) }
    val deletedMsg = stringResource(R.string.worker_deployment_deleted)
    val errMsg = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WorkerDeploymentEvent.Deleted -> snackbar.showSnackbar(deletedMsg)
                is WorkerDeploymentEvent.Error -> snackbar.showSnackbar(event.message ?: errMsg)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.worker_deployments_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (!state.isLoading && state.deployments.isEmpty()) {
                    SkyEmptyState(
                        Icons.Outlined.History,
                        stringResource(R.string.worker_deployments_empty),
                        onSky,
                        stringResource(R.string.common_refresh),
                    ) { viewModel.load() }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                stringResource(R.string.worker_deployments_note),
                                color = onSky.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        itemsIndexed(state.deployments, key = { _, d -> d.id }) { index, dep ->
                            DeploymentRow(
                                dep = dep,
                                isActive = index == 0,
                                canWrite = state.canWrite,
                                busy = state.busyId == dep.id,
                                onDelete = { pendingDelete = dep },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }

    pendingDelete?.let { dep ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.worker_deployment_delete_title)) },
            text = { Text(stringResource(R.string.worker_deployment_delete_msg, dep.id.take(8))) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(dep); pendingDelete = null }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun DeploymentRow(
    dep: WorkerDeployment,
    isActive: Boolean,
    canWrite: Boolean,
    busy: Boolean,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Text(
                            stringResource(R.string.worker_deployment_active),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OcOrange,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        dep.message ?: dep.source ?: dep.id.take(8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    dep.id.take(8) + (dep.authorEmail?.let { " · $it" } ?: ""),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = OcOrange)
            } else if (canWrite && !isActive) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = Color(0xFFE5484D))
                }
            }
        }
    }
}
