package jiamin.chen.orangecloud.ui.builds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import jiamin.chen.orangecloud.data.model.BuildDisplayState
import jiamin.chen.orangecloud.data.model.WorkerBuild

/** Worker 的 CI 构建记录：状态、分支/commit、日志、取消。 */
@Composable
fun WorkerBuildsScreen(
    onBack: () -> Unit,
    viewModel: WorkerBuildsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkerBuildsEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.builds_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope || (state.loaded && state.builds.isEmpty()) ->
                        Box(Modifier.fillMaxSize()) {
                            SkyEmptyState(
                                Icons.Outlined.Build,
                                stringResource(
                                    if (state.missingScope) R.string.scope_missing else R.string.builds_empty,
                                ),
                                onSky,
                                stringResource(R.string.common_refresh),
                            ) { viewModel.load() }
                        }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stringResource(R.string.builds_notify_me),
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Switch(
                                            checked = state.watching,
                                            onCheckedChange = viewModel::setWatching,
                                        )
                                    }
                                    // 说清是尽力而为，别让用户以为是实时推送
                                    Text(
                                        stringResource(R.string.builds_notify_hint),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        items(state.builds, key = { it.buildUuid }) { build ->
                            BuildCard(
                                build = build,
                                canWrite = state.canWrite,
                                enabled = !state.isMutating,
                                onOpenLogs = { viewModel.loadLogs(build) },
                                onCancel = { viewModel.cancel(build) },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (state.logsBuildUuid != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeLogs() },
            title = { Text(stringResource(R.string.builds_logs)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    when {
                        state.isLoadingLogs -> Text(stringResource(R.string.common_loading), fontSize = 13.sp)
                        state.logs.isEmpty() -> Text(stringResource(R.string.builds_no_logs), fontSize = 13.sp)
                        else -> state.logs.take(200).forEach { line ->
                            Text(
                                line.line.orEmpty(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeLogs() }) { Text(stringResource(R.string.common_done)) }
            },
        )
    }
}

@Composable
private fun BuildCard(
    build: WorkerBuild,
    canWrite: Boolean,
    enabled: Boolean,
    onOpenLogs: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = stateColor(build.displayState), shape = CircleShape) {
                    Box(Modifier.size(8.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(stateLabel(build.displayState)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            build.triggerMetadata?.let { meta ->
                Row {
                    meta.branch?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                    }
                    meta.shortCommit?.let {
                        Text(
                            it,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                meta.author?.let {
                    Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenLogs) { Text(stringResource(R.string.builds_logs)) }
                // 只有进行中的构建才谈得上取消
                if (canWrite && build.displayState.isRunning) {
                    TextButton(onClick = onCancel, enabled = enabled) {
                        Text(stringResource(R.string.builds_cancel), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun stateLabel(state: BuildDisplayState) = when (state) {
    BuildDisplayState.QUEUED -> R.string.builds_state_queued
    BuildDisplayState.RUNNING -> R.string.builds_state_running
    BuildDisplayState.SUCCESS -> R.string.builds_state_success
    BuildDisplayState.FAILED -> R.string.builds_state_failed
    BuildDisplayState.CANCELLED -> R.string.builds_state_cancelled
    BuildDisplayState.SKIPPED -> R.string.builds_state_skipped
    BuildDisplayState.UNKNOWN -> R.string.hc_status_unknown
}

private fun stateColor(state: BuildDisplayState) = when (state) {
    BuildDisplayState.SUCCESS -> Color(0xFF2E7D32)
    BuildDisplayState.FAILED -> Color(0xFFC62828)
    BuildDisplayState.QUEUED, BuildDisplayState.RUNNING -> Color(0xFFEF6C00)
    else -> Color(0xFF757575)
}
