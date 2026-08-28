package jiamin.chen.orangecloud.ui.zonesettings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import jiamin.chen.orangecloud.data.model.SslCertificatePack

// MARK: - SSL/TLS 加密设置

@Composable
fun ZoneSslSettingsScreen(
    onBack: () -> Unit,
    viewModel: ZoneSslViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)
    var modeDialog by remember { mutableStateOf(false) }
    var tlsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errors.collect { msg -> snackbarHostState.showSnackbar(msg ?: genericErr) }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.ssl_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (state.missingScope) {
                    SkyEmptyState(
                        Icons.Outlined.Lock,
                        stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SelectorCard(
                            title = stringResource(R.string.ssl_mode),
                            value = stringResource(state.sslMode.titleRes),
                            subtitle = stringResource(state.sslMode.blurbRes),
                            enabled = state.canWrite && "ssl" !in state.updating,
                            onClick = { modeDialog = true },
                        )
                        SslToggleRow(
                            title = stringResource(R.string.ssl_always_https),
                            subtitle = stringResource(R.string.ssl_always_https_desc),
                            checked = state.alwaysUseHttps,
                            enabled = state.canWrite && "always_use_https" !in state.updating,
                            onChange = viewModel::setAlwaysUseHttps,
                        )
                        SslToggleRow(
                            title = stringResource(R.string.ssl_auto_rewrites),
                            subtitle = stringResource(R.string.ssl_auto_rewrites_desc),
                            checked = state.autoHttpsRewrites,
                            enabled = state.canWrite && "automatic_https_rewrites" !in state.updating,
                            onChange = viewModel::setAutoHttpsRewrites,
                        )
                        SelectorCard(
                            title = stringResource(R.string.ssl_min_tls),
                            value = state.minTls.title,
                            subtitle = null,
                            enabled = state.canWrite && "min_tls_version" !in state.updating,
                            onClick = { tlsDialog = true },
                        )
                        SslToggleRow(
                            title = stringResource(R.string.ssl_tls13),
                            subtitle = stringResource(R.string.ssl_tls13_desc),
                            checked = state.tls13,
                            enabled = state.canWrite && "tls_1_3" !in state.updating,
                            onChange = viewModel::setTls13,
                        )
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (modeDialog) {
        RadioDialog(
            title = stringResource(R.string.ssl_mode),
            options = SslMode.entries,
            selected = state.sslMode,
            label = { stringResource(it.titleRes) },
            sublabel = { stringResource(it.blurbRes) },
            onPick = { viewModel.setSslMode(it); modeDialog = false },
            onDismiss = { modeDialog = false },
        )
    }
    if (tlsDialog) {
        RadioDialog(
            title = stringResource(R.string.ssl_min_tls),
            options = MinTlsVersion.entries,
            selected = state.minTls,
            label = { it.title },
            sublabel = { null },
            onPick = { viewModel.setMinTls(it); tlsDialog = false },
            onDismiss = { tlsDialog = false },
        )
    }
}

// MARK: - SSL 证书

@Composable
fun ZoneSslCertsScreen(
    onBack: () -> Unit,
    viewModel: ZoneSslCertsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)
    val deletedMsg = stringResource(R.string.ssl_cert_deleted)
    var pendingDelete by remember { mutableStateOf<SslCertificatePack?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SslCertsEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is SslCertsEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.ssl_certs_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope -> SkyEmptyState(
                        Icons.Outlined.VerifiedUser,
                        stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }

                    state.loaded && state.packs.isEmpty() -> SkyEmptyState(
                        Icons.Outlined.VerifiedUser,
                        stringResource(R.string.ssl_certs_empty), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.universalLoaded) {
                            item {
                                SslToggleRow(
                                    title = stringResource(R.string.ssl_universal),
                                    subtitle = stringResource(R.string.ssl_universal_desc),
                                    checked = state.universalEnabled,
                                    enabled = state.canWrite && !state.isTogglingUniversal,
                                    onChange = viewModel::setUniversal,
                                )
                            }
                        }
                        items(state.packs, key = { it.id }) { pack ->
                            CertCard(
                                pack = pack,
                                canDelete = state.canWrite && !pack.isUniversal,
                                onDelete = { pendingDelete = pack },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    pendingDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.ssl_cert_delete_title)) },
            text = { Text(stringResource(R.string.ssl_cert_delete_msg)) },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; viewModel.deletePack(pack) }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.dns_cancel)) } },
        )
    }
}

// MARK: - 局部组件

@Composable
private fun SslToggleRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

@Composable
private fun SelectorCard(title: String, value: String, subtitle: String?, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CertCard(pack: SslCertificatePack, canDelete: Boolean, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(certTypeLabel(pack.type), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                val sub = buildString {
                    append(certStatusLabel(pack.status))
                    pack.expiresOnDay?.let { append(" · "); append(stringResource(R.string.ssl_cert_expires, it)) }
                }
                Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                pack.hosts?.takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(", "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = Color(0xFFE5484D))
                }
            }
        }
    }
}

/** 通用单选对话框（SSL 模式 / TLS 版本共用）。 */
@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    sublabel: @Composable (T) -> String?,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { opt ->
                    val sub = sublabel(opt)
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = opt == selected, onClick = { onPick(opt) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = opt == selected, onClick = { onPick(opt) })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label(opt), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (sub != null) {
                                Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) } },
    )
}

@Composable
private fun certTypeLabel(type: String?): String = when (type) {
    "universal" -> stringResource(R.string.ssl_cert_type_universal)
    "advanced" -> stringResource(R.string.ssl_cert_type_advanced)
    "sni_custom", "legacy_custom", "mh_custom", "keyless" -> stringResource(R.string.ssl_cert_type_custom)
    "total_tls" -> "Total TLS"
    else -> type ?: "—"
}

@Composable
private fun certStatusLabel(status: String?): String = when (status) {
    "active" -> stringResource(R.string.ssl_cert_status_active)
    "pending_validation" -> stringResource(R.string.ssl_cert_status_pending)
    "initializing" -> stringResource(R.string.ssl_cert_status_initializing)
    "expired" -> stringResource(R.string.ssl_cert_status_expired)
    else -> status ?: "—"
}
