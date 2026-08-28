package jiamin.chen.orangecloud.ui.healthcheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.data.model.HealthCheck

/**
 * 独立健康检查：源站是否在线。与负载均衡的监控器不同源，别混用。
 * 移动端定位是「看状态 + 临时干预」，创建检查字段多、属桌面场景，暂不提供。
 */
@Composable
fun HealthCheckScreen(
    onBack: () -> Unit,
    viewModel: HealthCheckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<HealthCheck?>(null) }
    val genericErr = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HealthCheckEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.hc_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope -> Box(Modifier.fillMaxSize()) {
                        SkyEmptyState(
                            Icons.Outlined.MonitorHeart,
                            stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                        ) { viewModel.load() }
                    }
                    state.loaded && state.checks.isEmpty() -> Box(Modifier.fillMaxSize()) {
                        SkyEmptyState(
                            Icons.Outlined.MonitorHeart,
                            stringResource(R.string.hc_empty), onSky, stringResource(R.string.common_refresh),
                        ) { viewModel.load() }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.alertAvailable) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                stringResource(R.string.hc_notify_me),
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Switch(
                                                checked = state.alertEnabled,
                                                onCheckedChange = viewModel::setAlertEnabled,
                                                enabled = state.canWriteAlerts,
                                            )
                                        }
                                        Text(
                                            stringResource(R.string.hc_notify_hint),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        items(state.checks, key = { it.id }) { check ->
                            HealthCheckCard(
                                check = check,
                                canWrite = state.canWrite,
                                enabled = !state.isMutating,
                                onToggleSuspend = { viewModel.setSuspended(check, check.suspended != true) },
                                onDelete = { pendingDelete = check },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    pendingDelete?.let { check ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.hc_delete_title)) },
            text = { Text(stringResource(R.string.hc_delete_message, check.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(check); pendingDelete = null }) {
                    Text(stringResource(R.string.dns_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.dns_cancel)) }
            },
        )
    }
}

@Composable
private fun HealthCheckCard(
    check: HealthCheck,
    canWrite: Boolean,
    enabled: Boolean,
    onToggleSuspend: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    check.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(statusLabel(check.displayStatus)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor(check.displayStatus),
                )
            }
            check.address?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // 只在真的异常时显示原因，避免正常态也占一行
            if (check.displayStatus == "unhealthy" && !check.failureReason.isNullOrBlank()) {
                Text(check.failureReason, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Row {
                check.type?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                }
                check.interval?.let {
                    Text(
                        stringResource(R.string.hc_interval_seconds, it),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canWrite) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleSuspend, enabled = enabled) {
                        Text(
                            stringResource(
                                if (check.suspended == true) R.string.hc_resume else R.string.hc_suspend,
                            ),
                        )
                    }
                    TextButton(onClick = onDelete, enabled = enabled) {
                        Text(stringResource(R.string.dns_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String) = when (status) {
    "healthy" -> R.string.hc_status_healthy
    "unhealthy" -> R.string.hc_status_unhealthy
    "suspended" -> R.string.hc_status_suspended
    else -> R.string.hc_status_unknown
}

private fun statusColor(status: String) = when (status) {
    "healthy" -> Color(0xFF2E7D32)
    "unhealthy" -> Color(0xFFC62828)
    "suspended" -> Color(0xFF757575)
    else -> Color(0xFFEF6C00)
}
