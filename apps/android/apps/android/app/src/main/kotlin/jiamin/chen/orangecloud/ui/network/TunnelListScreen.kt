package jiamin.chen.orangecloud.ui.network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.Tunnel
import jiamin.chen.orangecloud.ui.storage.StorageListBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunnelListScreen(
    onBack: () -> Unit,
    onOpenTunnel: (id: String, name: String) -> Unit,
    viewModel: TunnelListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Tunnel?>(null) }
    val deletedMsg = stringResource(R.string.dns_deleted)
    val errMsg = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TunnelListEvent.Created -> { showCreate = false; onOpenTunnel(event.tunnel.id, event.tunnel.name) }
                TunnelListEvent.Deleted -> { toDelete = null; snackbarHostState.showSnackbar(deletedMsg) }
                is TunnelListEvent.Error -> snackbarHostState.showSnackbar(event.message ?: errMsg)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.tunnel_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                StorageListBody(state, onSky, Icons.Outlined.Hub, stringResource(R.string.tunnel_empty), { viewModel.load() }) { tunnel ->
                    TunnelRow(
                        tunnel = tunnel,
                        onClick = { onOpenTunnel(tunnel.id, tunnel.name) },
                        onLongClick = if (viewModel.canWrite) ({ toDelete = tunnel }) else null,
                    )
                }
            }
            if (viewModel.canWrite) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.tunnel_new))
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showCreate) {
        TunnelCreateSheet(
            isSaving = isSaving,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onCreate = { viewModel.createTunnel(it) },
            onDismiss = { showCreate = false },
        )
    }
    toDelete?.let { tunnel ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.tunnel_delete_confirm_title)) },
            text = { Text(stringResource(R.string.tunnel_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTunnel(tunnel) }) {
                    Text(stringResource(R.string.tunnel_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TunnelRow(tunnel: Tunnel, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().let {
            if (onLongClick != null) it.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            else it.combinedClickable(onClick = onClick)
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tunnelStatusColor(tunnel.status)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tunnel.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // 连接数来自列表响应内嵌的 connections，CF 将于 2026-10-05 移除该字段。
                // 字段消失后此处只留状态——不为列表每行单发一次连接请求（N+1），
                // 也不能无条件显示 0，否则健康隧道会被误标成「0 个连接」。
                val status = stringResource(tunnelStatusLabel(tunnel.status))
                val connectionCount = tunnel.connections?.size ?: 0
                Text(
                    if (connectionCount > 0) {
                        "$status · ${stringResource(R.string.tunnel_connections, connectionCount)}"
                    } else {
                        status
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TunnelCreateSheet(
    isSaving: Boolean,
    sheetState: androidx.compose.material3.SheetState,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val canSave = name.trim().isNotEmpty() && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.tunnel_new), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.tunnel_new_name)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.tunnel_new_footer), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { onCreate(name.trim()) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.tunnel_create))
            }
        }
    }
}

internal fun tunnelStatusColor(status: String?): Color = when (status) {
    "healthy" -> Color(0xFF2FBF71)
    "degraded" -> Color(0xFFF5A623)
    "down" -> Color(0xFFE5484D)
    else -> Color(0xFF9AA0A6)
}

internal fun tunnelStatusLabel(status: String?): Int = when (status) {
    "healthy" -> R.string.tunnel_status_healthy
    "degraded" -> R.string.tunnel_status_degraded
    "down" -> R.string.tunnel_status_down
    "inactive" -> R.string.tunnel_status_inactive
    else -> R.string.tunnel_status_unknown
}
