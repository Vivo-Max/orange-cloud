package jiamin.chen.orangecloud.ui.firewall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import jiamin.chen.orangecloud.data.model.FirewallAccessRule

@Composable
fun ZoneAccessRulesScreen(
    onBack: () -> Unit,
    viewModel: ZoneAccessRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)
    val savedMsg = stringResource(R.string.ip_saved)
    val deletedMsg = stringResource(R.string.ip_deleted)
    // null = 列表；Some(rule=null) = 新建；Some(rule) = 编辑
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var pendingDelete by remember { mutableStateOf<FirewallAccessRule?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AccessRuleEvent.Saved -> { editor = null; snackbarHostState.showSnackbar(savedMsg) }
                AccessRuleEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is AccessRuleEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    val ed = editor
    if (ed != null) {
        AccessRuleEditor(
            existing = ed.rule,
            isSaving = state.isSaving,
            onCancel = { editor = null },
            onCreate = { mode, target, value, notes -> viewModel.create(mode, target, value, notes) },
            onUpdate = { id, mode, notes -> viewModel.update(id, mode, notes) },
        )
        return
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.ip_title),
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
                        Icons.Outlined.Block, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }

                    state.loaded && state.rules.isEmpty() -> SkyEmptyState(
                        Icons.Outlined.Block, stringResource(R.string.ip_empty), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.rules, key = { it.id }) { rule ->
                            AccessRuleCard(
                                rule = rule,
                                canWrite = state.canWrite,
                                onEdit = { editor = EditorState(rule) },
                                onDelete = { pendingDelete = rule },
                            )
                        }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(
                    onClick = { editor = EditorState(null) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.ip_add)) }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.ip_delete_title)) },
            text = { Text(rule.configuration?.value ?: rule.id) },
            confirmButton = {
                Text(
                    stringResource(R.string.dns_delete),
                    color = Color(0xFFE5484D),
                    modifier = Modifier.clickable { pendingDelete = null; viewModel.delete(rule) }.padding(8.dp),
                )
            },
            dismissButton = {
                Text(stringResource(R.string.dns_cancel), modifier = Modifier.clickable { pendingDelete = null }.padding(8.dp))
            },
        )
    }
}

private class EditorState(val rule: FirewallAccessRule?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccessRuleEditor(
    existing: FirewallAccessRule?,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onCreate: (AccessRuleMode, AccessRuleTarget, String, String?) -> Unit,
    onUpdate: (String, AccessRuleMode, String?) -> Unit,
) {
    val skyPhase = rememberSkyPhase()
    val onSky = skyPhase.onSky
    var mode by remember { mutableStateOf(AccessRuleMode.fromRaw(existing?.mode)) }
    var target by remember { mutableStateOf(AccessRuleTarget.fromRaw(existing?.configuration?.target)) }
    var value by remember { mutableStateOf(existing?.configuration?.value ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    val isEdit = existing != null
    val valid = isEdit || value.isNotBlank()

    SkyBackground(phase = skyPhase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(if (isEdit) R.string.ip_edit else R.string.ip_new),
                onSky = onSky,
                isLoading = isSaving,
                onRefresh = {
                    if (valid && !isSaving) {
                        if (isEdit) onUpdate(existing!!.id, mode, notes) else onCreate(mode, target, value, notes)
                    }
                },
                onBack = onCancel,
                titleSize = 22,
                backDescription = stringResource(R.string.dns_cancel),
                refreshDescription = stringResource(R.string.dns_save),
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 匹配对象（仅新建可改）
                Text(stringResource(R.string.ip_target), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isEdit) {
                    Text(
                        "${stringResource(target.labelRes)} · ${existing!!.configuration?.value ?: ""}",
                        fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccessRuleTarget.entries.forEach { t ->
                            FilterChip(selected = target == t, onClick = { target = t }, label = { Text(stringResource(t.labelRes)) })
                        }
                    }
                    OutlinedTextField(
                        value = value, onValueChange = { value = it },
                        label = { Text(stringResource(R.string.ip_value)) },
                        placeholder = { Text(target.placeholder) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                }

                Text(stringResource(R.string.ip_action), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccessRuleMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.labelRes)) })
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.ip_notes)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun AccessRuleCard(rule: FirewallAccessRule, canWrite: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(enabled = canWrite, onClick = onEdit)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(accessModeLabel(rule.mode), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(accessTargetLabel(rule.configuration?.target), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                rule.configuration?.value?.let {
                    Text(it, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                rule.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (canWrite) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = Color(0xFFE5484D))
                }
            }
        }
    }
}

@Composable
private fun accessModeLabel(mode: String?): String = when (mode) {
    "block" -> stringResource(R.string.ip_mode_block)
    "managed_challenge" -> stringResource(R.string.ip_mode_managed)
    "js_challenge" -> stringResource(R.string.ip_mode_js)
    "challenge" -> stringResource(R.string.ip_mode_challenge)
    "whitelist" -> stringResource(R.string.ip_mode_allow)
    else -> mode ?: "—"
}

@Composable
private fun accessTargetLabel(target: String?): String = when (target) {
    "ip" -> stringResource(R.string.ip_target_ip)
    "ip6" -> stringResource(R.string.ip_target_ip6)
    "ip_range" -> stringResource(R.string.ip_target_range)
    "asn" -> stringResource(R.string.ip_target_asn)
    "country" -> stringResource(R.string.ip_target_country)
    else -> target ?: "—"
}
