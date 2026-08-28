package jiamin.chen.orangecloud.ui.transform

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import jiamin.chen.orangecloud.data.model.HeaderTransform
import jiamin.chen.orangecloud.data.model.RewriteTarget
import jiamin.chen.orangecloud.data.model.TransformActionParameters
import jiamin.chen.orangecloud.data.model.TransformRule
import jiamin.chen.orangecloud.data.model.TransformRuleCreate
import jiamin.chen.orangecloud.data.model.UriRewrite

private data class EditorTarget(val phase: TransformPhase, val rule: TransformRule?)

@Composable
fun ZoneTransformScreen(
    onBack: () -> Unit,
    viewModel: ZoneTransformViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)
    val savedMsg = stringResource(R.string.tf_saved)
    val deletedMsg = stringResource(R.string.tf_deleted)
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<TransformPhase, TransformRule>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TransformEvent.Saved -> { editor = null; snackbarHostState.showSnackbar(savedMsg) }
                TransformEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is TransformEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    val target = editor
    if (target != null) {
        TransformEditor(
            phase = target.phase,
            rule = target.rule,
            isSaving = state.isSaving,
            onCancel = { editor = null },
            onSave = { ruleId, draft -> viewModel.save(target.phase, ruleId, draft) },
        )
        return
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.tf_title),
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
                        Icons.Outlined.SwapHoriz,
                        stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TransformPhase.entries.forEach { ph ->
                            PhaseSection(
                                title = stringResource(ph.titleRes),
                                rules = state.rules(ph),
                                canWrite = state.canWrite,
                                togglingRuleId = state.togglingRuleId,
                                onAdd = { editor = EditorTarget(ph, null) },
                                onToggle = { rule, on -> viewModel.toggle(ph, rule, on) },
                                onEdit = { rule -> editor = EditorTarget(ph, rule) },
                                onDelete = { rule -> pendingDelete = ph to rule },
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    pendingDelete?.let { (ph, rule) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.tf_delete_title)) },
            text = { Text(rule.description?.takeIf { it.isNotBlank() } ?: rule.expression ?: "") },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; viewModel.delete(ph, rule) }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.dns_cancel)) } },
        )
    }
}

@Composable
private fun PhaseSection(
    title: String,
    rules: List<TransformRule>,
    canWrite: Boolean,
    togglingRuleId: String?,
    onAdd: () -> Unit,
    onToggle: (TransformRule, Boolean) -> Unit,
    onEdit: (TransformRule) -> Unit,
    onDelete: (TransformRule) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (canWrite) {
            IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.tf_add)) }
        }
    }
    if (rules.isEmpty()) {
        Text(stringResource(R.string.tf_phase_empty), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    } else {
        rules.forEach { rule ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(enabled = canWrite) { onEdit(rule) }) {
                        Text(
                            rule.description?.takeIf { it.isNotBlank() } ?: stringResource(R.string.tf_unnamed),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                        )
                        rule.expression?.let {
                            Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                    Switch(
                        checked = rule.enabled ?: false,
                        onCheckedChange = { onToggle(rule, it) },
                        enabled = canWrite && togglingRuleId == null,
                    )
                    if (canWrite) {
                        IconButton(onClick = { onDelete(rule) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = Color(0xFFE5484D))
                        }
                    }
                }
            }
        }
    }
}

private class HeaderRow(name: String, op: HeaderOperation, value: String) {
    var name by mutableStateOf(name)
    var op by mutableStateOf(op)
    var value by mutableStateOf(value)
}

@Composable
private fun TransformEditor(
    phase: TransformPhase,
    rule: TransformRule?,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (ruleId: String?, draft: TransformRuleCreate) -> Unit,
) {
    val skyPhase = rememberSkyPhase()
    val onSky = skyPhase.onSky
    var expression by remember { mutableStateOf(rule?.expression ?: "") }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    var pathValue by remember { mutableStateOf(rule?.actionParameters?.uri?.path?.value ?: "") }
    var queryValue by remember { mutableStateOf(rule?.actionParameters?.uri?.query?.value ?: "") }
    val headers = remember {
        mutableStateListOf<HeaderRow>().apply {
            rule?.actionParameters?.headers?.forEach { (name, h) ->
                add(HeaderRow(name, HeaderOperation.fromRaw(h.operation), h.value ?: ""))
            }
        }
    }

    val valid = expression.isNotBlank() && (
        phase.isUrlRewrite || headers.any { it.name.isNotBlank() }
    )

    fun buildDraft(): TransformRuleCreate {
        val params = if (phase.isUrlRewrite) {
            TransformActionParameters(
                uri = UriRewrite(
                    path = pathValue.ifBlank { null }?.let { RewriteTarget(value = it) },
                    query = queryValue.ifBlank { null }?.let { RewriteTarget(value = it) },
                ),
            )
        } else {
            TransformActionParameters(
                headers = headers.filter { it.name.isNotBlank() }.associate { row ->
                    row.name to HeaderTransform(
                        operation = row.op.raw,
                        value = if (row.op == HeaderOperation.REMOVE) null else row.value.ifBlank { null },
                    )
                },
            )
        }
        return TransformRuleCreate(
            action = "rewrite",
            expression = expression.trim(),
            description = description.trim().ifBlank { null },
            enabled = enabled,
            actionParameters = params,
        )
    }

    SkyBackground(phase = skyPhase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(if (rule == null) R.string.tf_new else R.string.tf_edit),
                onSky = onSky,
                isLoading = isSaving,
                onRefresh = { if (valid && !isSaving) onSave(rule?.id, buildDraft()) },
                onBack = onCancel,
                titleSize = 22,
                backDescription = stringResource(R.string.dns_cancel),
                refreshDescription = stringResource(R.string.dns_save),
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(phase.titleRes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = expression, onValueChange = { expression = it },
                    label = { Text(stringResource(R.string.tf_expression)) },
                    placeholder = { Text("http.request.uri.path eq \"/old\"") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.tf_description)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tf_enabled), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (phase.isUrlRewrite) {
                    OutlinedTextField(
                        value = pathValue, onValueChange = { pathValue = it },
                        label = { Text(stringResource(R.string.tf_path)) },
                        placeholder = { Text("/new") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                    OutlinedTextField(
                        value = queryValue, onValueChange = { queryValue = it },
                        label = { Text(stringResource(R.string.tf_query)) },
                        placeholder = { Text("a=1&b=2") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                } else {
                    Text(stringResource(R.string.tf_headers), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    headers.forEachIndexed { idx, row ->
                        HeaderEditorRow(row = row, onRemove = { headers.removeAt(idx) })
                    }
                    OutlinedButton(onClick = { headers.add(HeaderRow("", HeaderOperation.SET, "")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tf_add_header))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderEditorRow(row: HeaderRow, onRemove: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.name, onValueChange = { row.name = it },
                    label = { Text(stringResource(R.string.tf_header_name)) },
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = Color(0xFFE5484D))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeaderOperation.entries.forEach { op ->
                    FilterChip(
                        selected = row.op == op,
                        onClick = { row.op = op },
                        label = { Text(stringResource(op.labelRes)) },
                    )
                }
            }
            if (row.op != HeaderOperation.REMOVE) {
                OutlinedTextField(
                    value = row.value, onValueChange = { row.value = it },
                    label = { Text(stringResource(R.string.tf_header_value)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
        }
    }
}
